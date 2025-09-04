package br.com.devjails.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import br.com.devjails.DevJailsPlugin;
import br.com.devjails.flag.SelectionService;
import br.com.devjails.util.Constants;

/**
 * Listener para tratar cliques com a varinha de seleção
 * Utiliza BaseListener com Template Method pattern
 */
public class SelectionListener extends BaseListener {
    
    private final SelectionService selectionService;
    
    public SelectionListener(DevJailsPlugin plugin) {
        super(plugin);
        this.selectionService = plugin.getSelectionService();
    }
    
    @Override
    protected String getBypassPermission() {
        return "djails.admin.selection"; // Permissão para usar a varinha
    }
    
    @Override
    protected long getCooldownDuration() {
        return Constants.Time.INTERACTION_COOLDOWN_MS;
    }
    
    @Override
    protected void onEventProcessed(Player player, Event event) {
        // Implementação específica para seleção - não usado neste caso
        // A lógica de seleção é tratada diretamente no onPlayerInteract
    }
    
    /**
     * Trata cliques com a varinha de seleção
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        // Verificar se o item é a varinha de seleção
        if (item == null || !selectionService.isWand(item)) {
            return;
        }
        
        // Verificar se é um clique com a varinha
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            // Definir posição 1
            selectionService.setPos1(player, event.getClickedBlock().getLocation());
            event.setCancelled(true);
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            // Definir posição 2
            selectionService.setPos2(player, event.getClickedBlock().getLocation());
            event.setCancelled(true);
        }
    }
}