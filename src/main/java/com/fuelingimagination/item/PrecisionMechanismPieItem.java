package com.fuelingimagination.item;

import com.fuelingimagination.block.entity.PrecisionMechanismPieBlockEntity;
import com.simibubi.create.AllFluids;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class PrecisionMechanismPieItem extends EdibleBlockItem {
    public static final String INFUSION_KEY = "gourmetfood_infusion";
    public static final int COOLDOWN_TICKS = 600;
    public static final int EFFECT_TICKS = 60;

    public PrecisionMechanismPieItem(Block block, Properties properties) {
        super(block, properties, 30);
    }

    public static List<MobEffectInstance> getInfusedEffects(ItemStack stack) {
        List<MobEffectInstance> effects = new ArrayList<>();
        if (stack.hasTag()) {
            for (Tag entry : stack.getTag().getList(INFUSION_KEY, Tag.TAG_COMPOUND)) {
                MobEffectInstance effect = MobEffectInstance.load((CompoundTag) entry);
                if (effect != null) {
                    effects.add(new MobEffectInstance(effect.getEffect(), EFFECT_TICKS, effect.getAmplifier()));
                }
            }
        }
        return effects;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        }
        return super.use(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof Player player && player.getCooldowns().isOnCooldown(this)) {
            return stack;
        }
        // Keep vanilla eating behavior and the intrinsic food effect without consuming the real item.
        super.finishUsingItem(stack.copy(), level, entity);
        if (!level.isClientSide) {
            for (MobEffectInstance effect : getInfusedEffects(stack)) {
                if (effect.getEffect().isInstantenous()) {
                    effect.getEffect().applyInstantenousEffect(entity, entity, entity, effect.getAmplifier(), 1.0D);
                } else {
                    entity.addEffect(effect);
                }
            }
            if (entity instanceof Player player) {
                player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
            }
        }
        return stack;
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        CompoundTag itemTag = context.getItemInHand().hasTag()
                ? context.getItemInHand().getTag().copy() : new CompoundTag();
        // Old saves may contain serving counts; version 1.0.1 makes all pies reusable.
        itemTag.remove("remaining_servings");
        InteractionResult result = super.place(context);
        if (result.consumesAction() && !context.getLevel().isClientSide
                && context.getLevel().getBlockEntity(context.getClickedPos()) instanceof PrecisionMechanismPieBlockEntity pie) {
            pie.setItemTag(itemTag);
        }
        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.fuelingimagination.reusable").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.fuelingimagination.cooldown", 30).withStyle(ChatFormatting.GRAY));
        List<MobEffectInstance> effects = getInfusedEffects(stack);
        if (!effects.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.fuelingimagination.infusion").withStyle(ChatFormatting.DARK_PURPLE));
            PotionUtils.addPotionTooltip(effects, tooltip, 1.0F);
        }
        tooltip.add(Component.translatable("tooltip.fuelingimagination.infusion_help").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new InfusionHandler(stack);
    }

    private static final class InfusionHandler implements IFluidHandlerItem, ICapabilityProvider {
        private final ItemStack stack;
        private final LazyOptional<IFluidHandlerItem> capability = LazyOptional.of(() -> this);

        private InfusionHandler(ItemStack stack) {
            this.stack = stack;
        }

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
            return cap == ForgeCapabilities.FLUID_HANDLER_ITEM ? capability.cast() : LazyOptional.empty();
        }

        @Override public ItemStack getContainer() { return stack; }
        @Override public int getTanks() { return 1; }
        @Override public FluidStack getFluidInTank(int tank) { return FluidStack.EMPTY; }
        @Override public int getTankCapacity(int tank) { return 1000; }
        @Override public FluidStack drain(FluidStack resource, FluidAction action) { return FluidStack.EMPTY; }
        @Override public FluidStack drain(int maxDrain, FluidAction action) { return FluidStack.EMPTY; }

        @Override
        public boolean isFluidValid(int tank, FluidStack resource) {
            return resource.getFluid().isSame(AllFluids.POTION.get()) || isMilk(resource);
        }

        private boolean isMilk(FluidStack resource) {
            return resource.getFluid().is(FluidTags.create(new ResourceLocation("forge", "milk")));
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (stack.getCount() != 1 || resource.isEmpty()) {
                return 0;
            }
            if (isMilk(resource)) {
                if (resource.getAmount() < 1000 || !stack.hasTag() || !stack.getTag().contains(INFUSION_KEY)) {
                    return 0;
                }
                if (action.execute()) {
                    stack.getTag().remove(INFUSION_KEY);
                }
                return 1000;
            }
            if (!resource.getFluid().isSame(AllFluids.POTION.get()) || resource.getAmount() < 250 || !resource.hasTag()) {
                return 0;
            }
            ListTag replacement = new ListTag();
            for (MobEffectInstance effect : PotionUtils.getAllEffects(resource.getTag())) {
                replacement.add(new MobEffectInstance(effect.getEffect(), EFFECT_TICKS, effect.getAmplifier())
                        .save(new CompoundTag()));
            }
            // Identical attributes are already installed: avoid repeatedly consuming fluid under a spout.
            if (replacement.isEmpty() || (stack.hasTag() && replacement.equals(stack.getTag().getList(INFUSION_KEY, Tag.TAG_COMPOUND)))) {
                return 0;
            }
            if (action.execute()) {
                stack.getOrCreateTag().put(INFUSION_KEY, replacement);
            }
            return 250;
        }
    }
}
