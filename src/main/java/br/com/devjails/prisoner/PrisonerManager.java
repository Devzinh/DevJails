package br.com.devjails.prisoner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import br.com.devjails.DevJailsPlugin;
import br.com.devjails.api.events.PlayerEscapeEvent;
import br.com.devjails.jail.Jail;
import br.com.devjails.jail.JailManager;
import br.com.devjails.message.MessageService;
import br.com.devjails.storage.Storage;
import br.com.devjails.util.Constants;
import br.com.devjails.util.Tasks;
import br.com.devjails.util.TimeParser;
import br.com.devjails.handcuff.HandcuffService;


public class PrisonerManager {
    
    private final DevJailsPlugin plugin;
    private final Storage storage;
    private final JailManager jailManager;
    private final MessageService messageService;
    private final Logger logger;
    private HandcuffService handcuffService;
    
    private final Map<UUID, Prisoner> prisoners;
    private final Set<UUID> pendingOfflineActions;
    private final Set<UUID> recentEscapeAttempts = ConcurrentHashMap.newKeySet(); 
    
    private BukkitTask expirationTask;
    
    public PrisonerManager(DevJailsPlugin plugin, Storage storage, JailManager jailManager, MessageService messageService, Logger logger) {
        this.plugin = plugin;
        this.storage = storage;
        this.jailManager = jailManager;
        this.messageService = messageService;
        this.logger = logger;
        this.prisoners = new ConcurrentHashMap<>();
        this.pendingOfflineActions = ConcurrentHashMap.newKeySet();
    }
    
    public void setHandcuffService(HandcuffService handcuffService) {
        this.handcuffService = handcuffService;
    }
    
    public CompletableFuture<Boolean> initialize() {
        return loadAllPrisoners().thenApply(success -> {
            if (success) {
                startExpirationTask();
                // Reduzir a verbosidade do log de inicialização
                if (prisoners.size() > 0) {
                    logger.info("PrisonerManager initialized - " + prisoners.size() + " prisoners loaded");
                }
            }
            return success;
        });
    }
    
    public void shutdown() {
        if (expirationTask != null) {
            expirationTask.cancel();
        }
    }    
    
    public CompletableFuture<Boolean> jailPlayer(UUID playerUuid, String jailName, String reason, String staff) {
        return jailPlayer(playerUuid, jailName, reason, staff, -1);
    }
    
    public CompletableFuture<Boolean> jailPlayer(UUID playerUuid, String jailName, String reason, String staff, long durationMillis) {
        Jail jail = jailManager.getJail(jailName);
        if (jail == null) {
            return CompletableFuture.completedFuture(false);
        }
        if (isJailed(playerUuid)) {
            return CompletableFuture.completedFuture(false);
        }
        Prisoner prisoner;
        if (durationMillis > 0) {
            prisoner = new Prisoner(playerUuid, jailName, reason, staff, durationMillis);
        } else {
            prisoner = new Prisoner(playerUuid, jailName, reason, staff);
        }
        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null) {
            prisoner.setOriginalLocation(player.getLocation());
        } else {
            Location spawnLocation = Bukkit.getWorlds().get(0).getSpawnLocation();
            prisoner.setOriginalLocation(spawnLocation);
        }
        
        return storage.savePrisoner(prisoner).thenApply(success -> {
            if (success) {
                prisoners.put(playerUuid, prisoner);

                if (player != null) {
                    prisoner.markOnline();
                    
                    Tasks.sync(() -> {
                        player.teleport(jail.getSpawnLocation());
                        notifyPlayerJailed(player, prisoner);
                        
                        if (handcuffService != null) {
                            handcuffService.handcuffPlayer(player, null);
                        }
                    });
                } else {
                    pendingOfflineActions.add(playerUuid);
                }

                // Usar log mais conciso
                logger.info("Player " + playerUuid + " jailed in " + jailName);
            }
            return success;
        });
    }
     
    public CompletableFuture<Boolean> unjailPlayer(UUID playerUuid, String staff) {
        Prisoner prisoner = prisoners.get(playerUuid);
        if (prisoner == null) {
            return CompletableFuture.completedFuture(false);
        }
        
        return storage.removePrisoner(playerUuid).thenApply(success -> {
            if (success) {
                prisoners.remove(playerUuid);
                pendingOfflineActions.remove(playerUuid);

                Player player = Bukkit.getPlayer(playerUuid);
                if (player != null) {
                    Tasks.sync(() -> {
                        Location releaseLocation = getReleaseLocation(prisoner);
                        if (releaseLocation != null) {
                            player.teleport(releaseLocation);
                        }
                        notifyPlayerUnjailed(player, staff);
                        
                        if (handcuffService != null) {
                            handcuffService.onPlayerUnjailed(player);
                        }
                    });
                }
            }
            return success;
        });
    }
       
    public CompletableFuture<Boolean> extendJailTime(UUID playerUuid, long additionalMillis) {
        Prisoner prisoner = prisoners.get(playerUuid);
        if (prisoner == null) {
            return CompletableFuture.completedFuture(false);
        }
        
        prisoner.extendTime(additionalMillis);
        
        return storage.savePrisoner(prisoner).thenApply(success -> {
            if (success) {
                // Usar log mais conciso
                logger.info("Jail time extended for " + playerUuid);
            }
            return success;
        });
    }    
    
    public boolean isJailed(UUID playerUuid) {
        return prisoners.containsKey(playerUuid);
    }
    
    public Prisoner getPrisoner(UUID playerUuid) {
        return prisoners.get(playerUuid);
    }
    
    public Collection<Prisoner> getAllPrisoners() {
        return prisoners.values();
    }
    
    public List<Prisoner> getPrisonersByJail(String jailName) {
        return prisoners.values().stream()
                .filter(p -> jailName.equalsIgnoreCase(p.getJailName()))
                .toList();
    }
    
    public List<Prisoner> getPrisonersWithBail() {
        return prisoners.values().stream()
                .filter(Prisoner::hasBail)
                .toList();
    }    
    
    public void onPlayerJoin(Player player) {
        UUID uuid = player.getUniqueId();
        
        Prisoner prisoner = prisoners.get(uuid);
        if (prisoner != null) {
            prisoner.markOnline();
        }
        
        if (pendingOfflineActions.contains(uuid)) {
            if (prisoner != null) {
                Jail jail = jailManager.getJail(prisoner.getJailName());
                if (jail != null) {
                    Tasks.syncDelayed(() -> {
                        player.teleport(jail.getSpawnLocation());
                        notifyPlayerJailed(player, prisoner);
                        
                        if (handcuffService != null) {
                            handcuffService.handcuffPlayer(player, null);
                        }
                    }, 20L);
                }
            }
            pendingOfflineActions.remove(uuid);
        }
    }
        
    public void onPlayerQuit(Player player) {
        UUID uuid = player.getUniqueId();
        
        Prisoner prisoner = prisoners.get(uuid);
        if (prisoner != null) {
            prisoner.markOffline();
        }
    }    
    
    public void handleEscapeAttempt(Player player, Jail jail) {
        UUID uuid = player.getUniqueId();
        Prisoner prisoner = prisoners.get(uuid);
        
        if (prisoner == null) {
            return;
        }

        // Anti-spam throttle for escape attempts
        if (recentEscapeAttempts.contains(uuid)) {
            return;
        }
        recentEscapeAttempts.add(uuid);
        Tasks.syncDelayed(() -> recentEscapeAttempts.remove(uuid), 100L);

        PlayerEscapeEvent escapeEvent = new PlayerEscapeEvent(player, prisoner, jail, 
            player.getLocation(), jail.getSpawnLocation());
        this.plugin.getServer().getPluginManager().callEvent(escapeEvent);
        
        if (escapeEvent.isCancelled()) {
            return;
        }

        boolean teleportBack = this.plugin.getConfig().getBoolean(Constants.Config.TELEPORT_BACK_ON_ESCAPE_KEY, true);
        if (teleportBack) {
            Tasks.sync(() -> player.teleport(jail.getSpawnLocation()));
        }

        double fineAmount = this.plugin.getConfig().getDouble(Constants.Config.FINE_ON_ESCAPE_KEY, 0.0);
        boolean fineApplied = false;
        if (fineAmount > 0 && this.plugin.getVaultHook().isEnabled()) {
            if (this.plugin.getVaultHook().hasBalance(player, fineAmount)) {
                if (this.plugin.getVaultHook().withdraw(player, fineAmount)) {
                    fineApplied = true;
                }
            }
        }

        long totalExtendMillis = 0;
        String timeExtended = null;
        
        String extendTimeStr = this.plugin.getConfig().getString(Constants.Config.EXTEND_ON_ESCAPE_KEY, "");
        if (!extendTimeStr.isEmpty() && !prisoner.isPermanent()) {
            try {
                long extendMillis = TimeParser.parseTimeToMillis(extendTimeStr);
                if (extendMillis > 0) {
                    totalExtendMillis += extendMillis;
                }
            } catch (Exception e) {
                logger.warning("Error parsing extend time: " + extendTimeStr);
            }
        }
        
        if (totalExtendMillis > 0 && !prisoner.isPermanent()) {
            prisoner.extendTime(totalExtendMillis);
            storage.savePrisoner(prisoner);
            timeExtended = TimeParser.formatTime(totalExtendMillis);
        }

        List<String> escapeCommands = this.plugin.getConfig().getStringList(Constants.Config.RUN_COMMANDS_ON_ESCAPE_KEY);
        
        List<String> allCommands = new ArrayList<>(escapeCommands);
        
        if (!allCommands.isEmpty()) {
            Tasks.sync(() -> {
                for (String command : allCommands) {
                    String processedCommand = command
                        .replace("{player}", player.getName())
                        .replace("{jail}", jail.getName());
                    
                    try {
                        if (processedCommand.startsWith("title ")) {
                            executeTitle(processedCommand, player);
                        } else {
                            this.plugin.getServer().dispatchCommand(this.plugin.getServer().getConsoleSender(), processedCommand);
                        }
                    } catch (Exception e) {
                        logger.warning("Error executing escape command: " + processedCommand);
                    }
                }
            });
        }

        boolean titleEnabled = this.plugin.getConfig().getBoolean(Constants.Config.ESCAPE_TITLE_ENABLED_KEY, true);
        if (titleEnabled) {
            String titleMain = this.plugin.getConfig().getString(Constants.Config.ESCAPE_TITLE_MAIN_KEY, 
                messageService.getMessage("escape_title_main"));
            String titleSubtitle = this.plugin.getConfig().getString(Constants.Config.ESCAPE_TITLE_SUBTITLE_KEY, 
                messageService.getMessage("escape_title_subtitle"));
            
            int fadeIn = this.plugin.getConfig().getInt(Constants.Config.ESCAPE_TITLE_FADE_IN_KEY, 10);
            int stay = this.plugin.getConfig().getInt(Constants.Config.ESCAPE_TITLE_STAY_KEY, 60);
            int fadeOut = this.plugin.getConfig().getInt(Constants.Config.ESCAPE_TITLE_FADE_OUT_KEY, 20);
            
            Tasks.sync(() -> {
                player.sendTitle(titleMain, titleSubtitle, fadeIn, stay, fadeOut);
            });
        }
        
        messageService.sendEscapeNotification(player, teleportBack, fineApplied ? fineAmount : 0, timeExtended);

        boolean logAttempts = this.plugin.getConfig().getBoolean(Constants.Config.LOG_ESCAPE_ATTEMPTS_KEY, true);
        if (logAttempts) {
            // Usar log mais conciso
            logger.info("Escape attempt by " + player.getName() + " from " + jail.getName());
        }
        
        boolean broadcastAttempts = this.plugin.getConfig().getBoolean(Constants.Config.BROADCAST_ESCAPE_ATTEMPTS_KEY, false);
        if (broadcastAttempts) {
            String broadcastMessage = messageService.getMessage("escape_broadcast", 
                "{player}", player.getName(), 
                "{jail}", jail.getName());
            messageService.broadcastToPermission(broadcastMessage, "djails.notify", Collections.emptySet());
        }
    }
    
    /**
     * Executa comandos de título de forma segura, evitando erros de JSON malformado
     */
    private void executeTitle(String command, Player player) {
        try {
            String[] parts = command.split(" ", 4);
            if (parts.length >= 4) {
                String titleType = parts[2];
                String message = parts[3];
                
                message = message.replace("§", "&");
                
                if ("title".equals(titleType)) {
                    player.sendTitle(message, "", 10, 40, 10);
                } else if ("subtitle".equals(titleType)) {
                    player.sendTitle("", message, 10, 40, 10);
                }
            }
        } catch (Exception e) {
            logger.warning("Error executing title command: " + command + " - " + e.getMessage());
        }
    }
    
    private Location getReleaseLocation(Prisoner prisoner) {
        if (plugin.getConfig().contains("release-spawn.world")) {
            try {
                String worldName = plugin.getConfig().getString("release-spawn.world");
                double x = plugin.getConfig().getDouble("release-spawn.x");
                double y = plugin.getConfig().getDouble("release-spawn.y");
                double z = plugin.getConfig().getDouble("release-spawn.z");
                float yaw = (float) plugin.getConfig().getDouble("release-spawn.yaw");
                float pitch = (float) plugin.getConfig().getDouble("release-spawn.pitch");
                
                org.bukkit.World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    return new Location(world, x, y, z, yaw, pitch);
                }
            } catch (Exception e) {
                logger.warning("Error loading custom spawn: " + e.getMessage());
            }
        }
        
        switch (prisoner.getPostReleaseSpawnChoice()) {
            case ORIGINAL_LOCATION:
                Location original = prisoner.getOriginalLocation();
                if (original != null && original.getWorld() != null) {
                    return original;
                }
                return Bukkit.getWorlds().get(0).getSpawnLocation();
                
            case WORLD_SPAWN:
            default:
                return Bukkit.getWorlds().get(0).getSpawnLocation();
        }
    }
    
    
    private void notifyPlayerJailed(Player player, Prisoner prisoner) {
        String duration = prisoner.isPermanent() ? 
            messageService.getLocalizedMessage(messageService.getPlayerLanguage(player), "time_permanent") :
            TimeParser.formatTime(prisoner.getRemainingTimeMillis());
            
        messageService.sendJailNotification(player, 
            prisoner.getJailName(), 
            prisoner.getReason(), 
            prisoner.getStaff(), 
            duration);
    }
    
    
    private void notifyPlayerUnjailed(Player player, String staff) {
        if (staff != null) {
            messageService.sendUnifiedMessage(player, "unjail_notify_player");
        } else {
            messageService.sendUnifiedMessage(player, "jail_expired");
        }
    }
    
    
    private void broadcastJailMessage(Prisoner prisoner) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(prisoner.getUuid());
        String playerName = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown";
        
        // Usar log mais conciso
        logger.info("Player " + playerName + " jailed in " + prisoner.getJailName());
        
        java.util.Set<java.util.UUID> exclude = java.util.Set.of(prisoner.getUuid());
        String messageKey = prisoner.isPermanent() ? "player_jailed" : "player_jailed_temp";
        Object[] placeholders = prisoner.isPermanent() ? 
            new Object[]{"{player}", playerName, "{jail}", prisoner.getJailName(), "{staff}", prisoner.getStaff()} :
            new Object[]{"{player}", playerName, "{jail}", prisoner.getJailName(), "{duration}", TimeParser.formatTime(prisoner.getRemainingTimeMillis()), "{staff}", prisoner.getStaff()};
            
        messageService.broadcastToPermission("djails.notify", exclude, messageKey, placeholders);
    }
    
    
    private void broadcastUnjailMessage(Prisoner prisoner, String staff) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(prisoner.getUuid());
        String playerName = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown";
        
        // Usar log mais conciso
        logger.info("Player " + playerName + " unjailed by " + (staff != null ? staff : "System"));

        java.util.Set<java.util.UUID> exclude = java.util.Set.of(prisoner.getUuid());
        messageService.broadcastToPermission("djails.notify", exclude, "player_unjailed",
            "{player}", playerName,
            "{staff}", staff != null ? staff : "System");
    }
    
    
    private CompletableFuture<Boolean> loadAllPrisoners() {
        return storage.loadAllPrisoners().thenApply(prisonerList -> {
            prisoners.clear();
            for (Prisoner prisoner : prisonerList) {
                prisoners.put(prisoner.getUuid(), prisoner);
                if (Bukkit.getPlayer(prisoner.getUuid()) != null) {
                    prisoner.markOnline();
                    pendingOfflineActions.add(prisoner.getUuid());
                }
            }
            return true;
        }).exceptionally(ex -> {
            logger.severe("Error loading prisoners: " + ex.getMessage());
            ex.printStackTrace();
            return false;
        });
    }
    
    
    private void startExpirationTask() {
        expirationTask = Tasks.syncRepeating(() -> {
            List<UUID> expired = new ArrayList<>();
            
            for (Prisoner prisoner : prisoners.values()) {
                if (!prisoner.isPermanent() && prisoner.isExpired()) {
                    expired.add(prisoner.getUuid());
                }
            }

            for (UUID uuid : expired) {
                unjailPlayer(uuid, null); 
            }
            
        }, 20L, 20L);
    }
    
    
    public CompletableFuture<Boolean> reload() {
        if (expirationTask != null) {
            expirationTask.cancel();
        }
        return initialize();
    }    
    
    public CompletableFuture<Boolean> isPrisoner(UUID playerUuid) {
        return CompletableFuture.completedFuture(prisoners.containsKey(playerUuid));
    }
    
    /**
     * Verifica se um jogador é um prisioneiro (versão síncrona)
     * @param playerUuid UUID do jogador
     * @return true se o jogador for um prisioneiro, false caso contrário
     */
    public boolean isPrisonerSync(UUID playerUuid) {
        return prisoners.containsKey(playerUuid);
    }
    
    public CompletableFuture<Boolean> setBail(UUID playerUuid, double amount, String staff) {
        Prisoner prisoner = prisoners.get(playerUuid);
        if (prisoner == null) {
            return CompletableFuture.completedFuture(false);
        }
        
        prisoner.setBailAmount(amount);
        
        return storage.savePrisoner(prisoner).thenApply(success -> {
            if (success) {
                // Usar log mais conciso
                logger.info("Bail set for player " + playerUuid);

                Player player = Bukkit.getPlayer(playerUuid);
                if (player != null && player.isOnline()) {
                    Tasks.sync(() -> {
                        if (player.isOnline()) {
                            messageService.sendMessage(player, "bail_set", 
                                "{amount}", String.format("%.2f", amount));
                        }
                    });
                }
            }
            return success;
        });
    }
    
    public CompletableFuture<Boolean> releasePrisoner(UUID playerUuid, String staff) {
        return unjailPlayer(playerUuid, staff);
    }
    
    public CompletableFuture<Boolean> jailPlayer(UUID playerUuid, String playerName, String jailName, 
                                                  String reason, String staff, boolean permanent, long durationMillis) {
        if (permanent) {
            return jailPlayer(playerUuid, jailName, reason, staff);
        } else {
            return jailPlayer(playerUuid, jailName, reason, staff, durationMillis);
        }
    }
}