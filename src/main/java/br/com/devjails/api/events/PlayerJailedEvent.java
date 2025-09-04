package br.com.devjails.api.events;

import br.com.devjails.prisoner.Prisoner;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Evento chamado quando um jogador estÃ¡ prestes a ser preso
 */
public class PlayerJailedEvent extends Event implements Cancellable {
    
    private static final HandlerList handlers = new HandlerList();
    
    private final Player player;
    private final Prisoner prisoner;
    private final String staff;
    private boolean cancelled = false;
    
    public PlayerJailedEvent(Player player, Prisoner prisoner, String staff) {
        this.player = player;
        this.prisoner = prisoner;
        this.staff = staff;
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
    public String getJailName() {
        return prisoner.getJailName();
    }
    public String getReason() {
        return prisoner.getReason();
    }
    
    /**
     * Verifica se Ã© uma prisÃ£o permanente
     */
    public boolean isPermanent() {
        return prisoner.isPermanent();
    }
    public long getDurationMillis() {
        if (isPermanent()) {
            return -1;
        }
        return prisoner.getRemainingTimeMillis();
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
}