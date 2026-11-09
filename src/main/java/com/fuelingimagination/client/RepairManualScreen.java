package com.fuelingimagination.client;

import com.fuelingimagination.content.damaged.DamagedBlocks;
import com.fuelingimagination.recipe.ArtisanCraftingRecipe;
import com.fuelingimagination.registry.ModRecipes;
import com.simibubi.create.Create;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

/**
 * The station's manual: three paged tabs — manufacture (recipes), modification (items and
 * materials) and repair (the repairable block list, ordered like Create's creative tab).
 */
public final class RepairManualScreen extends Screen {
    private static final int PANEL_WIDTH = 232;
    private static final int PANEL_HEIGHT = 178;
    private static final int TAB_W = 52;
    private static final int CELL = 27; // 1.5x of the usual 18px slot

    private static final String[] TABS = { "制造", "改装", "维修" };

    private record Row(ItemStack icon, String text, boolean header) { }
    private record RecipePage(String title, ItemStack result, NonNullList<Ingredient> ingredients, int w, int h) { }
    private record ModifyEntry(ItemStack item, int slots, List<ItemStack> materials, boolean unreadable) { }

    private final List<RecipePage> manufacture = new ArrayList<>();
    private final List<ModifyEntry> modify = new ArrayList<>();
    private final List<Row> repair = new ArrayList<>();

    private int tab;
    private final int[] page = new int[3];

    public RepairManualScreen() {
        super(Component.translatable("gui.fuelingimagination.manual.title"));
        buildRepair();
        buildModify();
        buildManufacture();
    }

    private static ItemStack item(String id) {
        var it = BuiltInRegistries.ITEM.get(new ResourceLocation(id));
        return it == null ? ItemStack.EMPTY : new ItemStack(it);
    }

    /** Create block registration order, used to order the repair list like Create's creative tab. */
    private static final Map<String, Integer> CREATE_ORDER = buildCreateOrder();

    private static Map<String, Integer> buildCreateOrder() {
        String[] ids = { "schematicannon","schematic_table","shaft","cogwheel","large_cogwheel","andesite_encased_shaft","brass_encased_shaft","andesite_encased_cogwheel","brass_encased_cogwheel","andesite_encased_large_cogwheel","brass_encased_large_cogwheel","gearbox","clutch","gearshift","encased_chain_drive","adjustable_chain_gearshift","belt","chain_conveyor","creative_motor","water_wheel","large_water_wheel","water_wheel_structure","encased_fan","nozzle","turntable","hand_crank","cuckoo_clock","mysterious_cuckoo_clock","millstone","crushing_wheel","crushing_wheel_controller","mechanical_press","mechanical_mixer","basin","blaze_burner","lit_blaze_burner","depot","weighted_ejector","chute","smart_chute","speedometer","stressometer","wooden_bracket","metal_bracket","fluid_pipe","encased_fluid_pipe","glass_fluid_pipe","mechanical_pump","smart_fluid_pipe","fluid_valve","copper_valve_handle","fluid_tank","creative_fluid_tank","hose_pulley","item_drain","spout","portable_fluid_interface","steam_engine","steam_whistle","steam_whistle_extension","powered_shaft","mechanical_piston","sticky_mechanical_piston","piston_extension_pole","mechanical_piston_head","gantry_carriage","gantry_shaft","windmill_bearing","mechanical_bearing","clockwork_bearing","rope_pulley","rope","pulley_magnet","elevator_pulley","cart_assembler","controller_rail","minecart_anchor","linear_chassis","secondary_linear_chassis","radial_chassis","sticker","contraption_controls","mechanical_drill","mechanical_saw","deployer","portable_storage_interface","redstone_contact","elevator_contact","mechanical_harvester","mechanical_plough","mechanical_roller","sail_frame","white_sail","andesite_casing","brass_casing","copper_casing","shadow_steel_casing","refined_radiance_casing","mechanical_crafter","sequenced_gearshift","flywheel","rotation_speed_controller","mechanical_arm","track","fake_track","railway_casing","track_station","track_signal","track_observer","small_bogey","large_bogey","controls","andesite_funnel","andesite_belt_funnel","brass_funnel","brass_belt_funnel","andesite_tunnel","brass_tunnel","content_observer","stockpile_switch","creative_crate","item_vault","item_hatch","packager","repackager","package_frogport","stock_link","stock_ticker","redstone_requester","factory_gauge","andesite_table_cloth","brass_table_cloth","copper_table_cloth","display_link","display_board","nixie_tube","rose_quartz_lamp","redstone_link","analog_lever","placard","pulse_repeater","pulse_extender","pulse_timer","powered_latch","powered_toggle_latch","lectern_controller","copper_backtank","netherite_backtank","peculiar_bell","haunted_bell","desk_bell","clipboard","andesite_ladder","brass_ladder","copper_ladder","andesite_scaffolding","brass_scaffolding","copper_scaffolding","metal_girder","metal_girder_encased_shaft","copycat_base","copycat_step","copycat_panel","copycat_bars","andesite_door","brass_door","copper_door","train_door","train_trapdoor","framed_glass_door","framed_glass_trapdoor","zinc_ore","deepslate_zinc_ore","raw_zinc_block","zinc_block","andesite_alloy_block","industrial_iron_block","weathered_iron_block","brass_block","cardboard_block","bound_cardboard_block","experience_block","rose_quartz_block","rose_quartz_tiles","small_rose_quartz_tiles" };
        Map<String, Integer> order = new HashMap<>();
        for (int i = 0; i < ids.length; i++) order.put("create:" + ids[i], i);
        return order;
    }

    private void buildRepair() {
        StreamSupport.stream(BuiltInRegistries.ITEM.spliterator(), false)
                .map(ItemStack::new)
                .filter(stack -> stack.is(DamagedBlocks.DAMAGEABLE_BLOCKS))
                .sorted(Comparator
                        .comparingInt((ItemStack s) -> {
                            String key = BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
                            return CREATE_ORDER.getOrDefault(key, Integer.MAX_VALUE);
                        })
                        .thenComparing(s -> BuiltInRegistries.ITEM.getKey(s.getItem()).toString()))
                .forEach(stack -> repair.add(new Row(stack, "", false)));
    }

    private static final List<String> BACKTANK_MATERIALS = List.of(
            "create:precision_mechanism", "fuelingimagination:universal_component",
            "fuelingimagination:compressed_gear_train");
    private static final List<String> SWORD_MATERIALS = List.of(
            "minecraft:copper_ingot", "minecraft:iron_ingot", "create:brass_ingot", "create:sturdy_sheet",
            "create:mechanical_saw", "create:mechanical_drill", "create:blaze_burner",
            "create:encased_fan", "create:rope_pulley", "create:package_frogport", "create:mechanical_plough");
    private static final List<String> JETPACK_MATERIALS = List.of(
            "create:precision_mechanism", "fuelingimagination:universal_component",
            "fuelingimagination:compressed_gear_train", "minecraft:elytra");
    private static final List<String> COMMAND_BLOCK_MATERIALS = List.of(
            "fuelingimagination:instruction_interface",
            "fuelingimagination:command_plugin_touch",
            "fuelingimagination:command_plugin_weather",
            "fuelingimagination:command_plugin_anchor",
            "fuelingimagination:command_plugin_effect",
            "fuelingimagination:command_plugin_power",
            "fuelingimagination:command_plugin_title",
            "fuelingimagination:command_plugin_sound",
            "fuelingimagination:command_plugin_redstone");

    private void buildModify() {
        modify.add(new ModifyEntry(item("create:copper_backtank"), 3, mats(BACKTANK_MATERIALS), false));
        modify.add(new ModifyEntry(item("create:netherite_backtank"), 3, mats(BACKTANK_MATERIALS), false));
        for (String jet : List.of("create_jetpack:jetpack", "create_jetpack:netherite_jetpack")) {
            ItemStack stack = item(jet);
            if (stack.isEmpty()) modify.add(new ModifyEntry(ItemStack.EMPTY, 0, List.of(), true));
            else modify.add(new ModifyEntry(stack, 4, mats(JETPACK_MATERIALS), false));
        }
        modify.add(new ModifyEntry(item("fuelingimagination:powered_sword"), 5, mats(SWORD_MATERIALS), false));
        modify.add(new ModifyEntry(item("fuelingimagination:command_block_q"), 1, mats(COMMAND_BLOCK_MATERIALS), false));
    }

    private static List<ItemStack> mats(List<String> ids) {
        List<ItemStack> out = new ArrayList<>();
        for (String id : ids) {
            ItemStack s = item(id);
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }

    private void buildManufacture() {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        for (ArtisanCraftingRecipe recipe : level.getRecipeManager().getAllRecipesFor(ModRecipes.ARTISAN_CRAFTING.get())) {
            ItemStack result = recipe.getResultItem(level.registryAccess());
            manufacture.add(new RecipePage(result.getHoverName().getString(), result,
                    recipe.getIngredients(), recipe.recipeWidth(), recipe.recipeHeight()));
        }
    }

    private int pageCount() {
        return switch (tab) {
            case 0 -> Math.max(1, manufacture.size());
            case 1 -> Math.max(1, modify.size());
            default -> Math.max(1, (repair.size() + 8) / 9);
        };
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        int right = left + PANEL_WIDTH;
        int bottom = top + PANEL_HEIGHT;

        graphics.fill(left, top, right, bottom, 0xF01A1410);
        graphics.fill(left, top, right, top + 1, 0xFF8B6A45);
        graphics.fill(left, bottom - 1, right, bottom, 0xFF8B6A45);
        graphics.fill(left, top, left + 1, bottom, 0xFF8B6A45);
        graphics.fill(right - 1, top, right, bottom, 0xFF8B6A45);

        graphics.drawCenteredString(font, Component.literal(TABS[tab]), left + TAB_W + (right - left - TAB_W) / 2,
                top + 7, 0xFFE6C88A);

        // Left tabs.
        int tabY = top + 22;
        for (int i = 0; i < TABS.length; i++) {
            boolean selected = i == tab;
            int x0 = left + 4, x1 = left + TAB_W - 2, y0 = tabY + i * 22, y1 = y0 + 18;
            graphics.fill(x0, y0, x1, y1, selected ? 0xFF5A4632 : 0xFF2E241C);
            graphics.fill(x0, y0, x1, y0 + 1, selected ? 0xFFE6C88A : 0xFF8B6A45);
            graphics.drawCenteredString(font, Component.literal(TABS[i]), (x0 + x1) / 2, y0 + 5,
                    selected ? 0xFFFFF2C0 : 0xFFD8CDBB);
        }

        int cx = left + TAB_W + 4;
        int cy = top + 22;
        switch (tab) {
            case 0 -> renderManufacture(graphics, cx, cy, right);
            case 1 -> renderModify(graphics, cx, cy, right);
            default -> renderRepair(graphics, cx, cy);
        }

        // Pager.
        int pagerY = bottom - 14;
        int p = page[tab];
        int total = pageCount();
        if (p > 0) graphics.drawString(font, "◀", cx, pagerY, 0xFFE6C88A, false);
        graphics.drawCenteredString(font, (p + 1) + "/" + total, cx + (right - cx) / 2, pagerY, 0xFFD8CDBB);
        if (p < total - 1) graphics.drawString(font, "▶", right - 12, pagerY, 0xFFE6C88A, false);
    }

    private void renderManufacture(GuiGraphics graphics, int cx, int cy, int right) {
        int p = page[tab];
        if (p >= manufacture.size()) return;
        RecipePage rp = manufacture.get(p);

        // Result item + name, centered.
        int contentW = right - cx;
        int titleW = font.width(rp.title());
        graphics.renderItem(rp.result(), cx + (contentW - 16) / 2, cy);
        graphics.drawCenteredString(font, rp.title(), cx + contentW / 2, cy + 20, 0xFFE6C88A);

        // 3x3 recipe, enlarged and centered.
        int gridW = 3 * CELL;
        int gx = cx + (contentW - gridW) / 2;
        int gy = cy + 36;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int x = gx + col * CELL, y = gy + row * CELL;
                graphics.fill(x - 1, y - 1, x + CELL - 2, y + CELL - 2, 0x208B6A45);
                if (col < rp.w() && row < rp.h()) {
                    Ingredient ing = rp.ingredients().get(col + row * rp.w());
                    ItemStack[] options = ing.getItems();
                    if (options.length > 0) {
                        graphics.renderItem(options[0], x + (CELL - 16) / 2, y + (CELL - 16) / 2);
                    }
                }
            }
        }
    }

    private void renderModify(GuiGraphics graphics, int cx, int cy, int right) {
        int p = page[tab];
        if (p >= modify.size()) return;
        ModifyEntry entry = modify.get(p);
        if (entry.unreadable()) {
            graphics.drawString(font, "你无法理解这个东西的结构", cx + 2, cy + 4, 0xFF9A8B7A, false);
            return;
        }
        graphics.renderItem(entry.item(), cx, cy);
        graphics.drawString(font, entry.item().getHoverName().getString() + " · " + entry.slots() + " 槽",
                cx + 20, cy + 4, 0xFFD8CDBB, false);
        // Its materials, icons only (no name / no effect).
        int x = cx;
        int y = cy + 22;
        for (ItemStack mat : entry.materials()) {
            graphics.renderItem(mat, x, y);
            x += 18;
            if (x + 16 > right - 2) { x = cx; y += 18; }
        }
    }

    private void renderRepair(GuiGraphics graphics, int cx, int cy) {
        int p = page[tab];
        int start = p * 9;
        int y = cy;
        for (int i = 0; i < 9; i++) {
            int index = start + i;
            if (index >= repair.size()) break;
            ItemStack stack = repair.get(index).icon();
            graphics.renderItem(stack, cx, y);
            graphics.drawString(font, (index + 1) + ". " + stack.getHoverName().getString(),
                    cx + 20, y + 2, 0xFFD8CDBB, false);
            y += 16;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int left = (width - PANEL_WIDTH) / 2;
            int top = (height - PANEL_HEIGHT) / 2;
            int bottom = top + PANEL_HEIGHT;
            int cx = left + TAB_W + 4;
            int pagerY = bottom - 16;
            int pagerH = 14;
            // Tabs.
            int tabY = top + 22;
            for (int i = 0; i < TABS.length; i++) {
                int y0 = tabY + i * 22, y1 = y0 + 18;
                if (mouseX >= left + 4 && mouseX <= left + TAB_W - 2 && mouseY >= y0 && mouseY <= y1) {
                    tab = i;
                    return true;
                }
            }
            // Pager arrows — doubled hitbox.
            if (mouseY >= pagerY - pagerH && mouseY <= pagerY + pagerH) {
                if (mouseX >= cx - 6 && mouseX <= cx + 20 && page[tab] > 0) { page[tab]--; return true; }
                int total = pageCount();
                if (mouseX >= left + PANEL_WIDTH - 20 && mouseX <= left + PANEL_WIDTH + 6
                        && page[tab] < total - 1) { page[tab]++; return true; }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int total = pageCount();
        if (delta > 0 && page[tab] > 0) page[tab]--;
        else if (delta < 0 && page[tab] < total - 1) page[tab]++;
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 || Minecraft.getInstance().options.keyInventory.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
