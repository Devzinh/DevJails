package br.com.devjails.api;

import br.com.devjails.flag.FlagRegion;
import br.com.devjails.jail.Jail;
import br.com.devjails.prisoner.Prisoner;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface DevJailsAPI {    
    /**
     * ObtÃ©m uma cadeia pelo nome
     * @param name nome da cadeia
     * @return cadeia ou null se nÃ£o encontrada
     */
    Jail getJail(String name);
    
    /**
     * ObtÃ©m todas as cadeias
     * @return coleÃ§Ã£o de cadeias
     */
    Collection<Jail> getAllJails();
    
    /**
     * Verifica se uma cadeia existe
     * @param name nome da cadeia
     * @return true se existe
     */
    boolean jailExists(String name);
    
    /**
     * Cria uma nova cadeia
     * @param name nome da cadeia
     * @param spawnLocation localizaÃ§Ã£o de spawn
     * @return future indicando sucesso
     */
    CompletableFuture<Boolean> createJail(String name, Location spawnLocation);
    
    /**
     * Remove uma cadeia
     * @param name nome da cadeia
     * @return future indicando sucesso
     */
    CompletableFuture<Boolean> removeJail(String name);    
    /**
     * Verifica se um jogador estÃ¡ preso
     * @param player jogador
     * @return true se estÃ¡ preso
     */
    boolean isJailed(OfflinePlayer player);
    
    /**
     * Verifica se um jogador estÃ¡ preso pelo UUID
     * @param uuid UUID do jogador
     * @return true se estÃ¡ preso
     */
    boolean isJailed(UUID uuid);
    
    /**
     * ObtÃ©m dados de um prisioneiro
     * @param player jogador
     * @return dados do prisioneiro ou null
     */
    Prisoner getPrisoner(OfflinePlayer player);
    
    /**
     * ObtÃ©m dados de um prisioneiro pelo UUID
     * @param uuid UUID do jogador
     * @return dados do prisioneiro ou null
     */
    Prisoner getPrisoner(UUID uuid);
    
    /**
     * ObtÃ©m todos os prisioneiros
     * @return coleÃ§Ã£o de prisioneiros
     */
    Collection<Prisoner> getAllPrisoners();
    
    /**
     * ObtÃ©m prisioneiros de uma cadeia especÃ­fica
     * @param jailName nome da cadeia
     * @return lista de prisioneiros
     */
    Collection<Prisoner> getPrisonersByJail(String jailName);
    
    /**
     * Prende um jogador permanentemente
     * @param player jogador a ser preso
     * @param jailName nome da cadeia
     * @param reason motivo da prisÃ£o
     * @param staff staff responsÃ¡vel
     * @return future indicando sucesso
     */
    CompletableFuture<Boolean> jailPlayer(OfflinePlayer player, String jailName, String reason, String staff);
    
    /**
     * Prende um jogador temporariamente
     * @param player jogador a ser preso
     * @param jailName nome da cadeia
     * @param reason motivo da prisÃ£o
     * @param staff staff responsÃ¡vel
     * @param durationMillis duraÃ§Ã£o em milissegundos
     * @return future indicando sucesso
     */
    CompletableFuture<Boolean> jailPlayer(OfflinePlayer player, String jailName, String reason, String staff, long durationMillis);
    
    /**
     * Solta um jogador
     * @param player jogador a ser solto
     * @param staff staff responsÃ¡vel (null para soltura automÃ¡tica)
     * @return future indicando sucesso
     */
    CompletableFuture<Boolean> unjailPlayer(OfflinePlayer player, String staff);
    
    /**
     * Estende o tempo de prisÃ£o de um jogador
     * @param player jogador
     * @param additionalMillis tempo adicional em milissegundos
     * @return future indicando sucesso
     */
    CompletableFuture<Boolean> extendJailTime(OfflinePlayer player, long additionalMillis);    
    /**
     * Define Fiança para um prisioneiro
     * @param player jogador preso
     * @param amount valor da Fiança
     * @return future indicando sucesso
     */
    CompletableFuture<Boolean> setBail(OfflinePlayer player, double amount);
    
    /**
     * Remove Fiança de um prisioneiro
     * @param player jogador preso
     * @return future indicando sucesso
     */
    CompletableFuture<Boolean> removeBail(OfflinePlayer player);
    
    /**
     * Paga Fiança de um prisioneiro
     * @param prisoner jogador preso
     * @param payer jogador que estÃ¡ pagando
     * @return future indicando sucesso
     */
    CompletableFuture<Boolean> payBail(OfflinePlayer prisoner, OfflinePlayer payer);
    
    /**
     * ObtÃ©m prisioneiros com Fiança disponÃ­vel
     * @return lista de prisioneiros com Fiança
     */
    Collection<Prisoner> getPrisonersWithBail();    
    /**
     * ObtÃ©m uma flag pelo nome
     * @param name nome da flag
     * @return flag ou null se nÃ£o encontrada
     */
    FlagRegion getFlag(String name);
    
    /**
     * ObtÃ©m todas as flags
     * @return coleÃ§Ã£o de flags
     */
    Collection<FlagRegion> getAllFlags();
    
    /**
     * Verifica se uma flag existe
     * @param name nome da flag
     * @return true se existe
     */
    boolean flagExists(String name);
    
    /**
     * Cria uma nova flag
     * @param name nome da flag
     * @param pos1 primeira posiÃ§Ã£o
     * @param pos2 segunda posiÃ§Ã£o
     * @return future indicando sucesso
     */
    CompletableFuture<Boolean> createFlag(String name, Location pos1, Location pos2);
    
    /**
     * Remove uma flag
     * @param name nome da flag
     * @return future indicando sucesso
     */
    CompletableFuture<Boolean> removeFlag(String name);    
    /**
     * Verifica se uma localizaÃ§Ã£o estÃ¡ dentro de uma Ã¡rea de cadeia
     * @param location localizaÃ§Ã£o a verificar
     * @return cadeia que contÃ©m a localizaÃ§Ã£o ou null
     */
    Jail findJailAtLocation(Location location);
    
    /**
     * Verifica se uma localizaÃ§Ã£o estÃ¡ dentro de uma flag especÃ­fica
     * @param location localizaÃ§Ã£o
     * @param flagName nome da flag
     * @return true se estÃ¡ dentro
     */
    boolean isLocationInFlag(Location location, String flagName);
    
    /**
     * Vincula uma cadeia a uma Ã¡rea
     * @param jailName nome da cadeia
     * @param areaRef referÃªncia da Ã¡rea (flag:nome ou wg:regiao)
     * @return future indicando sucesso
     */
    CompletableFuture<Boolean> linkJailToArea(String jailName, String areaRef);
    
    /**
     * Desvincula uma cadeia de sua Ã¡rea
     * @param jailName nome da cadeia
     * @return future indicando sucesso
     */
    CompletableFuture<Boolean> unlinkJail(String jailName);    
    /**
     * ObtÃ©m estatÃ­sticas do plugin
     * @return estatÃ­sticas
     */
    DevJailsStats getStats();
    
    /**
     * Verifica se o sistema de Fiança estÃ¡ habilitado
     * @return true se habilitado
     */
    boolean isBailSystemEnabled();
    
    /**
     * Verifica se WorldEdit estÃ¡ disponÃ­vel
     * @return true se disponÃ­vel
     */
    boolean isWorldEditAvailable();
    
    /**
     * Verifica se WorldGuard estÃ¡ disponÃ­vel
     * @return true se disponÃ­vel
     */
    boolean isWorldGuardAvailable();
    
    /**
     * ObtÃ©m a versÃ£o da API
     * @return versÃ£o da API
     */
    String getAPIVersion();

    class DevJailsStats {
        private final int totalJails;
        private final int totalPrisoners;
        private final int totalFlags;
        private final int prisonersWithBail;
        private final String storageType;
        
        public DevJailsStats(int totalJails, int totalPrisoners, int totalFlags, int prisonersWithBail, String storageType) {
            this.totalJails = totalJails;
            this.totalPrisoners = totalPrisoners;
            this.totalFlags = totalFlags;
            this.prisonersWithBail = prisonersWithBail;
            this.storageType = storageType;
        }
        
        public int getTotalJails() { return totalJails; }
        public int getTotalPrisoners() { return totalPrisoners; }
        public int getTotalFlags() { return totalFlags; }
        public int getPrisonersWithBail() { return prisonersWithBail; }
        public String getStorageType() { return storageType; }
        
        @Override
        public String toString() {
            return String.format("DevJailsStats{jails=%d, prisoners=%d, flags=%d, bail=%d, storage='%s'}",
                    totalJails, totalPrisoners, totalFlags, prisonersWithBail, storageType);
        }
    }
}