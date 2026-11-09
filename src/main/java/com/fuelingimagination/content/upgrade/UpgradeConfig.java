package com.fuelingimagination.content.upgrade;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.fuelingimagination.FuelingImagination;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data-driven upgrade layouts.
 *
 * <p>Each JSON under {@code data/<namespace>/fuelingimagination/upgrades/} declares which items
 * are modifiable and what slots they expose. Slot types are {@code armor}, {@code upgrade},
 * {@code accessory} and {@code material}. Material slots accept objects carrying per-item stats
 * (damage and the two attack-speed values), so the same slot can reshape a tool differently.
 */
public final class UpgradeConfig extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "fuelingimagination/upgrades";
    public static final UpgradeConfig INSTANCE = new UpgradeConfig();

    /** Stats a material contributes to the final tool. */
    public record MaterialStats(double damage, double gasSpeed, double drySpeed) { }

    /** One slot on the ring: what it accepts and what it is called. */
    public record Slot(String id, String type, List<ResourceLocation> items, List<TagKey<Item>> tags,
                       Map<ResourceLocation, MaterialStats> materials) {
        public boolean accepts(ItemStack stack) {
            if (stack.isEmpty()) return false;
            ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (key != null && (items.contains(key) || materials.containsKey(key))) return true;
            for (TagKey<Item> tag : tags) if (stack.is(tag)) return true;
            if ("armor".equals(type) && stack.getItem() instanceof net.minecraft.world.item.ArmorItem armor) {
                return armor.getEquipmentSlot() == net.minecraft.world.entity.EquipmentSlot.CHEST;
            }
            return false;
        }

        public MaterialStats statsFor(ItemStack stack) {
            ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
            return key == null ? null : materials.get(key);
        }
    }

    private static final Map<Item, List<Slot>> LAYOUTS = new HashMap<>();

    private UpgradeConfig() {
        super(new Gson(), DIRECTORY);
    }

    /** The client reload listener is unreliable; when it produced nothing, read the files directly. */
    public static void ensureLoaded() {
        if (!LAYOUTS.isEmpty()) return;
        if (!net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) return;
        try {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            var rm = mc.getResourceManager();
            Map<ResourceLocation, JsonElement> files = new HashMap<>();
            rm.listResources(DIRECTORY, path -> path.getPath().endsWith(".json")).forEach((rl, res) -> {
                try (var in = res.open()) {
                    files.put(rl, com.google.gson.JsonParser.parseReader(
                            new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)));
                } catch (Exception ignored) { }
            });
            if (!files.isEmpty()) INSTANCE.apply(files, rm, mc.getProfiler());
        } catch (Throwable ignored) { }
    }

    public static List<Slot> slotsFor(ItemStack stack) {
        if (stack.isEmpty()) return List.of();
        ensureLoaded();
        List<Slot> slots = LAYOUTS.get(stack.getItem());
        return slots == null ? List.of() : slots;
    }

    public static boolean isModifiable(ItemStack stack) {
        return !slotsFor(stack).isEmpty();
    }

    /**
     * Whether any layout is present at all. The client-side reload listener is unreliable (it
     * sometimes applies zero files), so slot acceptance must fall back to "let the server decide"
     * instead of rejecting everything when this returns false.
     */
    public static boolean isLoaded() {
        ensureLoaded();
        return !LAYOUTS.isEmpty();
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        LAYOUTS.clear();
        files.forEach((file, element) -> {
            if (!element.isJsonObject()) return;
            JsonObject root = element.getAsJsonObject();
            List<Slot> slots = new ArrayList<>();
            if (root.has("slots")) {
                for (JsonElement entry : root.getAsJsonArray("slots")) {
                    JsonObject slot = entry.getAsJsonObject();
                    String type = slot.has("type") ? slot.get("type").getAsString() : "upgrade";
                    List<ResourceLocation> items = new ArrayList<>();
                    List<TagKey<Item>> tags = new ArrayList<>();
                    Map<ResourceLocation, MaterialStats> materials = new HashMap<>();
                    if (slot.has("accepts")) {
                        for (JsonElement accept : slot.getAsJsonArray("accepts")) {
                            if (accept.isJsonObject()) {
                                // Material slot: { "item": "...", "damage": 3, "gas_speed": -0.2, "dry_speed": -0.6 }
                                JsonObject obj = accept.getAsJsonObject();
                                ResourceLocation id = new ResourceLocation(obj.get("item").getAsString());
                                materials.put(id, new MaterialStats(
                                        obj.get("damage").getAsDouble(),
                                        obj.get("gas_speed").getAsDouble(),
                                        obj.get("dry_speed").getAsDouble()));
                            } else {
                                String value = accept.getAsString();
                                if (value.startsWith("#")) tags.add(ItemTags.create(new ResourceLocation(value.substring(1))));
                                else items.add(new ResourceLocation(value));
                            }
                        }
                    }
                    slots.add(new Slot(slot.get("id").getAsString(), type, items, tags, materials));
                }
            }
            if (root.has("items")) {
                for (JsonElement item : root.getAsJsonArray("items")) {
                    Item target = ForgeRegistries.ITEMS.getValue(new ResourceLocation(item.getAsString()));
                    if (target != null && target != Items.AIR) LAYOUTS.put(target, List.copyOf(slots));
                }
            }
        });
        FuelingImagination.LOGGER.info("Loaded {} upgrade layout(s)", LAYOUTS.size());
    }
}
