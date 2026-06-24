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
        // Create default config
        saveDefaultConfig();
        
        // Register main command
        if (getCommand("store") != null) {
            getCommand("store").setExecutor(this);
        }

        // Register custom config aliases
        registerDynamicAliases();
        
        // Send Custom Advertisement to Console (Without standard Bukkit logger prefixes)
        CommandSender console = Bukkit.getConsoleSender();
        console.sendMessage(Component.text("---N-e-x-u-s-S-e-t-u-p-s---"));
        console.sendMessage(Component.text("Buy Premium Plugins for"));
        console.sendMessage(Component.text("mustafa8907.com.tr"));
        console.sendMessage(Component.text("discord.gg/mustafa8907"));
        console.sendMessage(Component.text("---N-e-x-u-s-S-e-t-u-p-s---"));
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
            // Apply the default permission to aliases as well
            dynamicCmd.setPermission("storeplugin.use");
            dynamicCmd.setPermissionMessage("You do not have permission to use this command!");
            
            Bukkit.getServer().getCommandMap().register("storeplugin", dynamicCmd);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Reload Command Logic (/store reload)
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("storeplugin.admin")) {
                sender.sendMessage(miniMessage.deserialize("<red>You do not have permission to reload!</red>"));
                return true;
            }
            reloadConfig();
            sender.sendMessage(miniMessage.deserialize("<green>StorePlugin configuration reloaded successfully!</green>"));
            return true;
        }

        // Check if sender is a player for UI elements
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be executed by a player.");
            return true;
        }

        // Feature Toggles
        boolean sendChat = getConfig().getBoolean("store-settings.send-chat", true);
        boolean sendActionBar = getConfig().getBoolean("store-settings.send-actionbar", false);
        boolean sendBossBar = getConfig().getBoolean("store-settings.send-bossbar", false);

        // 1. Chat Message (Now supports Multi-line list)
        if (sendChat) {
            List<String> chatMessages = getConfig().getStringList("store-settings.chat-message");
            for (String line : chatMessages) {
                player.sendMessage(miniMessage.deserialize(line));
            }
        }

        // 2. Action Bar
        if (sendActionBar) {
            String actionBarMsg = getConfig().getString("store-settings.actionbar-message", "");
            if (!actionBarMsg.isEmpty()) {
                player.sendActionBar(miniMessage.deserialize(actionBarMsg));
            }
        }

        // 3. Sound Effect
        String soundKey = getConfig().getString("store-settings.sound", "ENTITY_PLAYER_LEVELUP");
        try {
            Sound sound = Sound.valueOf(soundKey.toUpperCase());
            float volume = (float) getConfig().getDouble("store-settings.sound-volume", 1.0);
            float pitch = (float) getConfig().getDouble("store-settings.sound-pitch", 1.0);
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            getLogger().warning("Invalid sound defined in config: " + soundKey);
        }

        // 4. Boss Bar
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

                    BossBar bossBar = BossBar.bossBar(title, 1.0f, color, overlay);
                    player.showBossBar(bossBar);

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
