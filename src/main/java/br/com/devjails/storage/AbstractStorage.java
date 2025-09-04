package br.com.devjails.storage;

import br.com.devjails.flag.FlagRegion;
import br.com.devjails.jail.Jail;
import br.com.devjails.prisoner.Prisoner;
import br.com.devjails.util.Tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Classe base abstrata para implementações de Storage
 * Fornece padrões comuns de error handling, async patterns e estrutura básica
 */
public abstract class AbstractStorage implements Storage {
    
    protected final File dataFolder;
    protected final Logger logger;
    protected volatile boolean initialized = false;
    
    public AbstractStorage(File dataFolder, Logger logger) {
        this.dataFolder = dataFolder;
        this.logger = logger;
    }
    
    @Override
    public CompletableFuture<Boolean> initialize() {
        return Tasks.asyncThenSync(() -> {
            try {
                if (!dataFolder.exists()) {
                    dataFolder.mkdirs();
                }
                
                boolean result = doInitialize();
                if (result) {
                    initialized = true;
                    logger.info(getStorageType() + " inicializado com sucesso!");
                } else {
                    logger.severe("Falha ao inicializar " + getStorageType());
                }
                return result;
            } catch (Exception e) {
                logger.severe("Erro ao inicializar " + getStorageType() + ": " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }, null);
    }
    
    @Override
    public CompletableFuture<Void> shutdown() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        try {
            doShutdown();
            initialized = false;
            future.complete(null);
        } catch (Exception e) {
            logger.warning("Erro ao finalizar " + getStorageType() + ": " + e.getMessage());
            future.completeExceptionally(e);
        }
        
        return future;
    }
    
    @Override
    public CompletableFuture<List<Jail>> loadAllJails() {
        return Tasks.asyncThenSync(() -> {
            List<Jail> jails = new ArrayList<>();
            
            try {
                List<String> jailNames = getAllJailNames();
                for (String jailName : jailNames) {
                    try {
                        Jail jail = loadJail(jailName).join();
                        if (jail != null) {
                            jails.add(jail);
                        }
                    } catch (Exception e) {
                        logger.warning("Erro ao carregar jail " + jailName + ": " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                logger.severe("Erro ao carregar todas as jails: " + e.getMessage());
                e.printStackTrace();
            }
            
            return jails;
        }, null);
    }
    
    @Override
    public CompletableFuture<List<Prisoner>> loadAllPrisoners() {
        return Tasks.asyncThenSync(() -> {
            List<Prisoner> prisoners = new ArrayList<>();
            
            try {
                List<UUID> prisonerUuids = getAllPrisonerUuids();
                for (UUID uuid : prisonerUuids) {
                    try {
                        Prisoner prisoner = loadPrisoner(uuid).join();
                        if (prisoner != null) {
                            prisoners.add(prisoner);
                        }
                    } catch (Exception e) {
                        logger.warning("Erro ao carregar prisioneiro " + uuid + ": " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                logger.severe("Erro ao carregar todos os prisioneiros: " + e.getMessage());
                e.printStackTrace();
            }
            
            return prisoners;
        }, null);
    }
    
    @Override
    public CompletableFuture<List<FlagRegion>> loadAllFlags() {
        return Tasks.asyncThenSync(() -> {
            List<FlagRegion> flags = new ArrayList<>();
            
            try {
                List<String> flagNames = getAllFlagNames();
                for (String flagName : flagNames) {
                    try {
                        FlagRegion flag = loadFlag(flagName).join();
                        if (flag != null) {
                            flags.add(flag);
                        }
                    } catch (Exception e) {
                        logger.warning("Erro ao carregar flag " + flagName + ": " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                logger.severe("Erro ao carregar todas as flags: " + e.getMessage());
                e.printStackTrace();
            }
            
            return flags;
        }, null);
    }
    
    @Override
    public CompletableFuture<List<Prisoner>> loadPrisonersByJail(String jailName) {
        return loadAllPrisoners().thenApply(prisoners -> 
            prisoners.stream()
                .filter(prisoner -> jailName.equals(prisoner.getJailName()))
                .toList()
        );
    }
    
    @Override
    public CompletableFuture<Boolean> isHealthy() {
        return Tasks.asyncThenSync(() -> {
            try {
                return initialized && doHealthCheck();
            } catch (Exception e) {
                logger.warning("Erro na verificação de saúde do " + getStorageType() + ": " + e.getMessage());
                return false;
            }
        }, null);
    }
    
    @Override
    public CompletableFuture<StorageStats> getStats() {
        return Tasks.asyncThenSync(() -> {
            try {
                int jailCount = getAllJailNames().size();
                int prisonerCount = getAllPrisonerUuids().size();
                int flagCount = getAllFlagNames().size();
                
                return new StorageStats(jailCount, prisonerCount, flagCount, getStorageType());
            } catch (Exception e) {
                logger.warning("Erro ao obter estatísticas do " + getStorageType() + ": " + e.getMessage());
                return new StorageStats(0, 0, 0, getStorageType());
            }
        }, null);
    }
    
    /**
     * Executa operação com tratamento de erro padronizado
     */
    protected <T> CompletableFuture<T> executeWithErrorHandling(String operation, Tasks.AsyncSupplier<T> supplier) {
        return Tasks.asyncThenSync(() -> {
            try {
                return supplier.get();
            } catch (Exception e) {
                logger.severe("Erro na operação " + operation + ": " + e.getMessage());
                e.printStackTrace();
                throw e;
            }
        }, null);
    }
    
    /**
     * Executa operação booleana com tratamento de erro padronizado
     */
    protected CompletableFuture<Boolean> executeBooleanOperation(String operation, Tasks.AsyncSupplier<Boolean> supplier) {
        return Tasks.asyncThenSync(() -> {
            try {
                return supplier.get();
            } catch (Exception e) {
                logger.severe("Erro na operação " + operation + ": " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }, null);
    }
    
    // Métodos abstratos que devem ser implementados pelas subclasses
    
    /**
     * Retorna o tipo de storage (ex: "SQLiteStorage", "YamlStorage")
     */
    protected abstract String getStorageType();
    
    /**
     * Inicialização específica da implementação
     */
    protected abstract boolean doInitialize() throws Exception;
    
    /**
     * Finalização específica da implementação
     */
    protected abstract void doShutdown() throws Exception;
    
    /**
     * Verificação de saúde específica da implementação
     */
    protected abstract boolean doHealthCheck() throws Exception;
    
    /**
     * Obtém lista de nomes de todas as jails
     */
    protected abstract List<String> getAllJailNames() throws Exception;
    
    /**
     * Obtém lista de UUIDs de todos os prisioneiros
     */
    protected abstract List<UUID> getAllPrisonerUuids() throws Exception;
    
    /**
     * Obtém lista de nomes de todas as flags
     */
    protected abstract List<String> getAllFlagNames() throws Exception;
}