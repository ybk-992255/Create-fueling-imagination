package com.fuelingimagination.mixin;

import com.fuelingimagination.content.upgrade.BacktankHandPump;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Poses the player's arms while hand-pumping a backtank.
 *
 * <p>The pose has to be applied at the end of the model's own animation pass: the render event
 * fires before {@code setupAnim}, so anything written there is overwritten immediately. The target
 * is the SRG name of {@code setupAnim} for the {@code LivingEntity} descriptor, which is what the
 * renderer actually calls.
 */
@Mixin(value = PlayerModel.class, remap = false)
public abstract class PlayerModelMixin {
    /** Radians per tick of the crank orbit. */
    private static final float ORBIT_SPEED = 0.35F;
    /** Radius of the crank orbit, in arm radians. */
    private static final float ORBIT_RADIUS = 0.32F;
    /** Base arm elevation, as if gripping a crank in front of the chest. */
    private static final float ARM_LIFT = -0.8F;
    /** How far the arms sit from the body. */
    /** Thirty degrees inward, so both hands meet in front of the chest. */
    private static final float ARM_SPREAD = 0.52F;

    @Inject(method = "m_6973_(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("RETURN"), remap = false)
    private void fuelingimagination$pumpPose(LivingEntity entity, float limbSwing, float limbSwingAmount,
                                            float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (!(entity instanceof Player player)) return;

        PlayerModel<?> self = (PlayerModel<?>) (Object) this;

        // Armour mode keeps both arms up as if aiming, copying the crossbow hold pose so the suit
        // always looks locked on target instead of swinging with the walk cycle.
        if (com.fuelingimagination.content.armor.ArmorMode.isActive(player)) {
            self.rightArm.xRot = self.head.xRot - Mth.HALF_PI + 0.1F;
            self.rightArm.yRot = self.head.yRot;
            self.rightArm.zRot = 0.0F;
            self.leftArm.xRot = self.head.xRot - Mth.HALF_PI + 0.35F;
            self.leftArm.yRot = self.head.yRot + 0.28F;
            self.leftArm.zRot = 0.0F;
            return;
        }

        if (!BacktankHandPump.isPumping(player)) return;
        float phase = ageInTicks * ORBIT_SPEED;

        // Right arm holds the crank steady.
        self.rightArm.xRot = ARM_LIFT;
        self.rightArm.zRot = -ARM_SPREAD;
        self.rightArm.yRot = 0.0F;

        // Left arm keeps its reach but traces a circle around the shoulder.
        self.leftArm.xRot = ARM_LIFT + Mth.cos(phase) * ORBIT_RADIUS;
        self.leftArm.zRot = ARM_SPREAD + Mth.sin(phase) * ORBIT_RADIUS;
        self.leftArm.yRot = 0.0F;
    }
}
