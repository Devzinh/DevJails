package br.com.devjails.gui;

import java.util.Arrays;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import br.com.devjails.DevJailsPlugin;
import br.com.devjails.message.MessageService;
import br.com.devjails.prisoner.Prisoner;
import br.com.devjails.util.Tasks;
import br.com.devjails.util.TimeParser;

/**
 * GUI de opÃ§Ãµes para um prisioneiro especÃ­fico
 */
public class PrisonerOptionsGUI implements Listener {
    
    private static final int GUI_SIZE = 27;
    
    private final DevJailsPlugin plugin;
    private final MessageService messageService;
    private final Player viewer;
    private final Prisoner prisoner;
    
    private Inventory inventory;
    
    public PrisonerOptionsGUI(DevJailsPlugin plugin, Player viewer, Prisoner prisoner) {
        this.plugin = plugin;
        this.messageService = plugin.getMessageService();
        this.viewer = viewer;
        this.prisoner = prisoner;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Abre a GUI de opÃ§Ãµes
     */
    public void open() {
        createInventory();
        viewer.openInventory(inventory);
    }
    
    /**
     * Cria o inventory da GUI
     */
    private void createInventory() {
        String title = messageService.getMessage("prisoners_gui_options_title")
            .replace("{player}", prisoner.getPlayerName());
        
        this.inventory = Bukkit.createInventory(viewer, GUI_SIZE, title);

        // Item para liberar prisioneiro
        ItemStack unjailItem = new ItemStack(Material.LIME_DYE);
        ItemMeta unjailMeta = unjailItem.getItemMeta();
        unjailMeta.setDisplayName("§a§l⚡ " + messageService.getMessage("prisoners_gui_unjail"));
        unjailMeta.setLore(Arrays.asList(
            "§7Clique para soltar este prisioneiro",
            "§7imediatamente da cadeia.",
            "",
            "§e▶ Clique para executar"
        ));
        unjailItem.setItemMeta(unjailMeta);
        inventory.setItem(10, unjailItem);

        // Item para estender tempo (apenas para prisões temporárias)
        if (!prisoner.isPermanent()) {
            ItemStack extendItem = new ItemStack(Material.CLOCK);
            ItemMeta extendMeta = extendItem.getItemMeta();
            extendMeta.setDisplayName("§e§l⏰ " + messageService.getMessage("prisoners_gui_extend"));
            extendMeta.setLore(Arrays.asList(
                "§7Clique para estender o tempo",
                "§7de prisão deste jogador.",
                "",
                "§7Tempo atual: §f" + (prisoner.isPermanent() ? "Permanente" : 
                    prisoner.getRemainingTimeMillis() > 0 ? 
                    TimeParser.formatTime(prisoner.getRemainingTimeMillis()) : "Expirado"),
                "",
                "§e▶ Clique para executar"
            ));
            extendItem.setItemMeta(extendMeta);
            inventory.setItem(12, extendItem);
        }

        // Item para teleportar até o prisioneiro
        ItemStack teleportItem = new ItemStack(Material.ENDER_PEARL);
        ItemMeta teleportMeta = teleportItem.getItemMeta();
        teleportMeta.setDisplayName("§b§l🌀 " + messageService.getMessage("prisoners_gui_teleport")
            .replace("{player}", prisoner.getPlayerName()));
        teleportMeta.setLore(Arrays.asList(
            "§7Clique para se teleportar",
            "§7até este prisioneiro.",
            "",
            "§7Status: " + (Bukkit.getPlayer(prisoner.getPlayerId()) != null ? "§aOnline" : "§cOffline"),
            "",
            "§e▶ Clique para executar"
        ));
        teleportItem.setItemMeta(teleportMeta);
        inventory.setItem(14, teleportItem);
        // Item para definir fiança
        ItemStack setBailItem = new ItemStack(Material.GOLD_INGOT);
        ItemMeta setBailMeta = setBailItem.getItemMeta();
        setBailMeta.setDisplayName("§6§l💰 " + messageService.getMessage("prisoners_gui_set_bail"));
        String currentBail = prisoner.getBailAmount() != null && prisoner.getBailAmount() > 0 
            ? "§a$" + String.format("%.2f", prisoner.getBailAmount())
            : "§cNenhuma";
        setBailMeta.setLore(Arrays.asList(
            "§7Clique para definir ou alterar",
            "§7o valor da fiança.",
            "",
            "§7Fiança atual: " + currentBail,
            "",
            "§e▶ Clique para executar"
        ));
        setBailItem.setItemMeta(setBailMeta);
        inventory.setItem(16, setBailItem);

        // Item para remover fiança (apenas se houver fiança)
        if (prisoner.getBailAmount() != null && prisoner.getBailAmount() > 0) {
            ItemStack removeBailItem = new ItemStack(Material.REDSTONE);
            ItemMeta removeBailMeta = removeBailItem.getItemMeta();
            removeBailMeta.setDisplayName("§c§l❌ " + messageService.getMessage("prisoners_gui_remove_bail"));
            removeBailMeta.setLore(Arrays.asList(
                "§7Clique para remover a fiança",
                "§7deste prisioneiro.",
                "",
                "§7Fiança atual: §a$" + String.format("%.2f", prisoner.getBailAmount()),
                "",
                "§c▶ Clique para remover"
            ));
            removeBailItem.setItemMeta(removeBailMeta);
            inventory.setItem(25, removeBailItem);
        }

        // Item para voltar
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName("§7« " + messageService.getMessage("gui_back"));
        backMeta.setLore(Arrays.asList("§7Voltar para a lista de prisioneiros"));
        backItem.setItemMeta(backMeta);
        inventory.setItem(18, backItem);

        // Item para fechar
        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeItem.getItemMeta();
        closeMeta.setDisplayName("§c✖ " + messageService.getMessage("gui_close"));
        closeMeta.setLore(Arrays.asList("§7Fechar esta interface"));
        closeItem.setItemMeta(closeMeta);
        inventory.setItem(26, closeItem);
        
        // Adicionar informações do prisioneiro no centro
        ItemStack infoItem = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta infoMeta = infoItem.getItemMeta();
        infoMeta.setDisplayName("§e§l📋 Informações do Prisioneiro");
        infoMeta.setLore(Arrays.asList(
            "§7Nome: §f" + prisoner.getPlayerName(),
            "§7Cadeia: §f" + prisoner.getJailName(),
            "§7Motivo: §f" + prisoner.getReason(),
            "§7Staff: §f" + prisoner.getStaff(),
            "§7Tempo restante: §f" + (prisoner.isPermanent() ? "Permanente" : 
                prisoner.getRemainingTimeMillis() > 0 ? 
                TimeParser.formatTime(prisoner.getRemainingTimeMillis()) : "Expirado"),
            "§7Fiança: " + (prisoner.getBailAmount() != null && prisoner.getBailAmount() > 0 
                ? "§a$" + String.format("%.2f", prisoner.getBailAmount()) : "§cNenhuma")
        ));
        infoItem.setItemMeta(infoMeta);
        inventory.setItem(4, infoItem);
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) {
            return;
        }
        
        event.setCancelled(true);
        
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        if (!player.equals(viewer)) {
            return;
        }
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }
        
        int slot = event.getSlot();
        
        switch (slot) {
            case 10: 
                unjailPrisoner(player);
                break;
                
            case 12: 
                if (!prisoner.isPermanent()) {
                    extendPrisonTime(player);
                }
                break;
                
            case 14: 
                teleportToPrisoner(player);
                break;
                
            case 16:
                setBail(player);
                break;
                
            case 25: 
                if (prisoner.getBailAmount() != null && prisoner.getBailAmount() > 0) {
                    removeBail(player);
                }
                break;
                
            case 18: 
                player.closeInventory();
                PrisonersGUI prisonersGUI = new PrisonersGUI(plugin, player);
                prisonersGUI.open(1);
                break;
                
            case 26: 
                player.closeInventory();
                break;
        }
    }
    
    /**
     * Solta o prisioneiro
     */
    private void unjailPrisoner(Player staff) {
        plugin.getPrisonerManager().releasePrisoner(prisoner.getPlayerId(), staff.getName())
            .thenAccept(success -> {
                Tasks.sync(() -> {
                    if (success) {
                        messageService.sendMessage(staff, "player_unjailed", 
                            "{player}", prisoner.getPlayerName(),
                            "{staff}", staff.getName());

                        Player target = Bukkit.getPlayer(prisoner.getPlayerId());
                        if (target != null && target.isOnline()) {
                            messageService.sendMessage(target, "unjail_notify_player");
                        }
                        
                        staff.closeInventory();
                    } else {
                        messageService.sendMessage(staff, "unjail_failed");
                    }
                });
            })
            .exceptionally(ex -> {
                Tasks.sync(() -> {
                    plugin.getLogger().severe("Erro ao soltar prisioneiro: " + ex.getMessage());
                    messageService.sendMessage(staff, "storage_error");
                });
                return null;
            });
    }
    
    /**
     * Estende o tempo de prisÃ£o
     */
    private void extendPrisonTime(Player staff) {

        staff.closeInventory();
        messageService.sendMessage(staff, "extend_time_instruction", "{player}", prisoner.getPlayerName());
        
        

        messageService.sendMessage(staff, "use_extend_command", 
            "{command}", "/djails extend " + prisoner.getPlayerName() + " <tempo>");
    }
    
    /**
     * Teleporta atÃ© o prisioneiro
     */
    private void teleportToPrisoner(Player staff) {
        Player target = Bukkit.getPlayer(prisoner.getPlayerId());
        
        if (target == null || !target.isOnline()) {
            messageService.sendMessage(staff, "player_offline", "{player}", prisoner.getPlayerName());
            return;
        }
        
        staff.teleport(target.getLocation());
        messageService.sendMessage(staff, "teleported_to_prisoner", "{player}", prisoner.getPlayerName());
        
        staff.closeInventory();
    }
    private void setBail(Player staff) {

        staff.closeInventory();
        messageService.sendMessage(staff, "set_bail_instruction", "{player}", prisoner.getPlayerName());
        
        

        messageService.sendMessage(staff, "use_bail_command", 
            "{command}", "/djails bail set " + prisoner.getPlayerName() + " <valor>");
    }
    
    /**
     * Remove a Fiança do prisioneiro
     */
    private void removeBail(Player staff) {
        plugin.getPrisonerManager().setBail(prisoner.getPlayerId(), 0.0, staff.getName())
            .thenAccept(success -> {
                if (success) {
                    messageService.sendMessage(staff, "bail_removed", "{player}", prisoner.getPlayerName());

                    prisoner.setBailAmount(0.0);

                    open();
                } else {
                    messageService.sendMessage(staff, "bail_remove_failed");
                }
            })
            .exceptionally(ex -> {
                plugin.getLogger().severe("Erro ao remover Fiança: " + ex.getMessage());
                messageService.sendMessage(staff, "storage_error");
                return null;
            });
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().equals(inventory)) {

            InventoryClickEvent.getHandlerList().unregister(this);
            InventoryCloseEvent.getHandlerList().unregister(this);
        }
    }
}