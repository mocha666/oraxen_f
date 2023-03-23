package io.th0rgal.oraxen.mechanics.provided.gameplay.bookshelf;

import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.api.events.OraxenBookshelfInteractEvent;
import io.th0rgal.oraxen.api.events.OraxenBookshelfPlaceEvent;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.Utils;
import io.th0rgal.oraxen.utils.breaker.BreakerSystem;
import io.th0rgal.oraxen.utils.breaker.HardnessModifier;
import io.th0rgal.protectionlib.ProtectionLib;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;

import java.util.List;
import java.util.Objects;

public class BookshelfMechanicListener implements Listener {
    private final MechanicFactory factory = BookshelfMechanicFactory.getInstance();

    public BookshelfMechanicListener() {
        BreakerSystem.MODIFIERS.add(getHardnessModifier());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void callInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null || event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (block.getType() != Material.CHISELED_BOOKSHELF) return;
        BookshelfMechanic mechanic = OraxenBlocks.getBookshelfMechanic(block);
        if (mechanic == null) return;

        OraxenBookshelfInteractEvent oraxenEvent = new OraxenBookshelfInteractEvent(mechanic, block, event.getBlockFace(), event.getPlayer(), event.getItem(), event.getHand());
        Bukkit.getPluginManager().callEvent(oraxenEvent);
        if (oraxenEvent.isCancelled()) event.setCancelled(true);
    }

    // TODO try and fix these and not just cancel them
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonPush(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(block -> block.getType().equals(Material.CHISELED_BOOKSHELF)))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonPull(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(block -> block.getType().equals(Material.CHISELED_BOOKSHELF)))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPhysics(final BlockPhysicsEvent event) {
        if (event.getBlock().getType() == Material.CHISELED_BOOKSHELF) {
            event.setCancelled(true);
            event.getBlock().getState().update(true, false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractNoteBlock(OraxenBookshelfInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        BookshelfMechanic mechanic = event.getMechanic();
        if (mechanic == null) return;

        if (!player.isSneaking()) {
            if (mechanic.hasClickActions()) {
                mechanic.runClickActions(player);
                event.setCancelled(true);
            }
        }

    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreakingCustomBlock(final BlockBreakEvent event) {
        final Block block = event.getBlock();

        BookshelfMechanic mechanic = OraxenBlocks.getBookshelfMechanic(block);
        if (mechanic == null || !event.isDropItems()) return;
        if (OraxenBlocks.remove(block.getLocation(), event.getPlayer())) {
            event.setCancelled(true);
            return;
        }
        event.setDropItems(false);
    }

    @EventHandler
    public void onExplosionDestroy(EntityExplodeEvent event) {
        List<Block> blockList = event.blockList().stream().filter(block -> block.getType().equals(Material.CHISELED_BOOKSHELF)).toList();
        blockList.forEach(block -> OraxenBlocks.remove(block.getLocation(), null));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPrePlacingCustomBlock(final PlayerInteractEvent event) {
        final String itemID = OraxenItems.getIdByItem(event.getItem());
        final Player player = event.getPlayer();
        final Block placedAgainst = event.getClickedBlock();

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || placedAgainst == null) return;
        if (factory.isNotImplementedIn(itemID)) return;
        if (placedAgainst.getType().isInteractable() && placedAgainst.getType() != Material.NOTE_BLOCK) return;

        // determines the new block data of the block
        BookshelfMechanic mechanic = (BookshelfMechanic) factory.getMechanic(itemID);
        BlockFace face = event.getBlockFace();

        BlockData data = BookshelfMechanicFactory.createBookshelfData(mechanic.getCustomVariation());
        Block placedBlock = makePlayerPlaceBlock(player, event.getHand(), event.getItem(), placedAgainst, face, data);
        if (placedBlock != null) OraxenBlocks.place(mechanic.getItemID(), placedBlock.getLocation());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onMiddleClick(final InventoryCreativeEvent event) {
        if (!(event.getInventory().getHolder() instanceof Player player)) return;
        if (event.getClick() != ClickType.CREATIVE || event.getCursor().getType() != Material.CHISELED_BOOKSHELF) return;

        final RayTraceResult rayTraceResult = player.rayTraceBlocks(6.0);
        if (rayTraceResult == null) return;
        final Block block = rayTraceResult.getHitBlock();
        if (block == null) return;
        BookshelfMechanic mechanic = OraxenBlocks.getBookshelfMechanic(block);
        if (mechanic == null) return;

        ItemStack item = OraxenItems.getItemById(mechanic.getItemID()).build();
        for (int i = 0; i <= 8; i++) {
            if (player.getInventory().getItem(i) == null) continue;
            if (Objects.equals(OraxenItems.getIdByItem(player.getInventory().getItem(i)), OraxenItems.getIdByItem(item))) {
                player.getInventory().setHeldItemSlot(i);
                event.setCancelled(true);
                return;
            }
        }
        event.setCursor(item);
    }

    private HardnessModifier getHardnessModifier() {
        return new HardnessModifier() {

            @Override
            public boolean isTriggered(final Player player, final Block block, final ItemStack tool) {
                BookshelfMechanic mechanic = OraxenBlocks.getBookshelfMechanic(block);
                return mechanic != null && mechanic.hasHardness;
            }

            @Override
            public void breakBlock(final Player player, final Block block, final ItemStack tool) {
                block.setType(Material.AIR);
            }

            @Override
            public long getPeriod(final Player player, final Block block, final ItemStack tool) {
                BookshelfMechanic mechanic = OraxenBlocks.getBookshelfMechanic(block);
                if (mechanic == null) return 0;

                final long period = mechanic.getPeriod();
                double modifier = 1;
                if (mechanic.getDrop().canDrop(tool)) {
                    modifier *= 0.4;
                    final int diff = mechanic.getDrop().getDiff(tool);
                    if (diff >= 1)
                        modifier *= Math.pow(0.9, diff);
                }
                return (long) (period * modifier);
            }
        };
    }

    public Block makePlayerPlaceBlock(final Player player, final EquipmentSlot hand, final ItemStack item,
                                      final Block placedAgainst, final BlockFace face, final BlockData newBlock) {
        final Block target;
        final String sound;
        final Material type = placedAgainst.getType();

        if (BlockHelpers.REPLACEABLE_BLOCKS.contains(type))
            target = placedAgainst;
        else {
            target = placedAgainst.getRelative(face);
            if (!target.getType().isAir() && !target.isLiquid() && target.getType() != Material.LIGHT) return null;
        }

        final BlockData curentBlockData = target.getBlockData();
        final boolean isFlowing = (newBlock.getMaterial() == Material.WATER || newBlock.getMaterial() == Material.LAVA);
        target.setBlockData(newBlock, isFlowing);
        final BlockState currentBlockState = target.getState();
        final BookshelfMechanic againstMechanic = OraxenBlocks.getBookshelfMechanic(placedAgainst);

        final BlockPlaceEvent blockPlaceEvent = new BlockPlaceEvent(target, currentBlockState, placedAgainst, item, player, true, hand);
        Bukkit.getPluginManager().callEvent(blockPlaceEvent);

        if (BlockHelpers.correctAllBlockStates(target, player, face, item)) blockPlaceEvent.setCancelled(true);
        if (player.getGameMode() == GameMode.ADVENTURE) blockPlaceEvent.setCancelled(true);
        if (againstMechanic != null && againstMechanic.hasClickActions())
            blockPlaceEvent.setCancelled(true);
        if (BlockHelpers.isStandingInside(player, target) || !ProtectionLib.canBuild(player, target.getLocation()))
            blockPlaceEvent.setCancelled(true);
        if (!blockPlaceEvent.canBuild() || blockPlaceEvent.isCancelled()) {
            target.setBlockData(curentBlockData);
            return null;
        }

        final OraxenBookshelfPlaceEvent oraxenPlaceEvent = new OraxenBookshelfPlaceEvent(OraxenBlocks.getBookshelfMechanic(target), target, player);
        Bukkit.getPluginManager().callEvent(oraxenPlaceEvent);
        if (oraxenPlaceEvent.isCancelled()) {
            target.setBlockData(curentBlockData); // false to cancel physic
            return null;
        }

        if (isFlowing) {
            if (newBlock.getMaterial() == Material.WATER) sound = "item.bucket.empty";
            else sound = "item.bucket.empty_" + newBlock.getMaterial().toString().toLowerCase();
        } else sound = null;

        if (!player.getGameMode().equals(GameMode.CREATIVE)) {
            if (item.getType().toString().toLowerCase().contains("bucket")) item.setType(Material.BUCKET);
            else item.setAmount(item.getAmount() - 1);
        }

        if (sound != null)
            BlockHelpers.playCustomBlockSound(target.getLocation(), sound, SoundCategory.BLOCKS, 0.8f, 0.8f);
        Utils.swingHand(player, hand);

        return target;
    }

}
