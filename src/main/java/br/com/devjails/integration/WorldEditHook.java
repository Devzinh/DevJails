package br.com.devjails.integration;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.SessionManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.logging.Logger;

/**
 * Integração com WorldEdit para seleções
 */
public class WorldEditHook {
    
    private final Logger logger;
    private WorldEditPlugin worldEditPlugin;
    private boolean enabled = false;
    
    public WorldEditHook(Logger logger) {
        this.logger = logger;
    }
    
    /**
     * Inicializa a integração com WorldEdit
     */
    public boolean initialize() {
        try {
            worldEditPlugin = (WorldEditPlugin) Bukkit.getPluginManager().getPlugin("WorldEdit");
            
            if (worldEditPlugin == null) {
                // Usar log mais conciso
                logger.info("WorldEdit not found - using built-in selection system");
                return false;
            }
            
            enabled = true;
            // Usar log mais conciso
                logger.info("WorldEdit integration enabled");
            return true;
        } catch (Exception e) {
            logger.warning("Error initializing WorldEdit: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Verifica se a integração está habilitada
     */
    public boolean isEnabled() {
        return enabled && worldEditPlugin != null;
    }
    
    /**
     * Obtém a seleção atual do jogador
     * @return array com [pos1, pos2] ou null se não houver seleção
     */
    public Location[] getSelection(Player player) {
        if (!isEnabled()) {
            return null;
        }
        
        try {
            SessionManager sessionManager = WorldEdit.getInstance().getSessionManager();
            LocalSession session = sessionManager.get(BukkitAdapter.adapt(player));
            
            if (session == null) {
                return null;
            }
            
            Region selection = session.getSelection(BukkitAdapter.adapt(player.getWorld()));
            
            if (selection == null) {
                return null;
            }
            
            BlockVector3 min = selection.getMinimumPoint();
            BlockVector3 max = selection.getMaximumPoint();
            
            Location pos1 = new Location(player.getWorld(), min.getX(), min.getY(), min.getZ());
            Location pos2 = new Location(player.getWorld(), max.getX(), max.getY(), max.getZ());
            
            return new Location[]{pos1, pos2};
            
        } catch (IncompleteRegionException e) {

            return null;
        } catch (Exception e) {
            logger.warning("Error getting WorldEdit selection for " + player.getName() + ": " + e.getMessage());
            return null;
        }
    }
    
    
    public boolean hasCompleteSelection(Player player) {
        Location[] selection = getSelection(player);
        return selection != null && selection.length == 2 && selection[0] != null && selection[1] != null;
    }
    
    public boolean setSelection(Player player, Location pos1, Location pos2) {
        if (!isEnabled()) {
            return false;
        }
        
        try {
            SessionManager sessionManager = WorldEdit.getInstance().getSessionManager();
            LocalSession session = sessionManager.get(BukkitAdapter.adapt(player));
            
            if (session == null) {
                return false;
            }
            
            BlockVector3 min = BlockVector3.at(pos1.getBlockX(), pos1.getBlockY(), pos1.getBlockZ());
            BlockVector3 max = BlockVector3.at(pos2.getBlockX(), pos2.getBlockY(), pos2.getBlockZ());
            
            com.sk89q.worldedit.regions.CuboidRegion region = 
                new com.sk89q.worldedit.regions.CuboidRegion(
                    BukkitAdapter.adapt(pos1.getWorld()), 
                    min, 
                    max
                );

            com.sk89q.worldedit.regions.selector.CuboidRegionSelector selector = 
                new com.sk89q.worldedit.regions.selector.CuboidRegionSelector(
                    BukkitAdapter.adapt(pos1.getWorld()),
                    min,
                    max
                );
            
            session.setRegionSelector(BukkitAdapter.adapt(pos1.getWorld()), selector);
            session.dispatchCUISelection(BukkitAdapter.adapt(player));
            return true;
            
        } catch (Exception e) {
            logger.warning("Error setting WorldEdit selection for " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }
    
    
    public boolean clearSelection(Player player) {
        if (!isEnabled()) {
            return false;
        }
        
        try {
            SessionManager sessionManager = WorldEdit.getInstance().getSessionManager();
            LocalSession session = sessionManager.get(BukkitAdapter.adapt(player));
            
            if (session == null) {
                return false;
            }
            
            session.getRegionSelector(BukkitAdapter.adapt(player.getWorld())).clear();
            session.dispatchCUISelection(BukkitAdapter.adapt(player));
            return true;
            
        } catch (Exception e) {
            logger.warning("Error clearing WorldEdit selection for " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }
    
    public int getSelectionVolume(Player player) {
        if (!isEnabled()) {
            return 0;
        }
        
        try {
            SessionManager sessionManager = WorldEdit.getInstance().getSessionManager();
            LocalSession session = sessionManager.get(BukkitAdapter.adapt(player));
            
            if (session == null) {
                return 0;
            }
            
            Region selection = session.getSelection(BukkitAdapter.adapt(player.getWorld()));
            
            if (selection == null) {
                return 0;
            }
            
            return (int) selection.getVolume();
            
        } catch (Exception e) {
            return 0;
        }
    }
    
    
    public boolean isInSelection(Player player, Location location) {
        if (!isEnabled()) {
            return false;
        }
        
        try {
            SessionManager sessionManager = WorldEdit.getInstance().getSessionManager();
            LocalSession session = sessionManager.get(BukkitAdapter.adapt(player));
            
            if (session == null) {
                return false;
            }
            
            Region selection = session.getSelection(BukkitAdapter.adapt(location.getWorld()));
            
            if (selection == null) {
                return false;
            }
            
            BlockVector3 point = BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());
            return selection.contains(point);
            
        } catch (Exception e) {
            return false;
        }
    }
}