package br.com.devjails.flag;

import br.com.devjails.message.MessageService;
import br.com.devjails.util.Tasks;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Manager para gerenciar visualização e preview de flags
 */
public class FlagManager {
    
    private final MessageService messageService;
    private final Logger logger;

    private final Map<UUID, BukkitTask> activePreviewTasks;
    
    public FlagManager(MessageService messageService, Logger logger) {
        this.messageService = messageService;
        this.logger = logger;
        this.activePreviewTasks = new HashMap<>();
    }
    
    /**
     * Inicia preview de uma flag com partículas
     */
    public void startFlagPreview(Player player, FlagRegion flag) {
        stopFlagPreview(player); 
        
        BukkitTask task = Tasks.syncRepeating(() -> {
            showFlagParticles(flag);
        }, 0L, 10L); 
        
        activePreviewTasks.put(player.getUniqueId(), task);

        Tasks.syncDelayed(() -> {
            stopFlagPreview(player);
            messageService.sendMessage(player, "flag_preview_end", "{flag}", flag.getName());
        }, 200L); 
    }
    
    /**
     * Para o preview de flag de um jogador
     */
    public void stopFlagPreview(Player player) {
        BukkitTask task = activePreviewTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }
    
    /**
     * Para todos os previews ativos
     */
    public void stopAllPreviews() {
        activePreviewTasks.values().forEach(BukkitTask::cancel);
        activePreviewTasks.clear();
    }
    
    /**
     * Mostra partículas delimitando a flag
     */
    private void showFlagParticles(FlagRegion flag) {
        World world = flag.getWorld();
        if (world == null) {
            return;
        }
        
        FlagRegion.Vector3 min = flag.getRegion().getMin();
        FlagRegion.Vector3 max = flag.getRegion().getMax();

        showCuboidOutline(world, min, max);
    }
    
    /**
     * Mostra contorno de um cuboid com partículas
     */
    private void showCuboidOutline(World world, FlagRegion.Vector3 min, FlagRegion.Vector3 max) {
        int minX = min.getX();
        int minY = min.getY();
        int minZ = min.getZ();
        int maxX = max.getX();
        int maxY = max.getY();
        int maxZ = max.getZ();

        showVerticalLine(world, minX, minY, maxY, minZ);
        showVerticalLine(world, maxX, minY, maxY, minZ);
        showVerticalLine(world, minX, minY, maxY, maxZ);
        showVerticalLine(world, maxX, minY, maxY, maxZ);

        showHorizontalLine(world, minX, maxX, minY, minZ);
        showHorizontalLine(world, minX, maxX, minY, maxZ);
        showHorizontalLine(world, minX, maxX, maxY, minZ);
        showHorizontalLine(world, minX, maxX, maxY, maxZ);

        showDepthLine(world, minX, minY, minZ, maxZ);
        showDepthLine(world, maxX, minY, minZ, maxZ);
        showDepthLine(world, minX, maxY, minZ, maxZ);
        showDepthLine(world, maxX, maxY, minZ, maxZ); 
    }
    
    /**
     * Mostra linha vertical de partículas
     */
    private void showVerticalLine(World world, int x, int minY, int maxY, int z) {
        for (int y = minY; y <= maxY; y += 2) {
            Location loc = new Location(world, x + 0.5, y + 0.5, z + 0.5);
            world.spawnParticle(Particle.FLAME, loc, 1, 0, 0, 0, 0);
        }
    }
    
    /**
     * Mostra linha horizontal de partículas
     */
    private void showHorizontalLine(World world, int minX, int maxX, int y, int z) {
        for (int x = minX; x <= maxX; x += 2) {
            Location loc = new Location(world, x + 0.5, y + 0.5, z + 0.5);
            world.spawnParticle(Particle.FLAME, loc, 1, 0, 0, 0, 0);
        }
    }
    
    /**
     * Mostra linha de profundidade de partículas
     */
    private void showDepthLine(World world, int x, int y, int minZ, int maxZ) {
        for (int z = minZ; z <= maxZ; z += 2) {
            Location loc = new Location(world, x + 0.5, y + 0.5, z + 0.5);
            world.spawnParticle(Particle.FLAME, loc, 1, 0, 0, 0, 0);
        }
    }
    
    /**
     * Verifica se uma flag contém uma localização
     */
    public boolean isLocationInFlag(FlagRegion flag, Location location) {
        return flag.contains(location);
    }
    
    /**
     * Calcula a distância de uma localização ao centro de uma flag
     */
    public double getDistanceToFlagCenter(FlagRegion flag, Location location) {
        Location center = flag.getCenter();
        if (center == null || !center.getWorld().equals(location.getWorld())) {
            return Double.MAX_VALUE;
        }
        
        return location.distance(center);
    }
    
    /**
     * Encontra a flag mais próxima de uma localização
     */
    public FlagRegion findNearestFlag(Location location, Iterable<FlagRegion> flags) {
        FlagRegion nearest = null;
        double minDistance = Double.MAX_VALUE;
        
        for (FlagRegion flag : flags) {
            if (!flag.getWorldName().equals(location.getWorld().getName())) {
                continue;
            }
            
            double distance = getDistanceToFlagCenter(flag, location);
            if (distance < minDistance) {
                minDistance = distance;
                nearest = flag;
            }
        }
        
        return nearest;
    }
    
    /**
     * Verifica se duas flags se sobrepõem
     */
    public boolean flagsOverlap(FlagRegion flag1, FlagRegion flag2) {
        if (!flag1.getWorldName().equals(flag2.getWorldName())) {
            return false;
        }
        
        FlagRegion.Vector3 min1 = flag1.getRegion().getMin();
        FlagRegion.Vector3 max1 = flag1.getRegion().getMax();
        FlagRegion.Vector3 min2 = flag2.getRegion().getMin();
        FlagRegion.Vector3 max2 = flag2.getRegion().getMax();
        boolean overlapX = max1.getX() >= min2.getX() && min1.getX() <= max2.getX();
        boolean overlapY = max1.getY() >= min2.getY() && min1.getY() <= max2.getY();
        boolean overlapZ = max1.getZ() >= min2.getZ() && min1.getZ() <= max2.getZ();
        
        return overlapX && overlapY && overlapZ;
    }
    
    /**
     * Calcula a área de sobreposição entre duas flags
     */
    public int getOverlapVolume(FlagRegion flag1, FlagRegion flag2) {
        if (!flagsOverlap(flag1, flag2)) {
            return 0;
        }
        
        FlagRegion.Vector3 min1 = flag1.getRegion().getMin();
        FlagRegion.Vector3 max1 = flag1.getRegion().getMax();
        FlagRegion.Vector3 min2 = flag2.getRegion().getMin();
        FlagRegion.Vector3 max2 = flag2.getRegion().getMax();

        int overlapMinX = Math.max(min1.getX(), min2.getX());
        int overlapMaxX = Math.min(max1.getX(), max2.getX());
        int overlapMinY = Math.max(min1.getY(), min2.getY());
        int overlapMaxY = Math.min(max1.getY(), max2.getY());
        int overlapMinZ = Math.max(min1.getZ(), min2.getZ());
        int overlapMaxZ = Math.min(max1.getZ(), max2.getZ());
        
        if (overlapMinX > overlapMaxX || overlapMinY > overlapMaxY || overlapMinZ > overlapMaxZ) {
            return 0;
        }
        
        return (overlapMaxX - overlapMinX + 1) *
               (overlapMaxY - overlapMinY + 1) *
               (overlapMaxZ - overlapMinZ + 1);
    }
    
    public FlagInfo getFlagInfo(FlagRegion flag) {
        return new FlagInfo(
            flag.getName(),
            flag.getWorldName(),
            flag.getRegion().getMin(),
            flag.getRegion().getMax(),
            flag.getVolume(),
            flag.getCenter()
        );
    }
    
    /**
     * Limpa recursos quando jogador sai
     */
    public void onPlayerQuit(Player player) {
        stopFlagPreview(player);
    }
    
    /**
     * Informações detalhadas de uma flag
     */
    public static class FlagInfo {
        private final String name;
        private final String worldName;
        private final FlagRegion.Vector3 min;
        private final FlagRegion.Vector3 max;
        private final int volume;
        private final Location center;
        
        public FlagInfo(String name, String worldName, FlagRegion.Vector3 min, 
                       FlagRegion.Vector3 max, int volume, Location center) {
            this.name = name;
            this.worldName = worldName;
            this.min = min;
            this.max = max;
            this.volume = volume;
            this.center = center;
        }
        
        public String getName() { return name; }
        public String getWorldName() { return worldName; }
        public FlagRegion.Vector3 getMin() { return min; }
        public FlagRegion.Vector3 getMax() { return max; }
        public int getVolume() { return volume; }
        public Location getCenter() { return center; }
        
        @Override
        public String toString() {
            return String.format("FlagInfo{name='%s', world='%s', volume=%d, min=%s, max=%s}",
                    name, worldName, volume, min, max);
        }
    }
}