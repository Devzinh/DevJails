package br.com.devjails.command.subcommands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import br.com.devjails.DevJailsPlugin;
import br.com.devjails.bail.BailService;
import br.com.devjails.command.BaseSubCommand;
import br.com.devjails.gui.BailGUI;

/**
 * Subcomando /djails bail <set|remove|gui> [player] [amount]
 */
public class BailSubCommand extends BaseSubCommand {
    
    public BailSubCommand(DevJailsPlugin plugin) {
        super(plugin);
    }
    
    @Override
    public String getName() {
        return "bail";
    }
    
    @Override
    public String getDescription() {
        return "Gerencia sistema de Fianças";
    }
    
    @Override
    public String getUsage() {
        return "/djails bail <set|remove|gui> [jogador] [valor]";
    }
    
    @Override
    public List<String> getAliases() {
        return Arrays.asList("fianca", "f");
    }
    
    @Override
    public String getPermission() {
        return "djails.bail";
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!checkArgs(sender, args, 1)) {
            return;
        }
        
        BailService bailService = plugin.getBailService();
        if (!bailService.isEnabled()) {
            messageService.sendMessage(sender, "bail_enabled_vault_missing");
            return;
        }
        
        executeSubCommand(sender, args, getUsage(),
            new DefaultSubCommandHandler(this::setBail, "set", "definir"),
            new DefaultSubCommandHandler(this::removeBail, "remove", "remover"),
            new DefaultSubCommandHandler((s, a) -> openBailGUI(s), "gui", "menu")
        );
    }
    private void setBail(CommandSender sender, String[] args) {
        if (!sender.hasPermission("djails.bail.set")) {
            messageService.sendMessage(sender, "no_permission");
            return;
        }
        
        if (!checkArgs(sender, args, 3)) {
            messageService.sendMessage(sender, "command_usage", 
                "{usage}", "/djails bail set <jogador> <valor>");
            return;
        }
        
        String playerName = args[1];
        OfflinePlayer target = validateOfflinePlayer(sender, playerName);
        if (target == null) return;

        Double amount = validateAndParseDouble(sender, args[2], "invalid_bail_amount");
        if (amount == null || amount <= 0) {
            messageService.sendMessage(sender, "invalid_bail_amount");
            return;
        }
        executeWithPrisonerCheck(sender, target, isPrisoner -> {
            if (!isPrisoner) {
                messageService.sendMessage(sender, "player_not_jailed", "{player}", playerName);
                return;
            }
            
            executeAsync(sender,
                plugin.getBailService().setBail(target, amount, sender.getName()),
                success -> {
                    if (success) {
                        messageService.sendMessage(sender, "bail_set", 
                            "{player}", playerName,
                            "{amount}", String.format("%.2f", amount));

                        Player onlinePlayer = target.getPlayer();
                        if (onlinePlayer != null && onlinePlayer.isOnline()) {
                            messageService.sendMessage(onlinePlayer, "bail_set_notify", 
                                "{amount}", String.format("%.2f", amount));
                        }
                    } else {
                        messageService.sendMessage(sender, "bail_set_failed");
                    }
                },
                "definir fiança para " + playerName
            );
        });
    }
    
    /**
     * Remove Fiança de um prisioneiro
     */
    private void removeBail(CommandSender sender, String[] args) {
        if (!sender.hasPermission("djails.bail.remove")) {
            messageService.sendMessage(sender, "no_permission");
            return;
        }
        
        if (!checkArgs(sender, args, 2)) {
            messageService.sendMessage(sender, "command_usage", 
                "{usage}", "/djails bail remove <jogador>");
            return;
        }
        
        String playerName = args[1];
        OfflinePlayer target = validateOfflinePlayer(sender, playerName);
        if (target == null) return;
        executeWithPrisonerCheck(sender, target, isPrisoner -> {
            if (!isPrisoner) {
                messageService.sendMessage(sender, "player_not_jailed", "{player}", playerName);
                return;
            }

            executeAsync(sender,
                plugin.getBailService().removeBail(target, sender.getName()),
                success -> {
                    if (success) {
                        messageService.sendMessage(sender, "bail_removed", "{player}", playerName);

                        Player onlinePlayer = target.getPlayer();
                        if (onlinePlayer != null && onlinePlayer.isOnline()) {
                            messageService.sendMessage(onlinePlayer, "bail_removed_notify");
                        }
                    } else {
                        messageService.sendMessage(sender, "bail_remove_failed");
                    }
                },
                "remover fiança de " + playerName
            );
        });
    }
    
    /**
     * Abre GUI de Fianças
     */
    private void openBailGUI(CommandSender sender) {
        if (!sender.hasPermission("djails.bail.gui")) {
            messageService.sendMessage(sender, "no_permission");
            return;
        }
        
        if (!requirePlayer(sender)) {
            return;
        }
        
        Player player = (Player) sender;
        BailGUI bailGUI = new BailGUI(plugin, player);
        bailGUI.open(1);
    }
    
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filterStartsWith(Arrays.asList("set", "remove", "gui"), args[0]);
        } else if (args.length == 2) {
            return filterStartsWith(getPrisonerNames(), args[1]);
        } else if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            // Sugestões de valores de fiança
            return filterStartsWith(Arrays.asList("100", "500", "1000", "5000", "10000"), args[2]);
        }
        
        return super.tabComplete(sender, args);
    }
}