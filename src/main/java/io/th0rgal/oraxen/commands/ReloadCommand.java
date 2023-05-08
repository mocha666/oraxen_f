package io.th0rgal.oraxen.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.TextArgument;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.font.FontManager;
import io.th0rgal.oraxen.hud.HudManager;
import io.th0rgal.oraxen.items.ItemUpdater;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.recipes.RecipesManager;
import io.th0rgal.oraxen.sound.SoundManager;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class ReloadCommand {

    private static void reloadItems(CommandSender sender) {
        Message.RELOAD.send(sender, AdventureUtils.tagResolver("reloaded", "items"));
        OraxenItems.loadItems();

        if (!Settings.AUTO_UPDATE_ITEMS.toBool()) return;

        //TODO Make this async?

        if (Settings.UPDATE_ITEMS_ON_RELOAD.toBool()) {
            Logs.logInfo("Updating all items in player-inventories...");
            for (Player player : Bukkit.getServer().getOnlinePlayers()) {
                PlayerInventory inventory = player.getInventory();
                Bukkit.getScheduler().runTaskAsynchronously(OraxenPlugin.get(), () -> {
                    for (int i = 0; i < inventory.getSize(); i++) {
                        ItemStack oldItem = inventory.getItem(i);
                        ItemStack newItem = ItemUpdater.updateItem(oldItem);
                        if (oldItem == null || oldItem.equals(newItem)) continue;
                        inventory.setItem(i, newItem);
                    }
                });
            }
        }

        if (Settings.UPDATE_FURNITURE_ON_RELOAD.toBool()) {
            Logs.logInfo("Updating all placed furniture...");
            for (World world : Bukkit.getServer().getWorlds()) {
                for (Entity entity : world.getEntities())
                    if (OraxenFurniture.isFurniture(entity))
                        ItemUpdater.furnitureToUpdate.add(entity);
            }
        }

    }

    private static void reloadPack(CommandSender sender) {
        Message.PACK_REGENERATED.send(sender);
        OraxenPlugin oraxen = OraxenPlugin.get();
        oraxen.setFontManager(new FontManager(oraxen.getConfigsManager()));
        oraxen.setSoundManager(new SoundManager(oraxen.getConfigsManager().getSound()));
        oraxen.getResourcePack().generate(oraxen.getFontManager(), oraxen.getSoundManager());
        oraxen.getUploadManager().uploadAsyncAndSendToPlayers(oraxen.getResourcePack(), true, true);
    }

    private static void reloadHud(CommandSender sender) {
        Message.RELOAD.send(sender, AdventureUtils.tagResolver("reloaded", "hud"));
        OraxenPlugin.get().reloadConfigs();
        HudManager hudManager = new HudManager(OraxenPlugin.get().getConfigsManager());
        OraxenPlugin.get().setHudManager(hudManager);
        hudManager.loadHuds(hudManager.getHudConfigSection());
        hudManager.parsedHudDisplays = hudManager.generateHudDisplays();
        hudManager.reregisterEvents();
        hudManager.restartTask();
    }

    private static void reloadGestures(CommandSender sender) {
        Message.RELOAD.send(sender, AdventureUtils.tagResolver("reloaded", "gestures"));
        OraxenPlugin.get().getGesturesManager().reload();
    }

    public CommandAPICommand getReloadCommand() {
        return new CommandAPICommand("reload")
                .withAliases("rl")
                .withPermission("oraxen.command.reload")
                .withArguments(new TextArgument("type").replaceSuggestions(
                        ArgumentSuggestions.strings("items", "pack", "hud", "recipes", "messages", "all")))
                .executes((sender, args) -> {
                    switch (((String) args[0]).toUpperCase()) {
                        case "HUD" -> reloadHud(sender);
                        case "ITEMS" -> {
                            reloadItems(sender);
                            OraxenPlugin.get().getInvManager().regen();
                        }
                        case "PACK" -> reloadPack(sender);
                        case "RECIPES" -> RecipesManager.reload();
                        case "CONFIGS" -> OraxenPlugin.get().reloadConfigs();
                        default -> {
                            OraxenPlugin oraxen = OraxenPlugin.get();
                            MechanicsManager.unloadListeners();
                            MechanicsManager.registerNativeMechanics();
                            OraxenPlugin.get().reloadConfigs();
                            reloadItems(sender);
                            reloadPack(sender);
                            reloadHud(sender);
                            RecipesManager.reload();
                            OraxenPlugin.get().getInvManager().regen();
                        }
                    }
                    // This does not clear the tablist, and I am not sure how to do it otherwise
                    FontManager manager = new FontManager(OraxenPlugin.get().getConfigsManager());
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        manager.sendGlyphTabCompletion(player, false);
                    }
                });
    }

}
