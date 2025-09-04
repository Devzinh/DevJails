package br.com.devjails.storage;

import br.com.devjails.flag.FlagRegion;
import br.com.devjails.jail.Jail;
import br.com.devjails.prisoner.Prisoner;
import br.com.devjails.util.Tasks;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;


public class YamlStorage extends AbstractStorage {
    
    private File jailsFile;
    private File prisonersFile;
    private File flagsFile;
    
    private YamlConfiguration jailsConfig;
    private YamlConfiguration prisonersConfig;
    private YamlConfiguration flagsConfig;

    public YamlStorage(File dataFolder, Logger logger) {
        super(dataFolder, logger);
    }
    
    @Override
    protected String getStorageType() {
        return "YamlStorage";
    }
    
    @Override
    protected boolean doInitialize() throws Exception {
        jailsFile = new File(dataFolder, "jails.yml");
        prisonersFile = new File(dataFolder, "prisoners.yml");
        flagsFile = new File(dataFolder, "flags.yml");
        jailsConfig = YamlConfiguration.loadConfiguration(jailsFile);
        prisonersConfig = YamlConfiguration.loadConfiguration(prisonersFile);
        flagsConfig = YamlConfiguration.loadConfiguration(flagsFile);
        return true;
    }
    
    @Override
    protected void doShutdown() throws Exception {
        // YAML não precisa de cleanup especial
    }
    
    @Override
    protected boolean doHealthCheck() throws Exception {
        return jailsFile != null && prisonersFile != null && flagsFile != null &&
               jailsConfig != null && prisonersConfig != null && flagsConfig != null;
    }
    
    @Override
    protected List<String> getAllJailNames() throws Exception {
        List<String> names = new ArrayList<>();
        ConfigurationSection jailsSection = jailsConfig.getConfigurationSection("jails");
        if (jailsSection != null) {
            names.addAll(jailsSection.getKeys(false));
        }
        return names;
    }
    
    @Override
    protected List<UUID> getAllPrisonerUuids() throws Exception {
        List<UUID> uuids = new ArrayList<>();
        ConfigurationSection prisonersSection = prisonersConfig.getConfigurationSection("prisoners");
        if (prisonersSection != null) {
            for (String uuidStr : prisonersSection.getKeys(false)) {
                try {
                    uuids.add(UUID.fromString(uuidStr));
                } catch (IllegalArgumentException e) {
                    logger.warning("Invalid UUID found: " + uuidStr);
                }
            }
        }
        return uuids;
    }
    
    @Override
    protected List<String> getAllFlagNames() throws Exception {
        List<String> names = new ArrayList<>();
        ConfigurationSection flagsSection = flagsConfig.getConfigurationSection("flags");
        if (flagsSection != null) {
            names.addAll(flagsSection.getKeys(false));
        }
        return names;
    }    
    
    @Override
    public CompletableFuture<Boolean> saveJail(Jail jail) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ConfigurationSection section = jailsConfig.createSection("jails." + jail.getName());
                
                Location spawn = jail.getSpawnLocation();
                section.set("world", spawn.getWorld().getName());
                section.set("x", spawn.getX());
                section.set("y", spawn.getY());
                section.set("z", spawn.getZ());
                section.set("yaw", spawn.getYaw());
                section.set("pitch", spawn.getPitch());
                
                section.set("areaBinding", jail.getAreaBinding().getValue());
                section.set("areaRef", jail.getAreaRef());
                
                jailsConfig.save(jailsFile);
                return true;
            } catch (IOException e) {
                logger.severe("Error saving jail " + jail.getName() + ": " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        });
    }
    
    @Override
    public CompletableFuture<Jail> loadJail(String name) {
        return CompletableFuture.supplyAsync(() -> {
            ConfigurationSection section = jailsConfig.getConfigurationSection("jails." + name);
            if (section == null) {
                return null;
            }
            
            try {
                String worldName = section.getString("world");
                double x = section.getDouble("x");
                double y = section.getDouble("y");
                double z = section.getDouble("z");
                float yaw = (float) section.getDouble("yaw");
                float pitch = (float) section.getDouble("pitch");
                
                Location spawn = new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
                
                Jail.AreaBinding areaBinding = Jail.AreaBinding.fromString(
                    section.getString("areaBinding", "none")
                );
                String areaRef = section.getString("areaRef");
                
                return new Jail(name, spawn, areaBinding, areaRef);
            } catch (Exception e) {
                logger.severe("Error loading jail " + name + ": " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        });
    }
    
    @Override
    public CompletableFuture<Boolean> removeJail(String name) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                jailsConfig.set("jails." + name, null);
                jailsConfig.save(jailsFile);
                return true;
            } catch (IOException e) {
                logger.severe("Error removing jail " + name + ": " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        });
    }
    
    
    @Override
    public CompletableFuture<Boolean> savePrisoner(Prisoner prisoner) {
        return Tasks.asyncThenSync(() -> {
            try {
                String uuidStr = prisoner.getUuid().toString();
                ConfigurationSection section = prisonersConfig.createSection("prisoners." + uuidStr);
                
                section.set("jailName", prisoner.getJailName());
                section.set("reason", prisoner.getReason());
                section.set("staff", prisoner.getStaff());
                section.set("startEpoch", prisoner.getStartEpoch());
                section.set("endEpoch", prisoner.getEndEpoch());
                section.set("bailAmount", prisoner.getBailAmount());
                section.set("bailEnabled", prisoner.isBailEnabled());
                section.set("handcuffed", prisoner.isHandcuffed());
                section.set("postReleaseSpawnChoice", prisoner.getPostReleaseSpawnChoice().getValue());
                if (prisoner.getOriginalLocation() != null) {
                    Location loc = prisoner.getOriginalLocation();
                    ConfigurationSection locSection = section.createSection("originalLocation");
                    locSection.set("world", loc.getWorld().getName());
                    locSection.set("x", loc.getX());
                    locSection.set("y", loc.getY());
                    locSection.set("z", loc.getZ());
                    locSection.set("yaw", loc.getYaw());
                    locSection.set("pitch", loc.getPitch());
                }
                
                prisonersConfig.save(prisonersFile);
                return true;
            } catch (IOException e) {
                logger.severe("Error saving prisoner " + prisoner.getUuid() + ": " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }, null).thenApply(result -> result);
    }
    
    @Override
    public CompletableFuture<Prisoner> loadPrisoner(UUID uuid) {
        return Tasks.asyncThenSync(() -> {
            String uuidStr = uuid.toString();
            ConfigurationSection section = prisonersConfig.getConfigurationSection("prisoners." + uuidStr);
            if (section == null) {
                return null;
            }
            
            try {
                String jailName = section.getString("jailName");
                String reason = section.getString("reason");
                String staff = section.getString("staff");
                long startEpoch = section.getLong("startEpoch");
                Long endEpoch = section.contains("endEpoch") && section.get("endEpoch") != null 
                    ? section.getLong("endEpoch") : null;
                
                Prisoner prisoner = new Prisoner(uuid, jailName, reason, staff);
                prisoner.setStartEpoch(startEpoch);
                prisoner.setEndEpoch(endEpoch);
                
                if (section.contains("bailAmount") && section.get("bailAmount") != null) {
                    prisoner.setBailAmount(section.getDouble("bailAmount"));
                }
                prisoner.setBailEnabled(section.getBoolean("bailEnabled", false));
                prisoner.setHandcuffed(section.getBoolean("handcuffed", false));
                
                String spawnChoiceStr = section.getString("postReleaseSpawnChoice", "world_spawn");
                prisoner.setPostReleaseSpawnChoice(Prisoner.PostReleaseSpawnChoice.fromString(spawnChoiceStr));
                if (section.contains("originalLocation")) {
                    ConfigurationSection locSection = section.getConfigurationSection("originalLocation");
                    String worldName = locSection.getString("world");
                    double x = locSection.getDouble("x");
                    double y = locSection.getDouble("y");
                    double z = locSection.getDouble("z");
                    float yaw = (float) locSection.getDouble("yaw");
                    float pitch = (float) locSection.getDouble("pitch");
                    
                    Location originalLocation = new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
                    prisoner.setOriginalLocation(originalLocation);
                }
                
                return prisoner;
            } catch (Exception e) {
                logger.severe("Error loading prisoner " + uuid + ": " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        }, null).thenApply(result -> result);
    }
    
    @Override
    public CompletableFuture<Boolean> removePrisoner(UUID uuid) {
        return Tasks.asyncThenSync(() -> {
            try {
                String uuidStr = uuid.toString();
                prisonersConfig.set("prisoners." + uuidStr, null);
                prisonersConfig.save(prisonersFile);
                return true;
            } catch (IOException e) {
                logger.severe("Error removing prisoner " + uuid + ": " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }, null).thenApply(result -> result);
    }
    

    
    @Override
    public CompletableFuture<List<Prisoner>> loadPrisonersByJail(String jailName) {
        return loadAllPrisoners().thenApply(prisoners -> {
            List<Prisoner> result = new ArrayList<>();
            for (Prisoner prisoner : prisoners) {
                if (jailName.equals(prisoner.getJailName())) {
                    result.add(prisoner);
                }
            }
            return result;
        });
    }    
    
    @Override
    public CompletableFuture<Boolean> saveFlag(FlagRegion flag) {
        return Tasks.asyncThenSync(() -> {
            try {
                ConfigurationSection section = flagsConfig.createSection("flags." + flag.getName());
                
                section.set("worldName", flag.getWorldName());
                
                FlagRegion.Vector3 min = flag.getRegion().getMin();
                FlagRegion.Vector3 max = flag.getRegion().getMax();
                
                section.set("min.x", min.getX());
                section.set("min.y", min.getY());
                section.set("min.z", min.getZ());
                
                section.set("max.x", max.getX());
                section.set("max.y", max.getY());
                section.set("max.z", max.getZ());
                
                flagsConfig.save(flagsFile);
                return true;
            } catch (IOException e) {
                logger.severe("Error saving flag " + flag.getName() + ": " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }, null).thenApply(result -> result);
    }
    
    @Override
    public CompletableFuture<FlagRegion> loadFlag(String name) {
        return Tasks.asyncThenSync(() -> {
            ConfigurationSection section = flagsConfig.getConfigurationSection("flags." + name);
            if (section == null) {
                return null;
            }
            
            try {
                String worldName = section.getString("worldName");
                
                int minX = section.getInt("min.x");
                int minY = section.getInt("min.y");
                int minZ = section.getInt("min.z");
                
                int maxX = section.getInt("max.x");
                int maxY = section.getInt("max.y");
                int maxZ = section.getInt("max.z");
                
                FlagRegion.Vector3 min = new FlagRegion.Vector3(minX, minY, minZ);
                FlagRegion.Vector3 max = new FlagRegion.Vector3(maxX, maxY, maxZ);
                FlagRegion.CuboidRegion region = new FlagRegion.CuboidRegion(min, max);
                
                return new FlagRegion(name, worldName, region);
            } catch (Exception e) {
                logger.severe("Error loading flag " + name + ": " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        }, null).thenApply(result -> result);
    }
    
    @Override
    public CompletableFuture<Boolean> removeFlag(String name) {
        return Tasks.asyncThenSync(() -> {
            try {
                flagsConfig.set("flags." + name, null);
                flagsConfig.save(flagsFile);
                return true;
            } catch (IOException e) {
                logger.severe("Error removing flag " + name + ": " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }, null).thenApply(result -> result);
    }
    
    
}