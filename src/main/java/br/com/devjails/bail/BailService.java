package br.com.devjails.bail;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import br.com.devjails.integration.VaultHook;
import br.com.devjails.message.MessageService;
import br.com.devjails.prisoner.Prisoner;
import br.com.devjails.prisoner.PrisonerManager;


public class BailService {
    
    private final PrisonerManager prisonerManager;
    private final VaultHook vaultHook;
    private final MessageService messageService;
    private final FileConfiguration config;
    private final Logger logger;
    
    private boolean enabled;
    private boolean allowSelfBail;
    private boolean logPayments;
    
    public BailService(PrisonerManager prisonerManager, VaultHook vaultHook, 
                      MessageService messageService, FileConfiguration config, Logger logger) {
        this.prisonerManager = prisonerManager;
        this.vaultHook = vaultHook;
        this.messageService = messageService;
        this.config = config;
        this.logger = logger;
        
        loadConfig();
    }
    
    
    private void loadConfig() {
        this.enabled = config.getBoolean("bail.enabled", true) && vaultHook.isEnabled();
        this.allowSelfBail = config.getBoolean("bail.allow-self-bail", true);
        this.logPayments = config.getBoolean("bail.log-bail-payments", true);
        
        if (!vaultHook.isEnabled() && config.getBoolean("bail.enabled", true)) {
            logger.warning("Bail system disabled - Vault not found!");
        }
    }
    
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public CompletableFuture<Boolean> setBail(OfflinePlayer prisoner, double amount, String staff) {
        if (!enabled) {
            return CompletableFuture.completedFuture(false);
        }
        
        if (amount < 0) {
            return CompletableFuture.completedFuture(false);
        }
        
        return prisonerManager.setBail(prisoner.getUniqueId(), amount, staff)
            .thenApply(success -> {
                if (success && logPayments) {
                    // Usar log mais conciso
                    logger.info("Bail set for " + prisoner.getName() + " by " + staff);
                }
                return success;
            });
    }
    
    
    public CompletableFuture<Boolean> removeBail(OfflinePlayer prisoner, String staff) {
        if (!enabled) {
            return CompletableFuture.completedFuture(false);
        }
        
        return setBail(prisoner, 0.0, staff);
    }
    
    
    public CompletableFuture<BailResult> payBail(Player payer, OfflinePlayer prisoner) {
        if (!enabled) {
            return CompletableFuture.completedFuture(BailResult.SYSTEM_DISABLED);
        }
        if (payer.equals(prisoner) && !allowSelfBail) {
            return CompletableFuture.completedFuture(BailResult.SELF_BAIL_DISABLED);
        }
        
        return prisonerManager.isPrisoner(prisoner.getUniqueId())
            .thenCompose(isPrisoner -> {
                if (!isPrisoner) {
                    return CompletableFuture.completedFuture(BailResult.NOT_JAILED);
                }
                
                return CompletableFuture.supplyAsync(() -> {
                    Prisoner prisonerData = prisonerManager.getPrisoner(prisoner.getUniqueId());
                    if (prisonerData == null) {
                        return BailResult.NOT_JAILED;
                    }
                    
                    double bailAmount = prisonerData.getBailAmount();
                    if (bailAmount <= 0) {
                        return BailResult.NO_BAIL_SET;
                    }
                    if (!vaultHook.hasBalance(payer, bailAmount)) {
                        return BailResult.INSUFFICIENT_FUNDS;
                    }

                    if (!vaultHook.withdraw(payer, bailAmount)) {
                        return BailResult.PAYMENT_FAILED;
                    }
                    
                    return bailAmount; 
                }).thenCompose(result -> {
                    if (result instanceof BailResult) {
                        return CompletableFuture.completedFuture((BailResult) result);
                    }
                    
                    double bailAmount = (Double) result;

                    return prisonerManager.releasePrisoner(prisoner.getUniqueId(), payer.getName())
                        .thenApply(released -> {
                            if (released) {
                                if (logPayments) {
                                    // Usar log mais conciso
                                    logger.info("Bail paid for " + prisoner.getName() + " by " + payer.getName());
                                }
                                return BailResult.SUCCESS;
                            } else {
                                try {
                                    vaultHook.deposit(payer, bailAmount);
                                } catch (Exception e) {
                                    logger.warning("Error refunding bail: " + e.getMessage());
                                }
                                return BailResult.RELEASE_FAILED;
                            }
                        });
                });
            })
            .exceptionally(ex -> {
                logger.severe("Error processing bail payment: " + ex.getMessage());
                return BailResult.ERROR;
            });
    }
    
    public CompletableFuture<BailInfo> getBailInfo(OfflinePlayer prisoner) {
        if (!enabled) {
            return CompletableFuture.completedFuture(null);
        }
        
        return prisonerManager.isPrisoner(prisoner.getUniqueId())
            .thenCompose(isPrisoner -> {
                if (!isPrisoner) {
                    return CompletableFuture.completedFuture(null);
                }
                
                return CompletableFuture.supplyAsync(() -> {
                    Prisoner prisonerData = prisonerManager.getPrisoner(prisoner.getUniqueId());
                    if (prisonerData == null || prisonerData.getBailAmount() <= 0) {
                        return null;
                    }
                    
                    return new BailInfo(
                        prisoner.getUniqueId(),
                        prisoner.getName(),
                        prisonerData.getBailAmount(),
                        prisonerData.getJailName(),
                        prisonerData.getReason()
                    );
                });
            });
    }
    
    
    public void reload() {
        loadConfig();
        // Usar log mais conciso
        logger.info("BailService reloaded");
    }
    
    
    public enum BailResult {
        SUCCESS,
        SYSTEM_DISABLED,
        NOT_JAILED,
        NO_BAIL_SET,
        INSUFFICIENT_FUNDS,
        PAYMENT_FAILED,
        RELEASE_FAILED,
        SELF_BAIL_DISABLED,
        ERROR
    }
    
    
    public static class BailInfo {
        private final java.util.UUID playerId;
        private final String playerName;
        private final double amount;
        private final String jailName;
        private final String reason;
        
        public BailInfo(java.util.UUID playerId, String playerName, double amount, String jailName, String reason) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.amount = amount;
            this.jailName = jailName;
            this.reason = reason;
        }
        
        public java.util.UUID getPlayerId() { return playerId; }
        public String getPlayerName() { return playerName; }
        public double getAmount() { return amount; }
        public String getJailName() { return jailName; }
        public String getReason() { return reason; }
    }
}