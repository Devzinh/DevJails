package br.com.devjails.util;

import br.com.devjails.message.MessageService;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.function.Predicate;

/**
 * Classe utilitária para validações comuns
 * Centraliza lógica de validação para reduzir duplicação de código
 */
public class ValidationUtils {
    
    /**
     * Resultado de uma validação
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorKey;
        private final Object[] errorArgs;
        
        public ValidationResult(boolean valid, String errorKey, Object... errorArgs) {
            this.valid = valid;
            this.errorKey = errorKey;
            this.errorArgs = errorArgs;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getErrorKey() {
            return errorKey;
        }
        
        public Object[] getErrorArgs() {
            return errorArgs;
        }
        
        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }
        
        public static ValidationResult error(String errorKey, Object... errorArgs) {
            return new ValidationResult(false, errorKey, errorArgs);
        }
    }
    
    /**
     * Valida se um jogador existe e já jogou no servidor
     */
    public static ValidationResult validateOfflinePlayer(OfflinePlayer player, String playerName) {
        if (player == null || (!player.hasPlayedBefore() && !player.isOnline())) {
            return ValidationResult.error("player_not_found", "{player}", playerName);
        }
        return ValidationResult.success();
    }
    
    /**
     * Valida se um jogador não está tentando executar uma ação em si mesmo
     */
    public static ValidationResult validateNotSelf(CommandSender sender, String targetPlayerName) {
        if (sender instanceof Player && sender.getName().equalsIgnoreCase(targetPlayerName)) {
            return ValidationResult.error("cannot_target_self");
        }
        return ValidationResult.success();
    }
    
    /**
     * Valida e converte uma string para double
     */
    public static ValidationResult validateDouble(String numberStr, String fieldName) {
        try {
            double value = Double.parseDouble(numberStr);
            return ValidationResult.success();
        } catch (NumberFormatException e) {
            return ValidationResult.error("invalid_number", "{field}", fieldName, "{value}", numberStr);
        }
    }
    
    /**
     * Valida e converte uma string para double com valor mínimo
     */
    public static ValidationResult validateDouble(String numberStr, String fieldName, double minValue) {
        try {
            double value = Double.parseDouble(numberStr);
            if (value < minValue) {
                return ValidationResult.error("number_too_small", "{field}", fieldName, "{min}", minValue);
            }
            return ValidationResult.success();
        } catch (NumberFormatException e) {
            return ValidationResult.error("invalid_number", "{field}", fieldName, "{value}", numberStr);
        }
    }
    
    /**
     * Valida e converte uma string para int
     */
    public static ValidationResult validateInteger(String numberStr, String fieldName) {
        try {
            int value = Integer.parseInt(numberStr);
            return ValidationResult.success();
        } catch (NumberFormatException e) {
            return ValidationResult.error("invalid_number", "{field}", fieldName, "{value}", numberStr);
        }
    }
    
    /**
     * Valida e converte uma string para int com valor mínimo
     */
    public static ValidationResult validateInteger(String numberStr, String fieldName, int minValue) {
        try {
            int value = Integer.parseInt(numberStr);
            if (value < minValue) {
                return ValidationResult.error("number_too_small", "{field}", fieldName, "{min}", minValue);
            }
            return ValidationResult.success();
        } catch (NumberFormatException e) {
            return ValidationResult.error("invalid_number", "{field}", fieldName, "{value}", numberStr);
        }
    }
    
    /**
     * Valida se uma string não está vazia ou nula
     */
    public static ValidationResult validateNotEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            return ValidationResult.error("field_required", "{field}", fieldName);
        }
        return ValidationResult.success();
    }
    
    /**
     * Valida se uma string tem um comprimento mínimo
     */
    public static ValidationResult validateMinLength(String value, String fieldName, int minLength) {
        if (value == null || value.length() < minLength) {
            return ValidationResult.error("field_too_short", "{field}", fieldName, "{min}", minLength);
        }
        return ValidationResult.success();
    }
    
    /**
     * Valida se uma string tem um comprimento máximo
     */
    public static ValidationResult validateMaxLength(String value, String fieldName, int maxLength) {
        if (value != null && value.length() > maxLength) {
            return ValidationResult.error("field_too_long", "{field}", fieldName, "{max}", maxLength);
        }
        return ValidationResult.success();
    }
    
    /**
     * Valida se um UUID é válido
     */
    public static ValidationResult validateUUID(String uuidStr) {
        try {
            UUID.fromString(uuidStr);
            return ValidationResult.success();
        } catch (IllegalArgumentException e) {
            return ValidationResult.error("invalid_uuid", "{value}", uuidStr);
        }
    }
    
    /**
     * Valida usando um predicado customizado
     */
    public static <T> ValidationResult validateCustom(T value, Predicate<T> validator, String errorKey, Object... errorArgs) {
        if (validator.test(value)) {
            return ValidationResult.success();
        }
        return ValidationResult.error(errorKey, errorArgs);
    }
    
    /**
     * Envia mensagem de erro baseada no resultado da validação
     */
    public static boolean sendErrorIfInvalid(ValidationResult result, CommandSender sender, MessageService messageService) {
        if (!result.isValid()) {
            messageService.sendMessage(sender, result.getErrorKey(), result.getErrorArgs());
            return true;
        }
        return false;
    }
    
    /**
     * Valida múltiplos resultados e retorna o primeiro erro encontrado
     */
    public static ValidationResult validateAll(ValidationResult... results) {
        for (ValidationResult result : results) {
            if (!result.isValid()) {
                return result;
            }
        }
        return ValidationResult.success();
    }
    
    /**
     * Valida se pelo menos uma validação passou
     */
    public static ValidationResult validateAny(ValidationResult... results) {
        for (ValidationResult result : results) {
            if (result.isValid()) {
                return ValidationResult.success();
            }
        }
        // Retorna o primeiro erro se nenhuma validação passou
        return results.length > 0 ? results[0] : ValidationResult.error("validation_failed");
    }
}