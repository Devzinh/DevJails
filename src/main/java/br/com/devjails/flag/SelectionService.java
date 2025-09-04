package br.com.devjails.flag;

import br.com.devjails.integration.WorldEditHook;
import br.com.devjails.message.MessageService;
import br.com.devjails.util.Text;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Serviço para gerenciar seleções de área
 * Usa WorldEdit se disponível, senão usa sistema próprio
 */
public class SelectionService {
    
    private final WorldEditHook worldEditHook;
    private final MessageService messageService;
    private final Logger logger;

    private final Map<UUID, Selection> playerSelections;
    
    public SelectionService(WorldEditHook worldEditHook, MessageService messageService, Logger logger) {
        this.worldEditHook = worldEditHook;
        this.messageService = messageService;
        this.logger = logger;
        this.playerSelections = new HashMap<>();
    }
    
    /**
     * Obtém a seleção atual do jogador
     * @return array com [pos1, pos2] ou null se não houver seleção completa
     */
    public Location[] getPlayerSelection(Player player) {

        if (worldEditHook.isEnabled()) {
            Location[] weSelection = worldEditHook.getSelection(player);
            if (weSelection != null) {
                return weSelection;
            }
        }

        Selection selection = playerSelections.get(player.getUniqueId());
        if (selection != null && selection.isComplete()) {
            return new Location[]{selection.getPos1(), selection.getPos2()};
        }
        
        return null;
    }
    
    
    public boolean hasCompleteSelection(Player player) {
        Location[] selection = getPlayerSelection(player);
        return selection != null && selection.length == 2 && 
               selection[0] != null && selection[1] != null;
    }
    
    public void setPos1(Player player, Location location) {
        UUID uuid = player.getUniqueId();
        Selection selection = playerSelections.computeIfAbsent(uuid, k -> new Selection());
        selection.setPos1(location);
        
        messageService.sendMessage(player, "selection_pos1", 
            "{x}", location.getBlockX(),
            "{y}", location.getBlockY(),
            "{z}", location.getBlockZ());
    }
    
    public void setPos2(Player player, Location location) {
        UUID uuid = player.getUniqueId();
        Selection selection = playerSelections.computeIfAbsent(uuid, k -> new Selection());
        selection.setPos2(location);
        
        messageService.sendMessage(player, "selection_pos2", 
            "{x}", location.getBlockX(),
            "{y}", location.getBlockY(),
            "{z}", location.getBlockZ());

        if (selection.isComplete()) {
            int volume = selection.getVolume();
            messageService.sendMessage(player, "selection_volume", "{volume}", volume);
        }
    }
    
    
    public void clearSelection(Player player) {

        if (worldEditHook.isEnabled()) {
            worldEditHook.clearSelection(player);
        }

        playerSelections.remove(player.getUniqueId());
    }
    
    public int getSelectionVolume(Player player) {

        if (worldEditHook.isEnabled()) {
            int weVolume = worldEditHook.getSelectionVolume(player);
            if (weVolume > 0) {
                return weVolume;
            }
        }

        Selection selection = playerSelections.get(player.getUniqueId());
        if (selection != null && selection.isComplete()) {
            return selection.getVolume();
        }
        
        return 0;
    }
    
    
    public ItemStack createWand() {
        ItemStack wand = new ItemStack(Material.GOLDEN_AXE);
        ItemMeta meta = wand.getItemMeta();
        
        if (meta != null) {
            String name = messageService.getMessage("wand_name");
            meta.setDisplayName(Text.colorizeToString(name));
            
            String[] loreLines = messageService.getMessage("wand_lore").split("\n");
            meta.setLore(Arrays.stream(loreLines)
                .map(Text::colorizeToString)
                .toList());
            
            wand.setItemMeta(meta);
        }
        
        return wand;
    }
    
    
    public boolean isWand(ItemStack item) {
        if (item == null || item.getType() != Material.GOLDEN_AXE) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }
        
        String expectedName = Text.colorizeToString(messageService.getMessage("wand_name"));
        String actualName = meta.getDisplayName();
        
        return expectedName.equals(actualName);
    }
    
    
    public ValidationResult validateSelection(Player player, int maxVolume) {
        Location[] selection = getPlayerSelection(player);
        
        if (selection == null) {
            return new ValidationResult(false, "selection_incomplete");
        }
        
        Location pos1 = selection[0];
        Location pos2 = selection[1];
        
        if (pos1 == null || pos2 == null) {
            return new ValidationResult(false, "selection_incomplete");
        }
        
        if (!pos1.getWorld().equals(pos2.getWorld())) {
            return new ValidationResult(false, "selection_different_worlds");
        }
        
        int volume = getSelectionVolume(player);
        if (maxVolume > 0 && volume > maxVolume) {
            return new ValidationResult(false, "selection_too_large", "{max}", maxVolume);
        }
        
        return new ValidationResult(true, null);
    }
    
    
    public void onPlayerQuit(Player player) {
        playerSelections.remove(player.getUniqueId());
    }
    
    
    private static class Selection {
        private Location pos1;
        private Location pos2;
        
        public Location getPos1() {
            return pos1;
        }
        
        public void setPos1(Location pos1) {
            this.pos1 = pos1;
        }
        
        public Location getPos2() {
            return pos2;
        }
        
        public void setPos2(Location pos2) {
            this.pos2 = pos2;
        }
        
        public boolean isComplete() {
            return pos1 != null && pos2 != null && pos1.getWorld().equals(pos2.getWorld());
        }
        
        public int getVolume() {
            if (!isComplete()) {
                return 0;
            }
            
            int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
            int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
            int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
            int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
            int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
            int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
            
            return (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        }
    }
    
    
    public static class ValidationResult {
        private final boolean valid;
        private final String errorKey;
        private final Object[] errorArgs;
        
        public ValidationResult(boolean valid, String errorKey, Object... errorArgs) {
            this.valid = valid;
            this.errorKey = errorKey;
            this.errorArgs = errorArgs;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getErrorKey() {
            return errorKey;
        }
        
        public Object[] getErrorArgs() {
            return errorArgs;
        }
    }
}