package com.fuelingimagination.event;

import com.fuelingimagination.FuelingImagination;
import com.fuelingimagination.entity.Scavenger;
import com.fuelingimagination.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Natural spawning, modelled on the wandering trader but more frequent.
 *
 * <p>Every {@value #INTERVAL} ticks the overworld rolls {@value #CHANCE} for one scavenger near a
 * random player. At most {@value #MAX_ALIVE} may exist across all dimensions at once.
 */
@Mod.EventBusSubscriber(modid = FuelingImagination.MOD_ID)
public final class ScavengerSpawner {
    private static final int INTERVAL = 12000;
    private static final float CHANCE = 0.5F;
    private static final int MAX_ALIVE = 3;
    private static final int MIN_DISTANCE = 24;
    private static final int MAX_DISTANCE = 48;

    private static int timer = INTERVAL;

    private ScavengerSpawner() { }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (--timer > 0) return;
        timer = INTERVAL;

        MinecraftServer server = event.getServer();
        ServerLevel level = server.overworld();
        if (level.players().isEmpty()) return;
        if (level.random.nextFloat() >= CHANCE) return;
        if (countAlive(server) >= MAX_ALIVE) return;

        List<ServerPlayer> players = level.players();
        trySpawn(level, players.get(level.random.nextInt(players.size())));
    }

    /** Attempts one spawn near the player, ignoring the timer and the chance roll. */
    public static boolean trySpawn(ServerLevel level, ServerPlayer player) {
        if (countAlive(level.getServer()) >= MAX_ALIVE) return false;
        BlockPos pos = findSpawn(level, player.blockPosition(), level.random);
        if (pos == null) return false;

        Scavenger scavenger = ModEntities.SCAVENGER.get().create(level);
        if (scavenger == null) return false;
        scavenger.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                level.random.nextFloat() * 360.0F, 0.0F);
        scavenger.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
        return level.addFreshEntity(scavenger);
    }

    private static int countAlive(MinecraftServer server) {
        int count = 0;
        for (ServerLevel level : server.getAllLevels()) {
            for (var ignored : level.getEntities(ModEntities.SCAVENGER.get(), entity -> true)) count++;
        }
        return count;
    }

    /** Finds a surface position on solid ground with room to stand. */
    private static BlockPos findSpawn(ServerLevel level, BlockPos origin, RandomSource random) {
        for (int attempt = 0; attempt < 20; attempt++) {
            int distance = MIN_DISTANCE + random.nextInt(MAX_DISTANCE - MIN_DISTANCE + 1);
            double angle = random.nextDouble() * Math.PI * 2.0D;
            int x = origin.getX() + (int) Math.round(Math.cos(angle) * distance);
            int z = origin.getZ() + (int) Math.round(Math.sin(angle) * distance);
            if (!level.hasChunkAt(new BlockPos(x, origin.getY(), z))) continue;

            BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
            if (surface.getY() <= level.getMinBuildHeight() + 1) continue;
            if (!level.getBlockState(surface.below()).isSolid()) continue;
            if (!level.isEmptyBlock(surface) || !level.isEmptyBlock(surface.above())) continue;
            if (level.getBlockState(surface).getFluidState().isSource()) continue;
            return surface;
        }
        return null;
    }
}
