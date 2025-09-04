package br.com.devjails.command.subcommands;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import br.com.devjails.DevJailsPlugin;
import br.com.devjails.api.events.PlayerUnjailedEvent;
import br.com.devjails.command.BaseSubCommand;
import br.com.devjails.prisoner.Prisoner;
import br.com.devjails.util.Tasks;

/**
 * Subcomando /djails unjail <player>
 */
public class UnjailSubCommand extends BaseSubCommand {
    
    public UnjailSubCommand(DevJailsPlugin plugin) {
        super(plugin);
    }
    
    @Override
    public String getName() {
        return "unjail";
    }
    
    @Override
    public String getDescription() {
        return "Solta um prisioneiro";
    }
    
    @Override
    public String getUsage() {
        return "/djails unjail <jogador>";
    }
    
    @Override
    public List<String> getAliases() {
        return Arrays.asList("soltar", "release", "free");
    }
    
    @Override
    public String getPermission() {
        return "djails.unjail";
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!checkArgs(sender, args, 1)) {
            return;
        }
        
        String playerName = args[0];
        OfflinePlayer offlinePlayer = validateOfflinePlayer(sender, playerName);
        if (offlinePlayer == null) return;
        
        executeWithPrisonerCheck(sender, offlinePlayer, isPrisoner -> {
            if (!isPrisoner) {
                messageService.sendMessage(sender, "player_not_jailed", "{player}", playerName);
                return;
            }
            Tasks.async(() -> {
                Prisoner prisoner = plugin.getPrisonerManager().getPrisoner(offlinePlayer.getUniqueId());
                if (prisoner == null) {
                    Tasks.sync(() -> messageService.sendMessage(sender, "player_not_jailed", "{player}", playerName));
                    return;
                }
                
                String staffName = sender.getName();
                Player onlinePlayer = offlinePlayer.getPlayer();

                if (onlinePlayer != null && onlinePlayer.isOnline()) {
                    // Events must be called from main thread
                    Tasks.sync(() -> {
                        PlayerUnjailedEvent event = new PlayerUnjailedEvent(
                            onlinePlayer, 
                            prisoner, 
                            staffName, 
                            PlayerUnjailedEvent.ReleaseReason.MANUAL
                        );
                        Bukkit.getPluginManager().callEvent(event);
                        
                        if (event.isCancelled()) {
                            messageService.sendMessage(sender, "unjail_cancelled", "{player}", playerName);
                            return;
                        }
                        
                        // Continue with release if event not cancelled
                        Tasks.async(() -> {
                            executeAsync(sender,
                                plugin.getPrisonerManager().releasePrisoner(offlinePlayer.getUniqueId(), staffName),
                                success -> {
                                    if (!success) {
                                        messageService.sendMessage(sender, "unjail_failed");
                                    }
                                    // Mensagem de sucesso é enviada via broadcast pelo PrisonerManager
                                },
                                "soltar prisioneiro " + playerName
                            );
                        });
                    });
                    return; // Exit here for online players to avoid duplicate execution
                }

                executeAsync(sender,
                    plugin.getPrisonerManager().releasePrisoner(offlinePlayer.getUniqueId(), staffName),
                    success -> {
                        if (!success) {
                            messageService.sendMessage(sender, "unjail_failed");
                        }
                        // Mensagem de sucesso é enviada via broadcast pelo PrisonerManager
                    },
                    "soltar prisioneiro " + playerName
                );
            });
        });
    }
    
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filterStartsWith(getPrisonerNames(), args[0]);
        }
        
        return super.tabComplete(sender, args);
    }
}