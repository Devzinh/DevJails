package br.com.devjails.api.events;

import br.com.devjails.prisoner.Prisoner;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Evento chamado quando um jogador estÃ¡ prestes a ser solto
 */
public class PlayerUnjailedEvent extends Event implements Cancellable {
    
    private static final HandlerList handlers = new HandlerList();
    
    private final Player player;
    private final Prisoner prisoner;
    private final String staff;
    private final ReleaseReason releaseReason;
    private boolean cancelled = false;
    
    public PlayerUnjailedEvent(Player player, Prisoner prisoner, String staff, ReleaseReason releaseReason) {
        this.player = player;
        this.prisoner = prisoner;
        this.staff = staff;
        this.releaseReason = releaseReason;
    }
    public Player getPlayer() {
        return player;
    }
    public Prisoner getPrisoner() {
        return prisoner;
    }
    public String getStaff() {
        return staff;
    }
    public ReleaseReason getReleaseReason() {
        return releaseReason;
    }
    public String getJailName() {
        return prisoner.getJailName();
    }
    
    /**
     * Verifica se foi uma soltura manual (por staff)
     */
    public boolean isManualRelease() {
        return releaseReason == ReleaseReason.MANUAL && staff != null;
    }
    
    /**
     * Verifica se foi uma soltura automÃ¡tica (tempo expirado)
     */
    public boolean isAutomaticRelease() {
        return releaseReason == ReleaseReason.TIME_EXPIRED;
    }
    
    /**
     * Verifica se foi soltura por Fiança
     */
    public boolean isBailRelease() {
        return releaseReason == ReleaseReason.BAIL_PAID;
    }
    
    @Override
    public boolean isCancelled() {
        return cancelled;
    }
    
    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
    
    
    public enum ReleaseReason {
        MANUAL,        
        TIME_EXPIRED,  
        BAIL_PAID,     
        PLUGIN_RELOAD, 
        OTHER          
    }
}