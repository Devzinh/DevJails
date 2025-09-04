package br.com.devjails.api.events;

import br.com.devjails.jail.Jail;
import br.com.devjails.prisoner.Prisoner;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;


public class PlayerEscapeEvent extends Event implements Cancellable {
    
    private static final HandlerList handlers = new HandlerList();
    
    private final Player player;
    private final Prisoner prisoner;
    private final Jail jail;
    private final Location fromLocation;
    private final Location toLocation;
    private boolean cancelled = false;
    
    public PlayerEscapeEvent(Player player, Prisoner prisoner, Jail jail, Location fromLocation, Location toLocation) {
        this.player = player;
        this.prisoner = prisoner;
        this.jail = jail;
        this.fromLocation = fromLocation;
        this.toLocation = toLocation;
    }
    public Player getPlayer() {
        return player;
    }
    public Prisoner getPrisoner() {
        return prisoner;
    }
    public Jail getJail() {
        return jail;
    }
    public Location getFromLocation() {
        return fromLocation;
    }
    public Location getToLocation() {
        return toLocation;
    }
    public String getJailName() {
        return jail.getName();
    }

    public boolean hasRemainingTime() {
        return prisoner.isPermanent() || prisoner.getRemainingTimeMillis() > 0;
    }
    public long getRemainingTimeMillis() {
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