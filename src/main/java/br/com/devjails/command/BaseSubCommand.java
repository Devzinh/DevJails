package br.com.devjails.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import br.com.devjails.DevJailsPlugin;
import br.com.devjails.message.MessageService;

public abstract class BaseSubCommand implements SubCommand {
    
    protected final DevJailsPlugin plugin;
    protected final MessageService messageService;
    
    public BaseSubCommand(DevJailsPlugin plugin) {
        this.plugin = plugin;
        this.messageService = plugin.getMessageService();
    }
    
    @Override
    public List<String> getAliases() {
        return Collections.emptyList();
    }
    
    @Override
    public boolean hasPermission(CommandSender sender) {
        String permission = getPermission();
        return permission == null || sender.hasPermission(permission) || sender.hasPermission("djails.admin");
    }
    
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
    
    
    protected boolean requirePlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            messageService.sendMessage(sender, "command_only_player");
            return false;
        }
        return true;
    }
    
    
    protected Player findOnlinePlayer(CommandSender sender, String playerName) {
        Player player = Bukkit.getPlayerExact(playerName);
        if (player == null) {
            messageService.sendMessage(sender, "player_not_found", "{player}", playerName);
            return null;
        }
        return player;
    }
    
    
    protected org.bukkit.OfflinePlayer findOfflinePlayer(String playerName) {
        return Bukkit.getOfflinePlayer(playerName);
    }
    
    
    protected boolean checkArgs(CommandSender sender, String[] args, int minArgs) {
        if (args.length < minArgs) {
            messageService.sendMessage(sender, "command_usage", "{usage}", getUsage());
            return false;
        }
        return true;
    }
    
    protected List<String> getOnlinePlayerNames() {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }
        return names;
    }
    
    
    protected List<String> filterStartsWith(List<String> list, String prefix) {
        if (prefix.isEmpty()) {
            return list;
        }
        
        List<String> filtered = new ArrayList<>();
        String lowerPrefix = prefix.toLowerCase();
        
        for (String item : list) {
            if (item.toLowerCase().startsWith(lowerPrefix)) {
                filtered.add(item);
            }
        }
        
        return filtered;
    }
    
    protected List<String> filterStartsWith(List<String> list, String prefix, String prependText) {
        List<String> results = new ArrayList<>();
        
        for (String item : filterStartsWith(list, prefix)) {
            results.add(prependText + item);
        }
        
        return results;
    }

    protected boolean hasSpecificPermission(CommandSender sender, String permission) {
        if (!sender.hasPermission(permission) && !sender.hasPermission("djails.admin")) {
            messageService.sendMessage(sender, "no_permission");
            return false;
        }
        return true;
    }

    protected Player validatePlayerWithPermission(CommandSender sender, String permission) {
        if (!requirePlayer(sender)) {
            return null;
        }
        
        Player player = (Player) sender;
        if (!hasSpecificPermission(sender, permission)) {
            return null;
        }
        
        return player;
    }
    
    protected boolean executeSubCommand(CommandSender sender, String[] args, String usage, SubCommandHandler... handlers) {
        if (args.length == 0) {
            messageService.sendMessage(sender, "command_usage", "{usage}", usage);
            return false;
        }
        
        String subCommand = args[0].toLowerCase();
        
        for (SubCommandHandler handler : handlers) {
            if (handler.matches(subCommand)) {
                handler.execute(sender, args);
                return true;
            }
        }
        
        messageService.sendMessage(sender, "command_usage", "{usage}", usage);
        return false;
    }

    @FunctionalInterface
    public interface SubCommandHandler {
        void execute(CommandSender sender, String[] args);
        
        default boolean matches(String subCommand) {
            return false;
        }
    }

    public static class DefaultSubCommandHandler implements SubCommandHandler {
        private final String[] aliases;
        private final SubCommandExecutor executor;
        
        public DefaultSubCommandHandler(SubCommandExecutor executor, String... aliases) {
            this.aliases = aliases;
            this.executor = executor;
        }
        
        @Override
        public boolean matches(String subCommand) {
            for (String alias : aliases) {
                if (alias.equalsIgnoreCase(subCommand)) {
                    return true;
                }
            }
            return false;
        }
        
        @Override
        public void execute(CommandSender sender, String[] args) {
            executor.execute(sender, args);
        }
    }
    
    @FunctionalInterface
    public interface SubCommandExecutor {
        void execute(CommandSender sender, String[] args);
    }
    
    // ========== MÉTODOS UTILITÁRIOS PARA VALIDAÇÃO ==========
    
    protected OfflinePlayer validateOfflinePlayer(CommandSender sender, String playerName) {
        OfflinePlayer offlinePlayer = findOfflinePlayer(playerName);
        if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
            messageService.sendMessage(sender, "player_not_found", "{player}", playerName);
            return null;
        }
        return offlinePlayer;
    }
    
    protected boolean validateNotSelf(CommandSender sender, String targetPlayerName) {
        if (sender.getName().equalsIgnoreCase(targetPlayerName)) {
            messageService.sendMessage(sender, "cannot_jail_self");
            return false;
        }
        return true;
    }

    protected Double validateAndParseDouble(CommandSender sender, String numberStr, String fieldName) {
        try {
            double value = Double.parseDouble(numberStr);
            if (value < 0) {
                messageService.sendMessage(sender, "invalid_number", "{number}", numberStr);
                return null;
            }
            return value;
        } catch (NumberFormatException e) {
            messageService.sendMessage(sender, "invalid_number", "{number}", numberStr);
            return null;
        }
    }

    protected br.com.devjails.jail.Jail validateJail(CommandSender sender, String jailName) {
        br.com.devjails.jail.Jail jail = plugin.getJailManager().getJail(jailName);
        if (jail == null) {
            messageService.sendMessage(sender, "jail_not_found", "{jail}", jailName);
            return null;
        }
        return jail;
    }
    
    // ========== MÉTODOS UTILITÁRIOS PARA OPERAÇÕES ASSÍNCRONAS ==========

    protected void executeWithPrisonerCheck(CommandSender sender, OfflinePlayer player, 
                                           java.util.function.Consumer<Boolean> onResult) {
        plugin.getPrisonerManager().isPrisoner(player.getUniqueId())
            .thenAccept(onResult)
            .exceptionally(ex -> {
                plugin.getLogger().severe("Error checking prisoner status: " + ex.getMessage());
                messageService.sendMessage(sender, "storage_error");
                return null;
            });
    }

    protected <T> void executeAsync(CommandSender sender, java.util.concurrent.CompletableFuture<T> future,
                                  java.util.function.Consumer<T> onSuccess, String errorContext) {
        future.thenAccept(onSuccess)
              .exceptionally(ex -> {
                  plugin.getLogger().severe("Error in " + errorContext + ": " + ex.getMessage());
                  messageService.sendMessage(sender, "storage_error");
                  return null;
              });
    }

     // ========== MÉTODOS UTILITÁRIOS PARA TAB COMPLETION ==========
    
    protected List<String> getJailNames() {
        return new ArrayList<>(plugin.getJailManager().getJailNames());
    }

    protected List<String> getCommonReasons() {
        return Collections.emptyList();
    }

    protected List<String> getCommonDurations() {
        return Collections.emptyList();
    }

    protected List<String> getPrisonerNames() {
        List<String> names = new ArrayList<>();
        plugin.getPrisonerManager().getAllPrisoners().forEach(prisoner -> {
            if (prisoner.getPlayerName() != null) {
                names.add(prisoner.getPlayerName());
            }
        });
        return names;
    }
}