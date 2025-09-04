package br.com.devjails.command.subcommands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import br.com.devjails.DevJailsPlugin;
import br.com.devjails.command.BaseSubCommand;
import br.com.devjails.jail.JailManager;

/**
 * Subcomando /djails link
 */
public class LinkSubCommand extends BaseSubCommand {
    
    public LinkSubCommand(DevJailsPlugin plugin) {
        super(plugin);
    }
    
    @Override
    public String getName() {
        return "link";
    }
    
    @Override
    public String getDescription() {
        return "Vincula uma cadeia a uma área";
    }
    
    @Override
    public String getUsage() {
        return "/djails link <cadeia> <flag|wg:região>";
    }
    
    @Override
    public List<String> getAliases() {
        return Arrays.asList();
    }
    
    @Override
    public String getPermission() {
        return "djails.link";
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            messageService.sendMessage(sender, "command_only_player");
            return;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("djails.link") && !player.hasPermission("djails.admin")) {
            messageService.sendMessage(player, "no_permission");
            return;
        }
        
        if (args.length < 2) {
            messageService.sendMessage(sender, "command_usage", "{usage}", getUsage());
            return;
        }
        
        String jailName = args[0];
        String areaRef = args[1];
        
        JailManager jailManager = plugin.getJailManager();
        
        // Verificar se a cadeia existe
        if (!jailManager.jailExists(jailName)) {
            messageService.sendMessage(player, "jail_not_found", "{jail}", jailName);
            return;
        }
        
        // Vincular a cadeia à área
        CompletableFuture<Boolean> future = jailManager.linkJailToArea(jailName, areaRef);
        future.thenAccept(success -> {
            if (success) {
                messageService.sendMessage(player, "jail_linked", "{jail}", jailName, "{area}", areaRef);
            } else {
                messageService.sendMessage(player, "link_invalid_format");
            }
        }).exceptionally(throwable -> {
            messageService.sendMessage(player, "storage_error");
            plugin.getLogger().severe("Erro ao vincular cadeia " + jailName + " à área " + areaRef + ": " + throwable.getMessage());
            return null;
        });
    }
    
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            JailManager jailManager = plugin.getJailManager();
            List<String> jailNames = jailManager.getJailNames().stream().collect(Collectors.toList());
            return filterStartsWith(jailNames, args[0]);
        }
        
        if (args.length == 2) {
            List<String> suggestions = new ArrayList<>();
            
            // Adicionar flags disponíveis
            JailManager jailManager = plugin.getJailManager();
            suggestions.addAll(jailManager.getFlagNames());
            
            // Adicionar regiões do WorldGuard se disponível
            if (plugin.getWorldGuardHook().isEnabled() && sender instanceof Player) {
                Player player = (Player) sender;
                World world = player.getWorld();
                Set<String> regionNames = plugin.getWorldGuardHook().getRegionNames(world);
                
                // Adicionar regiões com prefixo "wg:"
                for (String regionName : regionNames) {
                    suggestions.add("wg:" + regionName);
                }
            }
            
            return filterStartsWith(suggestions, args[1]);
        }
        
        return super.tabComplete(sender, args);
    }
}