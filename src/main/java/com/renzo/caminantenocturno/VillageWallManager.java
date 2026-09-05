package com.renzo.caminantenocturno;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;

@Mod.EventBusSubscriber(modid=CaminanteNocturnoMod.MODID,bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class VillageWallManager {
    private static final int CHECK_INTERVAL = 100;
    private static final int SEARCH_RADIUS_CHUNKS = 16;

    // Medidas basadas en las cuatro fotos de referencia.
    private static final int WALL_RADIUS = 54;
    private static final int WALL_THICKNESS = 4;
    private static final int WALL_HEIGHT = 5;
    private static final int MODULE = 7;
    private static final int PASSAGE_HALF_WIDTH = 2;

    private static final Set<Long> SESSION_SEEN = new HashSet<>();

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        for (ServerLevel level : event.getServer().getAllLevels()) {
            if (!level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) continue;
            if (level.getGameTime() % CHECK_INTERVAL != 0) continue;

            VillageWallSavedData data = VillageWallSavedData.get(level);

            for (ServerPlayer player : level.players()) {
                for (int ox = -8; ox <= 8; ox += 8) {
                    for (int oz = -8; oz <= 8; oz += 8) {
                        BlockPos origin = player.blockPosition().offset(ox * 16, 0, oz * 16);
                        BlockPos center = level.findNearestMapStructure(
                                StructureTags.VILLAGE, origin, SEARCH_RADIUS_CHUNKS, false);

                        if (center == null) continue;
                        long key = key(center);
                        if (!SESSION_SEEN.add(key) || data.isWalled(key)) continue;

                        buildWall(level, center);
                        data.markWalled(key);
                    }
                }
            }
        }
    }

    private static long key(BlockPos p) {
        return (((long)p.getX()) & 0xffffffffL) << 32 | (((long)p.getZ()) & 0xffffffffL);
    }

    private static void buildWall(ServerLevel level, BlockPos c) {
        int minX = c.getX() - WALL_RADIUS;
        int maxX = c.getX() + WALL_RADIUS;
        int minZ = c.getZ() - WALL_RADIUS;
        int maxZ = c.getZ() + WALL_RADIUS;

        buildHorizontal(level, c, minX, maxX, minZ, +1);
        buildHorizontal(level, c, minX, maxX, maxZ, -1);
        buildVertical(level, c, minZ, maxZ, minX, +1);
        buildVertical(level, c, minZ, maxZ, maxX, -1);

        buildCornerTower(level, minX + 3, minZ + 3);
        buildCornerTower(level, maxX - 3, minZ + 3);
        buildCornerTower(level, minX + 3, maxZ - 3);
        buildCornerTower(level, maxX - 3, maxZ - 3);
    }

    private static void buildHorizontal(ServerLevel level, BlockPos c, int minX, int maxX, int outerZ, int inward) {
        for (int start = minX; start <= maxX; start += MODULE) {
            int end = Math.min(start + MODULE - 1, maxX);
            int baseY = segmentBaseHorizontal(level, start, end, outerZ, inward);

            for (int x = start; x <= end; x++) {
                boolean passage = Math.abs(x - c.getX()) <= PASSAGE_HALF_WIDTH;
                int idx = x - minX;
                buildHorizontalSlice(level, x, outerZ, inward, baseY, passage, idx);
            }
        }
    }

    private static void buildVertical(ServerLevel level, BlockPos c, int minZ, int maxZ, int outerX, int inward) {
        for (int start = minZ; start <= maxZ; start += MODULE) {
            int end = Math.min(start + MODULE - 1, maxZ);
            int baseY = segmentBaseVertical(level, start, end, outerX, inward);

            for (int z = start; z <= end; z++) {
                boolean passage = Math.abs(z - c.getZ()) <= PASSAGE_HALF_WIDTH;
                int idx = z - minZ;
                buildVerticalSlice(level, outerX, z, inward, baseY, passage, idx);
            }
        }
    }

    private static int segmentBaseHorizontal(ServerLevel level, int start, int end, int outerZ, int inward) {
        int base = level.getMinBuildHeight();
        for (int x = start; x <= end; x++)
            for (int t = 0; t < WALL_THICKNESS; t++)
                base = Math.max(base, groundY(level, x, outerZ + inward * t));
        return base;
    }

    private static int segmentBaseVertical(ServerLevel level, int start, int end, int outerX, int inward) {
        int base = level.getMinBuildHeight();
        for (int z = start; z <= end; z++)
            for (int t = 0; t < WALL_THICKNESS; t++)
                base = Math.max(base, groundY(level, outerX + inward * t, z));
        return base;
    }

    private static void buildHorizontalSlice(ServerLevel level, int x, int outerZ, int inward,
                                             int baseY, boolean passage, int index) {
        if (passage) {
            clearHorizontalPassage(level, x, outerZ, inward, baseY);
            return;
        }

        for (int t = 0; t < WALL_THICKNESS; t++) {
            int z = outerZ + inward * t;
            buildWallCell(level, x, z, baseY, t == 0, t == WALL_THICKNESS - 1, index);
        }

        if (Math.floorMod(index, MODULE) == 0) {
            buildHorizontalButtress(level, x, outerZ, inward, baseY);
        }

        if (Math.floorMod(index, 12) == 4) {
            int z = outerZ + inward * (WALL_THICKNESS - 1);
            placeLantern(level, new BlockPos(x, baseY + WALL_HEIGHT + 2, z));
        }
    }

    private static void buildVerticalSlice(ServerLevel level, int outerX, int z, int inward,
                                           int baseY, boolean passage, int index) {
        if (passage) {
            clearVerticalPassage(level, outerX, z, inward, baseY);
            return;
        }

        for (int t = 0; t < WALL_THICKNESS; t++) {
            int x = outerX + inward * t;
            buildWallCell(level, x, z, baseY, t == 0, t == WALL_THICKNESS - 1, index);
        }

        if (Math.floorMod(index, MODULE) == 0) {
            buildVerticalButtress(level, outerX, z, inward, baseY);
        }

        if (Math.floorMod(index, 12) == 4) {
            int x = outerX + inward * (WALL_THICKNESS - 1);
            placeLantern(level, new BlockPos(x, baseY + WALL_HEIGHT + 2, z));
        }
    }

    private static void buildWallCell(ServerLevel level, int x, int z, int baseY,
                                      boolean outerEdge, boolean innerEdge, int index) {
        int ground = groundY(level, x, z);

        for (int y = ground; y <= baseY; y++) {
            level.setBlock(new BlockPos(x, y, z), Blocks.STONE_BRICKS.defaultBlockState(), 3);
        }

        for (int dy = 1; dy <= WALL_HEIGHT; dy++) {
            BlockState block = (dy == 2 || dy == 4)
                    ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState()
                    : Blocks.STONE_BRICKS.defaultBlockState();
            level.setBlock(new BlockPos(x, baseY + dy, z), block, 3);
        }

        // Camino de ronda ancho y plano.
        level.setBlock(new BlockPos(x, baseY + WALL_HEIGHT + 1, z),
                Blocks.STONE_BRICK_SLAB.defaultBlockState(), 3);

        // Almenas gruesas de dos bloques de largo, como en la referencia.
        if ((outerEdge || innerEdge) && Math.floorMod(index, 4) < 2) {
            level.setBlock(new BlockPos(x, baseY + WALL_HEIGHT + 2, z),
                    Blocks.STONE_BRICKS.defaultBlockState(), 3);
        }
    }

    private static void buildHorizontalButtress(ServerLevel level, int x, int outerZ, int inward, int baseY) {
        for (int depth = 1; depth <= 3; depth++) {
            int z = outerZ - inward * depth;
            int height = 5 - depth;
            int ground = groundY(level, x, z);
            for (int y = ground; y <= baseY; y++)
                level.setBlock(new BlockPos(x, y, z), Blocks.STONE_BRICKS.defaultBlockState(), 3);
            for (int dy = 1; dy <= height; dy++)
                level.setBlock(new BlockPos(x, baseY + dy, z), Blocks.STONE_BRICKS.defaultBlockState(), 3);
        }
    }

    private static void buildVerticalButtress(ServerLevel level, int outerX, int z, int inward, int baseY) {
        for (int depth = 1; depth <= 3; depth++) {
            int x = outerX - inward * depth;
            int height = 5 - depth;
            int ground = groundY(level, x, z);
            for (int y = ground; y <= baseY; y++)
                level.setBlock(new BlockPos(x, y, z), Blocks.STONE_BRICKS.defaultBlockState(), 3);
            for (int dy = 1; dy <= height; dy++)
                level.setBlock(new BlockPos(x, baseY + dy, z), Blocks.STONE_BRICKS.defaultBlockState(), 3);
        }
    }

    private static void clearHorizontalPassage(ServerLevel level, int x, int outerZ, int inward, int baseY) {
        for (int t = -1; t <= WALL_THICKNESS; t++) {
            int z = outerZ + inward * t;
            for (int y = baseY + 1; y <= baseY + WALL_HEIGHT + 3; y++)
                level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
        }
    }

    private static void clearVerticalPassage(ServerLevel level, int outerX, int z, int inward, int baseY) {
        for (int t = -1; t <= WALL_THICKNESS; t++) {
            int x = outerX + inward * t;
            for (int y = baseY + 1; y <= baseY + WALL_HEIGHT + 3; y++)
                level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
        }
    }

    private static void buildCornerTower(ServerLevel level, int cx, int cz) {
        final int radius = 4;
        final int height = 9;

        int baseY = level.getMinBuildHeight();
        for (int x = cx - radius; x <= cx + radius; x++)
            for (int z = cz - radius; z <= cz + radius; z++)
                baseY = Math.max(baseY, groundY(level, x, z));

        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int z = cz - radius; z <= cz + radius; z++) {
                int ground = groundY(level, x, z);
                boolean edge = x == cx - radius || x == cx + radius
                        || z == cz - radius || z == cz + radius;

                for (int y = ground; y <= baseY; y++)
                    level.setBlock(new BlockPos(x, y, z), Blocks.STONE_BRICKS.defaultBlockState(), 3);

                if (edge) {
                    for (int dy = 1; dy <= height; dy++) {
                        BlockState block = (dy == 3 || dy == 6)
                                ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState()
                                : Blocks.STONE_BRICKS.defaultBlockState();
                        level.setBlock(new BlockPos(x, baseY + dy, z), block, 3);
                    }

                    int edgeIndex = Math.abs(x - cx) + Math.abs(z - cz);
                    if ((edgeIndex & 1) == 0) {
                        level.setBlock(new BlockPos(x, baseY + height + 1, z),
                                Blocks.STONE_BRICKS.defaultBlockState(), 3);
                    }
                } else {
                    level.setBlock(new BlockPos(x, baseY, z), Blocks.STONE_BRICKS.defaultBlockState(), 3);
                    for (int dy = 1; dy < height; dy++)
                        level.setBlock(new BlockPos(x, baseY + dy, z), Blocks.AIR.defaultBlockState(), 3);

                    level.setBlock(new BlockPos(x, baseY + height, z),
                            Blocks.STONE_BRICK_SLAB.defaultBlockState(), 3);
                }
            }
        }

        // Acceso interior a la parte superior mediante escalera de mano.
        int ladderX = cx - radius + 1;
        int ladderZ = cz;
        for (int dy = 1; dy < height; dy++) {
            BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.EAST);
            level.setBlock(new BlockPos(ladderX, baseY + dy, ladderZ), ladder, 3);
        }

        // Faroles en la azotea, como en la referencia.
        placeLantern(level, new BlockPos(cx - 2, baseY + height + 1, cz - 2));
        placeLantern(level, new BlockPos(cx + 2, baseY + height + 1, cz - 2));
        placeLantern(level, new BlockPos(cx - 2, baseY + height + 1, cz + 2));
        placeLantern(level, new BlockPos(cx + 2, baseY + height + 1, cz + 2));
    }

    private static void placeLantern(ServerLevel level, BlockPos pos) {
        if (level.getBlockState(pos).isAir()) {
            level.setBlock(pos, Blocks.LANTERN.defaultBlockState(), 3);
        }
    }

    private static int groundY(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
    }
}
