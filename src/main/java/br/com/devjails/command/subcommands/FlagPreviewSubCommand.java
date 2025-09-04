package br.com.devjails.command.subcommands;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import br.com.devjails.DevJailsPlugin;
import br.com.devjails.command.BaseSubCommand;
import br.com.devjails.flag.FlagManager;
import br.com.devjails.flag.FlagRegion;
import br.com.devjails.jail.JailManager;

/**
 * Subcomando /djails flagpreview
 */
public class FlagPreviewSubCommand extends BaseSubCommand {
    
    public FlagPreviewSubCommand(DevJailsPlugin plugin) {
        super(plugin);
    }
    
    @Override
    public String getName() {
        return "flagpreview";
    }
    
    @Override
    public String getDescription() {
        return "Pré-visualiza uma área";
    }
    
    @Override
    public String getUsage() {
        return "/djails flagpreview <nome>";
    }
    
    @Override
    public List<String> getAliases() {
        return Arrays.asList();
    }
    
    @Override
    public String getPermission() {
        return "djails.flagpreview";
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = validatePlayerWithPermission(sender, "djails.flagpreview");
        if (player == null) {
            return;
        }
        
        if (!checkArgs(sender, args, 1)) {
            return;
        }
        
        String flagName = args[0];
        
        JailManager jailManager = plugin.getJailManager();
        FlagManager flagManager = plugin.getFlagManager();
        
        // Verificar se a flag existe
        if (!jailManager.flagExists(flagName)) {
            messageService.sendMessage(player, "flag_not_found", "{flag}", flagName);
            return;
        }
        
        // Obter a flag
        FlagRegion flag = jailManager.getFlag(flagName);
        if (flag == null) {
            messageService.sendMessage(player, "flag_not_found", "{flag}", flagName);
            return;
        }
        
        // Iniciar a pré-visualização da flag
        flagManager.startFlagPreview(player, flag);
        messageService.sendMessage(player, "flag_preview_start", "{flag}", flagName);
    }
    
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            JailManager jailManager = plugin.getJailManager();
            List<String> flagNames = jailManager.getFlagNames().stream().collect(Collectors.toList());
            return filterStartsWith(flagNames, args[0]);
        }
        
        return super.tabComplete(sender, args);
    }
}