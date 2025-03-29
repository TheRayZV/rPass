package therayzv.rPass.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import therayzv.rPass.RPass;
import therayzv.rPass.utils.MessageUtil;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

public class PassCheckListener implements Listener {
    
    private final RPass plugin;
    private BukkitTask checkTask;
    private final ConcurrentHashMap<UUID, Long> warningTimes = new ConcurrentHashMap<>();
    
    public PassCheckListener(RPass plugin) {
        this.plugin = plugin;
        startCheckTask();
    }
    
    /**
     * Запускает задачу проверки проходок
     */
    public void startCheckTask() {
        if (checkTask != null) {
            checkTask.cancel();
        }
        
        int checkInterval = plugin.getConfig().getInt("kick.check-interval", 60);
        
        checkTask = new BukkitRunnable() {
            @Override
            public void run() {
                checkOnlinePlayers();
            }
        }.runTaskTimer(plugin, 20L * 20, 20L * checkInterval); // Начинаем проверку через 20 секунд после старта
    }
    
    /**
     * Останавливает задачу проверки проходок
     */
    public void stopCheckTask() {
        if (checkTask != null) {
            checkTask.cancel();
            checkTask = null;
        }
    }
    
    /**
     * Проверяет игрока на наличие проходки при логине
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) {
            return; // Игрок уже не может войти по другим причинам
        }
        
        Player player = event.getPlayer();
        
        // Не проверяем администраторов, но проверяем напрямую, а не через кэшированные разрешения
        if (player.isOp()) {
            plugin.getLogger().info("Игрок " + player.getName() + " является оператором (проверено в onPlayerLogin), пропускаем проверку");
            return;
        }
        
        // Простая проверка без сложной логики, чтобы быстрее обработать вход
        if (!plugin.getPassManager().hasPass(player)) {
            // Проверка на право администратора для быстрого входа
            if (player.hasPermission("rpass.admin")) {
                plugin.getLogger().info("Игрок " + player.getName() + " имеет права администратора, пропускаем проверку проходки");
                return;
            }
            
            // Получаем многострочное сообщение из конфигурации
            List<String> messageLines = plugin.getConfig().getStringList("kick.no-pass-message");
            String kickMessage;
            
            if (!messageLines.isEmpty()) {
                // Формируем сообщение с переносами строк
                StringBuilder sb = new StringBuilder();
                for (String line : messageLines) {
                    if (sb.length() > 0) {
                        sb.append("\n");
                    }
                    sb.append(MessageUtil.colorize(line));
                }
                kickMessage = sb.toString();
            } else {
                // Запасной вариант, если список строк пуст
                kickMessage = MessageUtil.colorize("&cУ вас нет проходки на сервер!");
            }
            
            // Используем метод disallow для отказа во входе
            event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, Component.text(kickMessage));
            
            // Логируем событие
            plugin.getLogger().info("Игрок " + player.getName() + " не смог войти: нет проходки");
        }
    }
    
    /**
     * Проверяет всех онлайн игроков на наличие проходки
     */
    private void checkOnlinePlayers() {
        long currentTime = System.currentTimeMillis();
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Актуальная проверка статуса оператора
            if (player.isOp()) {
                continue;
            }
            
            // Отложенная проверка разрешений
            if (player.hasPermission("rpass.admin")) {
                continue;
            }
            
            // Проверяем проходку напрямую
            if (!plugin.getPassManager().hasPass(player)) {
                UUID playerUUID = player.getUniqueId();
                
                // Отправляем предупреждение только раз в минуту
                Long lastWarning = warningTimes.get(playerUUID);
                if (lastWarning == null || currentTime - lastWarning > 60000) { // 60 секунд
                    MessageUtil messageUtil = plugin.getMessageUtil();
                    messageUtil.sendTitle(player, "kick.title", "kick.subtitle");
                    warningTimes.put(playerUUID, currentTime);
                    
                    // Кикаем через 10 секунд после предупреждения
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (player.isOnline() && !player.isOp() && !player.hasPermission("rpass.admin") && !plugin.getPassManager().hasPass(player)) {
                                player.kick(createExpiredKickMessage());
                                warningTimes.remove(playerUUID);
                            }
                        }
                    }.runTaskLater(plugin, 20L * 10);
                }
            }
        }
    }
    
    /**
     * Отслеживает присоединение игрока и проверяет его проходку
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Актуальная проверка статуса оператора
        if (player.isOp()) {
            plugin.getLogger().info("Игрок " + player.getName() + " является оператором (проверено в onPlayerJoin)");
            return;
        }
        
        // Запускаем большую часть проверок асинхронно
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // Проверка, что игрок все еще онлайн
            if (!player.isOnline()) {
                return;
            }
            
            // Основная проверка проходки
            boolean hasPass = plugin.getPassManager().hasPass(player);
            boolean hasAdminPerm = player.hasPermission("rpass.admin");
            
            // Если есть проходка или права админа, нет смысла делать дальнейшие проверки
            if (hasPass || hasAdminPerm) {
                if (hasAdminPerm) {
                    plugin.getLogger().info("Игрок " + player.getName() + " имеет права администратора (проверено асинхронно)");
                }
                return;
            }
            
            // Если игрок не имеет проходки, возвращаемся в основной поток для показа сообщения и кика
            Bukkit.getScheduler().runTask(plugin, () -> {
                // Проверяем, что игрок все еще онлайн и все еще не имеет прав/проходки
                if (!player.isOnline() || player.isOp() || player.hasPermission("rpass.admin") || plugin.getPassManager().hasPass(player)) {
                    return;
                }
                
                // Показываем предупреждение
                MessageUtil messageUtil = plugin.getMessageUtil();
                messageUtil.sendTitle(player, "kick.title", "kick.subtitle");
                
                // Кикаем через 5 секунд
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (player.isOnline() && !player.isOp() && !player.hasPermission("rpass.admin") && !plugin.getPassManager().hasPass(player)) {
                            player.kick(createExpiredKickMessage());
                        }
                    }
                }.runTaskLater(plugin, 20L * 5);
            });
        });
    }
    
    /**
     * Формирует компонент сообщения для кика при истечении проходки
     */
    private Component createExpiredKickMessage() {
        List<String> messageLines = plugin.getConfig().getStringList("kick.expired-message");
        if (!messageLines.isEmpty()) {
            // Формируем сообщение с переносами строк
            StringBuilder sb = new StringBuilder();
            for (String line : messageLines) {
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append(MessageUtil.colorize(line));
            }
            return Component.text(sb.toString());
        } else {
            // Запасной вариант, если список строк пуст
            return Component.text(MessageUtil.colorize("&cВаша проходка истекла! Обратитесь к администрации для продления."));
        }
    }
} 