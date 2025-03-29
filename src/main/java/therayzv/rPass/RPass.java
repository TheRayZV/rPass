package therayzv.rPass;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import therayzv.rPass.command.PassCommand;
import therayzv.rPass.listener.PassCheckListener;
import therayzv.rPass.placeholder.PassPlaceholder;
import therayzv.rPass.utils.MessageUtil;
import therayzv.rPass.utils.PassManager;

import java.util.Objects;

public final class RPass extends JavaPlugin {

    private PassManager passManager;
    private MessageUtil messageUtil;
    private PassCheckListener passCheckListener;

    @Override
    public void onEnable() {
        // Создаем конфигурацию
        saveDefaultConfig();
        
        // Инициализируем менеджер проходок
        passManager = new PassManager(getDataFolder(), this);
        
        // Инициализируем утилиту сообщений
        messageUtil = new MessageUtil(getConfig());
        
        // Регистрируем слушатель проверки проходок
        passCheckListener = new PassCheckListener(this);
        getServer().getPluginManager().registerEvents(passCheckListener, this);
        
        // Регистрируем команду
        Objects.requireNonNull(getCommand("rpass")).setExecutor(new PassCommand(this));
        
        // Регистрируем плейсхолдеры, если доступны
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                // Создаем экземпляр нашего плейсхолдера
                new PassPlaceholder(this).register();
                getLogger().info("PlaceholderAPI найден, плейсхолдеры успешно зарегистрированы");
                
                // Добавляем информационные логи для диагностики
                getLogger().info("Для использования плейсхолдеров используйте следующие форматы:");
                getLogger().info("%rpass_time% - время до окончания проходки");
                getLogger().info("%rpass_time_compact% - компактное отображение времени");
                getLogger().info("%rpass_has% - наличие проходки (true/false)");
                getLogger().info("%rpass_permanent% - вечная ли проходка (true/false)");
                getLogger().info("%rpass_status% - статус проходки (Активна/Истекла/Нет проходки)");
            } catch (Exception e) {
                getLogger().warning("Не удалось зарегистрировать плейсхолдеры: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            getLogger().info("PlaceholderAPI не найден. Плейсхолдеры не будут работать.");
        }
        
        getLogger().info("Плагин успешно запущен!");
    }

    @Override
    public void onDisable() {
        // Останавливаем задачу проверки проходок
        if (passCheckListener != null) {
            passCheckListener.stopCheckTask();
        }
        
        // Сохраняем проходки
        if (passManager != null) {
            passManager.savePasses();
        }
        
        getLogger().info("Плагин успешно выключен!");
    }
    
    /**
     * Получает менеджер проходок
     * @return менеджер проходок
     */
    public PassManager getPassManager() {
        return passManager;
    }
    
    /**
     * Получает утилиту сообщений
     * @return утилита сообщений
     */
    public MessageUtil getMessageUtil() {
        return messageUtil;
    }
    
    @Override
    public void reloadConfig() {
        super.reloadConfig();
        
        // Перезагружаем утилиту сообщений
        messageUtil = new MessageUtil(getConfig());
        
        // Перезапускаем задачу проверки проходок
        if (passCheckListener != null) {
            passCheckListener.startCheckTask();
        }
        
        // Перезагружаем плейсхолдеры, если PlaceholderAPI доступен
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                // Пробуем найти наш экземпляр плейсхолдера и обновить его
                Bukkit.getServer().getServicesManager().getRegistrations(getClassLoader().loadClass("me.clip.placeholderapi.expansion.PlaceholderExpansion"))
                    .stream()
                    .map(registration -> registration.getProvider())
                    .filter(expansion -> expansion instanceof PassPlaceholder)
                    .findFirst()
                    .ifPresent(expansion -> ((PassPlaceholder) expansion).reloadConfig());
                
                getLogger().info("Настройки плейсхолдеров успешно обновлены");
            } catch (Exception e) {
                getLogger().warning("Не удалось обновить плейсхолдеры: " + e.getMessage());
            }
        }
    }
}
