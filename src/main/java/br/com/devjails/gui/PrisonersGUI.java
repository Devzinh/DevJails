package br.com.devjails.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import br.com.devjails.DevJailsPlugin;
import br.com.devjails.prisoner.Prisoner;
import br.com.devjails.util.TimeParser;


public class PrisonersGUI extends BaseGUI<Prisoner> {
    
    private static final int ITEMS_PER_PAGE = 45;
    private static final int GUI_SIZE = 54;
    
    public PrisonersGUI(DevJailsPlugin plugin, Player viewer) {
        super(plugin, viewer, new ArrayList<>());
    }
    
    @Override
    protected int getItemsPerPage() {
        return ITEMS_PER_PAGE;
    }
    
    @Override
    protected int getGuiSize() {
        return GUI_SIZE;
    }
    
    @Override
    protected String getGuiTitle() {
        return messageService.getMessage("prisoners_gui_title");
    }
    
    @Override
    protected String getNoItemsMessage() {
        return messageService.getMessage("prisoners_gui_no_prisoners");
    }
    
    @Override
    protected int getPreviousPageSlot() {
        return 45;
    }
    
    @Override
    protected int getNextPageSlot() {
        return 53;
    }
    
    @Override
    protected int getCloseSlot() {
        return 47;
    }
    
    @Override
    protected int getPageInfoSlot() {
        return 49;
    }
    
    @Override
    protected void loadData() {
        items.clear();
        Collection<Prisoner> prisonerList = plugin.getPrisonerManager().getAllPrisoners();
        items.addAll(prisonerList);
    }
    
    @Override
    protected int[] getContentSlots() {
        int[] slots = new int[ITEMS_PER_PAGE];
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            slots[i] = i;
        }
        return slots;
    }
    
    @Override
    protected ItemStack createItemStack(Prisoner prisoner) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        
        meta.setDisplayName("§c" + prisoner.getPlayerName());
        
        List<String> lore = Arrays.asList(
            "§7Cadeia: §f" + prisoner.getJailName(),
            "§7Motivo: §f" + prisoner.getReason(),
            "§7Staff: §f" + prisoner.getStaff(),
            "§7Tempo restante: §f" + (prisoner.isPermanent() ? "Permanente" : TimeParser.formatTime(prisoner.getRemainingTimeMillis())),
            "§7Fiança: §f" + (prisoner.getBailAmount() != null && prisoner.getBailAmount() > 0 
                ? "$" + String.format("%.2f", prisoner.getBailAmount())
                : "Nenhuma"),
            "",
            "§e§lClique esquerdo: §7Teletransportar",
            "§e§lClique direito: §7Opções do prisioneiro"
        );
        
        meta.setLore(lore);
        
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(prisoner.getUuid()));
        
        item.setItemMeta(meta);
        return item;
    }
    
    @Override
    protected void handleContentClick(InventoryClickEvent event, int slot) {
        if (!isContentSlot(slot)) {
            return;
        }
        
        int itemIndex = getItemIndexFromSlot(slot);
        if (itemIndex < 0 || itemIndex >= items.size()) {
            return;
        }
        
        Prisoner prisoner = items.get(itemIndex);
        Player player = (Player) event.getWhoClicked();
        
        if (event.isShiftClick()) {
            teleportToPrisoner(player, prisoner);
        } else {
            openPrisonerOptions(player, prisoner);
        }
    }
    
    private void teleportToPrisoner(Player staff, Prisoner prisoner) {
        Player target = Bukkit.getPlayer(prisoner.getPlayerId());
        
        if (target == null || !target.isOnline()) {
            messageService.sendMessage(staff, "player_offline", "{player}", prisoner.getPlayerName());
            return;
        }
        
        staff.teleport(target.getLocation());
        messageService.sendMessage(staff, "teleported_to_prisoner", "{player}", prisoner.getPlayerName());

        staff.closeInventory();
    }
    
    private void openPrisonerOptions(Player staff, Prisoner prisoner) {
        PrisonerOptionsGUI optionsGUI = new PrisonerOptionsGUI(plugin, staff, prisoner);
        optionsGUI.open();
    }
}