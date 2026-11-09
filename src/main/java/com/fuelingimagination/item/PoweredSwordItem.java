package com.fuelingimagination.item;

import com.fuelingimagination.content.upgrade.UpgradeConfig;
import com.fuelingimagination.content.upgrade.UpgradeData;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.simibubi.create.content.equipment.armor.BacktankUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;

/**
 * A multi-tool powered by the wearer's backtank instead of durability.
 *
 * <p>A material slot reshapes the blade (damage and the two attack-speed values) and up to two
 * accessory slots add behaviours. Attacks and block breaking draw air; with no air the tool still
 * works but swings on its slower "dry" speed. Kills are counted by {@link PoweredSwordEvents}.
 */
public final class PoweredSwordItem extends SwordItem {
    /** One air unit per hit/break, expressed as the tank fraction Create uses. */
    private static final int USES_PER_TANK = 900;
    /** usesPerTank values that consume 10 and 50 air units respectively (900 / air). */
    private static final int AIR_10 = 90;
    private static final int AIR_50 = 18;

    /** Dirt-like blocks the plough turns into farmland. */
    private static final Set<Block> TILLABLE = Set.of(
            Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.COARSE_DIRT, Blocks.ROOTED_DIRT,
            Blocks.DIRT_PATH, Blocks.MYCELIUM, Blocks.PODZOL);
    /** Set on the stack whenever the last hit could not draw air. */
    public static final String NBT_DRY = "fuelingimagination:sword_dry";
    /** Kills recorded on the stack (capped at 999). */
    public static final String NBT_KILLS = "fuelingimagination:sword_kills";

    public PoweredSwordItem() {
        super(Tiers.STONE, 3, -2.4F, new Properties().stacksTo(1));
    }

    // ---------------------------------------------------------------- no durability / no enchanting

    @Override public boolean isDamageable(ItemStack stack) { return false; }
    @Override public boolean canBeDepleted() { return false; }
    @Override public boolean isEnchantable(ItemStack stack) { return false; }
    @Override public int getEnchantmentValue() { return 0; }
    @Override public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) { return false; }

    // ---------------------------------------------------------------- material / accessory helpers

    private static boolean has(ItemStack stack, String id) {
        return UpgradeData.hasComponent(stack, id);
    }

    private static UpgradeConfig.MaterialStats materialStats(ItemStack stack) {
        String id = UpgradeData.readMaterial(stack);
        if (id.isEmpty()) return null;
        ItemStack mat = UpgradeData.stackOf(id);
        if (mat.isEmpty()) return null;
        for (UpgradeConfig.Slot slot : UpgradeConfig.slotsFor(stack)) {
            if ("material".equals(slot.type())) {
                UpgradeConfig.MaterialStats stats = slot.statsFor(mat);
                if (stats != null) return stats;
            }
        }
        return null;
    }

    private static boolean playerHasAir(Player player) {
        return !BacktankUtil.getAllWithAir(player).isEmpty();
    }

    // ---------------------------------------------------------------- attributes

    /** The material fully determines the blade stats; without one the sword keeps its stone base. */
    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> map = ArrayListMultimap.create();
        if (slot == EquipmentSlot.MAINHAND) {
            UpgradeConfig.MaterialStats material = materialStats(stack);
            boolean dry = stack.hasTag() && stack.getTag().getBoolean(NBT_DRY);
            // The material's numbers are deltas on top of the stone base, not replacements.
            double damage = 3.0D + (material != null ? material.damage() : 0.0D);
            double speed;
            if (material != null) speed = -2.4D + (dry ? material.drySpeed() : material.gasSpeed());
            else speed = dry ? -3.2D : -2.4D;
            map.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID,
                    "Weapon modifier", damage, AttributeModifier.Operation.ADDITION));
            map.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID,
                    "Weapon modifier", speed, AttributeModifier.Operation.ADDITION));
        }
        return map;
    }

    // ---------------------------------------------------------------- combat / breaking

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean drained = false;
        if (attacker instanceof Player player) {
            drained = BacktankUtil.canAbsorbDamage(player, USES_PER_TANK);
        }
        stack.getOrCreateTag().putBoolean(NBT_DRY, !drained);
        super.hurtEnemy(stack, target, attacker);

        // The saw turns a full-charge swing into a panel-damage sweep across everything nearby.
        if (attacker instanceof Player player && has(stack, "create:mechanical_saw")
                && playerHasAir(player) && player.getAttackStrengthScale(0.5F) >= 0.9F) {
            double panel = 1.0D + (materialStats(stack) != null ? materialStats(stack).damage() : 3.0D);
            Vec3 look = player.getLookAngle();
            Vec3 eye = player.getEyePosition();
            for (LivingEntity other : player.level().getEntitiesOfClass(LivingEntity.class,
                    target.getBoundingBox().inflate(1.0D, 0.25D, 1.0D), e -> e != player && e != target && e.isAlive())) {
                if (other.distanceToSqr(player) > 9.0D) continue;
                Vec3 to = other.position().subtract(eye).normalize();
                if (look.dot(to) < Math.cos(Math.toRadians(60))) continue;
                other.invulnerableTime = 0;
                other.hurt(player.damageSources().playerAttack(player), (float) panel);
            }
        }
        return true;
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, net.minecraft.core.BlockPos pos,
                             LivingEntity entity) {
        if (entity instanceof Player player) {
            boolean drained = BacktankUtil.canAbsorbDamage(player, USES_PER_TANK);
            stack.getOrCreateTag().putBoolean(NBT_DRY, !drained);
        }
        return super.mineBlock(stack, level, state, pos, entity);
    }

    // ---------------------------------------------------------------- drill accessory

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (has(stack, "create:mechanical_drill")) return 12.0F; // gold speed
        return super.getDestroySpeed(stack, state);
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return has(stack, "create:mechanical_drill") || super.isCorrectToolForDrops(stack, state);
    }

    // ---------------------------------------------------------------- saw accessory

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return has(stack, "create:mechanical_saw") ? UseAnim.BOW : UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return has(stack, "create:mechanical_saw") ? 72000 : 0;
    }

    private static boolean onCooldown(ItemStack stack, Level level, String id, int seconds) {
        return level.getGameTime() - stack.getOrCreateTag().getLong("fuelingimagination:cd_" + id) < seconds * 20L;
    }

    private static void startCooldown(ItemStack stack, Level level, String id) {
        stack.getOrCreateTag().putLong("fuelingimagination:cd_" + id, level.getGameTime());
    }

    /** Returns the living entity the player is looking at within {@code reach} blocks, or null. */
    private static LivingEntity raycastEntity(Player player, double reach) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(reach));
        AABB box = player.getBoundingBox().expandTowards(player.getLookAngle().scale(reach)).inflate(1.0D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player.level(), player, eye, end, box,
                e -> e != player && e.isAlive() && !e.isSpectator());
        return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (has(stack, "create:mechanical_saw")) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        boolean hasActive = has(stack, "create:encased_fan") || has(stack, "create:rope_pulley")
                || has(stack, "create:package_frogport") || has(stack, "create:mechanical_plough");
        if (!hasActive) return super.use(level, player, hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (tryBlower(level, player, stack)
                || tryRope(level, player, stack)
                || tryFrogport(level, player, stack)
                || tryPlow(level, player, stack)) {
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    /** Encased fan: fling nearby entities back with a puff; 50 air, 5s cooldown. */
    private boolean tryBlower(Level level, Player player, ItemStack stack) {
        if (!has(stack, "create:encased_fan")) return false;
        if (onCooldown(stack, level, "blower", 5)) return true;
        if (!BacktankUtil.canAbsorbDamage(player, AIR_50)) return true;
        startCooldown(stack, level, "blower");
        boolean blaze = has(stack, "create:blaze_burner");
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(4.0D), e -> e != player && e.isAlive())) {
            Vec3 away = e.position().subtract(player.position());
            if (away.lengthSqr() < 1.0E-4D) away = player.getLookAngle();
            away = away.normalize();
            e.push(away.x * 1.6D, 0.5D, away.z * 1.6D);
            if (blaze) e.setSecondsOnFire(5);
        }
        for (int i = 0; i < 12; i++) {
            double ox = player.getRandom().nextDouble() * 2.0D - 1.0D;
            double oz = player.getRandom().nextDouble() * 2.0D - 1.0D;
            level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, player.getX() + ox, player.getY() + 1.0D,
                    player.getZ() + oz, ox * 0.2D, 0.06D, oz * 0.2D);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 0.8F, 1.4F);
        return true;
    }

    /** Rope pulley: leash the mob in the crosshair to the player; 10 air, 5s cooldown. */
    private boolean tryRope(Level level, Player player, ItemStack stack) {
        if (!has(stack, "create:rope_pulley")) return false;
        if (onCooldown(stack, level, "rope", 5)) return true;
        if (!BacktankUtil.canAbsorbDamage(player, AIR_10)) return true;
        startCooldown(stack, level, "rope");
        LivingEntity target = raycastEntity(player, 24.0D);
        if (target instanceof Mob mob) {
            mob.setLeashedTo(player, true);
            mob.getPersistentData().putBoolean("fuelingimagination:rope_leashed", true);
            level.playSound(null, player.blockPosition(), SoundEvents.FISHING_BOBBER_THROW,
                    SoundSource.PLAYERS, 0.8F, 1.2F);
        }
        return true;
    }

    /** Package frogport: pack a vanilla mob into a package; 10 air, 5s cooldown. */
    private boolean tryFrogport(Level level, Player player, ItemStack stack) {
        if (!has(stack, "create:package_frogport")) return false;
        if (onCooldown(stack, level, "frogport", 5)) return true;
        if (!BacktankUtil.canAbsorbDamage(player, AIR_10)) return true;
        startCooldown(stack, level, "frogport");
        LivingEntity target = raycastEntity(player, 8.0D);
        if (target instanceof Mob mob) {
            ResourceLocation typeKey = EntityType.getKey(mob.getType());
            if (typeKey == null || !"minecraft".equals(typeKey.getNamespace())) return true;
            if (mob.getMaxHealth() > 50.0D) return true;
            String name = mob.getName().getString();
            CompoundTag entityTag = new CompoundTag();
            mob.saveWithoutId(entityTag);
            mob.discard();
            Item pkgItem = BuiltInRegistries.ITEM.get(
                    com.simibubi.create.content.logistics.box.PackageStyles.STYLES.get(0).getItemId());
            if (pkgItem != null && pkgItem != net.minecraft.world.item.Items.AIR) {
                ItemStack pkg = new ItemStack(pkgItem);
                pkg.getOrCreateTag().put("fuelingimagination:entity", entityTag);
                pkg.getOrCreateTag().putString("fuelingimagination:entity_type", typeKey.toString());
                pkg.getOrCreateTag().putString("fuelingimagination:entity_name", name);
                if (!player.addItem(pkg)) player.drop(pkg, false);
                level.playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP,
                        SoundSource.PLAYERS, 0.6F, 1.4F);
            }
        }
        return true;
    }

    /** Mechanical plough: till a 5x5x1 patch of dirt into farmland; 10 air, no cooldown. */
    private boolean tryPlow(Level level, Player player, ItemStack stack) {
        if (!has(stack, "create:mechanical_plough")) return false;
        if (!BacktankUtil.canAbsorbDamage(player, AIR_10)) return true;
        HitResult hit = player.pick(5.0D, 1.0F, false);
        if (hit.getType() != HitResult.Type.BLOCK) return true;
        BlockPos center = ((BlockHitResult) hit).getBlockPos();
        boolean tilled = false;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos pos = center.offset(dx, 0, dz);
                BlockState state = level.getBlockState(pos);
                if (!level.getBlockState(pos.above()).isAir()) continue;
                if (TILLABLE.contains(state.getBlock())) {
                    level.setBlock(pos, Blocks.FARMLAND.defaultBlockState(), 3);
                    tilled = true;
                }
            }
        }
        if (tilled) {
            level.playSound(null, center, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return true;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingTicks) {
        if (!(entity instanceof Player player)) return;
        if (!has(stack, "create:mechanical_saw")) return;
        // Consume one air per second while sawing.
        if (player.tickCount % 20 == 0) BacktankUtil.canAbsorbDamage(player, USES_PER_TANK);
        if (player.tickCount % 5 != 0) return;
        if (!playerHasAir(player)) return;

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(3.0D), t -> t != player && t.isAlive())) {
            if (target.distanceToSqr(player) > 9.0D) continue;
            Vec3 to = target.position().subtract(eye).normalize();
            if (look.dot(to) < Math.cos(Math.toRadians(60))) continue;
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().generic(), 2.0F); // saw hits do not knock back
            if (has(stack, "create:blaze_burner")) target.setSecondsOnFire(5);
            if (target.isDeadOrDying()) com.fuelingimagination.event.PoweredSwordEvents.countKill(stack, target);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 4));
        }
    }

    // ---------------------------------------------------------------- client display

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return BacktankUtil.isBarVisible(stack, USES_PER_TANK);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return BacktankUtil.getBarWidth(stack, USES_PER_TANK);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return BacktankUtil.getBarColor(stack, USES_PER_TANK);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        int kills = stack.hasTag() ? stack.getTag().getInt(NBT_KILLS) : 0;
        tooltip.add(Component.translatable("tooltip.fuelingimagination.powered_sword.kills",
                String.format("%03d", Math.min(999, kills))).withStyle(ChatFormatting.AQUA));

        String material = UpgradeData.readMaterial(stack);
        if (!material.isEmpty()) {
            ItemStack mat = UpgradeData.stackOf(material);
            if (!mat.isEmpty()) {
                tooltip.add(Component.translatable("tooltip.fuelingimagination.powered_sword.material",
                        mat.getHoverName()).withStyle(ChatFormatting.GRAY));
            }
        }
        for (String accessory : UpgradeData.readComponents(stack)) {
            ItemStack acc = UpgradeData.stackOf(accessory);
            if (!acc.isEmpty()) {
                tooltip.add(Component.translatable("tooltip.fuelingimagination.powered_sword.accessory",
                        acc.getHoverName()).withStyle(ChatFormatting.GRAY));
            }
        }
        if (stack.hasTag() && stack.getTag().getBoolean(NBT_DRY)) {
            tooltip.add(Component.translatable("tooltip.fuelingimagination.powered_sword.dry")
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
