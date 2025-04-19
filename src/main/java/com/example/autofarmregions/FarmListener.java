package com.example.autofarmregions;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldedit.math.BlockVector3;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.HashSet;

public class FarmListener implements Listener {
    private final AutoFarmRegions plugin;
    private final Config config;
    private final Random random = new Random();
    private final MiniMessage miniMessage;
    private final BukkitAudiences adventure;
    private final Map<Location, Long> cropGrowthTimes = new HashMap<>();
    private BukkitTask growthTask;
    private final List<Location> growthQueue = new ArrayList<>();
    private final Set<Location> processedCrops = new HashSet<>();
    private long nextGrowthTime = 0;
    private boolean isProcessingGrowth = false;
    private final RegionContainer regionContainer;
    private final RegionQuery regionQuery;

    private static final Set<Material> CROP_MATERIALS = Set.of(
        Material.WHEAT,
        Material.CARROTS,
        Material.POTATOES,
        Material.BEETROOTS,
        Material.NETHER_WART
    );

    public FarmListener(AutoFarmRegions plugin) {
        this.plugin = plugin;
        this.config = plugin.getPluginConfig();
        this.miniMessage = MiniMessage.miniMessage();
        this.adventure = plugin.getAdventure();
        this.regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
        this.regionQuery = regionContainer.createQuery();
        startGrowthTask();
    }

    public void cleanup() {
        if (growthTask != null) {
            growthTask.cancel();
            growthTask = null;
        }
        growthQueue.clear();
        processedCrops.clear();
    }

    private void startGrowthTask() {
        if (growthTask != null) {
            growthTask.cancel();
        }

        // Run every 5 ticks instead of every tick to reduce load
        growthTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!config.isGrowthEnabled() || isProcessingGrowth) {
                    return;
                }

                long currentTime = System.currentTimeMillis();
                if (currentTime < nextGrowthTime) {
                    return;
                }

                isProcessingGrowth = true;
                try {
                    // If queue is empty, find a new crop to start the chain
                    if (growthQueue.isEmpty()) {
                        findNewCropToStartChain();
                    }

                    // If we have crops to grow, grow the next one
                    if (!growthQueue.isEmpty()) {
                        processNextCropInQueue();
                    } else {
                        // If no crops to grow, set next growth time based on interval
                        nextGrowthTime = currentTime + (long)(config.getGrowthInterval() * 1000);
                    }
                } finally {
                    isProcessingGrowth = false;
                }
            }
        }.runTaskTimer(plugin, 1L, 5L); // Run every 5 ticks instead of every tick
    }

    private void findNewCropToStartChain() {
        for (String regionName : plugin.getEnabledRegions()) {
            for (World world : Bukkit.getWorlds()) {
                ProtectedRegion region = regionContainer.get(BukkitAdapter.adapt(world)).getRegion(regionName);
                if (region != null) {
                    BlockVector3 min = region.getMinimumPoint();
                    BlockVector3 max = region.getMaximumPoint();
                    
                    // Try to find a crop that's not fully grown
                    for (int x = min.getX(); x <= max.getX(); x += 2) { // Check every other block
                        for (int y = min.getY(); y <= max.getY(); y++) {
                            for (int z = min.getZ(); z <= max.getZ(); z += 2) { // Check every other block
                                Location loc = new Location(world, x, y, z);
                                Block block = loc.getBlock();
                                if (isCrop(block.getType()) && !isFullyGrown(block) && !processedCrops.contains(loc)) {
                                    growthQueue.add(loc);
                                    processedCrops.add(loc);
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void processNextCropInQueue() {
        Location loc = growthQueue.remove(0);
        Block block = loc.getBlock();
        if (isCrop(block.getType()) && !isFullyGrown(block)) {
            growCrop(block);
            
            // Find and queue adjacent crops more efficiently
            World world = loc.getWorld();
            int baseX = loc.getBlockX();
            int baseY = loc.getBlockY();
            int baseZ = loc.getBlockZ();
            
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && z == 0) continue;
                    
                    Location adjacent = new Location(world, baseX + x, baseY, baseZ + z);
                    if (processedCrops.contains(adjacent)) continue;
                    
                    Block adjacentBlock = world.getBlockAt(baseX + x, baseY, baseZ + z);
                    if (isCrop(adjacentBlock.getType()) && 
                        !isFullyGrown(adjacentBlock) && 
                        !growthQueue.contains(adjacent) &&
                        isInEnabledRegion(adjacent)) {
                        growthQueue.add(adjacent);
                        processedCrops.add(adjacent);
                    }
                }
            }
        }
        
        // Set next growth time based on chain delay
        nextGrowthTime = System.currentTimeMillis() + (config.getChainDelay() * 50);
    }

    private void growCrop(Block block) {
        if (block.getBlockData() instanceof Ageable ageable) {
            ageable.setAge(ageable.getMaximumAge());
            block.setBlockData(ageable);

            if (config.isShowGrowthParticles()) {
                Location particleLoc = block.getLocation().add(0.5, 0.5, 0.5);
                try {
                    Particle particle = Particle.valueOf(config.getGrowthParticle());
                    block.getWorld().spawnParticle(particle, particleLoc, 5, 0.2, 0.2, 0.2, 0);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid particle type in config: " + config.getGrowthParticle());
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Material type = block.getType();
        if (!isCrop(type)) {
            return;
        }

        Location location = block.getLocation();
        if (!isInEnabledRegion(location)) {
            return;
        }

        // Schedule immediate replant
        if (config.isAutoReplantEnabled()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    Block newBlock = location.getBlock();
                    if (newBlock.getType() == Material.AIR) {
                        newBlock.setType(type);
                        if (newBlock.getBlockData() instanceof Ageable ageable) {
                            ageable.setAge(0);
                            newBlock.setBlockData(ageable);
                        }
                    }
                }
            }.runTaskLater(plugin, 1L);
        }

        Player player = event.getPlayer();
        if (config.isRequireTools() && !isUsingAllowedTool(player)) {
            event.setCancelled(true);
            adventure.player(player).sendMessage(miniMessage.deserialize(config.getToolRequiredMessage()));
            return;
        }

        if (config.isPreventImmatureBreak() && !isFullyGrown(block)) {
            event.setCancelled(true);
            if (config.isShowImmatureMessage()) {
                adventure.player(player).sendMessage(miniMessage.deserialize(config.getCropImmatureMessage()));
            }
            return;
        }

        handleInventoryDrops(event, block);
    }

    private void handleInventoryDrops(BlockBreakEvent event, Block block) {
        event.setDropItems(false); // Prevent natural drops
        Player player = event.getPlayer();
        PlayerInventory inventory = player.getInventory();
        Material cropType = block.getType();
        Location dropLocation = block.getLocation().add(0.5, 0.5, 0.5);
        
        // Create items
        ItemStack drop = new ItemStack(getDropItem(cropType));
        ItemStack seed = new ItemStack(getSeedItem(cropType));
        
        if (config.isAddToInventoryEnabled()) {
            // Try to add items to inventory
            HashMap<Integer, ItemStack> remainingDrop = inventory.addItem(drop.clone());
            HashMap<Integer, ItemStack> remainingSeed = inventory.addItem(seed.clone());
            
            // Check if any items couldn't be added
            if (!remainingDrop.isEmpty() || !remainingSeed.isEmpty()) {
                // Show notifications first
                sendInventoryFullNotifications(player);
                
                // Then handle drops if enabled
                if (config.isDropIfFullEnabled()) {
                    remainingDrop.values().forEach(item -> 
                        block.getWorld().dropItemNaturally(dropLocation, item));
                    remainingSeed.values().forEach(item -> 
                        block.getWorld().dropItemNaturally(dropLocation, item));
                }
            }
        } else {
            // Drop items naturally if add-to-inventory is disabled
            block.getWorld().dropItemNaturally(dropLocation, drop);
            block.getWorld().dropItemNaturally(dropLocation, seed);
        }
    }

    private void sendInventoryFullNotifications(Player player) {
        // Always show action bar if enabled
        if (config.isInventoryFullActionBarEnabled()) {
            String actionBarMsg = config.getInventoryFullActionBar()
                .replace("<red>", "§c")
                .replace("<gray>", "§7");
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, 
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText(actionBarMsg));
        }

        // Show title if enabled
        if (config.isInventoryFullTitleEnabled()) {
            String title = config.getInventoryFullTitle()
                .replace("<red>", "§c")
                .replace("<gray>", "§7");
            String subtitle = config.getInventoryFullSubtitle()
                .replace("<red>", "§c")
                .replace("<gray>", "§7");
            player.sendTitle(title, subtitle, 10, config.getInventoryFullTitleDuration(), 10);
        }
    }

    private boolean isCrop(Material type) {
        return CROP_MATERIALS.contains(type);
    }

    private boolean isFullyGrown(Block block) {
        if (block.getBlockData() instanceof Ageable ageable) {
            return ageable.getAge() == ageable.getMaximumAge();
        }
        return false;
    }

    private boolean isUsingAllowedTool(Player player) {
        ItemStack tool = player.getInventory().getItemInMainHand();
        return config.getAllowedTools().contains(tool.getType().name());
    }

    private boolean isInEnabledRegion(Location location) {
        return regionQuery.getApplicableRegions(BukkitAdapter.adapt(location))
            .getRegions()
            .stream()
            .anyMatch(region -> plugin.getEnabledRegions().contains(region.getId()));
    }

    private Material getDropItem(Material crop) {
        return switch (crop) {
            case WHEAT -> Material.WHEAT;
            case CARROTS -> Material.CARROT;
            case POTATOES -> Material.POTATO;
            case BEETROOTS -> Material.BEETROOT;
            case NETHER_WART -> Material.NETHER_WART;
            default -> Material.AIR;
        };
    }

    private Material getSeedItem(Material crop) {
        return switch (crop) {
            case WHEAT -> Material.WHEAT_SEEDS;
            case CARROTS -> Material.CARROT;
            case POTATOES -> Material.POTATO;
            case BEETROOTS -> Material.BEETROOT_SEEDS;
            case NETHER_WART -> Material.NETHER_WART;
            default -> Material.AIR;
        };
    }
} 