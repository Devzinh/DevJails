package br.com.devjails.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * UtilitÃ¡rio para converter strings de tempo em milissegundos
 * Suporta formatos como: 1d2h30m15s, 2h, 30m, etc.
 */
public class TimeParser {
    
    private static final Pattern TIME_PATTERN = Pattern.compile(
        "(?:(\\d+)d)?(?:(\\d+)h)?(?:(\\d+)m)?(?:(\\d+)s)?",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final long SECOND_MILLIS = 1000L;
    private static final long MINUTE_MILLIS = 60L * SECOND_MILLIS;
    private static final long HOUR_MILLIS = 60L * MINUTE_MILLIS;
    private static final long DAY_MILLIS = 24L * HOUR_MILLIS;
    
    /**
     * Converte uma string de tempo para milissegundos
     * @param timeString formato: 1d2h30m15s
     * @return milissegundos ou -1 se invÃ¡lido
     */
    public static long parseTimeToMillis(String timeString) {
        if (timeString == null || timeString.trim().isEmpty()) {
            return -1;
        }
        
        timeString = timeString.trim().toLowerCase();
        Matcher matcher = TIME_PATTERN.matcher(timeString);
        
        if (!matcher.matches()) {
            return -1;
        }
        
        long totalMillis = 0;

        if (matcher.group(1) != null) {
            totalMillis += Long.parseLong(matcher.group(1)) * DAY_MILLIS;
        }

        if (matcher.group(2) != null) {
            totalMillis += Long.parseLong(matcher.group(2)) * HOUR_MILLIS;
        }

        if (matcher.group(3) != null) {
            totalMillis += Long.parseLong(matcher.group(3)) * MINUTE_MILLIS;
        }

        if (matcher.group(4) != null) {
            totalMillis += Long.parseLong(matcher.group(4)) * SECOND_MILLIS;
        }
        
        return totalMillis > 0 ? totalMillis : -1;
    }
    
    /**
     * Converte milissegundos para uma string legÃ­vel
     * @param millis milissegundos
     * @return string formatada (ex: "1d 2h 30m 15s")
     */
    public static String formatTime(long millis) {
        if (millis <= 0) {
            return "0s";
        }
        
        StringBuilder sb = new StringBuilder();
        
        long days = millis / DAY_MILLIS;
        millis %= DAY_MILLIS;
        
        long hours = millis / HOUR_MILLIS;
        millis %= HOUR_MILLIS;
        
        long minutes = millis / MINUTE_MILLIS;
        millis %= MINUTE_MILLIS;
        
        long seconds = millis / SECOND_MILLIS;
        
        if (days > 0) {
            sb.append(days).append("d ");
        }
        if (hours > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0) {
            sb.append(minutes).append("m ");
        }
        if (seconds > 0 || sb.length() == 0) {
            sb.append(seconds).append("s");
        }
        
        return sb.toString().trim();
    }
    
    /**
     * Converte milissegundos para uma string compacta
     * @param millis milissegundos
     * @return string compacta (ex: "1d2h30m15s")
     */
    public static String formatTimeCompact(long millis) {
        if (millis <= 0) {
            return "0s";
        }
        
        StringBuilder sb = new StringBuilder();
        
        long days = millis / DAY_MILLIS;
        millis %= DAY_MILLIS;
        
        long hours = millis / HOUR_MILLIS;
        millis %= HOUR_MILLIS;
        
        long minutes = millis / MINUTE_MILLIS;
        millis %= MINUTE_MILLIS;
        
        long seconds = millis / SECOND_MILLIS;
        
        if (days > 0) {
            sb.append(days).append("d");
        }
        if (hours > 0) {
            sb.append(hours).append("h");
        }
        if (minutes > 0) {
            sb.append(minutes).append("m");
        }
        if (seconds > 0 || sb.length() == 0) {
            sb.append(seconds).append("s");
        }
        
        return sb.toString();
    }
    
    
    public static boolean isValidTimeString(String timeString) {
        return parseTimeToMillis(timeString) > 0;
    }
    public static String getTimeUntil(long targetEpoch) {
        long remaining = targetEpoch - System.currentTimeMillis();
        return remaining > 0 ? formatTime(remaining) : "Expirado";
    }
}