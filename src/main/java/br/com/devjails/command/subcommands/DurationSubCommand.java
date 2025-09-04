package br.com.devjails.command.subcommands;

import br.com.devjails.DevJailsPlugin;
import br.com.devjails.command.BaseSubCommand;
import br.com.devjails.prisoner.Prisoner;
import br.com.devjails.util.Tasks;
import br.com.devjails.util.TimeParser;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

/**
 * Subcomando /djails duration [player]
 */
public class DurationSubCommand extends BaseSubCommand {
    
    public DurationSubCommand(DevJailsPlugin plugin) {
        super(plugin);
    }
    
    @Override
    public String getName() {
        return "duration";
    }
    
    @Override
    public String getDescription() {
        return "Verifica tempo restante de prisÃ£o";
    }
    
    @Override
    public String getUsage() {
        return "/djails duration [jogador]";
    }
    
    @Override
    public List<String> getAliases() {
        return Arrays.asList("tempo", "time");
    }
    
    @Override
    public String getPermission() {
        return "djails.duration";
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {

        if (args.length == 0) {
            if (!requirePlayer(sender)) {
                return;
            }
            
            Player player = (Player) sender;
            checkDuration(sender, player, true);
            return;
        }
        String targetName = args[0];
        OfflinePlayer target = findOfflinePlayer(targetName);
        
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            messageService.sendMessage(sender, "player_not_found", "{player}", targetName);
            return;
        }
        
        checkDuration(sender, target, false);
    }
    
    /**
     * Verifica duraÃ§Ã£o de prisÃ£o de um jogador
     */
    private void checkDuration(CommandSender sender, OfflinePlayer target, boolean isSelf) {
        plugin.getPrisonerManager().isPrisoner(target.getUniqueId())
            .thenAccept(isPrisoner -> {
                if (!isPrisoner) {
                    String messageKey = isSelf ? "duration_not_jailed_self" : "duration_not_jailed_other";
                    messageService.sendMessage(sender, messageKey, "{player}", target.getName());
                    return;
                }
                Tasks.async(() -> {
                    Prisoner prisoner = plugin.getPrisonerManager().getPrisoner(target.getUniqueId());
                    if (prisoner == null) {
                        String messageKey = isSelf ? "duration_not_jailed_self" : "duration_not_jailed_other";
                        Tasks.sync(() -> messageService.sendMessage(sender, messageKey, "{player}", target.getName()));
                        return;
                    }

                    String timeLeft;
                    if (prisoner.isPermanent()) {
                        timeLeft = messageService.getMessage("time_permanent");
                    } else {
                        long remaining = prisoner.getRemainingTimeMillis();
                        if (remaining <= 0) {
                            timeLeft = messageService.getMessage("time_expired");
                        } else {
                            timeLeft = TimeParser.formatTime(remaining);
                        }
                    }

                    Tasks.sync(() -> {
                        if (isSelf) {
                            messageService.sendMessage(sender, "duration_self", 
                                "{time_left}", timeLeft,
                                "{jail}", prisoner.getJailName(),
                                "{reason}", prisoner.getReason());
                        } else {
                            messageService.sendMessage(sender, "duration_other", 
                                "{player}", target.getName(),
                                "{time_left}", timeLeft,
                                "{jail}", prisoner.getJailName(),
                                "{reason}", prisoner.getReason());
                        }

                        if (prisoner.getBailAmount() != null && prisoner.getBailAmount() > 0) {
                            messageService.sendMessage(sender, "duration_bail_info", 
                                "{amount}", String.format("%.2f", prisoner.getBailAmount()));
                        }

                        messageService.sendMessage(sender, "duration_staff_info", 
                            "{staff}", prisoner.getStaff(),
                            "{date}", formatDate(prisoner.getStartEpoch()));
                    });
                });
            })
            .exceptionally(ex -> {
                plugin.getLogger().severe("Erro ao verificar prisioneiro: " + ex.getMessage());
                messageService.sendMessage(sender, "storage_error");
                return null;
            });
    }
    
    /**
     * Formata data em formato legÃ­vel
     */
    private String formatDate(long timestamp) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");
        return sdf.format(new java.util.Date(timestamp));
    }
    
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filterStartsWith(getOnlinePlayerNames(), args[0]);
        }
        
        return super.tabComplete(sender, args);
    }
}