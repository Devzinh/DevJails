package br.com.devjails.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import br.com.devjails.DevJailsPlugin;
import br.com.devjails.bail.BailService;
import br.com.devjails.prisoner.Prisoner;
import br.com.devjails.util.Text;
import br.com.devjails.util.TimeParser;

/**
 * GUI redesenhada para sistema de Fianças com interface moderna e organizada
 */
public class BailGUI extends BaseGUI<BailService.BailInfo> {
    
    private static final int ITEMS_PER_PAGE = 28; // Reduzido para melhor organização
    private static final int GUI_SIZE = 54; // 6 linhas para melhor layout
    
    private final BailService bailService;
    
    public BailGUI(DevJailsPlugin plugin, Player viewer) {
        super(plugin, viewer, new ArrayList<>());
        this.bailService = plugin.getBailService();
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
        return "§6§lSistema de Fianças";
    }
    
    @Override
    protected String getNoItemsMessage() {
        return "§cNenhuma fiança disponível!";
    }
    
    @Override
    protected int getPreviousPageSlot() {
        return 48;
    }
    
    @Override
    protected int getNextPageSlot() {
        return 50;
    }
    
    @Override
    protected int getCloseSlot() {
        return 49;
    }
    
    @Override
    protected int getPageInfoSlot() {
        return 4;
    }
    
    @Override
    public void open(int page) {
        if (!bailService.isEnabled()) {
            messageService.sendMessage(viewer, "bail_enabled_vault_missing");
            return;
        }
        super.open(page);
    }
    
    @Override
    protected void loadData() {
        items.clear();
        
        Collection<Prisoner> prisoners = plugin.getPrisonerManager().getAllPrisoners();
        
        for (Prisoner prisoner : prisoners) {
            if (prisoner.getBailAmount() != null && prisoner.getBailAmount() > 0) {
                items.add(new BailService.BailInfo(
                    prisoner.getUuid(),
                    prisoner.getPlayerName(),
                    prisoner.getBailAmount(),
                    prisoner.getJailName(),
                    prisoner.getReason()
                ));
            }
        }
    }
    
    @Override
    protected int[] getContentSlots() {
        // Slots de conteúdo em formato de grade com bordas
        return new int[]{
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
        };
     }
     
     @Override
     protected ItemStack createItemStack(BailService.BailInfo bailInfo) {
         ItemStack item = new ItemStack(Material.PLAYER_HEAD);
         SkullMeta meta = (SkullMeta) item.getItemMeta();
         
         // Nome do item
         meta.setDisplayName("§c" + bailInfo.getPlayerName());
         
         // Lore do item
         List<String> lore = Arrays.asList(
             "§7Cadeia: §f" + bailInfo.getJailName(),
             "§7Motivo: §f" + bailInfo.getReason(),
             "§7Valor da Fiança: §a$" + String.format("%.2f", bailInfo.getAmount()),
             "",
             "§e§lClique para pagar a fiança!",
             "§7O prisioneiro será libertado",
             "§7imediatamente após o pagamento."
         );
         
         meta.setLore(lore);
         
         // Define a textura da cabeça
         meta.setOwningPlayer(Bukkit.getOfflinePlayer(bailInfo.getPlayerId()));
         
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
         
         BailService.BailInfo bailInfo = items.get(itemIndex);
         Player player = (Player) event.getWhoClicked();
         
         payBail(player, bailInfo);
     }
    

    
    /**
     * Processa pagamento de Fiança
     */
    private void payBail(Player payer, BailService.BailInfo bailInfo) {
        OfflinePlayer prisoner = Bukkit.getOfflinePlayer(bailInfo.getPlayerId());

        payer.closeInventory();
        
        messageService.sendMessage(payer, "bail_processing");
        
        bailService.payBail(payer, prisoner)
            .thenAccept(result -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    switch (result) {
                        case SUCCESS:
                            messageService.sendMessage(payer, "bail_paid_success", 
                                "{player}", bailInfo.getPlayerName(),
                                "{amount}", String.format("%.2f", bailInfo.getAmount()));

                            Player onlinePrisoner = prisoner.getPlayer();
                            if (onlinePrisoner != null && onlinePrisoner.isOnline()) {
                                messageService.sendMessage(onlinePrisoner, "bail_freed_by_other", 
                                    "{player}", payer.getName());
                            }
                            break;
                            
                        case INSUFFICIENT_FUNDS:
                            messageService.sendMessage(payer, "bail_insufficient_funds", 
                                "{amount}", String.format("%.2f", bailInfo.getAmount()));
                            break;
                            
                        case NOT_JAILED:
                            messageService.sendMessage(payer, "bail_player_not_jailed", 
                                "{player}", bailInfo.getPlayerName());
                            break;
                            
                        case NO_BAIL_SET:
                            messageService.sendMessage(payer, "bail_not_set", 
                                "{player}", bailInfo.getPlayerName());
                            break;
                            
                        case SELF_BAIL_DISABLED:
                            messageService.sendMessage(payer, "bail_self_disabled");
                            break;
                            
                        case SYSTEM_DISABLED:
                            messageService.sendMessage(payer, "bail_system_disabled");
                            break;
                            
                        default:
                            messageService.sendMessage(payer, "bail_payment_failed");
                            break;
                    }
                });
            })
            .exceptionally(ex -> {
                plugin.getLogger().severe("Erro ao processar pagamento de Fiança: " + ex.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> {
                    messageService.sendMessage(payer, "bail_payment_error");
                });
                return null;
            });
    }
    

}