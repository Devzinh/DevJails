package br.com.devjails.command.subcommands;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import br.com.devjails.DevJailsPlugin;
import br.com.devjails.command.BaseSubCommand;
import br.com.devjails.gui.PrisonersGUI;
import br.com.devjails.jail.Jail;
import br.com.devjails.prisoner.Prisoner;
import br.com.devjails.util.Tasks;
import br.com.devjails.util.TimeParser;

/**
 * Subcomando /djails list <jails|prisoners>
 */
public class ListSubCommand extends BaseSubCommand {
    
    public ListSubCommand(DevJailsPlugin plugin) {
        super(plugin);
    }
    
    @Override
    public String getName() {
        return "list";
    }
    
    @Override
    public String getDescription() {
        return "Lista cadeias ou prisioneiros";
    }
    
    @Override
    public String getUsage() {
        return "/djails list <jails|prisoners>";
    }
    
    @Override
    public List<String> getAliases() {
        return Arrays.asList("listar", "ls");
    }
    
    @Override
    public String getPermission() {
        return "djails.list";
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!checkArgs(sender, args, 1)) {
            return;
        }
        
        String subAction = args[0].toLowerCase();
        
        switch (subAction) {
            case "jails":
            case "cadeias":
                listJails(sender);
                break;
                
            case "prisoners":
            case "prisioneiros":
                listPrisoners(sender);
                break;
                
            default:
                messageService.sendMessage(sender, "command_usage", "{usage}", getUsage());
                break;
        }
    }
    
    /**
     * Lista todas as cadeias
     */
    private void listJails(CommandSender sender) {
        Tasks.async(() -> {
            try {
                Collection<Jail> jails = plugin.getJailManager().getAllJails();
                
                Tasks.sync(() -> {
                    if (jails.isEmpty()) {
                        messageService.sendMessage(sender, "jail_list_empty");
                        return;
                    }
                    
                    messageService.sendMessage(sender, "jail_list_header");
                    
                    for (Jail jail : jails) {
                        String worldName = jail.getLocation().getWorld() != null 
                            ? jail.getLocation().getWorld().getName() 
                            : "Unknown";
                        
                        messageService.sendMessage(sender, "jail_list_item", 
                            "{jail}", jail.getName(),
                            "{world}", worldName);
                    }
                });
            } catch (Exception ex) {
                plugin.getLogger().severe("Erro ao listar cadeias: " + ex.getMessage());
                Tasks.sync(() -> messageService.sendMessage(sender, "storage_error"));
            }
        });
    }
    
    /**
     * Lista prisioneiros (GUI para jogadores, texto para console)
     */
    private void listPrisoners(CommandSender sender) {
        if (sender instanceof Player) {
            if (!sender.hasPermission("djails.list") && !sender.hasPermission("djails.admin")) {
                messageService.sendMessage(sender, "no_permission");
                return;
            }

            Player player = (Player) sender;
            PrisonersGUI gui = new PrisonersGUI(plugin, player);
            gui.open(1);
            
        } else {
            // Console sempre tem permissão para listar
            listPrisonersConsole(sender);
        }
    }
    
    /**
     * Lista prisioneiros no console/texto
     */
    private void listPrisonersConsole(CommandSender sender) {
        Tasks.async(() -> {
            try {
                Collection<Prisoner> prisoners = plugin.getPrisonerManager().getAllPrisoners();
                
                Tasks.sync(() -> {
                    if (prisoners.isEmpty()) {
                        messageService.sendMessage(sender, "prisoners_list_empty");
                        return;
                    }
                    
                    messageService.sendMessage(sender, "prisoners_list_header");
                    
                    for (Prisoner prisoner : prisoners) {
                        String locale = messageService.getPlayerLanguage((Player) sender);
                        String timeLeft = prisoner.isPermanent() 
                            ? messageService.getLocalizedMessage(locale, "time_permanent")
                            : TimeParser.formatTime(prisoner.getRemainingTimeMillis());
                        
                        String bailStatus = prisoner.getBailAmount() != null && prisoner.getBailAmount() > 0 
                            ? "$" + String.format("%.2f", prisoner.getBailAmount())
                            : messageService.getLocalizedMessage(locale, "bail_status_disabled").replace("&c", "");
                        
                        // Usar messageService para manter consistência na codificação
                        String listItem = messageService.getLocalizedMessage(locale, "prisoners_list_item",
                            "{player}", prisoner.getPlayerName(),
                            "{jail}", prisoner.getJailName(),
                            "{time_left}", timeLeft,
                            "{bail_status}", bailStatus
                        );
                        sender.sendMessage(listItem);
                    }
                });
            } catch (Exception ex) {
                plugin.getLogger().severe("Erro ao listar prisioneiros: " + ex.getMessage());
                Tasks.sync(() -> messageService.sendMessage(sender, "storage_error"));
            }
        });
    }
    

    
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filterStartsWith(Arrays.asList("jails", "prisoners"), args[0]);
        }
        
        return super.tabComplete(sender, args);
    }
}