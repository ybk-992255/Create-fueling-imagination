package com.fuelingimagination.registry;

import com.fuelingimagination.FuelingImagination;
import com.fuelingimagination.entity.Scavenger;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, FuelingImagination.MOD_ID);

    public static final RegistryObject<EntityType<Scavenger>> SCAVENGER = ENTITIES.register("scavenger",
            () -> EntityType.Builder.of(Scavenger::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .build("scavenger"));

    private ModEntities() { }
}
