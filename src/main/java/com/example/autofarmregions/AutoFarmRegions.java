package com.example.autofarmregions;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bstats.bukkit.Metrics;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class AutoFarmRegions extends JavaPlugin implements Listener {
    private static AutoFarmRegions instance;
    private FileConfiguration config;
    private FileConfiguration messages;
    private List<String> enabledRegions;
    private Map<Block, BukkitRunnable> regrowthTasks;
    private WorldGuard worldGuard;
    private ProtocolManager protocolManager;
    private UpdateChecker updateChecker;

    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize WorldGuard
        worldGuard = WorldGuard.getInstance();
        protocolManager = ProtocolLibrary.getProtocolManager();
        
        // Load configurations
        saveDefaultConfig();
        config = getConfig();
        loadMessages();
        enabledRegions = config.getStringList("enabled-regions");
        regrowthTasks = new HashMap<>();

        // Register events and commands
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("autofarmregions").setExecutor(new AutoFarmRegionsCommand(this));

        // Initialize bStats
        new Metrics(this, 25602);

        // Initialize update checker
        updateChecker = new UpdateChecker(this);
        updateChecker.checkForUpdates();

        getLogger().info("AutoFarmRegions has been enabled!");
    }

    private void loadMessages() {
        File messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    @Override
    public void onDisable() {
        // Cancel all regrowth tasks
        for (BukkitRunnable task : regrowthTasks.values()) {
            task.cancel();
        }
        regrowthTasks.clear();
        
        getLogger().info("AutoFarmRegions has been disabled!");
    }

    public void reloadConfiguration() {
        reloadConfig();
        config = getConfig();
        loadMessages();
        enabledRegions = config.getStringList("enabled-regions");
    }

    public String getMessage(String key) {
        return messages.getString(key);
    }

    public boolean addRegion(String regionName) {
        if (enabledRegions.contains(regionName)) {
            return false;
        }
        enabledRegions.add(regionName);
        config.set("enabled-regions", enabledRegions);
        saveConfig();
        return true;
    }

    public List<String> getEnabledRegions() {
        return new ArrayList<>(enabledRegions);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        // Check if the block is in an enabled region
        if (!isInEnabledRegion(block)) {
            return;
        }

        // Check if the block is a crop
        if (!isCrop(block.getType())) {
            return;
        }

        // Check if the crop is mature
        Ageable ageable = (Ageable) block.getBlockData();
        if (config.getBoolean("protection.prevent-immature-break") && ageable.getAge() != ageable.getMaximumAge()) {
            event.setCancelled(true);
            if (config.getBoolean("protection.show-immature-message")) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', getMessage("immature-crop")));
            }
            return;
        }

        // Check if tool is required and valid
        if (config.getBoolean("protection.require-tools")) {
            ItemStack tool = player.getInventory().getItemInMainHand();
            if (tool == null || !config.getStringList("protection.allowed-tools").contains(tool.getType().name())) {
                event.setCancelled(true);
                return;
            }
        }

        // Handle auto-replanting
        if (config.getBoolean("auto-replant.enabled")) {
            event.setDropItems(false);
            Material cropType = block.getType();
            
            // Add drops to inventory or drop them
            for (ItemStack drop : block.getDrops()) {
                if (config.getBoolean("auto-replant.add-to-inventory")) {
                    Map<Integer, ItemStack> remaining = player.getInventory().addItem(drop);
                    if (!remaining.isEmpty() && config.getBoolean("auto-replant.drop-if-full")) {
                        for (ItemStack item : remaining.values()) {
                            block.getWorld().dropItemNaturally(block.getLocation(), item);
                        }
                    } else if (!remaining.isEmpty()) {
                        showInventoryFullNotification(player);
                    }
                } else {
                    block.getWorld().dropItemNaturally(block.getLocation(), drop);
                }
            }

            // Schedule the replanting for the next tick to ensure the block is broken first
            Bukkit.getScheduler().runTask(this, () -> {
                // Replant the crop
                block.setType(cropType);
                Ageable newAgeable = (Ageable) block.getBlockData();
                newAgeable.setAge(0);
                block.setBlockData(newAgeable);

                // Schedule regrowth
                if (config.getBoolean("growth.enabled")) {
                    scheduleRegrowth(block, cropType);
                }
            });
        }
    }

    private boolean isInEnabledRegion(Block block) {
        RegionManager regions = worldGuard.getPlatform().getRegionContainer().get(BukkitAdapter.adapt(block.getWorld()));
        if (regions == null) return false;

        for (String regionName : enabledRegions) {
            ProtectedRegion region = regions.getRegion(regionName);
            if (region != null && region.contains(block.getX(), block.getY(), block.getZ())) {
                return true;
            }
        }
        return false;
    }

    private boolean isCrop(Material material) {
        return material == Material.WHEAT ||
               material == Material.CARROTS ||
               material == Material.POTATOES ||
               material == Material.BEETROOTS ||
               material == Material.NETHER_WART;
    }

    private void scheduleRegrowth(Block block, Material cropType) {
        // Cancel existing task if any
        if (regrowthTasks.containsKey(block)) {
            regrowthTasks.get(block).cancel();
            regrowthTasks.remove(block);
        }

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                block.setType(cropType);
                Ageable ageable = (Ageable) block.getBlockData();
                ageable.setAge(ageable.getMaximumAge());
                block.setBlockData(ageable);

                if (config.getBoolean("growth.show-particles")) {
                    Particle particle = Particle.valueOf(config.getString("growth.particle"));
                    block.getWorld().spawnParticle(particle, block.getLocation().add(0.5, 0.5, 0.5), 10, 0.3, 0.3, 0.3, 0);
                }

                regrowthTasks.remove(block);
            }
        };

        task.runTaskLater(this, config.getInt("growth.interval") * 20L);
        regrowthTasks.put(block, task);
    }

    private void showInventoryFullNotification(Player player) {
        if (config.getBoolean("auto-replant.inventory-full-notification.show-title")) {
            player.sendTitle(
                ChatColor.translateAlternateColorCodes('&', getMessage("inventory-full")),
                "",
                10,
                config.getInt("auto-replant.inventory-full-notification.title-duration"),
                10
            );
        }
        if (config.getBoolean("auto-replant.inventory-full-notification.show-action-bar")) {
            sendActionBar(player, ChatColor.translateAlternateColorCodes('&', getMessage("inventory-full")));
        }
    }

    private void sendActionBar(Player player, String message) {
        PacketContainer chatPacket = protocolManager.createPacket(PacketType.Play.Server.SET_ACTION_BAR_TEXT);
        chatPacket.getChatComponents().write(0, WrappedChatComponent.fromText(message));
        try {
            protocolManager.sendServerPacket(player, chatPacket);
        } catch (Exception e) {
            getLogger().warning("Failed to send action bar message: " + e.getMessage());
        }
    }

    public static AutoFarmRegions getInstance() {
        return instance;
    }
} 