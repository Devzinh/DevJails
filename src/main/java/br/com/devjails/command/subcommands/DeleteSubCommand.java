package br.com.devjails.command.subcommands;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import br.com.devjails.DevJailsPlugin;
import br.com.devjails.command.BaseSubCommand;
import br.com.devjails.jail.JailManager;

/**
 * Subcomando /djails delete
 */
public class DeleteSubCommand extends BaseSubCommand {
    
    public DeleteSubCommand(DevJailsPlugin plugin) {
        super(plugin);
    }
    
    @Override
    public String getName() {
        return "delete";
    }
    
    @Override
    public String getDescription() {
        return "Deleta elementos do sistema";
    }
    
    @Override
    public String getUsage() {
        return "/djails delete <jail|flag> <nome>";
    }
    
    @Override
    public List<String> getAliases() {
        return Arrays.asList();
    }
    
    @Override
    public String getPermission() {
        return "djails.delete";
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!requirePlayer(sender)) {
            return;
        }
        
        if (!checkArgs(sender, args, 2)) {
            return;
        }
        
        Player player = (Player) sender;
        
        executeSubCommand(sender, args, getUsage(),
            new DefaultSubCommandHandler((s, a) -> deleteJail(player, a[1]), "jail"),
            new DefaultSubCommandHandler((s, a) -> deleteFlag(player, a[1]), "flag")
        );
    }
    
    /**
     * Deleta uma cadeia
     */
    private void deleteJail(Player player, String name) {
        if (!player.hasPermission("djails.deletejail") && !player.hasPermission("djails.admin")) {
            messageService.sendMessage(player, "no_permission");
            return;
        }
        
        JailManager jailManager = plugin.getJailManager();
        
        // Verificar se a cadeia existe
        if (!jailManager.jailExists(name)) {
            messageService.sendMessage(player, "jail_not_found", "{jail}", name);
            return;
        }
        
        CompletableFuture<Boolean> future = jailManager.removeJail(name);
        future.thenAccept(success -> {
            if (success) {
                messageService.sendMessage(player, "jail_deleted", "{jail}", name);
            } else {
                messageService.sendMessage(player, "storage_error");
            }
        }).exceptionally(throwable -> {
            messageService.sendMessage(player, "storage_error");
            plugin.getLogger().severe("Erro ao deletar cadeia " + name + ": " + throwable.getMessage());
            return null;
        });
    }
    
    /**
     * Deleta uma área (flag)
     */
    private void deleteFlag(Player player, String name) {
        if (!player.hasPermission("djails.deleteflag") && !player.hasPermission("djails.admin")) {
            messageService.sendMessage(player, "no_permission");
            return;
        }
        
        JailManager jailManager = plugin.getJailManager();
        
        // Verificar se a flag existe
        if (!jailManager.flagExists(name)) {
            messageService.sendMessage(player, "flag_not_found", "{flag}", name);
            return;
        }
        
        CompletableFuture<Boolean> future = jailManager.removeFlag(name);
        future.thenAccept(success -> {
            if (success) {
                messageService.sendMessage(player, "flag_deleted", "{flag}", name);
            } else {
                messageService.sendMessage(player, "storage_error");
            }
        }).exceptionally(throwable -> {
            messageService.sendMessage(player, "storage_error");
            plugin.getLogger().severe("Erro ao deletar flag " + name + ": " + throwable.getMessage());
            return null;
        });
    }
    
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filterStartsWith(Arrays.asList("jail", "flag"), args[0]);
        }
        
        return super.tabComplete(sender, args);
    }
}