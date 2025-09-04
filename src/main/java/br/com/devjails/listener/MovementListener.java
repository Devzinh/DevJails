package br.com.devjails.listener;

import br.com.devjails.DevJailsPlugin;
import br.com.devjails.jail.Jail;
import br.com.devjails.prisoner.Prisoner;
import br.com.devjails.util.Constants;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.UUID;


public class MovementListener extends BaseListener {
    
    public MovementListener(DevJailsPlugin plugin) {
        super(plugin);
    }
    
    @Override
    protected String getBypassPermission() {
        return "djails.bypass.escape";
    }
    
    @Override
    protected long getCooldownDuration() {
        return Constants.Time.ESCAPE_COOLDOWN_MS;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        // Verificar se a detecção de fuga está habilitada
        if (!config.getBoolean("areas.escape-detection", true)) {
            return;
        }
        
        // Verificação de prisioneiro
        if (!isPrisonerCheck(player)) {
            return;
        }
        
        UUID playerId = player.getUniqueId();
        
        // Verificar cooldown para evitar spam de detecção
        if (isInCooldown(playerId, getCooldownDuration())) {
            return;
        }
        
        // Otimização: só processar se houve movimento real
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        
        if (hasPermissionBypass(player)) {
            return;
        }
        
        Prisoner prisoner = prisonerManager.getPrisoner(playerId);
        if (prisoner == null) {
            return;
        }
        
        Jail jail = plugin.getJailManager().getJail(prisoner.getJailName());
        if (jail == null) {
            return;
        }
        
        if (jail.hasArea()) {
            boolean inArea = plugin.getJailManager().isLocationInJailArea(jail, event.getTo());
            
            if (!inArea) {
                handleEscapeAttempt(player, jail, event);
            }
        }
    }
    
    @Override
    protected void onEventProcessed(Player player, Event event) {
        // Implementação específica para movimento - não usado neste caso
        // A lógica de escape é tratada diretamente no onPlayerMove
    }

    private void handleEscapeAttempt(Player player, Jail jail, PlayerMoveEvent event) {
        event.setCancelled(true);
        
        // Atualizar cooldown
        updateCooldown(player.getUniqueId());
        
        prisonerManager.handleEscapeAttempt(player, jail);
    }
}