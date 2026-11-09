package com.fuelingimagination.entity;

import com.fuelingimagination.item.ScrapCrateItem;
import com.fuelingimagination.registry.ModEntities;
import com.fuelingimagination.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * 拾荒者: a nitwit-shaped villager that never takes a job and barters for food.
 *
 * <p>Right-click with food, up to sixteen at a time, one crate per item. He admires the offer
 * for six seconds, then throws one crate per second. Once the batch is done he cools down for
 * ninety seconds. No trading screen is ever opened.
 */
public class Scavenger extends Villager {
    private static final int ADMIRE_TICKS = 120;
    private static final int THROW_INTERVAL = 20;
    private static final int MAX_BATCH = 16;
    private static final int COOLDOWN_TICKS = 90 * 20;

    private int admiringTicks;
    private int pendingCrates;
    private int throwDelay;
    private long cooldownUntil;

    public Scavenger(EntityType<? extends Scavenger> type, Level level) {
        super(type, level);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType reason, @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data, tag);
        // No profession, no workstation, no trade screen: a nitwit that happens to barter.
        setVillagerData(getVillagerData().setType(VillagerType.PLAINS)
                .setProfession(VillagerProfession.NONE).setLevel(1));
        return result;
    }

    /** Food that the scavenger accepts. Poisonous potato and pufferfish are refused. */
    public static boolean isBarterFood(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.is(Items.POISONOUS_POTATO) || stack.is(Items.PUFFERFISH)) return false;
        return stack.getFoodProperties(null) != null;
    }

    public boolean canBarter() {
        return admiringTicks <= 0 && pendingCrates <= 0 && level().getGameTime() >= cooldownUntil;
    }

    /** Remaining cooldown in ticks, exposed for mods such as Jade to display. */
    public long getBarterCooldownTicks() {
        return Math.max(0L, cooldownUntil - level().getGameTime());
    }

    public int getPendingCrates() {
        return pendingCrates;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (isBarterFood(held) && canBarter()) {
            if (!level().isClientSide) {
                int count = Math.min(MAX_BATCH, held.getCount());
                if (!player.getAbilities().instabuild) held.shrink(count);
                pendingCrates = count;
                admiringTicks = ADMIRE_TICKS;
                throwDelay = THROW_INTERVAL;
                setDeltaMovement(Vec3.ZERO);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (admiringTicks > 0) {
            admiringTicks--;
            if (admiringTicks == 0) throwDelay = THROW_INTERVAL;
        } else if (pendingCrates > 0) {
            if (--throwDelay <= 0) {
                throwCrate();
                if (--pendingCrates == 0) cooldownUntil = level().getGameTime() + COOLDOWN_TICKS;
                throwDelay = THROW_INTERVAL;
            }
        }
    }

    private void throwCrate() {
        RandomSource random = level().random;
        ItemStack crate = ScrapCrateItem.create(random);
        ItemEntity drop = new ItemEntity(level(), getX(), getY() + 1.4D, getZ(), crate);
        Vec3 look = getLookAngle();
        drop.setDeltaMovement(look.x * 0.25D, 0.3D, look.z * 0.25D);
        drop.setPickUpDelay(20);
        level().addFreshEntity(drop);
    }

    @Override
    public void die(net.minecraft.world.damagesource.DamageSource source) {
        if (!level().isClientSide) {
            // One crate regardless of how the batch was going.
            spawnAtLocation(ScrapCrateItem.create(level().random), 0.5F);
        }
        super.die(source);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("AdmiringTicks", admiringTicks);
        tag.putInt("PendingCrates", pendingCrates);
        tag.putLong("BarterCooldown", cooldownUntil);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        admiringTicks = tag.getInt("AdmiringTicks");
        pendingCrates = tag.getInt("PendingCrates");
        cooldownUntil = tag.getLong("BarterCooldown");
    }
}
