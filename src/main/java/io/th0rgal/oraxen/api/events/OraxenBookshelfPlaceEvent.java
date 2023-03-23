package io.th0rgal.oraxen.api.events;

import io.lumine.mythic.utils.text.Books;
import io.th0rgal.oraxen.mechanics.provided.gameplay.bookshelf.BookshelfMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class OraxenBookshelfPlaceEvent extends Event implements Cancellable {

    private final BookshelfMechanic mechanic;
    private final Player player;
    private final Block block;
    private boolean isCancelled;
    private static final HandlerList HANDLERS = new HandlerList();

    public OraxenBookshelfPlaceEvent(BookshelfMechanic mechanic, Block block, Player player) {
        this.mechanic = mechanic;
        this.block = block;
        this.player = player;
        this.isCancelled = false;
    }

    /**
     * @return The BookshelfMechanic of this block
     */
    public BookshelfMechanic getMechanic() {
        return mechanic;
    }

    /**
     * @return The player who placed this block
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * @return The block that was placed
     */
    public Block getBlock() {
        return block;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        isCancelled = cancel;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return getHandlerList();
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
