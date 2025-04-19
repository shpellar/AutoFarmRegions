package com.example.autofarmregions;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Config {
    private final Plugin plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    private File configFile;
    private File messagesFile;

    public Config(Plugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
        this.messagesFile = new File(plugin.getDataFolder(), "messages.yml");
    }

    public void loadConfig() {
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public void saveEnabledRegions(List<String> regions) {
        config.set("enabled-regions", regions);
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save config to " + configFile);
        }
    }

    // Auto-replant settings
    public boolean isAutoReplantEnabled() {
        return config.getBoolean("auto-replant.enabled", true);
    }

    public boolean isAddToInventoryEnabled() {
        return config.getBoolean("auto-replant.add-to-inventory", true);
    }

    public boolean isDropIfFullEnabled() {
        return config.getBoolean("auto-replant.drop-if-full", false);
    }

    // Inventory full notification settings
    public boolean isInventoryFullTitleEnabled() {
        return config.getBoolean("auto-replant.inventory-full-notification.show-title", false);
    }

    public boolean isInventoryFullActionBarEnabled() {
        return config.getBoolean("auto-replant.inventory-full-notification.show-action-bar", true);
    }

    public int getInventoryFullTitleDuration() {
        return config.getInt("auto-replant.inventory-full-notification.title-duration", 40);
    }

    public String getInventoryFullTitle() {
        return messages.getString("inventory-full-title", "<red>Inventory Full!");
    }

    public String getInventoryFullSubtitle() {
        return messages.getString("inventory-full-subtitle", "<gray>Items will be dropped on the ground</gray>");
    }

    public String getInventoryFullActionBar() {
        return messages.getString("inventory-full-action-bar", "<red>Your inventory is full! Items will be dropped on the ground");
    }

    // Growth settings
    public boolean isGrowthEnabled() {
        return config.getBoolean("growth.enabled", true);
    }

    public int getGrowthSpeed() {
        return config.getInt("growth.speed", 2);
    }

    public int getMinRandomTicks() {
        return config.getInt("growth.min-random-ticks", 1);
    }

    public int getMaxRandomTicks() {
        return config.getInt("growth.max-random-ticks", 3);
    }

    public int getGrowthCheckIntervalSeconds() {
        return config.getInt("growth.accelerated-growth.check-interval-seconds", 60);
    }

    public int getGrowthAgeIncreaseAmount() {
        return config.getInt("growth.accelerated-growth.age-increase-amount", 7);
    }

    public boolean isShowGrowthParticles() {
        return config.getBoolean("growth.show-particles", true);
    }

    public String getGrowthParticle() {
        return config.getString("growth.particle", "VILLAGER_HAPPY");
    }

    public int getInitialDelay() {
        return config.getInt("growth.initial-delay", 20);
    }

    public int getChainDelay() {
        return config.getInt("growth.chain-delay", 2);
    }

    public int getChainRadius() {
        return config.getInt("growth.chain-radius", 3);
    }

    public int getGrowthInterval() {
        return config.getInt("growth.interval", 30);
    }

    // Protection settings
    public boolean isPreventImmatureBreak() {
        return config.getBoolean("protection.prevent-immature-break", true);
    }

    public boolean isShowImmatureMessage() {
        return config.getBoolean("protection.show-immature-message", false);
    }

    public boolean isRequireTools() {
        return config.getBoolean("protection.require-tools", false);
    }

    public List<String> getAllowedTools() {
        return config.getStringList("protection.allowed-tools");
    }

    // Messages
    public String getNoPermissionMessage() {
        return messages.getString("no-permission", "<red>You don't have permission to use this command!");
    }

    public String getHelpMessage() {
        return """
            <gradient:gold:yellow>AutoFarmRegions Help</gradient>
            <gray>Commands:</gray>
            <white>/afr help</white> - Show this help message
            <white>/afr add <region></white> - Add a region to auto-farm
            <white>/afr remove <region></white> - Remove a region from auto-farm
            <white>/afr list</white> - List enabled regions
            <white>/afr reload</white> - Reload the configuration""";
    }

    public String getReloadMessage() {
        return "<green>Configuration reloaded successfully!";
    }

    public String getUsageMessage() {
        return "<red>Usage: /afr <add|remove|list|reload> [region]";
    }

    public String getNoRegionsMessage() {
        return messages.getString("no-regions-enabled", "<red>No regions are currently enabled for auto-farm!");
    }

    public String getListRegionsHeader() {
        return messages.getString("regions-list", "<gradient:gold:yellow>Enabled Regions</gradient>");
    }

    public String getAddRegionUsageMessage() {
        return getUsageMessage();
    }

    public String getRemoveRegionUsageMessage() {
        return getUsageMessage();
    }

    public String getRegionNotFoundMessage() {
        return messages.getString("region-not-found", "<red>Region not found!");
    }

    public String getRegionAlreadyEnabledMessage() {
        return messages.getString("region-already-enabled", "<red>Region is already enabled!");
    }

    public String getRegionAddedMessage() {
        return messages.getString("region-added", "<green>Region has been added to auto-farm!");
    }

    public String getRegionNotEnabledMessage() {
        return messages.getString("region-not-enabled", "<red>Region is not enabled!");
    }

    public String getRegionRemovedMessage() {
        return messages.getString("region-removed", "<green>Region has been removed from auto-farm!");
    }

    public String getCropImmatureMessage() {
        return messages.getString("crop-immature", "<red>This crop is not fully grown yet!");
    }

    public String getToolRequiredMessage() {
        return messages.getString("tool-required", "<red>You need a hoe to break this crop!");
    }
} 