package br.com.devjails.api;

import br.com.devjails.flag.FlagRegion;
import br.com.devjails.integration.VaultHook;
import br.com.devjails.integration.WorldEditHook;
import br.com.devjails.integration.WorldGuardHook;
import br.com.devjails.jail.Jail;
import br.com.devjails.jail.JailManager;
import br.com.devjails.prisoner.Prisoner;
import br.com.devjails.prisoner.PrisonerManager;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DevJailsAPIImpl implements DevJailsAPI {
    
    private static final String API_VERSION = "1.0.0";
    
    private final JailManager jailManager;
    private final PrisonerManager prisonerManager;
    private final VaultHook vaultHook;
    private final WorldEditHook worldEditHook;
    private final WorldGuardHook worldGuardHook;
    
    public DevJailsAPIImpl(JailManager jailManager, PrisonerManager prisonerManager,
                           VaultHook vaultHook, WorldEditHook worldEditHook, WorldGuardHook worldGuardHook) {
        this.jailManager = jailManager;
        this.prisonerManager = prisonerManager;
        this.vaultHook = vaultHook;
        this.worldEditHook = worldEditHook;
        this.worldGuardHook = worldGuardHook;
    }    
    @Override
    public Jail getJail(String name) {
        return jailManager.getJail(name);
    }
    
    @Override
    public Collection<Jail> getAllJails() {
        return jailManager.getAllJails();
    }
    
    @Override
    public boolean jailExists(String name) {
        return jailManager.jailExists(name);
    }
    
    @Override
    public CompletableFuture<Boolean> createJail(String name, Location spawnLocation) {
        return jailManager.createOrUpdateJail(name, spawnLocation);
    }
    
    @Override
    public CompletableFuture<Boolean> removeJail(String name) {
        return jailManager.removeJail(name);
    }    
    @Override
    public boolean isJailed(OfflinePlayer player) {
        return isJailed(player.getUniqueId());
    }
    
    @Override
    public boolean isJailed(UUID uuid) {
        return prisonerManager.isJailed(uuid);
    }
    
    @Override
    public Prisoner getPrisoner(OfflinePlayer player) {
        return getPrisoner(player.getUniqueId());
    }
    
    @Override
    public Prisoner getPrisoner(UUID uuid) {
        return prisonerManager.getPrisoner(uuid);
    }
    
    @Override
    public Collection<Prisoner> getAllPrisoners() {
        return prisonerManager.getAllPrisoners();
    }
    
    @Override
    public Collection<Prisoner> getPrisonersByJail(String jailName) {
        return prisonerManager.getPrisonersByJail(jailName);
    }
    
    @Override
    public CompletableFuture<Boolean> jailPlayer(OfflinePlayer player, String jailName, String reason, String staff) {
        return prisonerManager.jailPlayer(player.getUniqueId(), jailName, reason, staff);
    }
    
    @Override
    public CompletableFuture<Boolean> jailPlayer(OfflinePlayer player, String jailName, String reason, String staff, long durationMillis) {
        return prisonerManager.jailPlayer(player.getUniqueId(), jailName, reason, staff, durationMillis);
    }
    
    @Override
    public CompletableFuture<Boolean> unjailPlayer(OfflinePlayer player, String staff) {
        return prisonerManager.unjailPlayer(player.getUniqueId(), staff);
    }
    
    @Override
    public CompletableFuture<Boolean> extendJailTime(OfflinePlayer player, long additionalMillis) {
        return prisonerManager.extendJailTime(player.getUniqueId(), additionalMillis);
    }    
    @Override
    public CompletableFuture<Boolean> setBail(OfflinePlayer player, double amount) {
        Prisoner prisoner = getPrisoner(player);
        if (prisoner == null) {
            return CompletableFuture.completedFuture(false);
        }
        
        prisoner.setBailAmount(amount);
        prisoner.setBailEnabled(amount > 0);
        
        
        return CompletableFuture.completedFuture(true);
    }
    
    @Override
    public CompletableFuture<Boolean> removeBail(OfflinePlayer player) {
        Prisoner prisoner = getPrisoner(player);
        if (prisoner == null) {
            return CompletableFuture.completedFuture(false);
        }
        
        prisoner.setBailAmount(null);
        prisoner.setBailEnabled(false);
        
        
        return CompletableFuture.completedFuture(true);
    }
    
    @Override
    public CompletableFuture<Boolean> payBail(OfflinePlayer prisoner, OfflinePlayer payer) {
        
        return CompletableFuture.completedFuture(false);
    }
    
    @Override
    public Collection<Prisoner> getPrisonersWithBail() {
        return prisonerManager.getPrisonersWithBail();
    }    
    @Override
    public FlagRegion getFlag(String name) {
        return jailManager.getFlag(name);
    }
    
    @Override
    public Collection<FlagRegion> getAllFlags() {
        return jailManager.getAllFlags();
    }
    
    @Override
    public boolean flagExists(String name) {
        return jailManager.flagExists(name);
    }
    
    @Override
    public CompletableFuture<Boolean> createFlag(String name, Location pos1, Location pos2) {
        return jailManager.createOrUpdateFlag(name, pos1, pos2);
    }
    
    @Override
    public CompletableFuture<Boolean> removeFlag(String name) {
        return jailManager.removeFlag(name);
    }    
    @Override
    public Jail findJailAtLocation(Location location) {
        return jailManager.findJailAtLocation(location);
    }
    
    @Override
    public boolean isLocationInFlag(Location location, String flagName) {
        FlagRegion flag = getFlag(flagName);
        return flag != null && flag.contains(location);
    }
    
    @Override
    public CompletableFuture<Boolean> linkJailToArea(String jailName, String areaRef) {
        return jailManager.linkJailToArea(jailName, areaRef);
    }
    
    @Override
    public CompletableFuture<Boolean> unlinkJail(String jailName) {
        return jailManager.unlinkJail(jailName);
    }    
    @Override
    public DevJailsStats getStats() {
        int totalJails = getAllJails().size();
        int totalPrisoners = getAllPrisoners().size();
        int totalFlags = getAllFlags().size();
        int prisonersWithBail = getPrisonersWithBail().size();
        String storageType = "Unknown"; 
        
        return new DevJailsStats(totalJails, totalPrisoners, totalFlags, prisonersWithBail, storageType);
    }
    
    @Override
    public boolean isBailSystemEnabled() {
        return vaultHook.isEnabled();
    }
    
    @Override
    public boolean isWorldEditAvailable() {
        return worldEditHook.isEnabled();
    }
    
    @Override
    public boolean isWorldGuardAvailable() {
        return worldGuardHook.isEnabled();
    }
    
    @Override
    public String getAPIVersion() {
        return API_VERSION;
    }
}