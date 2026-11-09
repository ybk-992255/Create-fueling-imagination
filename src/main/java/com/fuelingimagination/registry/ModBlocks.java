package com.fuelingimagination.registry;

import com.fuelingimagination.FuelingImagination;
import com.fuelingimagination.block.GearCookieBlock;
import com.fuelingimagination.block.PrecisionMechanismPieBlock;
import com.fuelingimagination.block.PoweredArtisanTableBlock;
import com.fuelingimagination.block.PoweredDismantlerBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, FuelingImagination.MOD_ID);

        public static final RegistryObject<Block> GEAR_COOKIE = BLOCKS.register("gear_cookie",
            () -> new GearCookieBlock(BlockBehaviour.Properties.of()
                    .strength(0.2F).sound(SoundType.WOOL).noOcclusion()));

    public static final RegistryObject<Block> PRECISION_MECHANISM_PIE = BLOCKS.register("precision_mechanism_pie",
            () -> new PrecisionMechanismPieBlock(BlockBehaviour.Properties.of()
                    .strength(0.3F).sound(SoundType.WOOL).noOcclusion()));

    public static final RegistryObject<Block> POWERED_ARTISAN_TABLE = BLOCKS.register("powered_artisan_table",
            () -> new PoweredArtisanTableBlock(BlockBehaviour.Properties.of()
                    .strength(2.5F, 1200.0F).sound(SoundType.WOOD).noOcclusion()
                    .requiresCorrectToolForDrops().pushReaction(PushReaction.BLOCK)));

    public static final RegistryObject<Block> POWERED_DISMANTLER = BLOCKS.register("powered_dismantler",
            () -> new PoweredDismantlerBlock(BlockBehaviour.Properties.of()
                    .strength(3.0F, 6.0F).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops().noOcclusion()));

    /** Same hardness, blast resistance, sound and tool tier as Create's block of raw zinc. */
    public static final RegistryObject<Block> TRASH_BLOCK = BLOCKS.register("trash_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.RAW_GOLD_BLOCK)
                    .mapColor(MapColor.GLOW_LICHEN)));

    public static final RegistryObject<Block> PROJECTOR = BLOCKS.register("projector",
            () -> new com.fuelingimagination.block.ProjectorBlock(BlockBehaviour.Properties.of()
                    .strength(2.0F, 6.0F).sound(SoundType.METAL).noOcclusion().noCollission()));

    public static final RegistryObject<Block> NOTE = BLOCKS.register("note",
            () -> new com.fuelingimagination.block.NoteBlock(BlockBehaviour.Properties.of()
                    .strength(0.5F).sound(SoundType.WOOL).noOcclusion()));

    private ModBlocks() { }
}
