package br.com.devjails.command.subcommands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.command.CommandSender;

import br.com.devjails.DevJailsPlugin;
import br.com.devjails.command.BaseSubCommand;

/**
 * Subcomando /djails help [page]
 */
public class HelpSubCommand extends BaseSubCommand {
    
    public HelpSubCommand(DevJailsPlugin plugin) {
        super(plugin);
    }
    
    @Override
    public String getName() {
        return "help";
    }
    
    @Override
    public String getDescription() {
        return "Mostra a ajuda dos comandos";
    }
    
    @Override
    public String getUsage() {
        return "/djails help [pÃ¡gina]";
    }
    
    @Override
    public List<String> getAliases() {
        return Arrays.asList("?", "ajuda");
    }
    
    @Override
    public String getPermission() {
        return "djails.help";
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        int page = 1;
        
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                page = 1;
            }
        }
        
        sendHelpPage(sender, page);
    }
    
    /**
     * Envia uma pÃ¡gina de ajuda
     */
    private void sendHelpPage(CommandSender sender, int page) {

        messageService.sendMessage(sender, "help_header");

        List<String> helpCommands = getAvailableCommands(sender);
        
        int commandsPerPage = 8;
        int totalPages = (int) Math.ceil((double) helpCommands.size() / commandsPerPage);
        
        if (page > totalPages) {
            page = totalPages;
        }
        
        int startIndex = (page - 1) * commandsPerPage;
        int endIndex = Math.min(startIndex + commandsPerPage, helpCommands.size());

        for (int i = startIndex; i < endIndex; i++) {
            String commandKey = helpCommands.get(i);
            messageService.sendMessage(sender, commandKey);
        }

        if (totalPages > 1) {
            messageService.sendMessage(sender, "help_pagination",
                "{page}", String.valueOf(page),
                "{total}", String.valueOf(totalPages),
                "{next}", String.valueOf(page + 1)
            );
        }
        
        messageService.sendMessage(sender, "help_footer");
    }
    private List<String> getAvailableCommands(CommandSender sender) {
        List<String> commands = new ArrayList<>();

        if (sender.hasPermission("djails.jail") || sender.hasPermission("djails.admin")) {
            commands.add("help_jail");
        }
        if (sender.hasPermission("djails.tempjail") || sender.hasPermission("djails.admin")) {
            commands.add("help_tempjail");
        }
        if (sender.hasPermission("djails.unjail") || sender.hasPermission("djails.admin")) {
            commands.add("help_unjail");
        }
        if (sender.hasPermission("djails.duration") || sender.hasPermission("djails.admin")) {
            commands.add("help_duration");
        }

        // Comandos relacionados a definição/configuração
        if ((sender.hasPermission("djails.setjail") || sender.hasPermission("djails.admin")) ||
            (sender.hasPermission("djails.setflag") || sender.hasPermission("djails.admin"))) {
            commands.add("help_setjail");
            commands.add("help_setflag");
        }
        
        // Comandos relacionados a deleção
        if ((sender.hasPermission("djails.deletejail") || sender.hasPermission("djails.admin")) ||
            (sender.hasPermission("djails.deleteflag") || sender.hasPermission("djails.admin"))) {
            commands.add("help_deletejail");
            commands.add("help_deleteflag");
        }
        
        // Comandos relacionados a seleção e visualização
        if (sender.hasPermission("djails.wand") || sender.hasPermission("djails.admin")) {
            commands.add("help_wand");
        }
        if (sender.hasPermission("djails.flagpreview") || sender.hasPermission("djails.admin")) {
            commands.add("help_flagpreview");
        }
        
        // Comandos relacionados a vinculação
        if (sender.hasPermission("djails.link") || sender.hasPermission("djails.admin")) {
            commands.add("help_link");
        }
        
        // Comandos relacionados a spawn
        if (sender.hasPermission("djails.spawn") || sender.hasPermission("djails.admin")) {
            commands.add("help_spawn");
        }
        
        // Comandos relacionados a reload
        if (sender.hasPermission("djails.reload") || sender.hasPermission("djails.admin")) {
            commands.add("help_reload");
        }

        if (sender.hasPermission("djails.handcuff") || sender.hasPermission("djails.admin")) {
            commands.add("help_handcuff");
        }
        
        // Comandos relacionados a fiança
        if (sender.hasPermission("djails.bail.set") || sender.hasPermission("djails.admin")) {
            commands.add("help_bail_set");
        }
        if (sender.hasPermission("djails.bail.remove") || sender.hasPermission("djails.admin")) {
            commands.add("help_bail_remove");
        }
        if (sender.hasPermission("djails.bail.gui") || sender.hasPermission("djails.admin")) {
            commands.add("help_bail_gui");
        }
        
        // Comandos relacionados a listagem
        if (sender.hasPermission("djails.list") || sender.hasPermission("djails.admin")) {
            commands.add("help_list");
        }
        
        return commands;
    }
    
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {

            List<String> pages = Arrays.asList("1", "2", "3");
            return filterStartsWith(pages, args[0]);
        }
        
        return super.tabComplete(sender, args);
    }
}