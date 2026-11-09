package com.fuelingimagination.registry;

import com.fuelingimagination.FuelingImagination;
import com.fuelingimagination.block.entity.PrecisionMechanismPieBlockEntity;
import com.fuelingimagination.block.entity.PoweredArtisanTableBlockEntity;
import com.fuelingimagination.block.entity.PoweredDismantlerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, FuelingImagination.MOD_ID);

    public static final RegistryObject<BlockEntityType<PrecisionMechanismPieBlockEntity>> PRECISION_MECHANISM_PIE =
            BLOCK_ENTITIES.register("precision_mechanism_pie",
                    () -> BlockEntityType.Builder.of(PrecisionMechanismPieBlockEntity::new,
                            ModBlocks.PRECISION_MECHANISM_PIE.get()).build(null));

    public static final RegistryObject<BlockEntityType<PoweredArtisanTableBlockEntity>> POWERED_ARTISAN_TABLE =
            BLOCK_ENTITIES.register("powered_artisan_table",
                    () -> BlockEntityType.Builder.of(PoweredArtisanTableBlockEntity::new,
                            ModBlocks.POWERED_ARTISAN_TABLE.get()).build(null));

    public static final RegistryObject<BlockEntityType<PoweredDismantlerBlockEntity>> POWERED_DISMANTLER =
            BLOCK_ENTITIES.register("powered_dismantler",
                    () -> BlockEntityType.Builder.of(PoweredDismantlerBlockEntity::new,
                            ModBlocks.POWERED_DISMANTLER.get()).build(null));

    public static final RegistryObject<BlockEntityType<com.fuelingimagination.block.entity.ProjectorBlockEntity>> PROJECTOR =
            BLOCK_ENTITIES.register("projector",
                    () -> BlockEntityType.Builder.of(com.fuelingimagination.block.entity.ProjectorBlockEntity::new,
                            ModBlocks.PROJECTOR.get()).build(null));

    private ModBlockEntities() { }
}
