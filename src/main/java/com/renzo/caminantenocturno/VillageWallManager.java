package com.renzo.caminantenocturno;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;

@Mod.EventBusSubscriber(modid=CaminanteNocturnoMod.MODID,bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class VillageWallManager {
    private static final int CHECK_INTERVAL = 200; // 10 s
    private static final int SEARCH_RADIUS_CHUNKS = 6;
    private static final int WALL_RADIUS = 52;
    private static final int WALL_HEIGHT = 4;
    private static final int GATE_HALF_WIDTH = 2;
    private static final Set<Long> WALLED = new HashSet<>();

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        for (ServerLevel level : event.getServer().getAllLevels()) {
            if (!level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) continue;
            if (level.getGameTime() % CHECK_INTERVAL != 0) continue;

            for (ServerPlayer player : level.players()) {
                Pair<BlockPos, Holder<Structure>> found =
                        level.findNearestMapStructure(StructureTags.VILLAGE, player.blockPosition(), SEARCH_RADIUS_CHUNKS, false);

                if (found == null) continue;
                BlockPos center = found.getFirst();
                long key = (((long)center.getX()) & 0xffffffffL) << 32 | (((long)center.getZ()) & 0xffffffffL);
                if (!WALLED.add(key)) continue;

                buildWall(level, center);
            }
        }
    }

    private static void buildWall(ServerLevel level, BlockPos center) {
        int minX = center.getX() - WALL_RADIUS;
        int maxX = center.getX() + WALL_RADIUS;
        int minZ = center.getZ() - WALL_RADIUS;
        int maxZ = center.getZ() + WALL_RADIUS;

        for (int x = minX; x <= maxX; x++) {
            buildColumn(level, center, x, minZ, isNorthSouthGate(center, x));
            buildColumn(level, center, x, maxZ, isNorthSouthGate(center, x));
        }

        for (int z = minZ + 1; z < maxZ; z++) {
            buildColumn(level, center, minX, z, isEastWestGate(center, z));
            buildColumn(level, center, maxX, z, isEastWestGate(center, z));
        }
    }

    private static boolean isNorthSouthGate(BlockPos center, int x) {
        return Math.abs(x - center.getX()) <= GATE_HALF_WIDTH;
    }

    private static boolean isEastWestGate(BlockPos center, int z) {
        return Math.abs(z - center.getZ()) <= GATE_HALF_WIDTH;
    }

    private static void buildColumn(ServerLevel level, BlockPos center, int x, int z, boolean gate) {
        BlockPos sample = new BlockPos(x, level.getMinBuildHeight(), z);
        if (!level.hasChunkAt(sample)) return;

        int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
        if (groundY <= level.getMinBuildHeight()) return;

        if (gate) {
            buildGate(level, x, groundY, z);
            return;
        }

        for (int dy = 1; dy <= WALL_HEIGHT; dy++) {
            BlockPos p = new BlockPos(x, groundY + dy, z);
            level.setBlock(p, Blocks.COBBLESTONE.defaultBlockState(), 3);
        }

        if (((x + z) & 1) == 0) {
            BlockPos cap = new BlockPos(x, groundY + WALL_HEIGHT + 1, z);
            level.setBlock(cap, Blocks.COBBLESTONE_WALL.defaultBlockState(), 3);
        }

        BlockPos torch = new BlockPos(x, groundY + WALL_HEIGHT + 1, z);
        if (((Math.abs(x-center.getX()) + Math.abs(z-center.getZ())) % 12) == 0) {
            level.setBlock(torch, Blocks.TORCH.defaultBlockState(), 3);
        }
    }

    private static void buildGate(ServerLevel level, int x, int groundY, int z) {
        BlockPos gatePos = new BlockPos(x, groundY + 1, z);
        level.setBlock(gatePos, Blocks.OAK_FENCE_GATE.defaultBlockState(), 3);

        for (int dy = 2; dy <= WALL_HEIGHT; dy++) {
            BlockPos air = new BlockPos(x, groundY + dy, z);
            level.setBlock(air, Blocks.AIR.defaultBlockState(), 3);
        }
    }
}
