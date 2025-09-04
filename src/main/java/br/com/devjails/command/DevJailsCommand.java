package br.com.devjails.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import br.com.devjails.DevJailsPlugin;
import br.com.devjails.command.subcommands.BailSubCommand;
import br.com.devjails.command.subcommands.DeleteSubCommand;
import br.com.devjails.command.subcommands.DurationSubCommand;
import br.com.devjails.command.subcommands.ExtendSubCommand;
import br.com.devjails.command.subcommands.FlagPreviewSubCommand;
import br.com.devjails.command.subcommands.HelpSubCommand;
import br.com.devjails.command.subcommands.JailSubCommand;
import br.com.devjails.command.subcommands.LinkSubCommand;
import br.com.devjails.command.subcommands.ListSubCommand;
import br.com.devjails.command.subcommands.ReloadSubCommand;
import br.com.devjails.command.subcommands.SetSubCommand;
import br.com.devjails.command.subcommands.SetSpawnSubCommand;
import br.com.devjails.command.subcommands.TempJailSubCommand;

import br.com.devjails.command.subcommands.UnjailSubCommand;
import br.com.devjails.command.subcommands.WandSubCommand;
import br.com.devjails.message.MessageService;


public class DevJailsCommand implements CommandExecutor, TabCompleter {
    
    private final DevJailsPlugin plugin;
    private final MessageService messageService;
    private final Map<String, SubCommand> subCommands;
    private final List<String> subCommandNames;
    
    public DevJailsCommand(DevJailsPlugin plugin) {
        this.plugin = plugin;
        this.messageService = plugin.getMessageService();
        this.subCommands = new HashMap<>();
        this.subCommandNames = new ArrayList<>();
        
        registerSubCommands();
    }
    
    /**
     * Registra todos os subcomandos
     */
    private void registerSubCommands() {

        registerSubCommand(new HelpSubCommand(plugin));
        registerSubCommand(new JailSubCommand(plugin));
        registerSubCommand(new TempJailSubCommand(plugin));
        registerSubCommand(new UnjailSubCommand(plugin));
        registerSubCommand(new DurationSubCommand(plugin));
        registerSubCommand(new ExtendSubCommand(plugin));
        registerSubCommand(new ListSubCommand(plugin));

        // Comandos adicionais necessitam de desenvolvimento adicional
        registerSubCommand(new SetSubCommand(plugin));
        registerSubCommand(new DeleteSubCommand(plugin));
        registerSubCommand(new WandSubCommand(plugin));
        registerSubCommand(new FlagPreviewSubCommand(plugin));
        registerSubCommand(new LinkSubCommand(plugin));
        registerSubCommand(new ReloadSubCommand(plugin));


        registerSubCommand(new SetSpawnSubCommand(plugin));

        registerSubCommand(new BailSubCommand(plugin));
    }
    
    /**
     * Registra um subcomando
     */
    private void registerSubCommand(SubCommand subCommand) {
        subCommands.put(subCommand.getName().toLowerCase(), subCommand);
        subCommandNames.add(subCommand.getName().toLowerCase());
        for (String alias : subCommand.getAliases()) {
            subCommands.put(alias.toLowerCase(), subCommand);
        }
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 0) {
            SubCommand helpCommand = subCommands.get("help");
            if (helpCommand != null) {
                helpCommand.execute(sender, new String[0]);
            }
            return true;
        }
        
        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = subCommands.get(subCommandName);
        
        if (subCommand == null) {
            messageService.sendMessage(sender, "command_not_found", "{command}", subCommandName);
            return true;
        }
        if (!subCommand.hasPermission(sender)) {
            messageService.sendMessage(sender, "no_permission");
            return true;
        }
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        try {
            subCommand.execute(sender, subArgs);
        } catch (Exception e) {
            messageService.sendMessage(sender, "command_error");
            plugin.getLogger().severe("Erro ao executar comando " + subCommandName + ": " + e.getMessage());
            e.printStackTrace();
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {

            String partial = args[0].toLowerCase();
            for (String subCommandName : subCommandNames) {
                SubCommand subCommand = subCommands.get(subCommandName);
                if (subCommand != null && subCommand.hasPermission(sender) && 
                    subCommandName.startsWith(partial)) {
                    completions.add(subCommandName);
                }
            }
        } else if (args.length > 1) {

            String subCommandName = args[0].toLowerCase();
            SubCommand subCommand = subCommands.get(subCommandName);
            
            if (subCommand != null && subCommand.hasPermission(sender)) {
                String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
                List<String> subCompletions = subCommand.tabComplete(sender, subArgs);
                if (subCompletions != null) {
                    completions.addAll(subCompletions);
                }
            }
        }
        
        Collections.sort(completions);
        return completions;
    }
    public Map<String, SubCommand> getSubCommands() {
        return Collections.unmodifiableMap(subCommands);
    }
    public List<String> getSubCommandNames() {
        return Collections.unmodifiableList(subCommandNames);
    }
}