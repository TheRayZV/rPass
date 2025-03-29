package therayzv.rPass.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import therayzv.rPass.RPass;
import therayzv.rPass.utils.TimeFormatter;
import therayzv.rPass.utils.PassManager;
import therayzv.rPass.utils.MessageUtil;

public class PassPlaceholder extends PlaceholderExpansion {

    private final RPass plugin;
    private String expiredText;
    private String permanentText;
    private String noPassText;

    public PassPlaceholder(RPass plugin) {
        this.plugin = plugin;
        reloadConfig();
    }
    
    /**
     * Перезагружает конфигурацию плейсхолдеров
     */
    public void reloadConfig() {
        FileConfiguration config = plugin.getConfig();
        this.expiredText = MessageUtil.colorize(config.getString("placeholders.expired-text", "&cИстекла"));
        this.permanentText = MessageUtil.colorize(config.getString("placeholders.permanent-text", "Навсегда"));
        this.noPassText = MessageUtil.colorize(config.getString("placeholders.no-pass-text", "Нет проходки"));
    }

    @Override
    public @NotNull String getIdentifier() {
        return "rpass";
    }

    @Override
    public @NotNull String getAuthor() {
        return "therayzv";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String identifier) {
        if (player == null) {
            return "";
        }

        // %rpass_has% - имеет ли игрок проходку
        if (identifier.equals("has")) {
            return plugin.getPassManager().hasPass(player) ? "true" : "false";
        }

        // %rpass_time% - время до окончания проходки
        if (identifier.equals("time")) {
            long timeLeft = plugin.getPassManager().getPassTimeLeft(player);
            if (timeLeft <= 0 && timeLeft != PassManager.PERMANENT_PASS) {
                return noPassText;
            }
            if (timeLeft == PassManager.PERMANENT_PASS) {
                return permanentText;
            }
            return TimeFormatter.formatTime(timeLeft);
        }
        
        // %rpass_time_compact% - компактное отображение времени (например, 7д 12ч 30м)
        if (identifier.equals("time_compact")) {
            long timeLeft = plugin.getPassManager().getPassTimeLeft(player);
            if (timeLeft <= 0 && timeLeft != PassManager.PERMANENT_PASS) {
                return noPassText;
            }
            if (timeLeft == PassManager.PERMANENT_PASS) {
                return permanentText;
            }
            return formatCompactTime(timeLeft);
        }
        
        // %rpass_days% - только количество дней до окончания проходки
        if (identifier.equals("days")) {
            long timeLeft = plugin.getPassManager().getPassTimeLeft(player);
            if (timeLeft <= 0 && timeLeft != PassManager.PERMANENT_PASS) {
                return "0";
            }
            if (timeLeft == PassManager.PERMANENT_PASS) {
                return "∞";
            }
            return String.valueOf(timeLeft / (1000 * 60 * 60 * 24));
        }
        
        // %rpass_hours% - только количество часов до окончания проходки
        if (identifier.equals("hours")) {
            long timeLeft = plugin.getPassManager().getPassTimeLeft(player);
            if (timeLeft <= 0 && timeLeft != PassManager.PERMANENT_PASS) {
                return "0";
            }
            if (timeLeft == PassManager.PERMANENT_PASS) {
                return "∞";
            }
            return String.valueOf(timeLeft / (1000 * 60 * 60));
        }
        
        // %rpass_minutes% - только количество минут до окончания проходки
        if (identifier.equals("minutes")) {
            long timeLeft = plugin.getPassManager().getPassTimeLeft(player);
            if (timeLeft <= 0 && timeLeft != PassManager.PERMANENT_PASS) {
                return "0";
            }
            if (timeLeft == PassManager.PERMANENT_PASS) {
                return "∞";
            }
            return String.valueOf(timeLeft / (1000 * 60));
        }
        
        // %rpass_seconds% - только количество секунд до окончания проходки
        if (identifier.equals("seconds")) {
            long timeLeft = plugin.getPassManager().getPassTimeLeft(player);
            if (timeLeft <= 0 && timeLeft != PassManager.PERMANENT_PASS) {
                return "0";
            }
            if (timeLeft == PassManager.PERMANENT_PASS) {
                return "∞";
            }
            return String.valueOf(timeLeft / 1000);
        }
        
        // %rpass_permanent% - является ли проходка вечной
        if (identifier.equals("permanent")) {
            return plugin.getPassManager().isPermanent(player) ? "true" : "false";
        }
        
        // %rpass_status% - статус проходки (Активна/Истекла/Нет проходки)
        if (identifier.equals("status")) {
            long timeLeft = plugin.getPassManager().getPassTimeLeft(player);
            if (timeLeft == PassManager.PERMANENT_PASS) {
                return permanentText;
            } else if (timeLeft > 0) {
                return "&aАктивна";
            } else if (plugin.getPassManager().getAllPasses().containsKey(player.getName().toLowerCase())) {
                return expiredText;
            } else {
                return noPassText;
            }
        }

        return null;
    }
    
    /**
     * Форматирует время в компактном виде (например, 7д 12ч 30м)
     * @param millis время в миллисекундах
     * @return отформатированная строка времени
     */
    private String formatCompactTime(long millis) {
        if (millis == PassManager.PERMANENT_PASS) {
            return permanentText;
        }
        
        if (millis <= 0) {
            return "0с";
        }
        
        // Преобразование в более крупные единицы
        long totalSeconds = millis / 1000;
        
        long days = totalSeconds / (24 * 60 * 60);
        totalSeconds %= (24 * 60 * 60);
        
        long hours = totalSeconds / (60 * 60);
        totalSeconds %= (60 * 60);
        
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        
        StringBuilder result = new StringBuilder();
        
        if (days > 0) {
            result.append(days).append("д ");
        }
        
        if (hours > 0 || days > 0) {
            result.append(hours).append("ч ");
        }
        
        if (minutes > 0 || hours > 0 || days > 0) {
            result.append(minutes).append("м ");
        }
        
        if (seconds > 0 || minutes == 0 && hours == 0 && days == 0) {
            result.append(seconds).append("с");
        }
        
        return result.toString().trim();
    }
} 