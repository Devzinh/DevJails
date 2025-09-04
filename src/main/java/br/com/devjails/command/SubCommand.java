package br.com.devjails.command;

import org.bukkit.command.CommandSender;

import java.util.List;


public interface SubCommand {
    
    
    String getName();
    
    
    String getDescription();
    
    
    String getUsage();
    
    
    List<String> getAliases();
    
    
    String getPermission();
    
    
    boolean hasPermission(CommandSender sender);
    
    
    void execute(CommandSender sender, String[] args);
    
    
    List<String> tabComplete(CommandSender sender, String[] args);
}