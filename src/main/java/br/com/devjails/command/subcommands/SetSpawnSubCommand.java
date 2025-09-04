package br.com.devjails.command.subcommands;

import br.com.devjails.DevJailsPlugin;
import br.com.devjails.command.BaseSubCommand;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

/**
 * Subcomando /djails setspawn
 */
public class SetSpawnSubCommand extends BaseSubCommand {
    
    public SetSpawnSubCommand(DevJailsPlugin plugin) {
        super(plugin);
    }
    
    @Override
    public String getName() {
        return "setspawn";
    }
    
    @Override
    public String getDescription() {
        return "Define o local de spawn para jogadores liberados";
    }
    
    @Override
    public String getUsage() {
        return "/djails setspawn";
    }
    
    @Override
    public List<String> getAliases() {
        return Arrays.asList();
    }
    
    @Override
    public String getPermission() {
        return "djails.setspawn";
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!requirePlayer(sender)) {
            return;
        }
        
        Player player = (Player) sender;
        Location currentLocation = player.getLocation();
        
        // Salvar a localização no arquivo de configuração
        plugin.getConfig().set("release-spawn.world", currentLocation.getWorld().getName());
        plugin.getConfig().set("release-spawn.x", currentLocation.getX());
        plugin.getConfig().set("release-spawn.y", currentLocation.getY());
        plugin.getConfig().set("release-spawn.z", currentLocation.getZ());
        plugin.getConfig().set("release-spawn.yaw", currentLocation.getYaw());
        plugin.getConfig().set("release-spawn.pitch", currentLocation.getPitch());
        
        plugin.saveConfig();
        
        messageService.sendMessage(player, "setspawn_success", 
            "{x}", String.format("%.1f", currentLocation.getX()),
            "{y}", String.format("%.1f", currentLocation.getY()),
            "{z}", String.format("%.1f", currentLocation.getZ()),
            "{world}", currentLocation.getWorld().getName());
    }
    
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return super.tabComplete(sender, args);
    }
}