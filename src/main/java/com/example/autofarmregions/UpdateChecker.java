package com.example.autofarmregions;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateChecker implements Listener {
    private final AutoFarmRegions plugin;
    private final String currentVersion;
    private String latestVersion;
    private boolean updateAvailable = false;

    public UpdateChecker(AutoFarmRegions plugin) {
        this.plugin = plugin;
        this.currentVersion = plugin.getDescription().getVersion();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void checkForUpdates() {
        try {
            URL url = new URL("https://api.github.com/repos/shpellar/AutoFarmRegions/releases/latest");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
            connection.setRequestProperty("User-Agent", "AutoFarmRegions/" + currentVersion);

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // Extract version from tag_name using regex
                Pattern pattern = Pattern.compile("\"tag_name\":\\s*\"([^\"]+)\"");
                Matcher matcher = pattern.matcher(response.toString());

                if (matcher.find()) {
                    latestVersion = matcher.group(1).replace("v", ""); // Remove 'v' prefix if present
                    updateAvailable = !currentVersion.equals(latestVersion);

                    if (updateAvailable) {
                        Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + "[AutoFarmRegions] A new update is available!");
                        Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + "Current version: " + currentVersion);
                        Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + "Latest version: " + latestVersion);
                        Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + "Download: https://modrinth.com/plugin/autofarmregions/version/" + latestVersion);
                    } else {
                        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "[AutoFarmRegions] You are running the latest version!");
                    }
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to check for updates: " + e.getMessage());
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (updateAvailable && player.hasPermission("autofarmregions.admin")) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.sendMessage(ChatColor.YELLOW + "[AutoFarmRegions] A new update is available!");
                player.sendMessage(ChatColor.YELLOW + "Current version: " + currentVersion);
                player.sendMessage(ChatColor.YELLOW + "Latest version: " + latestVersion);
                player.sendMessage(ChatColor.YELLOW + "Download: https://modrinth.com/plugin/autofarmregions/version/" + latestVersion);
            }, 40L);
        }
    }
} 