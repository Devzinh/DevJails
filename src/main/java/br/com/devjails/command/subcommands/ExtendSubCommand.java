package br.com.devjails.command.subcommands;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import br.com.devjails.DevJailsPlugin;
import br.com.devjails.command.BaseSubCommand;
import br.com.devjails.prisoner.Prisoner;
import br.com.devjails.util.TimeParser;


public class ExtendSubCommand extends BaseSubCommand {
    
    public ExtendSubCommand(DevJailsPlugin plugin) {
        super(plugin);
    }
    
    @Override
    public String getName() {
        return "extend";
    }
    
    @Override
    public String getDescription() {
        return "Estende o tempo de prisão de um jogador";
    }
    
    @Override
    public String getUsage() {
        return "/djails extend <jogador> <tempo>";
    }
    
    @Override
    public List<String> getAliases() {
        return Arrays.asList("estender", "addtime");
    }
    
    @Override
    public String getPermission() {
        return "djails.extend";
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!checkArgs(sender, args, 2)) {
            return;
        }
        
        String playerName = args[0];
        String timeStr = args[1];
        
        // Validar e parsear tempo
        long additionalMillis;
        try {
            additionalMillis = TimeParser.parseTimeToMillis(timeStr);
            if (additionalMillis <= 0) {
                messageService.sendMessage(sender, "invalid_duration", "{duration}", timeStr);
                return;
            }
        } catch (Exception e) {
            messageService.sendMessage(sender, "invalid_duration", "{duration}", timeStr);
            return;
        }
        
        OfflinePlayer target = findOfflinePlayer(playerName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            messageService.sendMessage(sender, "player_not_found", "{player}", playerName);
            return;
        }
        
        executeAsync(sender, 
            plugin.getPrisonerManager().isPrisoner(target.getUniqueId())
                .thenCompose(isPrisoner -> {
                    if (!isPrisoner) {
                        messageService.sendMessage(sender, "player_not_jailed", "{player}", playerName);
                        return CompletableFuture.completedFuture(false);
                    }
                    
                    return plugin.getPrisonerManager().getPrisoner(target.getUniqueId()) != null && 
                           plugin.getPrisonerManager().getPrisoner(target.getUniqueId()).isPermanent() ?
                        CompletableFuture.completedFuture(false) :
                        plugin.getPrisonerManager().extendJailTime(target.getUniqueId(), additionalMillis);
                }),
            success -> {
                if (success) {
                    String formattedTime = TimeParser.formatTime(additionalMillis);
                    Prisoner prisoner = plugin.getPrisonerManager().getPrisoner(target.getUniqueId());
                    if (prisoner != null) {
                        String newTimeLeft = prisoner.isPermanent() ? 
                            messageService.getMessage("time_permanent") : 
                            TimeParser.formatTime(prisoner.getRemainingTimeMillis());
                        
                        messageService.sendMessage(sender, "extend_success", 
                            "{player}", target.getName(),
                            "{time}", formattedTime,
                            "{new_time}", newTimeLeft);
                        
                        // Notificar o jogador se estiver online
                        if (target.isOnline()) {
                            messageService.sendMessage(target.getPlayer(), "extend_notify", 
                                "{time}", formattedTime,
                                "{new_time}", newTimeLeft,
                                "{staff}", sender.getName());
                        }
                    }
                } else {
                    Prisoner prisoner = plugin.getPrisonerManager().getPrisoner(target.getUniqueId());
                    if (prisoner != null && prisoner.isPermanent()) {
                        messageService.sendMessage(sender, "extend_permanent_jail", "{player}", target.getName());
                    } else {
                        messageService.sendMessage(sender, "extend_error");
                    }
                }
            },
            "extending jail time for " + playerName
        );
    }
    
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filterStartsWith(getPrisonerNames(), args[0]);
        } else if (args.length == 2) {
            return filterStartsWith(getCommonDurations(), args[1]);
        }
        
        return super.tabComplete(sender, args);
    }
}