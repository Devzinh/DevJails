package br.com.devjails.integration;

import java.util.logging.Logger;

import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import net.milkbowl.vault.economy.Economy;


public class VaultHook {
    
    private final Logger logger;
    private Economy economy;
    private boolean enabled;
    
    public VaultHook(Logger logger) {
        this.logger = logger;
    }
    
    
    public boolean initialize() {
        try {
            if (JavaPlugin.getProvidingPlugin(Economy.class).isEnabled()) {
                RegisteredServiceProvider<Economy> rsp = JavaPlugin.getProvidingPlugin(Economy.class)
                        .getServer().getServicesManager().getRegistration(Economy.class);
                if (rsp != null) {
                    economy = rsp.getProvider();
                    enabled = true;
                    // Usar log mais conciso
                    logger.info("VaultHook initialized");
                    return true;
                }
            }
        } catch (Exception e) {
            logger.warning("Failed to initialize VaultHook: " + e.getMessage());
        }
        
        enabled = false;
        // Usar log mais conciso
        logger.info("VaultHook disabled - Vault not found or disabled");
        return false;
    }
    
    
    public boolean isEnabled() {
        return enabled && economy != null;
    }
    
    public double getBalance(OfflinePlayer player) {
        if (!isEnabled()) {
            return 0.0;
        }
        
        try {
            return economy.getBalance(player);
        } catch (Exception e) {
            logger.warning("Error getting balance for " + player.getName() + ": " + e.getMessage());
            return 0.0;
        }
    }
    
    
    public boolean hasBalance(OfflinePlayer player, double amount) {
        return getBalance(player) >= amount;
    }
    
    
    public boolean withdrawPlayer(OfflinePlayer player, double amount) {
        if (!isEnabled()) {
            return false;
        }
        
        try {
            return economy.withdrawPlayer(player, amount).transactionSuccess();
        } catch (Exception e) {
            logger.warning("Error withdrawing money from " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }
    
    
    public boolean withdraw(OfflinePlayer player, double amount) {
        return withdrawPlayer(player, amount);
    }
    
    
    public boolean depositPlayer(OfflinePlayer player, double amount) {
        if (!isEnabled()) {
            return false;
        }
        
        try {
            return economy.depositPlayer(player, amount).transactionSuccess();
        } catch (Exception e) {
            logger.warning("Error depositing money to " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }
    
    
    public boolean deposit(OfflinePlayer player, double amount) {
        return depositPlayer(player, amount);
    }
    
    
    public String format(double amount) {
        if (!isEnabled()) {
            return String.valueOf(amount);
        }
        
        try {
            return economy.format(amount);
        } catch (Exception e) {
            return String.valueOf(amount);
        }
    }
    
    public String getCurrencyNamePlural() {
        if (!isEnabled()) {
            return "coins";
        }
        
        try {
            return economy.currencyNamePlural();
        } catch (Exception e) {
            return "coins";
        }
    }
    
    public String getCurrencyNameSingular() {
        if (!isEnabled()) {
            return "coin";
        }
        
        try {
            return economy.currencyNameSingular();
        } catch (Exception e) {
            return "coin";
        }
    }
    
    
    public boolean hasAccount(OfflinePlayer player) {
        if (!isEnabled()) {
            return false;
        }
        
        try {
            return economy.hasAccount(player);
        } catch (Exception e) {
            return false;
        }
    }
    
    
    public boolean createAccount(OfflinePlayer player) {
        if (!isEnabled()) {
            return false;
        }
        
        try {
            return economy.createPlayerAccount(player);
        } catch (Exception e) {
            logger.warning("Error creating account for " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }
}