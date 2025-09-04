package br.com.devjails.command.subcommands;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.bukkit.command.CommandSender;

import br.com.devjails.DevJailsPlugin;
import br.com.devjails.command.BaseSubCommand;

/**
 * Subcomando /djails reload
 */
public class ReloadSubCommand extends BaseSubCommand {
    
    public ReloadSubCommand(DevJailsPlugin plugin) {
        super(plugin);
    }
    
    @Override
    public String getName() {
        return "reload";
    }
    
    @Override
    public String getDescription() {
        return "Recarrega as configurações do plugin";
    }
    
    @Override
    public String getUsage() {
        return "/djails reload";
    }
    
    @Override
    public List<String> getAliases() {
        return Arrays.asList();
    }
    
    @Override
    public String getPermission() {
        return "djails.reload";
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("djails.reload") && !sender.hasPermission("djails.admin")) {
            messageService.sendMessage(sender, "no_permission");
            return;
        }
        
        // Recarregar o plugin
        CompletableFuture<Boolean> future = plugin.reloadPlugin();
        future.thenAccept(success -> {
            if (success) {
                messageService.sendMessage(sender, "reload_success");
            } else {
                messageService.sendMessage(sender, "reload_failed");
            }
        }).exceptionally(throwable -> {
            messageService.sendMessage(sender, "reload_failed");
            plugin.getLogger().severe("Erro ao recarregar o plugin: " + throwable.getMessage());
            return null;
        });
    }
    
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return super.tabComplete(sender, args);
    }
}