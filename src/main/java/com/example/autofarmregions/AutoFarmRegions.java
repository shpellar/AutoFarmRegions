package com.example.autofarmregions;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AutoFarmRegions extends JavaPlugin implements TabCompleter {
    private static AutoFarmRegions instance;
    private Config config;
    private RegionManager regionManager;
    private final List<String> enabledRegions = new ArrayList<>();
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private BukkitAudiences adventure;
    private static final List<String> COMMANDS = Arrays.asList("reload", "list", "add", "remove");
    private FarmListener farmListener;

    @Override
    public void onEnable() {
        instance = this;
        config = new Config(this);
        config.loadConfig();
        adventure = BukkitAudiences.create(this);
        
        // Register event listener
        farmListener = new FarmListener(this);
        getServer().getPluginManager().registerEvents(farmListener, this);
        
        // Initialize region manager and load enabled regions
        regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(Bukkit.getWorlds().get(0)));
        enabledRegions.addAll(getConfig().getStringList("enabled-regions"));
        
        // Register tab completer
        getCommand("autofarm").setTabCompleter(this);
        
        getLogger().info("AutoFarmRegions has been enabled!");
    }

    @Override
    public void onDisable() {
        if (farmListener != null) {
            farmListener.cleanup();
        }
        if (adventure != null) {
            adventure.close();
            adventure = null;
        }
        getLogger().info("AutoFarmRegions has been disabled!");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("autofarmregions.admin")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            return COMMANDS.stream()
                    .filter(cmd -> cmd.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("autofarmregions.admin")) {
            adventure.sender(sender).sendMessage(miniMessage.deserialize(config.getNoPermissionMessage()));
            return true;
        }

        if (args.length == 0) {
            adventure.sender(sender).sendMessage(miniMessage.deserialize(config.getHelpMessage()));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                config.loadConfig();
                enabledRegions.clear();
                enabledRegions.addAll(getConfig().getStringList("enabled-regions"));
                adventure.sender(sender).sendMessage(miniMessage.deserialize(config.getReloadMessage()));
                break;
            case "list":
                listRegions(sender);
                break;
            case "add":
                if (args.length < 2) {
                    adventure.sender(sender).sendMessage(miniMessage.deserialize(config.getAddRegionUsageMessage()));
                    return true;
                }
                addRegion(args[1], sender);
                break;
            case "remove":
                if (args.length < 2) {
                    adventure.sender(sender).sendMessage(miniMessage.deserialize(config.getRemoveRegionUsageMessage()));
                    return true;
                }
                removeRegion(args[1], sender);
                break;
            default:
                adventure.sender(sender).sendMessage(miniMessage.deserialize(config.getHelpMessage()));
        }
        return true;
    }

    private void listRegions(CommandSender sender) {
        if (enabledRegions.isEmpty()) {
            adventure.sender(sender).sendMessage(miniMessage.deserialize(config.getNoRegionsMessage()));
            return;
        }
        
        StringBuilder message = new StringBuilder(config.getListRegionsHeader());
        for (String region : enabledRegions) {
            message.append("\n- ").append(region);
        }
        adventure.sender(sender).sendMessage(miniMessage.deserialize(message.toString()));
    }

    private void addRegion(String regionName, CommandSender sender) {
        if (enabledRegions.contains(regionName)) {
            adventure.sender(sender).sendMessage(miniMessage.deserialize(config.getRegionAlreadyEnabledMessage()));
            return;
        }

        enabledRegions.add(regionName);
        getConfig().set("enabled-regions", enabledRegions);
        saveConfig();
        adventure.sender(sender).sendMessage(miniMessage.deserialize(config.getRegionAddedMessage()));
    }

    private void removeRegion(String regionName, CommandSender sender) {
        if (!enabledRegions.contains(regionName)) {
            adventure.sender(sender).sendMessage(miniMessage.deserialize(config.getRegionNotEnabledMessage()));
            return;
        }

        enabledRegions.remove(regionName);
        getConfig().set("enabled-regions", enabledRegions);
        saveConfig();
        adventure.sender(sender).sendMessage(miniMessage.deserialize(config.getRegionRemovedMessage()));
    }

    public static AutoFarmRegions getInstance() {
        return instance;
    }

    public Config getPluginConfig() {
        return config;
    }

    public List<String> getEnabledRegions() {
        return enabledRegions;
    }

    public boolean isRegionEnabled(String regionName) {
        return enabledRegions.contains(regionName);
    }

    public BukkitAudiences getAdventure() {
        if (adventure == null) {
            throw new IllegalStateException("Tried to access Adventure when the plugin was disabled!");
        }
        return adventure;
    }
} 