package br.com.devjails.storage;

import br.com.devjails.flag.FlagRegion;
import br.com.devjails.jail.Jail;
import br.com.devjails.prisoner.Prisoner;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Interface para abstraÃ§Ã£o do sistema de armazenamento
 */
public interface Storage {
    
    /**
     * Inicializa o sistema de storage
     */
    CompletableFuture<Boolean> initialize();
    
    /**
     * Finaliza o sistema de storage
     */
    CompletableFuture<Void> shutdown();    
    /**
     * Salva uma jail
     */
    CompletableFuture<Boolean> saveJail(Jail jail);
    
    /**
     * Carrega uma jail pelo nome
     */
    CompletableFuture<Jail> loadJail(String name);
    
    /**
     * Remove uma jail
     */
    CompletableFuture<Boolean> removeJail(String name);
    
    /**
     * Lista todas as jails
     */
    CompletableFuture<List<Jail>> loadAllJails();    
    /**
     * Salva um prisioneiro
     */
    CompletableFuture<Boolean> savePrisoner(Prisoner prisoner);
    
    /**
     * Carrega um prisioneiro pelo UUID
     */
    CompletableFuture<Prisoner> loadPrisoner(UUID uuid);
    
    /**
     * Remove um prisioneiro
     */
    CompletableFuture<Boolean> removePrisoner(UUID uuid);
    
    /**
     * Lista todos os prisioneiros
     */
    CompletableFuture<List<Prisoner>> loadAllPrisoners();
    
    /**
     * Lista prisioneiros por jail
     */
    CompletableFuture<List<Prisoner>> loadPrisonersByJail(String jailName);    
    /**
     * Salva uma flag
     */
    CompletableFuture<Boolean> saveFlag(FlagRegion flag);
    
    /**
     * Carrega uma flag pelo nome
     */
    CompletableFuture<FlagRegion> loadFlag(String name);
    
    /**
     * Remove uma flag
     */
    CompletableFuture<Boolean> removeFlag(String name);
    
    /**
     * Lista todas as flags
     */
    CompletableFuture<List<FlagRegion>> loadAllFlags();    
    /**
     * Verifica se o storage estÃ¡ funcionando
     */
    CompletableFuture<Boolean> isHealthy();
    CompletableFuture<StorageStats> getStats();
    
    /**
     * EstatÃ­sticas do storage
     */
    class StorageStats {
        private final int jailCount;
        private final int prisonerCount;
        private final int flagCount;
        private final String type;
        
        public StorageStats(int jailCount, int prisonerCount, int flagCount, String type) {
            this.jailCount = jailCount;
            this.prisonerCount = prisonerCount;
            this.flagCount = flagCount;
            this.type = type;
        }
        
        public int getJailCount() {
            return jailCount;
        }
        
        public int getPrisonerCount() {
            return prisonerCount;
        }
        
        public int getFlagCount() {
            return flagCount;
        }
        
        public String getType() {
            return type;
        }
        
        @Override
        public String toString() {
            return String.format("StorageStats{type='%s', jails=%d, prisoners=%d, flags=%d}",
                    type, jailCount, prisonerCount, flagCount);
        }
    }
}