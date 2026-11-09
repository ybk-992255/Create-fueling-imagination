package com.fuelingimagination.block;

import com.fuelingimagination.block.entity.ProjectorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.fml.DistExecutor;

import java.util.Locale;

/**
 * The projector: a two-block-tall, 1x1 footprint, four-way facable block with no collision.
 *
 * <p>One block serves both presentation modes, switched from the editor: {@link Mode#HOLOGRAM}
 * billboards the caption so it always faces the viewer, {@link Mode#SHADOW} pins it to a fixed
 * plane aligned with {@link #FACING}.
 */
public class ProjectorBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final EnumProperty<Mode> MODE = EnumProperty.create("mode", Mode.class);
    /** The clickable / selection volume: the full 1x1x2 footprint. */
    private static final VoxelShape SHAPE = Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 2.0D, 1.0D);

    /** Presentation modes the projector can switch between. */
    public enum Mode implements StringRepresentable {
        HOLOGRAM,
        SHADOW;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public ProjectorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(ACTIVE, true)
                .setValue(MODE, Mode.HOLOGRAM));
    }

    public static boolean isHologram(BlockState state) {
        return state.getValue(MODE) == Mode.HOLOGRAM;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        builder.add(ACTIVE);
        builder.add(MODE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(ACTIVE, true)
                .setValue(MODE, Mode.HOLOGRAM);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ProjectorBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        // Shift opens the caption editor, a plain right click toggles the projection.
        BlockEntity be = level.getBlockEntity(pos);
        if (player.isShiftKeyDown()) {
            if (level.isClientSide && be instanceof ProjectorBlockEntity projector) {
                DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                        () -> () -> com.fuelingimagination.client.ClientScreenHooks.openProjector(projector));
            }
        } else if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(ACTIVE, !state.getValue(ACTIVE)), 3);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
