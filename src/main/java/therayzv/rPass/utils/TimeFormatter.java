package therayzv.rPass.utils;

import java.util.concurrent.TimeUnit;

public class TimeFormatter {

    /**
     * Преобразует строку времени в миллисекунды
     * Поддерживаемые форматы: 1d (дни), 1h (часы), 1m (минуты), 1s (секунды)
     * Пример: "1d12h30m" = 1 день, 12 часов и 30 минут
     * Значение "permanent" или "forever" означает вечную проходку
     *
     * @param timeString строка времени
     * @return время в миллисекундах
     */
    public static long parseTime(String timeString) {
        if (timeString == null || timeString.isEmpty()) {
            return 0;
        }
        
        // Проверка на вечную проходку
        if (timeString.equalsIgnoreCase("permanent") || 
            timeString.equalsIgnoreCase("forever") || 
            timeString.equalsIgnoreCase("навсегда") ||
            timeString.equalsIgnoreCase("вечно")) {
            return PassManager.PERMANENT_PASS;
        }
        
        long totalMillis = 0;
        StringBuilder numBuilder = new StringBuilder();
        
        for (int i = 0; i < timeString.length(); i++) {
            char c = timeString.charAt(i);
            
            if (Character.isDigit(c)) {
                numBuilder.append(c);
            } else {
                if (numBuilder.length() > 0) {
                    long value = Long.parseLong(numBuilder.toString());
                    
                    switch (c) {
                        case 'd':
                            totalMillis += TimeUnit.DAYS.toMillis(value);
                            break;
                        case 'h':
                            totalMillis += TimeUnit.HOURS.toMillis(value);
                            break;
                        case 'm':
                            totalMillis += TimeUnit.MINUTES.toMillis(value);
                            break;
                        case 's':
                            totalMillis += TimeUnit.SECONDS.toMillis(value);
                            break;
                        default:
                            // Игнорируем неизвестные символы
                    }
                    
                    numBuilder = new StringBuilder();
                }
            }
        }
        
        return totalMillis;
    }
    
    /**
     * Форматирует время в миллисекундах в человекочитаемый формат с правильным склонением единиц времени
     *
     * @param millis время в миллисекундах
     * @return отформатированная строка времени
     */
    public static String formatTime(long millis) {
        // Проверка на вечную проходку
        if (millis == PassManager.PERMANENT_PASS) {
            return "навсегда";
        }
        
        if (millis <= 0) {
            return "0 секунд";
        }
        
        // Преобразование в более крупные единицы (месяцы, годы)
        long totalSeconds = millis / 1000;
        
        long years = totalSeconds / (365 * 24 * 60 * 60);
        totalSeconds %= (365 * 24 * 60 * 60);
        
        long months = totalSeconds / (30 * 24 * 60 * 60);
        totalSeconds %= (30 * 24 * 60 * 60);
        
        long days = totalSeconds / (24 * 60 * 60);
        totalSeconds %= (24 * 60 * 60);
        
        long hours = totalSeconds / (60 * 60);
        totalSeconds %= (60 * 60);
        
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        
        StringBuilder result = new StringBuilder();
        
        // Добавляем годы с правильным склонением
        if (years > 0) {
            result.append(years).append(" ").append(getYearForm(years)).append(" ");
        }
        
        // Добавляем месяцы с правильным склонением
        if (months > 0 || years > 0) {
            result.append(months).append(" ").append(getMonthForm(months)).append(" ");
        }
        
        // Добавляем дни с правильным склонением
        if (days > 0 || months > 0 || years > 0) {
            result.append(days).append(" ").append(getDayForm(days)).append(" ");
        }
        
        // Добавляем часы с правильным склонением
        if (hours > 0 || days > 0 || months > 0 || years > 0) {
            result.append(hours).append(" ").append(getHourForm(hours)).append(" ");
        }
        
        // Добавляем минуты с правильным склонением
        if (minutes > 0 || hours > 0 || days > 0 || months > 0 || years > 0) {
            result.append(minutes).append(" ").append(getMinuteForm(minutes)).append(" ");
        }
        
        // Всегда добавляем секунды
        result.append(seconds).append(" ").append(getSecondForm(seconds));
        
        return result.toString();
    }
    
    /**
     * Получает правильную форму слова "год" в зависимости от числа
     */
    private static String getYearForm(long years) {
        if (years % 10 == 1 && years % 100 != 11) {
            return "год";
        } else if ((years % 10 == 2 || years % 10 == 3 || years % 10 == 4) && 
                  (years % 100 != 12 && years % 100 != 13 && years % 100 != 14)) {
            return "года";
        } else {
            return "лет";
        }
    }
    
    /**
     * Получает правильную форму слова "месяц" в зависимости от числа
     */
    private static String getMonthForm(long months) {
        if (months % 10 == 1 && months % 100 != 11) {
            return "месяц";
        } else if ((months % 10 == 2 || months % 10 == 3 || months % 10 == 4) && 
                  (months % 100 != 12 && months % 100 != 13 && months % 100 != 14)) {
            return "месяца";
        } else {
            return "месяцев";
        }
    }
    
    /**
     * Получает правильную форму слова "день" в зависимости от числа
     */
    private static String getDayForm(long days) {
        if (days % 10 == 1 && days % 100 != 11) {
            return "день";
        } else if ((days % 10 == 2 || days % 10 == 3 || days % 10 == 4) && 
                  (days % 100 != 12 && days % 100 != 13 && days % 100 != 14)) {
            return "дня";
        } else {
            return "дней";
        }
    }
    
    /**
     * Получает правильную форму слова "час" в зависимости от числа
     */
    private static String getHourForm(long hours) {
        if (hours % 10 == 1 && hours % 100 != 11) {
            return "час";
        } else if ((hours % 10 == 2 || hours % 10 == 3 || hours % 10 == 4) && 
                  (hours % 100 != 12 && hours % 100 != 13 && hours % 100 != 14)) {
            return "часа";
        } else {
            return "часов";
        }
    }
    
    /**
     * Получает правильную форму слова "минута" в зависимости от числа
     */
    private static String getMinuteForm(long minutes) {
        if (minutes % 10 == 1 && minutes % 100 != 11) {
            return "минута";
        } else if ((minutes % 10 == 2 || minutes % 10 == 3 || minutes % 10 == 4) && 
                  (minutes % 100 != 12 && minutes % 100 != 13 && minutes % 100 != 14)) {
            return "минуты";
        } else {
            return "минут";
        }
    }
    
    /**
     * Получает правильную форму слова "секунда" в зависимости от числа
     */
    private static String getSecondForm(long seconds) {
        if (seconds % 10 == 1 && seconds % 100 != 11) {
            return "секунда";
        } else if ((seconds % 10 == 2 || seconds % 10 == 3 || seconds % 10 == 4) && 
                  (seconds % 100 != 12 && seconds % 100 != 13 && seconds % 100 != 14)) {
            return "секунды";
        } else {
            return "секунд";
        }
    }
} 