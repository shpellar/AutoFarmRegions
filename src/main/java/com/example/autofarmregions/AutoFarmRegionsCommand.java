package com.example.autofarmregions;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class AutoFarmRegionsCommand implements CommandExecutor, TabCompleter {
    private final AutoFarmRegions plugin;

    public AutoFarmRegionsCommand(AutoFarmRegions plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        if (!sender.hasPermission("autofarmregions.admin")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getMessage("no-permission")));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                plugin.reloadConfiguration();
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    plugin.getMessage("reload-success")));
                break;

            case "addregion":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /" + label + " addregion <regionName>");
                    return true;
                }
                String regionName = args[1];
                if (plugin.addRegion(regionName)) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                        plugin.getMessage("region-added").replace("%region%", regionName)));
                } else {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                        plugin.getMessage("region-already-enabled")));
                }
                break;

            case "listregions":
                List<String> regions = plugin.getEnabledRegions();
                if (regions.isEmpty()) {
                    sender.sendMessage(ChatColor.YELLOW + "No regions are currently enabled.");
                } else {
                    sender.sendMessage(ChatColor.GREEN + "Enabled regions:");
                    for (String region : regions) {
                        sender.sendMessage(ChatColor.YELLOW + "- " + region);
                    }
                }
                break;

            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.add("reload");
            completions.add("addregion");
            completions.add("listregions");
        }
        
        return completions;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "AutoFarmRegions Commands:");
        sender.sendMessage(ChatColor.YELLOW + "/autofarmregions reload" + ChatColor.WHITE + " - Reload the configuration");
        sender.sendMessage(ChatColor.YELLOW + "/autofarmregions addregion <region>" + ChatColor.WHITE + " - Add a region to auto-farming");
        sender.sendMessage(ChatColor.YELLOW + "/autofarmregions listregions" + ChatColor.WHITE + " - List enabled regions");
    }
} 