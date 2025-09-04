package br.com.devjails.handcuff;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import br.com.devjails.message.MessageService;
import br.com.devjails.prisoner.Prisoner;
import br.com.devjails.prisoner.PrisonerManager;
import br.com.devjails.storage.Storage;
import br.com.devjails.util.Tasks;

/**
 * Serviço para gerenciar algemas de prisioneiros
 */
public class HandcuffService {
    
    private final PrisonerManager prisonerManager;
    private final MessageService messageService;
    private final Storage storage;
    private final Logger logger;
    private final FileConfiguration config;

    private int slownessLevel;
    private int darknessLevel;
    private boolean useMiningFatigue;
    private int miningFatigueLevel;

    private final Set<UUID> handcuffedPlayers;
    
    public HandcuffService(PrisonerManager prisonerManager, MessageService messageService, 
                          Storage storage, FileConfiguration config, Logger logger) {
        this.prisonerManager = prisonerManager;
        this.messageService = messageService;
        this.storage = storage;
        this.config = config;
        this.logger = logger;
        this.handcuffedPlayers = ConcurrentHashMap.newKeySet();
        
        loadConfig();
    }
    
    /**
     * Carrega Configurações dos efeitos de algemas
     */
    private void loadConfig() {
        slownessLevel = config.getInt("handcuffs.slowness-level", 3);
        darknessLevel = config.getInt("handcuffs.darkness-level", 1);
        useMiningFatigue = config.getBoolean("handcuffs.use-mining-fatigue", false);
        miningFatigueLevel = config.getInt("handcuffs.mining-fatigue-level", 2);

        slownessLevel = Math.max(1, Math.min(255, slownessLevel));
        darknessLevel = Math.max(1, Math.min(255, darknessLevel));
        miningFatigueLevel = Math.max(1, Math.min(255, miningFatigueLevel));
    }
    
    /**
     * Algema um jogador
     */
    public CompletableFuture<Boolean> handcuffPlayer(Player player, Player staff) {
        UUID uuid = player.getUniqueId();
        if (isHandcuffed(uuid)) {
            if (staff != null) {
                messageService.sendMessage(staff, "already_handcuffed", "{player}", player.getName());
            }
            return CompletableFuture.completedFuture(false);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            Prisoner prisoner = prisonerManager.getPrisoner(uuid);
            if (prisoner == null) {
                if (staff != null) {
                    Tasks.sync(() -> messageService.sendMessage(staff, "player_not_jailed", "{player}", player.getName()));
                }
                return false;
            }

            prisoner.setHandcuffed(true);
            handcuffedPlayers.add(uuid);

            Tasks.sync(() -> applyHandcuffEffects(player));
            
            return true;
        }).thenCompose(result -> {
            if (!result) {
                return CompletableFuture.completedFuture(false);
            }
            
            Prisoner prisoner = prisonerManager.getPrisoner(uuid);
            return storage.savePrisoner(prisoner)
                .thenApply(success -> {
                    if (success) {
                        Tasks.sync(() -> {
                            messageService.sendMessage(player, "handcuff_notify");

                            if (staff != null) {
                                messageService.sendMessage(staff, "handcuff_applied", "{player}", player.getName());
                            }
                        });

                        // Usar log mais conciso
                        if (staff != null) {
                            logger.info("Player " + player.getName() + " handcuffed by " + staff.getName());
                        }
                    }
                    return success;
                });
        });
    }
    
    /**
     * Remove algemas de um jogador (por UUID)
     */
    public CompletableFuture<Boolean> removeHandcuffs(UUID playerUuid) {
        if (!isHandcuffed(playerUuid)) {
            return CompletableFuture.completedFuture(false);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            Prisoner prisoner = prisonerManager.getPrisoner(playerUuid);
            if (prisoner == null) {

                handcuffedPlayers.remove(playerUuid);
                return true;
            }

            prisoner.setHandcuffed(false);
            handcuffedPlayers.remove(playerUuid);

            Player player = org.bukkit.Bukkit.getPlayer(playerUuid);
            if (player != null && player.isOnline()) {
                Tasks.sync(() -> removeHandcuffEffects(player));
            }
            
            return prisoner;
        }).thenCompose(result -> {
            if (result instanceof Boolean) {
                return CompletableFuture.completedFuture((Boolean) result);
            }
            
            Prisoner prisoner = (Prisoner) result;
            return storage.savePrisoner(prisoner)
                .thenApply(success -> {
                    if (success) {
                        Player player = org.bukkit.Bukkit.getPlayer(playerUuid);

                        if (player != null && player.isOnline()) {
                            Tasks.sync(() -> {
                                messageService.sendMessage(player, "unhandcuff_notify");
                            });
                        }

                        // Usar log mais conciso
                        logger.info("Handcuffs removed from player " + (player != null ? player.getName() : playerUuid.toString()));
                    }
                    return success;
                });
        });
    }
    
    /**
     * Verifica se um jogador está algemado
     */
    public boolean isHandcuffed(UUID playerUuid) {
        if (handcuffedPlayers.contains(playerUuid)) {
            return true;
        }
        try {
            Prisoner prisoner = prisonerManager.getPrisoner(playerUuid);
            return prisoner != null && prisoner.isHandcuffed();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Verifica se um jogador está algemado
     */
    public boolean isHandcuffed(Player player) {
        return isHandcuffed(player.getUniqueId());
    }
    
    /**
     * Aplica efeitos de algemas a um jogador
     */
    private void applyHandcuffEffects(Player player) {

        removeHandcuffEffects(player);

        PotionEffect slowness = new PotionEffect(
            PotionEffectType.SLOW, 
            Integer.MAX_VALUE, 
            slownessLevel - 1, 
            false, 
            false, 
            false
        );
        player.addPotionEffect(slowness);

        if (useMiningFatigue) {
            PotionEffect miningFatigue = new PotionEffect(
                PotionEffectType.SLOW_DIGGING,
                Integer.MAX_VALUE,
                miningFatigueLevel - 1,
                false,
                false,
                false
            );
            player.addPotionEffect(miningFatigue);
        } else {

            try {
                PotionEffectType darknessType = PotionEffectType.getByName("DARKNESS");
                if (darknessType != null) {
                    PotionEffect darkness = new PotionEffect(
                        darknessType,
                        Integer.MAX_VALUE,
                        darknessLevel - 1,
                        false,
                        false,
                        false
                    );
                    player.addPotionEffect(darkness);
                } else {

                    PotionEffect miningFatigue = new PotionEffect(
                        PotionEffectType.SLOW_DIGGING,
                        Integer.MAX_VALUE,
                        miningFatigueLevel - 1,
                        false,
                        false,
                        false
                    );
                    player.addPotionEffect(miningFatigue);
                }
            } catch (Exception e) {

                PotionEffect miningFatigue = new PotionEffect(
                    PotionEffectType.SLOW_DIGGING,
                    Integer.MAX_VALUE,
                    miningFatigueLevel - 1,
                    false,
                    false,
                    false
                );
                player.addPotionEffect(miningFatigue);
            }
        }
    }
    
    /**
     * Remove efeitos de algemas de um jogador
     */
    private void removeHandcuffEffects(Player player) {

        player.removePotionEffect(PotionEffectType.SLOW);

        player.removePotionEffect(PotionEffectType.SLOW_DIGGING);

        try {
            PotionEffectType darknessType = PotionEffectType.getByName("DARKNESS");
            if (darknessType != null) {
                player.removePotionEffect(darknessType);
            }
        } catch (Exception e) {

        }
    }
    
    /**
     * Processa login do jogador - reaplica algemas se necessário
     */
    public void onPlayerJoin(Player player) {
        UUID uuid = player.getUniqueId();
        Tasks.async(() -> {
            Prisoner prisoner = prisonerManager.getPrisoner(uuid);
            if (prisoner != null && prisoner.isHandcuffed()) {
                handcuffedPlayers.add(uuid);

                Tasks.syncDelayed(() -> applyHandcuffEffects(player), 20L); 
            }
        });
    }
    
    /**
     * Processa saída do jogador - limpa cache
     */
    public void onPlayerQuit(Player player) {
        handcuffedPlayers.remove(player.getUniqueId());
    }
    
    /**
     * Remove automaticamente algemas quando jogador é solto
     */
    public void onPlayerUnjailed(Player player) {
        if (isHandcuffed(player)) {
            handcuffedPlayers.remove(player.getUniqueId());
            removeHandcuffEffects(player);
        }
    }
    
    /**
     * Recarrega Configurações
     */
    public void reload() {
        loadConfig();
        // Usar log mais conciso
        logger.info("HandcuffService reloaded");
    }
    
    public HandcuffStats getStats() {
        int totalHandcuffed = handcuffedPlayers.size();
        return new HandcuffStats(totalHandcuffed, slownessLevel, darknessLevel, useMiningFatigue);
    }
    
    /**
     * Estatísticas das algemas
     */
    public static class HandcuffStats {
        private final int totalHandcuffed;
        private final int slownessLevel;
        private final int darknessLevel;
        private final boolean useMiningFatigue;
        
        public HandcuffStats(int totalHandcuffed, int slownessLevel, int darknessLevel, boolean useMiningFatigue) {
            this.totalHandcuffed = totalHandcuffed;
            this.slownessLevel = slownessLevel;
            this.darknessLevel = darknessLevel;
            this.useMiningFatigue = useMiningFatigue;
        }
        
        public int getTotalHandcuffed() { return totalHandcuffed; }
        public int getSlownessLevel() { return slownessLevel; }
        public int getDarknessLevel() { return darknessLevel; }
        public boolean isUsingMiningFatigue() { return useMiningFatigue; }
        
        @Override
        public String toString() {
            return String.format("HandcuffStats{handcuffed=%d, slowness=%d, darkness=%d, miningFatigue=%s}",
                    totalHandcuffed, slownessLevel, darknessLevel, useMiningFatigue);
        }
    }
}