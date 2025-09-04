package br.com.devjails.api.events;

import br.com.devjails.prisoner.Prisoner;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Evento chamado quando uma Fiança Ã© paga
 */
public class PlayerBailPaidEvent extends Event {
    
    private static final HandlerList handlers = new HandlerList();
    
    private final OfflinePlayer prisoner;
    private final Player payer;
    private final Prisoner prisonerData;
    private final double bailAmount;
    
    public PlayerBailPaidEvent(OfflinePlayer prisoner, Player payer, Prisoner prisonerData, double bailAmount) {
        this.prisoner = prisoner;
        this.payer = payer;
        this.prisonerData = prisonerData;
        this.bailAmount = bailAmount;
    }
    public OfflinePlayer getPrisoner() {
        return prisoner;
    }
    public Player getPayer() {
        return payer;
    }
    public Prisoner getPrisonerData() {
        return prisonerData;
    }
    public double getBailAmount() {
        return bailAmount;
    }
    public String getJailName() {
        return prisonerData.getJailName();
    }
    public String getReason() {
        return prisonerData.getReason();
    }
    public String getOriginalStaff() {
        return prisonerData.getStaff();
    }
    
    /**
     * Verifica se o prisioneiro estÃ¡ online
     */
    public boolean isPrisonerOnline() {
        return prisoner.isOnline();
    }
    
    /**
     * Verifica se o pagador pagou sua prÃ³pria Fiança
     */
    public boolean isSelfBail() {
        return prisoner.getUniqueId().equals(payer.getUniqueId());
    }
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
}