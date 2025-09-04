package br.com.devjails.util;

import java.nio.charset.StandardCharsets;

import org.bukkit.ChatColor;

// Adventure API removido para compatibilidade com Spigot


public class Text {
    
    // Constantes removidas para compatibilidade com Spigot
    
    
    // Método removido - use colorizeToString() para compatibilidade com Spigot
    
    
    public static String colorizeToString(String text) {
        if (text == null) {
            return "";
        }
        
        // Garantir que a string esteja na codificação correta
        try {
            text = new String(text.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Ignorar erro de codificação
        }
        
        return ChatColor.translateAlternateColorCodes('&', text);
    }
    
    
    public static String stripColors(String text) {
        if (text == null) {
            return "";
        }
        
        return ChatColor.stripColor(colorizeToString(text));
    }
    
    
    public static String center(String text, int length) {
        if (text == null) {
            text = "";
        }
        
        String stripped = stripColors(text);
        if (stripped.length() >= length) {
            return text;
        }
        
        int padding = (length - stripped.length()) / 2;
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < padding; i++) {
            sb.append(" ");
        }
        
        sb.append(text);
        
        while (sb.length() < length) {
            sb.append(" ");
        }
        
        return sb.toString();
    }
    
    
    public static String createSeparator(char character, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(character);
        }
        return sb.toString();
    }
    
    
    public static String createColoredSeparator(char character, int length, String color) {
        return colorizeToString(color + createSeparator(character, length));
    }
    
    
    public static String applyPlaceholder(String text, String placeholder, Object value) {
        if (text == null) {
            return "";
        }
        
        String valueStr = value != null ? value.toString() : "";
        return text.replace(placeholder, valueStr);
    }
    
    
    public static String applyPlaceholders(String text, Object... args) {
        if (text == null) {
            return "";
        }
        
        String result = text;
        
        for (int i = 0; i < args.length; i += 2) {
            if (i + 1 < args.length) {
                String placeholder = args[i].toString();
                Object value = args[i + 1];
                result = applyPlaceholder(result, placeholder, value);
            }
        }
        
        return result;
    }
    
    
    public static String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        
        String stripped = stripColors(text);
        if (stripped.length() <= maxLength) {
            return text;
        }

        int actualLength = 0;
        StringBuilder result = new StringBuilder();
        boolean inColorCode = false;
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            
            if (c == '&' && i + 1 < text.length()) {
                inColorCode = true;
                result.append(c);
            } else if (inColorCode) {
                result.append(c);
                inColorCode = false;
            } else {
                if (actualLength >= maxLength - 3) {
                    result.append("...");
                    break;
                }
                result.append(c);
                actualLength++;
            }
        }
        
        return result.toString();
    }
}