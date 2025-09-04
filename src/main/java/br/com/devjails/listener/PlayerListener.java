package br.com.devjails.listener;

import br.com.devjails.DevJailsPlugin;
import br.com.devjails.flag.SelectionService;
import br.com.devjails.handcuff.HandcuffService;
import br.com.devjails.util.Constants;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener para gerenciar eventos de entrada e saída de jogadores
 * Utiliza BaseListener com Template Method pattern
 */
public class PlayerListener extends BaseListener {
    
    private final SelectionService selectionService;
    private final HandcuffService handcuffService;
    
    public PlayerListener(DevJailsPlugin plugin) {
        super(plugin);
        this.selectionService = plugin.getSelectionService();
        this.handcuffService = plugin.getHandcuffService();
    }
    
    @Override
    protected String getBypassPermission() {
        return "djails.admin.player"; // Permissão administrativa para eventos de jogador
    }
    
    @Override
    protected long getCooldownDuration() {
        return Constants.Time.INTERACTION_COOLDOWN_MS;
    }
    
    @Override
    protected void onEventProcessed(Player player, Event event) {
        // Implementação específica para eventos de jogador - não usado neste caso
        // A lógica é tratada diretamente nos métodos de evento
    }
    
    /**
     * Processa entrada do jogador
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        prisonerManager.onPlayerJoin(player);

        if (handcuffService != null) {
            handcuffService.onPlayerJoin(player);
        }
    }
    
    /**
     * Processa saída do jogador
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        prisonerManager.onPlayerQuit(player);

        selectionService.onPlayerQuit(player);

        if (handcuffService != null) {
            handcuffService.onPlayerQuit(player);
        }

        // Limpar cooldowns do jogador (agora gerenciado pela BaseListener)
        removeCooldown(player.getUniqueId());

        plugin.getFlagManager().onPlayerQuit(player);
    }
}