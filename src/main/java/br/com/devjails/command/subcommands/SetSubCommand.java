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
 * Subcomando /djails set
 */
public class SetSubCommand extends BaseSubCommand {
    
    public SetSubCommand(DevJailsPlugin plugin) {
        super(plugin);
    }
    
    @Override
    public String getName() {
        return "set";
    }
    
    @Override
    public String getDescription() {
        return "Define/configura elementos do sistema";
    }
    
    @Override
    public String getUsage() {
        return "/djails set <jail|flag> <nome>";
    }
    
    @Override
    public List<String> getAliases() {
        return Arrays.asList();
    }
    
    @Override
    public String getPermission() {
        return "djails.set";
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
            new DefaultSubCommandHandler((s, a) -> setJail(player, a[1]), "jail"),
            new DefaultSubCommandHandler((s, a) -> setFlag(player, a[1]), "flag")
        );
    }
    
    /**
     * Cria ou atualiza uma cadeia na localização do jogador
     */
    private void setJail(Player player, String name) {
        if (!player.hasPermission("djails.setjail") && !player.hasPermission("djails.admin")) {
            messageService.sendMessage(player, "no_permission");
            return;
        }
        
        JailManager jailManager = plugin.getJailManager();
        
        CompletableFuture<Boolean> future = jailManager.createOrUpdateJail(name, player.getLocation());
        future.thenAccept(success -> {
            if (success) {
                messageService.sendMessage(player, "jail_created", "{jail}", name);
            } else {
                messageService.sendMessage(player, "storage_error");
            }
        }).exceptionally(throwable -> {
            messageService.sendMessage(player, "storage_error");
            plugin.getLogger().severe("Erro ao criar/atualizar cadeia " + name + ": " + throwable.getMessage());
            return null;
        });
    }
    
    /**
     * Cria ou atualiza uma área (flag) com a seleção atual do jogador
     */
    private void setFlag(Player player, String name) {
        if (!player.hasPermission("djails.setflag") && !player.hasPermission("djails.admin")) {
            messageService.sendMessage(player, "no_permission");
            return;
        }
        
        // Verificar se o jogador tem uma seleção completa
        if (!plugin.getSelectionService().hasCompleteSelection(player)) {
            messageService.sendMessage(player, "selection_incomplete");
            return;
        }
        
        JailManager jailManager = plugin.getJailManager();
        var selection = plugin.getSelectionService().getPlayerSelection(player);
        
        CompletableFuture<Boolean> future = jailManager.createOrUpdateFlag(name, selection[0], selection[1]);
        future.thenAccept(success -> {
            if (success) {
                messageService.sendMessage(player, "flag_created", "{flag}", name);
            } else {
                messageService.sendMessage(player, "storage_error");
            }
        }).exceptionally(throwable -> {
            messageService.sendMessage(player, "storage_error");
            plugin.getLogger().severe("Erro ao criar/atualizar flag " + name + ": " + throwable.getMessage());
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