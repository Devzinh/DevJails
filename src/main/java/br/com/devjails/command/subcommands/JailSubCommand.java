package br.com.devjails.command.subcommands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import br.com.devjails.DevJailsPlugin;
import br.com.devjails.command.BaseSubCommand;
import br.com.devjails.jail.Jail;


public class JailSubCommand extends BaseSubCommand {
    
    public JailSubCommand(DevJailsPlugin plugin) {
        super(plugin);
    }
    
    @Override
    public String getName() {
        return "jail";
    }
    
    @Override
    public String getDescription() {
        return "Prende um jogador permanentemente";
    }
    
    @Override
    public String getUsage() {
        return "/djails jail <jogador> <cadeia> <motivo>";
    }
    
    @Override
    public List<String> getAliases() {
        return Arrays.asList("prender", "arrest");
    }
    
    @Override
    public String getPermission() {
        return "djails.jail";
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!checkArgs(sender, args, 3)) {
            return;
        }
        
        String playerName = args[0];
        String jailName = args[1];
        String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        
        // Validações usando métodos utilitários
        Jail jail = validateJail(sender, jailName);
        if (jail == null) return;
        
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
                    true,
                    0
                ),
                success -> {
                    if (!success) {
                        messageService.sendMessage(sender, "jail_failed", "{player}", playerName);
                    }
                    // Sucesso é comunicado via broadcast pelo PrisonerManager
                },
                "prender jogador " + playerName
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
            return filterStartsWith(getCommonReasons(), args[2]);
        }
        
        return super.tabComplete(sender, args);
    }
}