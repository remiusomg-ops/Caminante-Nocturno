package com.renzo.caminantenocturno;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.StairBlock;
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

    private static final int WALL_RADIUS = 52;
    private static final int BODY_THICKNESS = 3;       // cuerpo central: 3 bloques
    private static final int TOTAL_WIDTH = 5;          // base y coronacion: 5 bloques
    private static final int BODY_HEIGHT = 6;          // hasta camino de ronda
    private static final int BUTTRESS_SPACING = 6;
    private static final int SEGMENT_LENGTH = 6;
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

        int nwBase = cornerBase(level, minX + 3, minZ + 3);
        int neBase = cornerBase(level, maxX - 3, minZ + 3);
        int swBase = cornerBase(level, minX + 3, maxZ - 3);
        int seBase = cornerBase(level, maxX - 3, maxZ - 3);

        buildHorizontal(level, c, minX, maxX, minZ, +1);
        buildHorizontal(level, c, minX, maxX, maxZ, -1);
        buildVertical(level, c, minZ, maxZ, minX, +1);
        buildVertical(level, c, minZ, maxZ, maxX, -1);

        buildCornerTower(level, minX + 3, minZ + 3, nwBase);
        buildCornerTower(level, maxX - 3, minZ + 3, neBase);
        buildCornerTower(level, minX + 3, maxZ - 3, swBase);
        buildCornerTower(level, maxX - 3, maxZ - 3, seBase);
    }

    private static void buildHorizontal(ServerLevel level, BlockPos c, int minX, int maxX, int outerZ, int inward) {
        Integer previousBase = null;

        for (int start = minX; start <= maxX; start += SEGMENT_LENGTH) {
            int end = Math.min(start + SEGMENT_LENGTH - 1, maxX);
            int sampled = sampleHorizontalSegment(level, start, end, outerZ, inward);

            // Evita saltos absurdos entre piezas: cada tramo solo puede variar 1 bloque.
            int baseY = previousBase == null
                    ? sampled
                    : Math.max(previousBase - 1, Math.min(previousBase + 1, sampled));
            previousBase = baseY;

            for (int x = start; x <= end; x++) {
                boolean passage = Math.abs(x - c.getX()) <= PASSAGE_HALF_WIDTH;
                buildHorizontalSection(level, x, outerZ, inward, baseY, passage, x - minX);
            }
        }
    }

    private static void buildVertical(ServerLevel level, BlockPos c, int minZ, int maxZ, int outerX, int inward) {
        Integer previousBase = null;

        for (int start = minZ; start <= maxZ; start += SEGMENT_LENGTH) {
            int end = Math.min(start + SEGMENT_LENGTH - 1, maxZ);
            int sampled = sampleVerticalSegment(level, start, end, outerX, inward);

            int baseY = previousBase == null
                    ? sampled
                    : Math.max(previousBase - 1, Math.min(previousBase + 1, sampled));
            previousBase = baseY;

            for (int z = start; z <= end; z++) {
                boolean passage = Math.abs(z - c.getZ()) <= PASSAGE_HALF_WIDTH;
                buildVerticalSection(level, outerX, z, inward, baseY, passage, z - minZ);
            }
        }
    }

    private static int sampleHorizontalSegment(ServerLevel level, int start, int end, int outerZ, int inward) {
        int[] heights = new int[end - start + 1];
        int i = 0;
        for (int x = start; x <= end; x++) {
            heights[i++] = naturalGround(level, x, outerZ + inward);
        }
        java.util.Arrays.sort(heights);
        return heights[heights.length / 2];
    }

    private static int sampleVerticalSegment(ServerLevel level, int start, int end, int outerX, int inward) {
        int[] heights = new int[end - start + 1];
        int i = 0;
        for (int z = start; z <= end; z++) {
            heights[i++] = naturalGround(level, outerX + inward, z);
        }
        java.util.Arrays.sort(heights);
        return heights[heights.length / 2];
    }

    // Seccion exacta de la referencia vista de frente:
    // y+1 = base de 5 bloques
    // y+2..y+5 = cuerpo central de 3
    // y+6 = coronacion de 5
    // y+7 = almenas solo en los dos bordes, alternadas
    private static void buildHorizontalSection(ServerLevel level, int x, int outerZ, int inward,
                                               int baseY, boolean passage, int index) {
        if (passage) {
            clearHorizontalPassage(level, x, outerZ, inward, baseY);
            return;
        }

        int bodyStart = outerZ;
        int outerFoot = outerZ - inward;
        int innerFoot = outerZ + inward * 3;

        // Base ancha de cinco.
        for (int d = -1; d <= 3; d++) {
            placeFoundationAndBlock(level, x, outerZ + inward * d, baseY + 1);
        }

        // Cuerpo central de tres bloques de grosor.
        for (int dy = 2; dy <= 5; dy++) {
            for (int d = 0; d <= 2; d++) {
                placeSolid(level, x, bodyStart + inward * d, baseY + dy, index, dy);
            }
        }

        // Cornisa/camino de ronda: vuelve a cinco bloques.
        for (int d = -1; d <= 3; d++) {
            level.setBlock(new BlockPos(x, baseY + BODY_HEIGHT, outerZ + inward * d),
                    Blocks.STONE_BRICKS.defaultBlockState(), 3);
        }

        // Piso plano superior interior de 3 bloques.
        for (int d = 0; d <= 2; d++) {
            level.setBlock(new BlockPos(x, baseY + BODY_HEIGHT + 1, outerZ + inward * d),
                    Blocks.STONE_BRICK_SLAB.defaultBlockState(), 3);
        }

        // Parapetos/almenas en ambos bordes, 2 llenos / 1 hueco.
        if (Math.floorMod(index, 3) != 2) {
            level.setBlock(new BlockPos(x, baseY + BODY_HEIGHT + 1, outerFoot),
                    Blocks.STONE_BRICKS.defaultBlockState(), 3);
            level.setBlock(new BlockPos(x, baseY + BODY_HEIGHT + 1, innerFoot),
                    Blocks.STONE_BRICKS.defaultBlockState(), 3);
        }

        if (Math.floorMod(index, BUTTRESS_SPACING) == 0) {
            buildHorizontalButtress(level, x, outerZ, inward, baseY);
        }
    }

    private static void buildVerticalSection(ServerLevel level, int outerX, int z, int inward,
                                             int baseY, boolean passage, int index) {
        if (passage) {
            clearVerticalPassage(level, outerX, z, inward, baseY);
            return;
        }

        int outerFoot = outerX - inward;
        int innerFoot = outerX + inward * 3;

        for (int d = -1; d <= 3; d++) {
            placeFoundationAndBlock(level, outerX + inward * d, z, baseY + 1);
        }

        for (int dy = 2; dy <= 5; dy++) {
            for (int d = 0; d <= 2; d++) {
                placeSolid(level, outerX + inward * d, z, baseY + dy, index, dy);
            }
        }

        for (int d = -1; d <= 3; d++) {
            level.setBlock(new BlockPos(outerX + inward * d, baseY + BODY_HEIGHT, z),
                    Blocks.STONE_BRICKS.defaultBlockState(), 3);
        }

        for (int d = 0; d <= 2; d++) {
            level.setBlock(new BlockPos(outerX + inward * d, baseY + BODY_HEIGHT + 1, z),
                    Blocks.STONE_BRICK_SLAB.defaultBlockState(), 3);
        }

        if (Math.floorMod(index, 3) != 2) {
            level.setBlock(new BlockPos(outerFoot, baseY + BODY_HEIGHT + 1, z),
                    Blocks.STONE_BRICKS.defaultBlockState(), 3);
            level.setBlock(new BlockPos(innerFoot, baseY + BODY_HEIGHT + 1, z),
                    Blocks.STONE_BRICKS.defaultBlockState(), 3);
        }

        if (Math.floorMod(index, BUTTRESS_SPACING) == 0) {
            buildVerticalButtress(level, outerX, z, inward, baseY);
        }
    }

    private static void placeFoundationAndBlock(ServerLevel level, int x, int z, int targetY) {
        int ground = naturalGround(level, x, z);
        for (int y = ground; y <= targetY; y++) {
            level.setBlock(new BlockPos(x, y, z), Blocks.STONE_BRICKS.defaultBlockState(), 3);
        }
    }

    private static void placeSolid(ServerLevel level, int x, int z, int y, int index, int dy) {
        BlockState block = (dy == 4 && Math.floorMod(index, 13) == 0)
                ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState()
                : Blocks.STONE_BRICKS.defaultBlockState();
        level.setBlock(new BlockPos(x, y, z), block, 3);
    }

    // Contrafuerte pequeño de la foto: escalonado, no columna alta.
    private static void buildHorizontalButtress(ServerLevel level, int x, int outerZ, int inward, int baseY) {
        int z1 = outerZ - inward * 2;
        int z2 = outerZ - inward * 3;

        placeFoundationAndBlock(level, x, z1, baseY + 2);
        placeFoundationAndBlock(level, x, z2, baseY + 1);

        BlockState stair = Blocks.STONE_BRICK_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, inward > 0 ? Direction.NORTH : Direction.SOUTH);
        level.setBlock(new BlockPos(x, baseY + 2, z2), stair, 3);
    }

    private static void buildVerticalButtress(ServerLevel level, int outerX, int z, int inward, int baseY) {
        int x1 = outerX - inward * 2;
        int x2 = outerX - inward * 3;

        placeFoundationAndBlock(level, x1, z, baseY + 2);
        placeFoundationAndBlock(level, x2, z, baseY + 1);

        BlockState stair = Blocks.STONE_BRICK_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, inward > 0 ? Direction.WEST : Direction.EAST);
        level.setBlock(new BlockPos(x2, baseY + 2, z), stair, 3);
    }

    private static void clearHorizontalPassage(ServerLevel level, int x, int outerZ, int inward, int baseY) {
        for (int d = -3; d <= 4; d++) {
            int z = outerZ + inward * d;
            for (int y = baseY + 1; y <= baseY + BODY_HEIGHT + 3; y++) {
                level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    private static void clearVerticalPassage(ServerLevel level, int outerX, int z, int inward, int baseY) {
        for (int d = -3; d <= 4; d++) {
            int x = outerX + inward * d;
            for (int y = baseY + 1; y <= baseY + BODY_HEIGHT + 3; y++) {
                level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    private static int cornerBase(ServerLevel level, int cx, int cz) {
        int base = level.getMinBuildHeight();
        for (int x = cx - 3; x <= cx + 3; x++) {
            for (int z = cz - 3; z <= cz + 3; z++) {
                base = Math.max(base, naturalGround(level, x, z));
            }
        }
        return base;
    }

    // Torre mas contenida para encajar con la nueva muralla.
    private static void buildCornerTower(ServerLevel level, int cx, int cz, int baseY) {
        final int radius = 3;
        final int height = 8;

        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int z = cz - radius; z <= cz + radius; z++) {
                int ground = naturalGround(level, x, z);
                boolean edge = x == cx - radius || x == cx + radius
                        || z == cz - radius || z == cz + radius;

                for (int y = ground; y <= baseY; y++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.STONE_BRICKS.defaultBlockState(), 3);
                }

                if (edge) {
                    for (int dy = 1; dy <= height; dy++) {
                        level.setBlock(new BlockPos(x, baseY + dy, z),
                                Blocks.STONE_BRICKS.defaultBlockState(), 3);
                    }
                    int edgeIndex = Math.abs(x - cx) + Math.abs(z - cz);
                    if ((edgeIndex & 1) == 0) {
                        level.setBlock(new BlockPos(x, baseY + height + 1, z),
                                Blocks.STONE_BRICKS.defaultBlockState(), 3);
                    }
                } else {
                    level.setBlock(new BlockPos(x, baseY, z), Blocks.STONE_BRICKS.defaultBlockState(), 3);
                    for (int dy = 1; dy < height; dy++) {
                        level.setBlock(new BlockPos(x, baseY + dy, z), Blocks.AIR.defaultBlockState(), 3);
                    }
                    level.setBlock(new BlockPos(x, baseY + height, z),
                            Blocks.STONE_BRICK_SLAB.defaultBlockState(), 3);
                }
            }
        }

        int ladderX = cx - radius + 1;
        for (int dy = 1; dy < height; dy++) {
            BlockState ladder = Blocks.LADDER.defaultBlockState()
                    .setValue(LadderBlock.FACING, Direction.EAST);
            level.setBlock(new BlockPos(ladderX, baseY + dy, cz), ladder, 3);
        }

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

    private static int naturalGround(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
    }
}
