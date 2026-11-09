package com.fuelingimagination.mixin;

import com.simibubi.create.content.logistics.box.PackageEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Releases a captured mob when its package is placed on the ground and broken. The creature package
 * stores the entity under {@code fuelingingimagination:entity}, so on destroy we spawn it instead of
 * letting the box drop.
 */
@Mixin(PackageEntity.class)
public abstract class PackageEntityMixin {
    @Inject(method = "destroy", at = @At("HEAD"), remap = false)
    private void fuelingimagination$releaseCreature(DamageSource source, CallbackInfo ci) {
        PackageEntity self = (PackageEntity) (Object) this;
        ItemStack box = self.getBox();
        if (!box.hasTag() || !box.getTag().contains("fuelingimagination:entity")) return;
        Level level = self.level();
        EntityType<?> type = EntityType.byString(box.getTag().getString("fuelingimagination:entity_type")).orElse(null);
        if (type == null) return;
        Entity entity = type.create(level);
        if (entity == null) return;
        entity.load(box.getTag().getCompound("fuelingimagination:entity"));
        entity.setPos(self.getX(), self.getY(), self.getZ());
        level.addFreshEntity(entity);
    }
}
