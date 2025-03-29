package therayzv.rPass.utils;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PassManager {
    private final File passFile;
    private final Map<String, Long> playerPasses; // Теперь храним ники вместо UUID
    private final Plugin plugin;
    
    // Константа для вечной проходки: -1 означает "бессрочно"
    public static final long PERMANENT_PASS = -1L;
    
    public PassManager(File dataFolder, Plugin plugin) {
        this.passFile = new File(dataFolder, "pass.dat");
        this.playerPasses = new ConcurrentHashMap<>();
        this.plugin = plugin;
        loadPasses();
    }
    
    /**
     * Загружает все проходки из файла
     */
    public void loadPasses() {
        playerPasses.clear();
        if (!passFile.exists()) {
            try {
                passFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        
        // Выполняем чтение асинхронно, затем обновляем данные в основном потоке
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<String, Long> tempPasses = new HashMap<>();
            
            try (BufferedReader reader = new BufferedReader(new FileReader(passFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Поддержка как старого формата (UUID:time), так и нового (nickname-time)
                    String[] parts;
                    if (line.contains(":")) {
                        parts = line.split(":");
                        // Конвертируем старый формат в новый, получая ник по UUID
                        try {
                            UUID uuid = UUID.fromString(parts[0]);
                            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                            String playerName = player.getName();
                            if (playerName != null && !playerName.isEmpty()) {
                                parts[0] = playerName;
                            } else {
                                plugin.getLogger().warning("Невозможно получить ник для UUID: " + uuid);
                                continue;
                            }
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("Неверный UUID в файле проходок: " + parts[0]);
                            continue;
                        }
                    } else if (line.contains("-")) {
                        parts = line.split("-");
                    } else {
                        plugin.getLogger().warning("Неверный формат строки проходки: " + line);
                        continue;
                    }
                    
                    if (parts.length != 2) {
                        plugin.getLogger().warning("Неверное количество частей в строке проходки: " + line);
                        continue;
                    }
                    
                    try {
                        String playerName = parts[0];
                        long expiryTime;
                        
                        // Обработка строковых значений для вечных проходок
                        if (parts[1].equalsIgnoreCase("permanent") || 
                            parts[1].equalsIgnoreCase("forever") || 
                            parts[1].equalsIgnoreCase("навсегда") ||
                            parts[1].equalsIgnoreCase("вечно")) {
                            expiryTime = PERMANENT_PASS;
                        } else {
                            expiryTime = Long.parseLong(parts[1]);
                        }
                        
                        // Проверяем, не истекла ли проходка
                        if (expiryTime != PERMANENT_PASS && expiryTime <= System.currentTimeMillis()) {
                            plugin.getLogger().info("Пропущена истекшая проходка для " + playerName);
                            continue;
                        }
                        
                        tempPasses.put(playerName.toLowerCase(), expiryTime);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Ошибка при чтении времени проходки: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            // Синхронизируем с основным потоком для обновления данных
            Bukkit.getScheduler().runTask(plugin, () -> {
                playerPasses.putAll(tempPasses);
                plugin.getLogger().info("Загружено " + tempPasses.size() + " проходок");
            });
        });
    }
    
    /**
     * Сохраняет все проходки в файл
     */
    public void savePasses() {
        // Создаем копию данных для безопасного использования в асинхронном потоке
        Map<String, Long> passesCopy = new HashMap<>(playerPasses);
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(passFile))) {
                for (Map.Entry<String, Long> entry : passesCopy.entrySet()) {
                    String saveValue;
                    if (entry.getValue() == PERMANENT_PASS) {
                        saveValue = "permanent";
                    } else {
                        saveValue = String.valueOf(entry.getValue());
                    }
                    writer.write(entry.getKey() + "-" + saveValue);
                    writer.newLine();
                }
                plugin.getLogger().info("Сохранено " + passesCopy.size() + " проходок");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Получает ключ игрока для карты проходок
     */
    private String getPlayerKey(OfflinePlayer player) {
        return player.getName() != null ? player.getName().toLowerCase() : player.getUniqueId().toString();
    }
    
    /**
     * Выдает или продлевает проходку игроку
     * @param player игрок
     * @param durationMillis продолжительность проходки в миллисекундах, -1 для бессрочной проходки
     * @return 0 - новая проходка выдана, 1 - проходка продлена, 2 - проходка уже вечная
     */
    public int givePass(OfflinePlayer player, long durationMillis) {
        String playerKey = getPlayerKey(player);
        
        // Сначала проверяем есть ли у игрока проходка
        if (playerPasses.containsKey(playerKey)) {
            long currentExpiry = playerPasses.get(playerKey);
            
            // Если проходка уже вечная, ничего не делаем
            if (currentExpiry == PERMANENT_PASS) {
                return 2; // Проходка уже вечная
            }
            
            // Если запрошена вечная проходка, устанавливаем ее
            if (durationMillis == PERMANENT_PASS) {
                playerPasses.put(playerKey, PERMANENT_PASS);
                plugin.getLogger().info("Проходка игрока " + playerKey + " стала вечной");
                savePasses();
                return 1; // Проходка продлена до вечной
            }
            
            // Получаем текущее время
            long currentTime = System.currentTimeMillis();
            
            // Проверяем, не истекла ли проходка
            if (currentExpiry <= currentTime) {
                // Если проходка истекла, устанавливаем новую от текущего момента
                long newExpiry = currentTime + durationMillis;
                playerPasses.put(playerKey, newExpiry);
                plugin.getLogger().info("Истекшая проходка игрока " + playerKey + " обновлена на " + 
                    TimeFormatter.formatTime(durationMillis) + " (с текущего момента)");
            } else {
                // Если проходка активна, добавляем время к существующему сроку
                long newExpiry = currentExpiry + durationMillis;
                playerPasses.put(playerKey, newExpiry);
                plugin.getLogger().info("Проходка игрока " + playerKey + " продлена на " + 
                    TimeFormatter.formatTime(durationMillis) + " (добавлено к существующему сроку)");
            }
            
            savePasses();
            return 1; // Проходка продлена
        }
        
        // Игрок не имеет проходки, выдаем новую
        long expiryTime = durationMillis == PERMANENT_PASS ? 
                PERMANENT_PASS : System.currentTimeMillis() + durationMillis;
        playerPasses.put(playerKey, expiryTime);
        savePasses();
        
        if (durationMillis == PERMANENT_PASS) {
            plugin.getLogger().info("Игроку " + playerKey + " выдана вечная проходка");
        } else {
            plugin.getLogger().info("Игроку " + playerKey + " выдана проходка на " + 
                TimeFormatter.formatTime(durationMillis));
        }
        
        return 0; // Новая проходка выдана
    }
    
    /**
     * Выдает игроку вечную проходку
     * @param player игрок
     * @return true если проходка успешно выдана, false если у игрока уже есть проходка
     */
    public boolean givePermanentPass(OfflinePlayer player) {
        return givePass(player, PERMANENT_PASS) == 0;
    }
    
    /**
     * Продлевает проходку всем игрокам
     * @param durationMillis продолжительность продления в миллисекундах
     * @return количество игроков, у которых была продлена проходка
     */
    public int extendAllPasses(long durationMillis) {
        int count = 0;
        
        // Создаем копию ключей для безопасной итерации
        List<String> playerKeys = new ArrayList<>(playerPasses.keySet());
        
        for (String playerKey : playerKeys) {
            long currentExpiry = playerPasses.get(playerKey);
            
            // Если у игрока вечная проходка, оставляем как есть
            if (currentExpiry == PERMANENT_PASS) {
                continue;
            }
            
            // Получаем OfflinePlayer по имени
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerKey);
            
            // Используем универсальный метод givePass для продления
            int result = givePass(player, durationMillis);
            
            // Если проходка действительно была продлена (не была вечной)
            if (result == 1) {
                count++;
            }
        }
        
        if (count > 0) {
            plugin.getLogger().info("Проходки продлены для " + count + " игроков на " + 
                  TimeFormatter.formatTime(durationMillis));
        }
        
        return count;
    }
    
    /**
     * Удаляет проходку у игрока
     * @param player игрок
     * @return true если проходка успешно удалена, false если у игрока нет проходки
     */
    public boolean removePass(OfflinePlayer player) {
        String playerKey = getPlayerKey(player);
        if (!playerPasses.containsKey(playerKey)) {
            return false;
        }
        
        playerPasses.remove(playerKey);
        savePasses();
        return true;
    }
    
    /**
     * Проверяет наличие проходки у игрока
     * @param player игрок
     * @return true если у игрока есть действующая проходка, иначе false
     */
    public boolean hasPass(OfflinePlayer player) {
        // Выполняем дополнительную проверку, является ли игрок оператором (онлайн-проверка)
        if (player instanceof Player && ((Player) player).isOp()) {
            plugin.getLogger().info("Игрок " + player.getName() + " является оператором (проверено в hasPass)");
            return true;
        }
        
        String playerKey = getPlayerKey(player);
        if (!playerPasses.containsKey(playerKey)) {
            return false;
        }
        
        // Если проходка вечная, она всегда действительна
        long expiryTime = playerPasses.get(playerKey);
        if (expiryTime == PERMANENT_PASS) {
            return true;
        }
        
        boolean valid = expiryTime > System.currentTimeMillis();
        if (!valid) {
            // Сразу удаляем просроченную проходку
            playerPasses.remove(playerKey);
            savePasses();
        }
        
        return valid;
    }
    
    /**
     * Возвращает время до конца проходки игрока
     * @param player игрок
     * @return время до конца проходки в миллисекундах, -1 для вечной проходки или если проходки нет
     */
    public long getPassTimeLeft(OfflinePlayer player) {
        // Если игрок оператор, у него вечная проходка
        if (player instanceof Player && ((Player) player).isOp()) {
            plugin.getLogger().info("Игрок " + player.getName() + " является оператором (проверено в getPassTimeLeft)");
            return PERMANENT_PASS;
        }
        
        String playerKey = getPlayerKey(player);
        if (!playerPasses.containsKey(playerKey)) {
            return -1;
        }
        
        long expiryTime = playerPasses.get(playerKey);
        
        // Если проходка вечная, возвращаем константу PERMANENT_PASS
        if (expiryTime == PERMANENT_PASS) {
            return PERMANENT_PASS;
        }
        
        long currentTime = System.currentTimeMillis();
        
        if (expiryTime <= currentTime) {
            playerPasses.remove(playerKey);
            savePasses();
            return -1;
        }
        
        return expiryTime - currentTime;
    }
    
    /**
     * Проверяет, является ли проходка игрока вечной
     * @param player игрок
     * @return true если проходка вечная, false если нет или проходки нет
     */
    public boolean isPermanent(OfflinePlayer player) {
        // Если игрок оператор, у него вечная проходка
        if (player instanceof Player && ((Player) player).isOp()) {
            plugin.getLogger().info("Игрок " + player.getName() + " является оператором (проверено в isPermanent)");
            return true;
        }
        
        String playerKey = getPlayerKey(player);
        if (!playerPasses.containsKey(playerKey)) {
            return false;
        }
        
        return playerPasses.get(playerKey) == PERMANENT_PASS;
    }
    
    /**
     * Удаляет просроченные проходки
     * @return количество удаленных проходок
     */
    public int cleanExpiredPasses() {
        int count = 0;
        long currentTime = System.currentTimeMillis();
        Iterator<Map.Entry<String, Long>> iterator = playerPasses.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            // Пропускаем вечные проходки
            if (entry.getValue() == PERMANENT_PASS) {
                continue;
            }
            
            if (entry.getValue() <= currentTime) {
                iterator.remove();
                count++;
            }
        }
        
        if (count > 0) {
            savePasses();
        }
        
        return count;
    }
    
    /**
     * Проверяет онлайн игроков на наличие проходки
     * @return список игроков без проходки
     */
    public List<Player> checkOnlinePlayers() {
        List<Player> playersWithoutPass = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Проверка на оператора (реалтайм проверка)
            if (player.isOp()) {
                plugin.getLogger().info("Игрок " + player.getName() + " является оператором (проверено в checkOnlinePlayers)");
                continue;
            }
            
            // Проверка на права (может кэшироваться)
            if (player.hasPermission("rpass.admin")) {
                continue;
            }
            
            if (!hasPass(player)) {
                playersWithoutPass.add(player);
            }
        }
        return playersWithoutPass;
    }
    
    /**
     * Устанавливает проходку игроку напрямую (для ручного редактирования)
     * @param playerName имя игрока
     * @param expiryTime время истечения проходки
     */
    public void setPassDirectly(String playerName, long expiryTime) {
        playerPasses.put(playerName.toLowerCase(), expiryTime);
        savePasses();
    }
    
    /**
     * Возвращает все проходки
     * @return карта проходок (имя игрока -> время истечения)
     */
    public Map<String, Long> getAllPasses() {
        return new HashMap<>(playerPasses);
    }
} 