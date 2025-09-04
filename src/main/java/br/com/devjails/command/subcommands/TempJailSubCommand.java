package br.com.devjails.command.subcommands;

import br.com.devjails.DevJailsPlugin;
import br.com.devjails.command.BaseSubCommand;
import br.com.devjails.jail.Jail;
import br.com.devjails.util.TimeParser;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Subcomando /djails tempjail <player> <jail> <duration> <reason>
 */
public class TempJailSubCommand extends BaseSubCommand {
    
    public TempJailSubCommand(DevJailsPlugin plugin) {
        super(plugin);
    }
    
    @Override
    public String getName() {
        return "tempjail";
    }
    
    @Override
    public String getDescription() {
        return "Prende um jogador temporariamente";
    }
    
    @Override
    public String getUsage() {
        return "/djails tempjail <jogador> <cadeia> [tempo] <motivo>";
    }
    
    @Override
    public List<String> getAliases() {
        return Arrays.asList("tjail", "temp", "prender-temp");
    }
    
    @Override
    public String getPermission() {
        return "djails.tempjail";
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!checkArgs(sender, args, 3)) {
            return;
        }
        
        String playerName = args[0];
        String jailName = args[1];
        String durationStr;
        String reason;
        
        // Verificar se o tempo foi especificado ou usar o padrão
        if (args.length >= 4 && isValidDuration(args[2])) {
            // Tempo especificado
            durationStr = args[2];
            reason = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
        } else {
            // Usar tempo padrão da configuração
            durationStr = plugin.getConfig().getString("jail.default-temp-duration", "1h");
            reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        }
        // Validações usando métodos utilitários
        Jail jail = validateJail(sender, jailName);
        if (jail == null) return;
        
        long durationMillis = TimeParser.parseTimeToMillis(durationStr);
        if (durationMillis <= 0) {
            messageService.sendMessage(sender, "invalid_duration", "{duration}", durationStr);
            return;
        }

        OfflinePlayer offlinePlayer = validateOfflinePlayer(sender, playerName);
        if (offlinePlayer == null) return;
        
        if (!validateNotSelf(sender, playerName)) return;
        
        // Executar com verificação de prisioneiro
        executeWithPrisonerCheck(sender, offlinePlayer, isPrisoner -> {
            if (isPrisoner) {
                messageService.sendMessage(sender, "player_already_jailed", "{player}", playerName);
                return;
            }
            
            String staff = sender.getName();
            executeAsync(sender,
                plugin.getPrisonerManager().jailPlayer(
                    offlinePlayer.getUniqueId(),
                    playerName,
                    jailName,
                    reason,
                    staff,
                    false,
                    durationMillis
                ),
                success -> {
                    if (!success) {
                        messageService.sendMessage(sender, "jail_failed", "{player}", playerName);
                    }
                    // Sucesso é comunicado via broadcast pelo PrisonerManager
                },
                "prender temporariamente jogador " + playerName
            );
        });
    }
    
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filterStartsWith(getOnlinePlayerNames(), args[0]);
        } else if (args.length == 2) {
            return filterStartsWith(getJailNames(), args[1]);
        } else if (args.length == 3) {
            // Sugestões de duração ou motivos (tempo é opcional)
            List<String> suggestions = new ArrayList<>();
            suggestions.addAll(filterStartsWith(getCommonDurations(), args[2]));
            suggestions.addAll(filterStartsWith(getCommonReasons(), args[2]));
            return suggestions;
        } else if (args.length == 4) {
            // Se o terceiro argumento é uma duração válida, sugerir motivos
            if (isValidDuration(args[2])) {
                return filterStartsWith(getCommonReasons(), args[3]);
            }
        }
        
        return super.tabComplete(sender, args);
    }
    
    /**
     * Verifica se uma string é um formato de tempo válido
     * @param duration String a ser verificada
     * @return true se for um formato válido
     */
    private boolean isValidDuration(String duration) {
        try {
            return TimeParser.parseTimeToMillis(duration) > 0;
        } catch (Exception e) {
            return false;
        }
    }
}