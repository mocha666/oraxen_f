package io.th0rgal.oraxen.mechanics.provided.gameplay.bookshelf;

import com.google.gson.JsonObject;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.ChiseledBookshelf;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BookshelfMechanicFactory extends MechanicFactory {
    private static final int VARIATION_ID_OFFSET = 4 * 2; // 4 faces, empty and filled states
    public static final Map<Integer, BookshelfMechanic> BLOCK_PER_VARIATION = new HashMap<>();
    private static JsonObject variants;
    private static BookshelfMechanicFactory instance;
    public final List<String> toolTypes;

    public BookshelfMechanicFactory(ConfigurationSection section) {
        super(section);
        instance = this;
        variants = new JsonObject();
        for (BlockFace blockFace : chiseledFaces) {
            variants.add(getBlockstateVariantName(blockFace, new ChiseledBookshelfOccupiedSlots(Set.of())), getModelJson("block/chiseled_bookshelf"));
            variants.add(getBlockstateVariantName(blockFace, new ChiseledBookshelfOccupiedSlots(Set.of(1,2,3,4,5,6))), getModelJson("block/chiseled_bookshelf"));
        }
        toolTypes = section.getStringList("tool_types");

        OraxenPlugin.get().getResourcePack().addModifiers(getMechanicID(), packFolder ->
                OraxenPlugin.get().getResourcePack().writeStringToVirtual(
                        "assets/minecraft/blockstates", "chiseled_bookshelf.json", getBlockstateContent())
        );

        MechanicsManager.registerListeners(OraxenPlugin.get(), new BookshelfMechanicListener());
    }

    private String getBlockstateContent() {
        JsonObject bookshelf = new JsonObject();
        bookshelf.add("variants", variants);
        return bookshelf.toString();
    }

    public static JsonObject getModelJson(String modelName) {
        JsonObject content = new JsonObject();
        content.addProperty("model", modelName);

        return content;
    }

    /**
     * Attempts to set the block directly to the model and texture of an Oraxen item.
     *
     * @param block  The block to update.
     * @param itemId The Oraxen item ID.
     */
    public static void setBlockModel(Block block, String itemId) {
        final MechanicFactory factory = MechanicsManager.getMechanicFactory("bookshelf");
        BookshelfMechanic mechanic = (BookshelfMechanic) factory.getMechanic(itemId);
        if  (mechanic == null) return;
        block.setBlockData(createBookshelfData(mechanic.getCustomVariation()), false);
    }

    @Override
    public Mechanic parse(ConfigurationSection section) {
        BookshelfMechanic mechanic = new BookshelfMechanic(this, section);
        variants.add(getBlockstateVariantName(mechanic.getCustomVariation()), getModelJson("block/chiseled_bookshelf"));

        BLOCK_PER_VARIATION.put(mechanic.getCustomVariation(), mechanic);
        addToImplemented(mechanic);
        return mechanic;
    }

    private static final List<BlockFace> chiseledFaces = List.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.WEST, BlockFace.EAST);

    public static ChiseledBookshelf createBookshelfData(int id) {
        ChiseledBookshelf data = (ChiseledBookshelf) Material.CHISELED_BOOKSHELF.createBlockData();
        int i = VARIATION_ID_OFFSET;
        for (BlockFace face : chiseledFaces)
            if ((id & 0x1 << i++) != 0) data.setFacing(face);
        for (int slot : new int[]{0, 1, 2, 3, 4, 5})
            i += (int) Math.pow(2, slot);
        return data;
    }

    public static int getCode(final ChiseledBookshelf blockData) {
        int sum = VARIATION_ID_OFFSET;
        for (BlockFace blockFace : blockData.getFaces())
            sum += (int) Math.pow(2, chiseledFaces.indexOf(blockFace));
        for (int slot : blockData.getOccupiedSlots())
            sum += (int) Math.pow(2, slot);


        return sum;
    }


    public static String getBlockstateVariantName(int id) {
        id += 8;
        String facing = chiseledFaces.get(id / VARIATION_ID_OFFSET).toString().toLowerCase();
        return getBlockstateVariantName(createBookshelfData(id));
    }

    public static String getBlockstateVariantName(ChiseledBookshelf blockData) {
        return getBlockstateVariantName(blockData.getFacing(), new ChiseledBookshelfOccupiedSlots(blockData.getOccupiedSlots()));
    }

    public static String getBlockstateVariantName(BlockFace facing, ChiseledBookshelfOccupiedSlots occupiedSlots) {
        return "facing=" + facing.toString().toLowerCase() + "," + occupiedSlots.toString();
    }

    public static BookshelfMechanic getBlockMechanic(int customVariation) {
        return BLOCK_PER_VARIATION.get(customVariation);
    }

    public static BookshelfMechanicFactory getInstance() {
        return instance;
    }

    public static class ChiseledBookshelfOccupiedSlots {
        public static boolean SLOT_0 = false;
        public static boolean SLOT_1 = false;
        public static boolean SLOT_2 = false;
        public static boolean SLOT_3 = false;
        public static boolean SLOT_4 = false;
        public static boolean SLOT_5 = false;


        public ChiseledBookshelfOccupiedSlots(Set<Integer> slots) {
            SLOT_0 = slots.contains(0);
            SLOT_1 = slots.contains(1);
            SLOT_2 = slots.contains(2);
            SLOT_3 = slots.contains(3);
            SLOT_4 = slots.contains(4);
            SLOT_5 = slots.contains(5);
        }

        @Override
        public String toString() {
            return "slot_0_occupied=" + SLOT_0 + ",slot_1_occupied=" + SLOT_1 + ",slot_2_occupied=" + SLOT_2 + ",slot_3_occupied=" + SLOT_3 + ",slot_4_occupied=" + SLOT_4 + ",slot_5_occupied=" + SLOT_5;
        }
    }

}
