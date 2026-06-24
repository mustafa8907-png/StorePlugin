package com.nexussetups.storeplugin;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class StorePlugin extends JavaPlugin implements CommandExecutor {

    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    @Override
    public void onEnable() {
        // Save default config if it doesn't exist
        saveDefaultConfig();
        
        // Register the main command defined in plugin.yml
        if (getCommand("store") != null) {
            getCommand("store").setExecutor(this);
        }

        // Register dynamic aliases from config.yml
        registerDynamicAliases();
        
        getLogger().info("StorePlugin enabled successfully! (Java 17 - 1.21.x)");
    }

    @Override
    public void onDisable() {
        getLogger().info("StorePlugin disabled.");
    }

    /**
     * Registers aliases defined in config.yml directly to the server's CommandMap.
     */
    private void registerDynamicAliases() {
        List<String> aliases = getConfig().getStringList("store-settings.command-aliases");
        if (aliases.isEmpty()) return;

        for (String alias : aliases) {
            BukkitCommand dynamicCmd = new BukkitCommand(alias) {
                @Override
                public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
                    return onCommand(sender, this, commandLabel, args);
                }
            };
            Bukkit.getServer().getCommandMap().register("storeplugin", dynamicCmd);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Prevent console execution (Required for UI elements and sounds)
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be executed by a player.");
            return true;
        }

        // Fetch toggle settings from config
        boolean sendChat = getConfig().getBoolean("store-settings.send-chat", true);
        boolean sendActionBar = getConfig().getBoolean("store-settings.send-actionbar", false);
        boolean sendBossBar = getConfig().getBoolean("store-settings.send-bossbar", false);

        // 1. Send Chat Message (MiniMessage Format)
        if (sendChat) {
            String chatMsg = getConfig().getString("store-settings.chat-message", "");
            if (!chatMsg.isEmpty()) {
                player.sendMessage(miniMessage.deserialize(chatMsg));
            }
        }

        // 2. Send Action Bar (MiniMessage Format)
        if (sendActionBar) {
            String actionBarMsg = getConfig().getString("store-settings.actionbar-message", "");
            if (!actionBarMsg.isEmpty()) {
                player.sendActionBar(miniMessage.deserialize(actionBarMsg));
            }
        }

        // 3. Play Sound Effect
        String soundKey = getConfig().getString("store-settings.sound", "ENTITY_PLAYER_LEVELUP");
        try {
            Sound sound = Sound.valueOf(soundKey.toUpperCase());
            float volume = (float) getConfig().getDouble("store-settings.sound-volume", 1.0);
            float pitch = (float) getConfig().getDouble("store-settings.sound-pitch", 1.0);
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            getLogger().warning("Invalid sound defined in config: " + soundKey);
        }

        // 4. Send Boss Bar (Adventure API & BukkitRunnable Scheduler)
        if (sendBossBar) {
            String bossBarMsg = getConfig().getString("store-settings.bossbar-message", "");
            if (!bossBarMsg.isEmpty()) {
                String colorKey = getConfig().getString("store-settings.bossbar-color", "GREEN");
                String overlayKey = getConfig().getString("store-settings.bossbar-overlay", "PROGRESS");
                int durationSeconds = getConfig().getInt("store-settings.bossbar-duration", 5);

                try {
                    BossBar.Color color = BossBar.Color.valueOf(colorKey.toUpperCase());
                    BossBar.Overlay overlay = BossBar.Overlay.valueOf(overlayKey.toUpperCase());
                    Component title = miniMessage.deserialize(bossBarMsg);

                    // Create BossBar instance
                    BossBar bossBar = BossBar.bossBar(title, 1.0f, color, overlay);
                    
                    // Display BossBar to the player
                    player.showBossBar(bossBar);

                    // Schedule removal task
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            player.hideBossBar(bossBar);
                        }
                    }.runTaskLater(this, durationSeconds * 20L); // 20 ticks = 1 second

                } catch (IllegalArgumentException e) {
                    getLogger().warning("Invalid BossBar color or overlay type defined in config!");
                }
            }
        }

        return true;
    }
}
