package br.com.devjails.integration;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Integração com WorldGuard para regiões
 */
public class WorldGuardHook {
    
    private final Logger logger;
    private WorldGuardPlugin worldGuardPlugin;
    private boolean enabled = false;
    
    public WorldGuardHook(Logger logger) {
        this.logger = logger;
    }
    
    /**
     * Inicializa a integração com WorldGuard
     */
    public boolean initialize() {
        try {
            worldGuardPlugin = (WorldGuardPlugin) Bukkit.getPluginManager().getPlugin("WorldGuard");
            
            if (worldGuardPlugin == null) {
                // Usar log mais conciso
                logger.info("WorldGuard not found - WG region binding not available");
                return false;
            }
            
            enabled = true;
            // Usar log mais conciso
            logger.info("WorldGuard integration enabled");
            return true;
        } catch (Exception e) {
            logger.warning("Error initializing WorldGuard: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Verifica se a integração está habilitada
     */
    public boolean isEnabled() {
        return enabled && worldGuardPlugin != null;
    }
    
    /**
     * Verifica se uma região existe
     */
    public boolean regionExists(World world, String regionName) {
        if (!isEnabled()) {
            return false;
        }
        
        try {
            RegionManager regionManager = WorldGuard.getInstance()
                .getPlatform()
                .getRegionContainer()
                .get(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(world));
            
            if (regionManager == null) {
                return false;
            }
            
            return regionManager.hasRegion(regionName);
        } catch (Exception e) {
            logger.warning("Error checking region " + regionName + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Verifica se uma localização está dentro de uma região específica
     */
    public boolean isLocationInRegion(Location location, String regionName) {
        if (!isEnabled()) {
            return false;
        }
        
        try {
            RegionManager regionManager = WorldGuard.getInstance()
                .getPlatform()
                .getRegionContainer()
                .get(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(location.getWorld()));
            
            if (regionManager == null) {
                return false;
            }
            
            ProtectedRegion region = regionManager.getRegion(regionName);
            if (region == null) {
                return false;
            }
            
            com.sk89q.worldedit.math.BlockVector3 point = com.sk89q.worldedit.math.BlockVector3.at(
                location.getBlockX(), 
                location.getBlockY(), 
                location.getBlockZ()
            );
            
            return region.contains(point);
        } catch (Exception e) {
            logger.warning("Error checking location in region " + regionName + ": " + e.getMessage());
            return false;
        }
    }
    
    public Set<String> getRegionsAtLocation(Location location) {
        if (!isEnabled()) {
            return Set.of();
        }
        
        try {
            RegionManager regionManager = WorldGuard.getInstance()
                .getPlatform()
                .getRegionContainer()
                .get(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(location.getWorld()));
            
            if (regionManager == null) {
                return Set.of();
            }
            
            com.sk89q.worldedit.math.BlockVector3 point = com.sk89q.worldedit.math.BlockVector3.at(
                location.getBlockX(), 
                location.getBlockY(), 
                location.getBlockZ()
            );
            
            return new HashSet<>(regionManager.getApplicableRegionsIDs(point));
        } catch (Exception e) {
            logger.warning("Error getting regions at location: " + e.getMessage());
            return Set.of();
        }
    }
    
    public RegionInfo getRegionInfo(World world, String regionName) {
        if (!isEnabled()) {
            return null;
        }
        
        try {
            RegionManager regionManager = WorldGuard.getInstance()
                .getPlatform()
                .getRegionContainer()
                .get(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(world));
            
            if (regionManager == null) {
                return null;
            }
            
            ProtectedRegion region = regionManager.getRegion(regionName);
            if (region == null) {
                return null;
            }
            
            com.sk89q.worldedit.math.BlockVector3 min = region.getMinimumPoint();
            com.sk89q.worldedit.math.BlockVector3 max = region.getMaximumPoint();
            
            Location minLoc = new Location(world, min.getX(), min.getY(), min.getZ());
            Location maxLoc = new Location(world, max.getX(), max.getY(), max.getZ());
            
            return new RegionInfo(regionName, region.getType().getName(), minLoc, maxLoc);
        } catch (Exception e) {
            logger.warning("Error getting region info " + regionName + ": " + e.getMessage());
            return null;
        }
    }
    
    public Location getRegionCenter(World world, String regionName) {
        if (!isEnabled()) {
            return null;
        }
        
        RegionInfo info = getRegionInfo(world, regionName);
        if (info == null) {
            return null;
        }
        
        Location min = info.getMinLocation();
        Location max = info.getMaxLocation();
        
        double centerX = (min.getX() + max.getX()) / 2.0;
        double centerY = (min.getY() + max.getY()) / 2.0;
        double centerZ = (min.getZ() + max.getZ()) / 2.0;
        
        return new Location(world, centerX, centerY, centerZ);
    }
    
    /**
     * Lista todas as regiões de um mundo
     */
    public Set<String> getRegionNames(World world) {
        if (!isEnabled()) {
            return Set.of();
        }
        
        try {
            RegionManager regionManager = WorldGuard.getInstance()
                .getPlatform()
                .getRegionContainer()
                .get(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(world));
            
            if (regionManager == null) {
                return Set.of();
            }
            
            return regionManager.getRegions().keySet();
        } catch (Exception e) {
            logger.warning("Error listing regions in world " + world.getName() + ": " + e.getMessage());
            return Set.of();
        }
    }
    
    /**
     * Informações de uma região WorldGuard
     */
    public static class RegionInfo {
        private final String name;
        private final String type;
        private final Location minLocation;
        private final Location maxLocation;
        
        public RegionInfo(String name, String type, Location minLocation, Location maxLocation) {
            this.name = name;
            this.type = type;
            this.minLocation = minLocation;
            this.maxLocation = maxLocation;
        }
        
        public String getName() {
            return name;
        }
        
        public String getType() {
            return type;
        }
        
        public Location getMinLocation() {
            return minLocation;
        }
        
        public Location getMaxLocation() {
            return maxLocation;
        }
        
        public int getVolume() {
            int dx = maxLocation.getBlockX() - minLocation.getBlockX() + 1;
            int dy = maxLocation.getBlockY() - minLocation.getBlockY() + 1;
            int dz = maxLocation.getBlockZ() - minLocation.getBlockZ() + 1;
            return dx * dy * dz;
        }
        
        @Override
        public String toString() {
            return "RegionInfo{" +
                    "name='" + name + '\'' +
                    ", type='" + type + '\'' +
                    ", volume=" + getVolume() +
                    '}';
        }
    }
}