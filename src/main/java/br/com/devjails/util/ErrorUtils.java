package br.com.devjails.util;

import br.com.devjails.message.MessageService;
import org.bukkit.command.CommandSender;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Classe utilitária para tratamento padronizado de erros
 * Centraliza padrões comuns de try-catch e error handling
 */
public class ErrorUtils {
    
    /**
     * Executa uma operação com tratamento de erro padronizado
     */
    public static <T> T executeWithErrorHandling(Supplier<T> operation,
                                                T defaultValue,
                                                String operationName,
                                                Logger logger) {
        try {
            return operation.get();
        } catch (Exception e) {
            logger.severe("Erro na operação " + operationName + ": " + e.getMessage());
            e.printStackTrace();
            return defaultValue;
        }
    }
    
    /**
     * Executa uma operação com tratamento de erro e callback customizado
     */
    public static <T> T executeWithErrorHandling(Supplier<T> operation,
                                                Function<Exception, T> errorHandler,
                                                String operationName,
                                                Logger logger) {
        try {
            return operation.get();
        } catch (Exception e) {
            logger.severe("Erro na operação " + operationName + ": " + e.getMessage());
            return errorHandler.apply(e);
        }
    }
    
    /**
     * Executa uma operação void com tratamento de erro
     */
    public static void executeWithErrorHandling(Runnable operation,
                                               String operationName,
                                               Logger logger) {
        try {
            operation.run();
        } catch (Exception e) {
            logger.severe("Erro na operação " + operationName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Executa uma operação void com tratamento de erro e callback
     */
    public static void executeWithErrorHandling(Runnable operation,
                                               Consumer<Exception> errorHandler,
                                               String operationName,
                                               Logger logger) {
        try {
            operation.run();
        } catch (Exception e) {
            logger.severe("Erro na operação " + operationName + ": " + e.getMessage());
            errorHandler.accept(e);
        }
    }
    
    /**
     * Executa uma operação com tratamento de erro para CommandSender
     */
    public static <T> T executeForSender(Supplier<T> operation,
                                       T defaultValue,
                                       CommandSender sender,
                                       MessageService messageService,
                                       String operationName,
                                       Logger logger) {
        try {
            return operation.get();
        } catch (Exception e) {
            messageService.sendMessage(sender, "operation_error");
            logger.severe("Erro na operação " + operationName + " para " + sender.getName() + ": " + e.getMessage());
            return defaultValue;
        }
    }
    
    /**
     * Executa uma operação void com tratamento de erro para CommandSender
     */
    public static void executeForSender(Runnable operation,
                                      CommandSender sender,
                                      MessageService messageService,
                                      String operationName,
                                      Logger logger) {
        try {
            operation.run();
        } catch (Exception e) {
            messageService.sendMessage(sender, "operation_error");
            logger.severe("Erro na operação " + operationName + " para " + sender.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Executa uma operação com múltiplos tipos de exceção
     */
    public static <T> T executeWithMultipleExceptions(Supplier<T> operation,
                                                     T defaultValue,
                                                     String operationName,
                                                     Logger logger,
                                                     Class<?>... expectedExceptions) {
        try {
            return operation.get();
        } catch (Exception e) {
            boolean isExpected = false;
            for (Class<?> expectedException : expectedExceptions) {
                if (expectedException.isInstance(e)) {
                    isExpected = true;
                    break;
                }
            }
            
            if (isExpected) {
                logger.warning("Exceção esperada na operação " + operationName + ": " + e.getMessage());
            } else {
                logger.severe("Erro inesperado na operação " + operationName + ": " + e.getMessage());
                e.printStackTrace();
            }
            
            return defaultValue;
        }
    }
    
    /**
     * Executa uma operação com retry em caso de erro
     */
    public static <T> T executeWithRetry(Supplier<T> operation,
                                       T defaultValue,
                                       int maxRetries,
                                       String operationName,
                                       Logger logger) {
        Exception lastException = null;
        
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return operation.get();
            } catch (Exception e) {
                lastException = e;
                if (attempt < maxRetries) {
                    logger.warning("Tentativa " + (attempt + 1) + " falhou para " + operationName + ", tentando novamente...");
                } else {
                    logger.severe("Operação " + operationName + " falhou após " + (maxRetries + 1) + " tentativas");
                }
            }
        }
        
        if (lastException != null) {
            lastException.printStackTrace();
        }
        
        return defaultValue;
    }
    
    /**
     * Executa uma operação com fallback em caso de erro
     */
    public static <T> T executeWithFallback(Supplier<T> primaryOperation,
                                          Supplier<T> fallbackOperation,
                                          String operationName,
                                          Logger logger) {
        try {
            return primaryOperation.get();
        } catch (Exception e) {
            logger.warning("Operação primária " + operationName + " falhou, usando fallback: " + e.getMessage());
            try {
                return fallbackOperation.get();
            } catch (Exception fallbackException) {
                logger.severe("Fallback também falhou para " + operationName + ": " + fallbackException.getMessage());
                fallbackException.printStackTrace();
                throw new RuntimeException("Tanto operação primária quanto fallback falharam", fallbackException);
            }
        }
    }
    
    /**
     * Executa uma operação com validação prévia
     */
    public static <T> T executeWithValidation(Supplier<Boolean> validator,
                                            Supplier<T> operation,
                                            T defaultValue,
                                            String validationError,
                                            String operationName,
                                            Logger logger) {
        try {
            if (!validator.get()) {
                logger.warning("Validação falhou para " + operationName + ": " + validationError);
                return defaultValue;
            }
            return operation.get();
        } catch (Exception e) {
            logger.severe("Erro na operação " + operationName + ": " + e.getMessage());
            e.printStackTrace();
            return defaultValue;
        }
    }
    
    /**
     * Cria um wrapper seguro para operações que podem falhar
     */
    public static <T> SafeOperation<T> safe(Supplier<T> operation) {
        return new SafeOperation<>(operation);
    }
    
    /**
     * Classe para operações seguras com fluent interface
     */
    public static class SafeOperation<T> {
        private final Supplier<T> operation;
        private T defaultValue;
        private Function<Exception, T> errorHandler;
        private String operationName = "unknown";
        private Logger logger;
        
        private SafeOperation(Supplier<T> operation) {
            this.operation = operation;
        }
        
        public SafeOperation<T> withDefault(T defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }
        
        public SafeOperation<T> withErrorHandler(Function<Exception, T> errorHandler) {
            this.errorHandler = errorHandler;
            return this;
        }
        
        public SafeOperation<T> withName(String operationName) {
            this.operationName = operationName;
            return this;
        }
        
        public SafeOperation<T> withLogger(Logger logger) {
            this.logger = logger;
            return this;
        }
        
        public T execute() {
            try {
                return operation.get();
            } catch (Exception e) {
                if (logger != null) {
                    logger.severe("Erro na operação " + operationName + ": " + e.getMessage());
                }
                
                if (errorHandler != null) {
                    return errorHandler.apply(e);
                } else {
                    return defaultValue;
                }
            }
        }
    }
    
    /**
     * Utilitário para logging padronizado de exceções
     */
    public static void logException(Exception e, String context, Logger logger) {
        logger.severe("Exceção em " + context + ": " + e.getClass().getSimpleName() + " - " + e.getMessage());
        if (logger.isLoggable(java.util.logging.Level.FINE)) {
            e.printStackTrace();
        }
    }
    
    /**
     * Utilitário para logging de warnings
     */
    public static void logWarning(String message, String context, Logger logger) {
        logger.warning("[" + context + "] " + message);
    }
    
    /**
     * Utilitário para logging de informações
     */
    public static void logInfo(String message, String context, Logger logger) {
        logger.info("[" + context + "] " + message);
    }
}