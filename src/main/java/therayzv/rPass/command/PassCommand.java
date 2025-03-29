package therayzv.rPass.command;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import therayzv.rPass.RPass;
import therayzv.rPass.utils.PassManager;
import therayzv.rPass.utils.TimeFormatter;
import therayzv.rPass.utils.MessageUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PassCommand implements CommandExecutor, TabCompleter {
    
    private final RPass plugin;
    
    public PassCommand(RPass plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            plugin.getMessageUtil().sendMessage(sender, "help-message");
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "give":
                return handleGiveCommand(sender, args);
            case "take":
                return handleTakeCommand(sender, args);
            case "extend": // Перенаправляем extend на give для обратной совместимости
                return handleGiveCommand(sender, args);
            case "extendall":
                return handleExtendAllCommand(sender, args);
            case "info":
                return handleInfoCommand(sender, args);
            case "list":
                return handleListCommand(sender);
            case "reload":
                return handleReloadCommand(sender);
            case "debug":
                return handleDebugCommand(sender);
            default:
                plugin.getMessageUtil().sendMessage(sender, "unknown-command");
                return false;
        }
    }
    
    /**
     * Обрабатывает команду выдачи проходки
     */
    private boolean handleGiveCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpass.give") && !sender.hasPermission("rpass.admin")) {
            plugin.getMessageUtil().sendMessage(sender, "no-permission");
            return false;
        }
        
        if (args.length < 3) {
            plugin.getMessageUtil().sendMessage(sender, "help-message");
            return false;
        }
        
        String playerName = args[1];
        String timeString = args[2];
        
        OfflinePlayer target = getOfflinePlayer(playerName);
        if (target == null) {
            plugin.getMessageUtil().sendMessage(sender, "player-not-found");
            return false;
        }
        
        long durationMillis = TimeFormatter.parseTime(timeString);
        
        // Используем новый универсальный метод givePass
        int result = plugin.getPassManager().givePass(target, durationMillis);
        
        switch (result) {
            case 0: // Новая проходка выдана
                if (durationMillis == PassManager.PERMANENT_PASS) {
                    plugin.getMessageUtil().sendMessage(sender, "pass-given-permanent", "player", target.getName());
                } else {
                    plugin.getMessageUtil().sendMessage(sender, "pass-given", "player", target.getName(), "time", TimeFormatter.formatTime(durationMillis));
                }
                break;
                
            case 1: // Проходка продлена
                if (durationMillis == PassManager.PERMANENT_PASS) {
                    plugin.getMessageUtil().sendMessage(sender, "pass-made-permanent", "player", target.getName());
                } else {
                    plugin.getMessageUtil().sendMessage(sender, "pass-extended", "player", target.getName(), "time", TimeFormatter.formatTime(durationMillis));
                }
                break;
                
            case 2: // Проходка уже вечная
                plugin.getMessageUtil().sendMessage(sender, "pass-already-permanent", "player", target.getName());
                break;
        }
        
        return true;
    }
    
    /**
     * Обрабатывает команду удаления проходки
     */
    private boolean handleTakeCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpass.take") && !sender.hasPermission("rpass.admin")) {
            plugin.getMessageUtil().sendMessage(sender, "no-permission");
            return false;
        }
        
        if (args.length < 2) {
            plugin.getMessageUtil().sendMessage(sender, "help-message");
            return false;
        }
        
        String playerName = args[1];
        OfflinePlayer target = getOfflinePlayer(playerName);
        
        if (target == null) {
            plugin.getMessageUtil().sendMessage(sender, "player-not-found");
            return false;
        }
        
        boolean success = plugin.getPassManager().removePass(target);
        if (success) {
            plugin.getMessageUtil().sendMessage(sender, "pass-taken", "player", target.getName());
        } else {
            plugin.getMessageUtil().sendMessage(sender, "pass-not-has", "player", target.getName());
        }
        
        return true;
    }
    
    /**
     * Обрабатывает команду продления проходки всем игрокам
     * 
     * Эта команда:
     * - Продлевает только существующие проходки
     * - Пропускает игроков с вечными проходками
     * - Для истекших проходок отсчитывает время от текущего момента
     * - Для активных проходок добавляет время к существующему сроку
     */
    private boolean handleExtendAllCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpass.extendall") && !sender.hasPermission("rpass.admin")) {
            plugin.getMessageUtil().sendMessage(sender, "no-permission");
            return false;
        }
        
        if (args.length < 2) {
            plugin.getMessageUtil().sendMessage(sender, "help-message");
            return false;
        }
        
        String timeString = args[1];
        long durationMillis = TimeFormatter.parseTime(timeString);
        
        // Получаем количество продлённых проходок
        int count = plugin.getPassManager().extendAllPasses(durationMillis);
        
        if (count == 0) {
            sender.sendMessage("§cНет проходок для продления.");
            return true;
        }
        
        if (durationMillis == PassManager.PERMANENT_PASS) {
            plugin.getMessageUtil().sendMessage(sender, "pass-all-made-permanent", "count", String.valueOf(count));
        } else {
            plugin.getMessageUtil().sendMessage(sender, "pass-all-extended", 
                    "time", TimeFormatter.formatTime(durationMillis),
                    "count", String.valueOf(count));
        }
        
        return true;
    }
    
    /**
     * Обрабатывает команду информации о проходке
     */
    private boolean handleInfoCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpass.info") && !sender.hasPermission("rpass.admin")) {
            plugin.getMessageUtil().sendMessage(sender, "no-permission");
            return false;
        }
        
        OfflinePlayer target;
        
        if (args.length < 2) {
            if (!(sender instanceof Player)) {
                plugin.getMessageUtil().sendMessage(sender, "help-message");
                return false;
            }
            target = (Player) sender;
        } else {
            String playerName = args[1];
            target = getOfflinePlayer(playerName);
            
            if (target == null) {
                plugin.getMessageUtil().sendMessage(sender, "player-not-found");
                return false;
            }
        }
        
        PassManager passManager = plugin.getPassManager();
        long timeLeft = passManager.getPassTimeLeft(target);
        
        if (timeLeft == PassManager.PERMANENT_PASS) {
            plugin.getMessageUtil().sendMessage(sender, "pass-info-permanent", "player", target.getName());
        } else if (timeLeft > 0) {
            // Получаем текущее время и вычисляем дату окончания
            long currentTime = System.currentTimeMillis();
            long expiryTime = currentTime + timeLeft;
            
            // Получаем отформатированное время и дату
            String formattedTime = TimeFormatter.formatTime(timeLeft);
            String expiryDate = plugin.getMessageUtil().formatDate(expiryTime);
            
            // Получаем цвет, соответствующий оставшемуся времени
            String timeColor = plugin.getMessageUtil().getTimeColor(timeLeft);
            String colorizedTime = MessageUtil.colorize(timeColor + formattedTime);
            
            // Отправляем сообщение с временем и датой
            plugin.getMessageUtil().sendMessage(sender, "pass-info", 
                    "time", colorizedTime,
                    "date", expiryDate,
                    "player", target.getName());
        } else {
            plugin.getMessageUtil().sendMessage(sender, "pass-not-has", "player", target.getName());
        }
        
        return true;
    }
    
    /**
     * Обрабатывает команду отображения всех проходок
     */
    private boolean handleListCommand(CommandSender sender) {
        if (!sender.hasPermission("rpass.list") && !sender.hasPermission("rpass.admin")) {
            plugin.getMessageUtil().sendMessage(sender, "no-permission");
            return false;
        }
        
        PassManager passManager = plugin.getPassManager();
        Map<String, Long> allPasses = passManager.getAllPasses();
        
        if (allPasses.isEmpty()) {
            plugin.getMessageUtil().sendMessage(sender, "pass-list-empty");
            return true;
        }
        
        plugin.getMessageUtil().sendMessage(sender, "pass-list-header");
        
        long currentTime = System.currentTimeMillis();
        int count = 0;
        int permanentCount = 0;
        int expiredCount = 0;
        int validCount = 0;
        
        for (Map.Entry<String, Long> entry : allPasses.entrySet()) {
            String playerName = entry.getKey();
            long expiryTime = entry.getValue();
            count++;
            
            if (expiryTime == PassManager.PERMANENT_PASS) {
                String permanentColor = plugin.getMessageUtil().getTimeColor(PassManager.PERMANENT_PASS);
                sender.sendMessage("§a" + playerName + " §7- " + permanentColor + "Навсегда");
                permanentCount++;
            } else if (expiryTime <= currentTime) {
                String expiredColor = plugin.getMessageUtil().getTimeColor(0);
                sender.sendMessage("§c" + playerName + " §7- " + expiredColor + "Истек");
                expiredCount++;
            } else {
                long timeLeft = expiryTime - currentTime;
                String timeColor = plugin.getMessageUtil().getTimeColor(timeLeft);
                String formattedTime = TimeFormatter.formatTime(timeLeft);
                sender.sendMessage("§a" + playerName + " §7- " + timeColor + formattedTime);
                validCount++;
            }
        }
        
        plugin.getMessageUtil().sendMessage(sender, "pass-list-footer", 
                "total", String.valueOf(count),
                "valid", String.valueOf(validCount),
                "permanent", String.valueOf(permanentCount),
                "expired", String.valueOf(expiredCount));
        
        return true;
    }
    
    /**
     * Обрабатывает команду перезагрузки плагина
     */
    private boolean handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("rpass.reload") && !sender.hasPermission("rpass.admin")) {
            plugin.getMessageUtil().sendMessage(sender, "no-permission");
            return false;
        }
        
        plugin.reloadConfig();
        plugin.getPassManager().loadPasses();
        
        plugin.getMessageUtil().sendMessage(sender, "config-reloaded");
        return true;
    }
    
    /**
     * Обрабатывает команду отладки
     */
    private boolean handleDebugCommand(CommandSender sender) {
        if (!sender.hasPermission("rpass.admin")) {
            plugin.getMessageUtil().sendMessage(sender, "no-permission");
            return false;
        }
        
        sender.sendMessage("§7----------- §cDebug Info §7-----------");
        
        // Проверяем статус OP для каждого игрока (если онлайн)
        for (Player player : Bukkit.getOnlinePlayers()) {
            boolean isOp = player.isOp();
            boolean hasAdminPerm = player.hasPermission("rpass.admin");
            String playerName = player.getName();
            
            sender.sendMessage("§7Игрок: §f" + playerName);
            sender.sendMessage("§7  OP: §f" + isOp);
            sender.sendMessage("§7  rpass.admin: §f" + hasAdminPerm);
            
            String playerKey = player.getName().toLowerCase();
            long expiryTime = -2;
            if (plugin.getPassManager().getAllPasses().containsKey(playerKey)) {
                expiryTime = plugin.getPassManager().getAllPasses().get(playerKey);
            }
            
            if (expiryTime == PassManager.PERMANENT_PASS) {
                sender.sendMessage("§7  Хранится в файле: §aДа (Навсегда)");
            } else if (expiryTime > 0) {
                long timeLeft = expiryTime - System.currentTimeMillis();
                sender.sendMessage("§7  Хранится в файле: §aДа (" + TimeFormatter.formatTime(timeLeft) + ")");
                sender.sendMessage("§7  Timestamp: §f" + expiryTime);
                
                // Добавляем дату в читаемом формате для помощи в отладке
                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                String formattedDate = sdf.format(new Date(expiryTime));
                sender.sendMessage("§7  Дата окончания: §f" + formattedDate);
            } else {
                sender.sendMessage("§7  Хранится в файле: §cНет");
            }
            
            boolean hasPass = plugin.getPassManager().hasPass(player);
            long timeLeft = plugin.getPassManager().getPassTimeLeft(player);
            sender.sendMessage("§7  hasPass(): §f" + hasPass);
            
            if (timeLeft == PassManager.PERMANENT_PASS) {
                sender.sendMessage("§7  getPassTimeLeft(): §fНавсегда");
            } else if (timeLeft > 0) {
                sender.sendMessage("§7  getPassTimeLeft(): §f" + TimeFormatter.formatTime(timeLeft));
            } else {
                sender.sendMessage("§7  getPassTimeLeft(): §f" + timeLeft);
            }
            
            sender.sendMessage("§7--------------------------------");
        }
        
        // Отображаем сырое содержимое файла проходок
        File passFile = new File(plugin.getDataFolder(), "pass.dat");
        if (passFile.exists()) {
            sender.sendMessage("§7Содержимое файла pass.dat:");
            
            try (BufferedReader reader = new BufferedReader(new FileReader(passFile))) {
                String line;
                int lineNumber = 1;
                while ((line = reader.readLine()) != null) {
                    sender.sendMessage("§7" + lineNumber + ": §f" + line);
                    lineNumber++;
                }
            } catch (IOException e) {
                sender.sendMessage("§cОшибка при чтении файла: " + e.getMessage());
            }
        } else {
            sender.sendMessage("§cФайл pass.dat не существует!");
        }
        
        return true;
    }
    
    /**
     * Получает оффлайн игрока по имени
     */
    private OfflinePlayer getOfflinePlayer(String name) {
        Player onlinePlayer = Bukkit.getPlayer(name);
        if (onlinePlayer != null) {
            return onlinePlayer;
        }
        
        try {
            UUID uuid = UUID.fromString(name);
            return Bukkit.getOfflinePlayer(uuid);
        } catch (IllegalArgumentException ignored) {
            // Не UUID, попробуем найти по имени
        }
        
        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            if (offlinePlayer.getName() != null && offlinePlayer.getName().equalsIgnoreCase(name)) {
                return offlinePlayer;
            }
        }
        
        return null;
    }
    
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            List<String> commands = new ArrayList<>();
            
            if (sender.hasPermission("rpass.give") || sender.hasPermission("rpass.admin")) {
                commands.add("give");
                commands.add("extend"); // Сохраняем для обратной совместимости
            }
            
            if (sender.hasPermission("rpass.take") || sender.hasPermission("rpass.admin")) {
                commands.add("take");
            }
            
            if (sender.hasPermission("rpass.extendall") || sender.hasPermission("rpass.admin")) {
                commands.add("extendall");
            }
            
            if (sender.hasPermission("rpass.info") || sender.hasPermission("rpass.admin")) {
                commands.add("info");
            }
            
            if (sender.hasPermission("rpass.list") || sender.hasPermission("rpass.admin")) {
                commands.add("list");
            }
            
            if (sender.hasPermission("rpass.reload") || sender.hasPermission("rpass.admin")) {
                commands.add("reload");
            }
            
            if (sender.hasPermission("rpass.debug") || sender.hasPermission("rpass.admin")) {
                commands.add("debug");
            }
            
            return commands.stream()
                    .filter(cmd -> cmd.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            if (Arrays.asList("give", "take", "info", "extend").contains(subCommand)) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            
            if (Arrays.asList("give", "extend", "extendall").contains(subCommand)) {
                List<String> timeExamples = Arrays.asList("1d", "7d", "30d", "1h", "12h", "30m", "permanent", "навсегда");
                return timeExamples.stream()
                        .filter(time -> time.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        
        return completions;
    }
} 