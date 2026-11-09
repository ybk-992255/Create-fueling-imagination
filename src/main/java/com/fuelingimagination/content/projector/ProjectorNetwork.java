package com.fuelingimagination.content.projector;

import com.fuelingimagination.FuelingImagination;
import com.fuelingimagination.block.ProjectorBlock;
import com.fuelingimagination.block.entity.ProjectorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

/** Client -> server channel for saving the projector caption, its framing and its mode. */
public final class ProjectorNetwork {
    private static final String VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(FuelingImagination.MOD_ID, "projector"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    private ProjectorNetwork() { }

    public static void register() {
        CHANNEL.messageBuilder(SetText.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SetText::encode)
                .decoder(SetText::decode)
                .consumerMainThread(SetText::handle)
                .add();
    }

    public static void send(BlockPos pos, String text, float scale, float distance,
                            float hue, float opacity, boolean hologram) {
        CHANNEL.sendToServer(new SetText(pos, text, scale, distance, hue, opacity, hologram));
    }

    record SetText(BlockPos pos, String text, float scale, float distance,
                   float hue, float opacity, boolean hologram) {
        void encode(FriendlyByteBuf buffer) {
            buffer.writeBlockPos(pos);
            buffer.writeUtf(text, ProjectorBlockEntity.MAX_LENGTH);
            buffer.writeFloat(scale);
            buffer.writeFloat(distance);
            buffer.writeFloat(hue);
            buffer.writeFloat(opacity);
            buffer.writeBoolean(hologram);
        }

        static SetText decode(FriendlyByteBuf buffer) {
            return new SetText(buffer.readBlockPos(), buffer.readUtf(ProjectorBlockEntity.MAX_LENGTH),
                    buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                    buffer.readBoolean());
        }

        void handle(Supplier<net.minecraftforge.network.NetworkEvent.Context> context) {
            ServerPlayer player = context.get().getSender();
            if (player != null && player.level() instanceof ServerLevel level
                    && level.isLoaded(pos) && level.getBlockEntity(pos) instanceof ProjectorBlockEntity projector
                    && player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) < 64.0D) {
                projector.apply(text, scale, distance, hue, opacity);
                BlockState state = level.getBlockState(pos);
                if (state.getBlock() instanceof ProjectorBlock) {
                    ProjectorBlock.Mode mode = hologram ? ProjectorBlock.Mode.HOLOGRAM : ProjectorBlock.Mode.SHADOW;
                    if (state.getValue(ProjectorBlock.MODE) != mode) {
                        level.setBlock(pos, state.setValue(ProjectorBlock.MODE, mode), 3);
                    }
                }
            }
            context.get().setPacketHandled(true);
        }
    }
}