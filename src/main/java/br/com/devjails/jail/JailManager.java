package br.com.devjails.jail;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.bukkit.Location;

import br.com.devjails.flag.FlagRegion;
import br.com.devjails.integration.WorldGuardHook;
import br.com.devjails.storage.Storage;


public class JailManager {
    
    private final Storage storage;
    private final WorldGuardHook worldGuardHook;
    private final Logger logger;
    
    private final Map<String, Jail> jails;
    private final Map<String, FlagRegion> flags;
    
    public JailManager(Storage storage, WorldGuardHook worldGuardHook, Logger logger) {
        this.storage = storage;
        this.worldGuardHook = worldGuardHook;
        this.logger = logger;
        this.jails = new ConcurrentHashMap<>();
        this.flags = new ConcurrentHashMap<>();
    }
    
    
    public CompletableFuture<Boolean> initialize() {
        return CompletableFuture.allOf(
            loadAllJails(),
            loadAllFlags()
        ).thenApply(v -> {
            // Usar log mais conciso
            if (jails.size() > 0 || flags.size() > 0) {
                logger.info("JailManager initialized - " + jails.size() + " jails, " + flags.size() + " flags");
            }
            return true;
        }).exceptionally(ex -> {
            logger.severe("Error initializing JailManager: " + ex.getMessage());
            ex.printStackTrace();
            return false;
        });
    }    
    
    public CompletableFuture<Boolean> createOrUpdateJail(String name, Location spawnLocation) {
        Jail jail = jails.get(name.toLowerCase());
        
        if (jail == null) {
            jail = new Jail(name, spawnLocation);
        } else {
            jail.setSpawnLocation(spawnLocation);
        }
        
        final Jail finalJail = jail;
        
        return storage.saveJail(jail).thenApply(success -> {
            if (success) {
                jails.put(name.toLowerCase(), finalJail);
                // Usar log mais conciso
                logger.info("Jail " + name + " saved");
            }
            return success;
        });
    }
    
    
    public CompletableFuture<Boolean> removeJail(String name) {
        return storage.removeJail(name).thenApply(success -> {
            if (success) {
                jails.remove(name.toLowerCase());
                // Usar log mais conciso
                logger.info("Jail " + name + " removed");
            }
            return success;
        });
    }
    
    public Jail getJail(String name) {
        return jails.get(name.toLowerCase());
    }
    
    
    public boolean jailExists(String name) {
        return jails.containsKey(name.toLowerCase());
    }
    
    public Collection<Jail> getAllJails() {
        return jails.values();
    }
    
    public Collection<String> getJailNames() {
        return jails.values().stream()
                .map(Jail::getName)
                .toList();
    }    
    
    public CompletableFuture<Boolean> createOrUpdateFlag(String name, Location pos1, Location pos2) {
        FlagRegion flag = new FlagRegion(name, pos1, pos2);
        
        return storage.saveFlag(flag).thenApply(success -> {
            if (success) {
                flags.put(name.toLowerCase(), flag);
                // Usar log mais conciso
                logger.info("Flag " + name + " saved");
            }
            return success;
        });
    }
    
    
    public CompletableFuture<Boolean> removeFlag(String name) {
        return storage.removeFlag(name).thenApply(success -> {
            if (success) {
                flags.remove(name.toLowerCase());

                jails.values().forEach(jail -> {
                    if (jail.getAreaBinding() == Jail.AreaBinding.FLAG && 
                        name.equalsIgnoreCase(jail.getAreaRef())) {
                        jail.setAreaBinding(Jail.AreaBinding.NONE);
                        jail.setAreaRef(null);
                        storage.saveJail(jail);
                    }
                });
                
                // Usar log mais conciso
                logger.info("Flag " + name + " removed");
            }
            return success;
        });
    }
    
    public FlagRegion getFlag(String name) {
        return flags.get(name.toLowerCase());
    }
    
    
    public boolean flagExists(String name) {
        return flags.containsKey(name.toLowerCase());
    }
    
    public Collection<FlagRegion> getAllFlags() {
        return flags.values();
    }
    
    public Collection<String> getFlagNames() {
        return flags.values().stream()
                .map(FlagRegion::getName)
                .toList();
    }    
    
    public CompletableFuture<Boolean> linkJailToArea(String jailName, String areaRef) {
        Jail jail = getJail(jailName);
        if (jail == null) {
            return CompletableFuture.completedFuture(false);
        }

        Jail.AreaBinding areaBinding;
        String actualRef;
        
        if (areaRef.startsWith("wg:")) {

            actualRef = areaRef.substring(3);
            areaBinding = Jail.AreaBinding.WORLDGUARD_REGION;
            if (!worldGuardHook.isEnabled()) {
                logger.warning("Attempt to link WG region but WorldGuard is not available");
                return CompletableFuture.completedFuture(false);
            }
            
            if (!worldGuardHook.regionExists(jail.getSpawnLocation().getWorld(), actualRef)) {
                logger.warning("WorldGuard region " + actualRef + " does not exist");
                return CompletableFuture.completedFuture(false);
            }
        } else if (areaRef.startsWith("flag:")) {

            actualRef = areaRef.substring(5);
            areaBinding = Jail.AreaBinding.FLAG;
            
            if (!flagExists(actualRef)) {
                logger.warning("Flag " + actualRef + " does not exist");
                return CompletableFuture.completedFuture(false);
            }
        } else {

            actualRef = areaRef;
            areaBinding = Jail.AreaBinding.FLAG;
            
            if (!flagExists(actualRef)) {
                logger.warning("Flag " + actualRef + " does not exist");
                return CompletableFuture.completedFuture(false);
            }
        }

        jail.setAreaBinding(areaBinding);
        jail.setAreaRef(actualRef);
        
        return storage.saveJail(jail).thenApply(success -> {
            if (success) {
                // Usar log mais conciso
                logger.info("Jail " + jailName + " linked to area");
            }
            return success;
        });
    }
    
    
    public CompletableFuture<Boolean> unlinkJail(String jailName) {
        Jail jail = getJail(jailName);
        if (jail == null) {
            return CompletableFuture.completedFuture(false);
        }
        
        jail.setAreaBinding(Jail.AreaBinding.NONE);
        jail.setAreaRef(null);
        
        return storage.saveJail(jail).thenApply(success -> {
            if (success) {
                // Usar log mais conciso
                logger.info("Jail " + jailName + " unlinked from area");
            }
            return success;
        });
    }    
    
    public boolean isLocationInJailArea(Jail jail, Location location) {
        if (!jail.hasArea()) {
            return false;
        }
        
        switch (jail.getAreaBinding()) {
            case FLAG:
                FlagRegion flag = getFlag(jail.getAreaRef());
                return flag != null && flag.contains(location);
                
            case WORLDGUARD_REGION:
                if (worldGuardHook.isEnabled()) {
                    return worldGuardHook.isLocationInRegion(location, jail.getAreaRef());
                }
                return false;
                
            default:
                return false;
        }
    }
    
    
    public Jail findJailAtLocation(Location location) {
        return jails.values().stream()
                .filter(jail -> isLocationInJailArea(jail, location))
                .findFirst()
                .orElse(null);
    }    
    
    private CompletableFuture<Void> loadAllJails() {
        return storage.loadAllJails().thenAccept(jailList -> {
            jails.clear();
            for (Jail jail : jailList) {
                jails.put(jail.getName().toLowerCase(), jail);
            }
        });
    }
    
    private CompletableFuture<Void> loadAllFlags() {
        return storage.loadAllFlags().thenAccept(flagList -> {
            flags.clear();
            for (FlagRegion flag : flagList) {
                flags.put(flag.getName().toLowerCase(), flag);
            }
        });
    }
    
    
    public CompletableFuture<Boolean> reload() {
        return initialize();
    }
}