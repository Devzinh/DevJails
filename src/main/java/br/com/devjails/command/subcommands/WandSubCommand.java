package br.com.devjails.command.subcommands;

import java.util.Arrays;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import br.com.devjails.DevJailsPlugin;
import br.com.devjails.command.BaseSubCommand;
import br.com.devjails.flag.SelectionService;

/**
 * Subcomando /djails wand
 */
public class WandSubCommand extends BaseSubCommand {
    
    public WandSubCommand(DevJailsPlugin plugin) {
        super(plugin);
    }
    
    @Override
    public String getName() {
        return "wand";
    }
    
    @Override
    public String getDescription() {
        return "Obtém a varinha de seleção";
    }
    
    @Override
    public String getUsage() {
        return "/djails wand";
    }
    
    @Override
    public List<String> getAliases() {
        return Arrays.asList();
    }
    
    @Override
    public String getPermission() {
        return "djails.wand";
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = validatePlayerWithPermission(sender, "djails.wand");
        if (player == null) {
            return;
        }
        
        SelectionService selectionService = plugin.getSelectionService();
        ItemStack wand = selectionService.createWand();
        
        // Dar a varinha ao jogador
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(wand);
        } else {
            player.getWorld().dropItem(player.getLocation(), wand);
        }
        
        messageService.sendMessage(player, "wand_given");
    }
    
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return super.tabComplete(sender, args);
    }
}