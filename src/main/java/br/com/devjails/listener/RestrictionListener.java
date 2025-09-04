package br.com.devjails.listener;

import java.util.List;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;

import br.com.devjails.DevJailsPlugin;
import br.com.devjails.util.Constants;
import br.com.devjails.util.Tasks;

/**
 * Listener para aplicar restriÃ§Ãµes aos prisioneiros
 * Utiliza BaseListener com Template Method pattern
 */
public class RestrictionListener extends BaseListener {
    
    public RestrictionListener(DevJailsPlugin plugin) {
        super(plugin);
    }
    
    @Override
    protected String getBypassPermission() {
        return "djails.bypass.restrictions";
    }
    
    @Override
    protected long getCooldownDuration() {
        return Constants.Time.INTERACTION_COOLDOWN_MS;
    }
    
    /**
     * Bloqueia comandos para prisioneiros
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        
        // Verificação assíncrona para comandos (não pode usar template method devido à natureza assíncrona)
        prisonerManager.isPrisoner(player.getUniqueId())
            .thenAccept(isPrisoner -> {
                if (!isPrisoner) {
                    return;
                }
                if (player.hasPermission("djails.bypass.commands")) {
                    return;
                }
                
                String command = event.getMessage().toLowerCase().split(" ")[0];
                if (isCommandBlocked(command)) {
                    Tasks.sync(() -> {
                        event.setCancelled(true);
                        messageService.sendUnifiedMessage(player, "restriction_command_blocked");
                    });
                }
            });
    }
    
    /**
     * Bloqueia quebra de blocos
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        processPlayerEvent(event.getPlayer(), event, "restrictions.block-break", "restriction_break_blocked");
    }
    
    /**
     * Bloqueia colocação de blocos
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        processPlayerEvent(event.getPlayer(), event, "restrictions.block-place", "restriction_place_blocked");
    }
    
    /**
     * Bloqueia interações
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        processPlayerEvent(event.getPlayer(), event, "restrictions.block-interact", "restriction_interact_blocked");
    }
    
    /**
     * Controla PvP na prisão
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player damager = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();
        
        // Verificar se pelo menos um dos jogadores é prisioneiro
        boolean damagerJailed = prisonerManager.isPrisonerSync(damager.getUniqueId());
        boolean victimJailed = prisonerManager.isPrisonerSync(victim.getUniqueId());
        
        if (!damagerJailed && !victimJailed) {
            return;
        }
        
        // Usar configuração invertida (pvp-enabled false = bloquear PvP)
        if (!config.getBoolean("restrictions.pvp-enabled", false)) {
            event.setCancelled(true);
            if (damagerJailed) {
                sendCooldownMessage(damager, "restriction_pvp_blocked", getCooldownDuration());
            }
        }
    }
    
    /**
     * Bloqueia chat se configurado
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        
        // Verificação específica para chat (evento assíncrono)
        if (!prisonerManager.isPrisonerSync(player.getUniqueId())) {
            return;
        }
        
        if (player.hasPermission(getBypassPermission())) {
            return;
        }
        
        if (config.getBoolean("restrictions.block-chat", false)) {
            event.setCancelled(true);
            
            // Usar método de cooldown da BaseListener
            Tasks.sync(() -> {
                sendCooldownMessage(player, "restriction_chat_blocked", getCooldownDuration());
            });
        }
    }
    
    /**
     * Bloqueia dormir
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        processPlayerEvent(event.getPlayer(), event, "restrictions.block-sleep", null);
    }
    
    /**
     * Bloqueia drop de itens
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        processPlayerEvent(event.getPlayer(), event, "restrictions.block-item-drop", null);
    }
    
    /**
     * Bloqueia pickup de itens
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        processPlayerEvent(event.getPlayer(), event, "restrictions.block-item-pickup", null);
    }
    
    
    private boolean isCommandBlocked(String command) {
        List<String> blockedCommands = config.getStringList("restrictions.blocked-commands");
        List<String> allowedCommands = config.getStringList("restrictions.allowed-commands");
        for (String allowed : allowedCommands) {
            if (command.startsWith(allowed.toLowerCase())) {
                return false;
            }
        }
        for (String blocked : blockedCommands) {
            if (blocked.equals("*") || command.startsWith(blocked.toLowerCase())) {
                return true;
            }
        }
        
        return false;
    }
}