package therayzv.rPass.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.Objects;
import java.util.regex.Pattern;

public class MessageUtil {
    
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final FileConfiguration config;
    
    public MessageUtil(FileConfiguration config) {
        this.config = config;
    }
    
    /**
     * Отправляет сообщение отправителю
     * @param sender отправитель
     * @param message ключ сообщения из конфига
     */
    public void sendMessage(CommandSender sender, String message) {
        String msg = getMessage(message);
        if (msg != null && !msg.isEmpty()) {
            sender.sendMessage(colorize(getPrefix() + msg));
        }
    }
    
    /**
     * Отправляет сообщение отправителю с подстановкой переменных
     * @param sender отправитель
     * @param message ключ сообщения из конфига
     * @param replacements пары значений для замены {ключ1, значение1, ключ2, значение2}
     */
    public void sendMessage(CommandSender sender, String message, String... replacements) {
        String msg = getMessage(message);
        if (msg != null && !msg.isEmpty()) {
            for (int i = 0; i < replacements.length; i += 2) {
                if (i + 1 < replacements.length) {
                    msg = msg.replace("%" + replacements[i] + "%", replacements[i + 1]);
                }
            }
            sender.sendMessage(colorize(getPrefix() + msg));
        }
    }
    
    /**
     * Отправляет title и subtitle игроку
     * @param player игрок
     * @param titleKey ключ заголовка из конфига
     * @param subtitleKey ключ подзаголовка из конфига
     */
    public void sendTitle(Player player, String titleKey, String subtitleKey) {
        String title = config.getString(titleKey);
        String subtitle = config.getString(subtitleKey);
        
        if (title == null) title = "";
        if (subtitle == null) subtitle = "";
        
        Title.Times times = Title.Times.times(
            Duration.ofSeconds(1),
            Duration.ofSeconds(3),
            Duration.ofSeconds(1)
        );
        
        player.showTitle(Title.title(
            Component.text(colorize(title)),
            Component.text(colorize(subtitle)),
            times
        ));
    }
    
    /**
     * Форматирует время в виде точной даты и времени
     * @param timestamp временная метка в миллисекундах
     * @return строка отформатированной даты
     */
    public String formatDate(long timestamp) {
        if (timestamp == PassManager.PERMANENT_PASS) {
            return "Навсегда";
        }
        
        String format = config.getString("placeholders.date-format", "dd.MM.yyyy HH:mm:ss");
        SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        return dateFormat.format(new Date(timestamp));
    }
    
    /**
     * Получает цвет для времени, основываясь на его значении
     * @param timeLeft оставшееся время в миллисекундах
     * @return строка с цветовым кодом Minecraft
     */
    public String getTimeColor(long timeLeft) {
        if (timeLeft == PassManager.PERMANENT_PASS) {
            return colorize(config.getString("placeholders.time-colors.permanent", "&b"));
        } else if (timeLeft <= 0) {
            return colorize(config.getString("placeholders.time-colors.expired", "&c"));
        } else if (timeLeft <= 24 * 60 * 60 * 1000) { // Меньше 1 дня
            return colorize(config.getString("placeholders.time-colors.warning", "&6"));
        } else {
            return colorize(config.getString("placeholders.time-colors.normal", "&e"));
        }
    }
    
    /**
     * Получает сообщение из конфига
     * @param key ключ сообщения
     * @return сообщение или null, если не найдено
     */
    private String getMessage(String key) {
        return config.getString("messages." + key);
    }
    
    /**
     * Получает префикс из конфига
     * @return префикс
     */
    private String getPrefix() {
        return Objects.requireNonNullElse(config.getString("messages.prefix"), "");
    }
    
    /**
     * Применяет цветовое форматирование к строке
     * @param text строка
     * @return отформатированная строка
     */
    public static String colorize(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }
} 