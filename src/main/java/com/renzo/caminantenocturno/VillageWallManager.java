package com.renzo.caminantenocturno;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;

@Mod.EventBusSubscriber(modid=CaminanteNocturnoMod.MODID,bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class VillageWallManager {
    private static final int CHECK_INTERVAL = 200; // 10 segundos
    private static final int SEARCH_RADIUS_CHUNKS = 8;
    private static final int WALL_RADIUS = 58;
    private static final int WALL_HEIGHT = 5;
    private static final int WALL_THICKNESS = 2;
    private static final int PASSAGE_HALF_WIDTH = 2; // 5 bloques abiertos
    private static final Set<Long> WALLED = new HashSet<>();

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        for (ServerLevel level : event.getServer().getAllLevels()) {
            if (!level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) continue;
            if (level.getGameTime() % CHECK_INTERVAL != 0) continue;

            for (ServerPlayer player : level.players()) {
                BlockPos center = level.findNearestMapStructure(
                        StructureTags.VILLAGE,
                        player.blockPosition(),
                        SEARCH_RADIUS_CHUNKS,
                        false
                );

                if (center == null) continue;

                long key = (((long) center.getX()) & 0xffffffffL) << 32
                        | (((long) center.getZ()) & 0xffffffffL);

                if (WALLED.add(key)) {
                    buildWall(level, center);
                }
            }
        }
    }

    private static void buildWall(ServerLevel level, BlockPos c) {
        int minX = c.getX() - WALL_RADIUS;
        int maxX = c.getX() + WALL_RADIUS;
        int minZ = c.getZ() - WALL_RADIUS;
        int maxZ = c.getZ() + WALL_RADIUS;

        // Norte y sur: dos bloques de grosor hacia el interior.
        for (int x = minX; x <= maxX; x++) {
            boolean passage = Math.abs(x - c.getX()) <= PASSAGE_HALF_WIDTH;
            for (int t = 0; t < WALL_THICKNESS; t++) {
                buildWallColumn(level, c, x, minZ + t, passage);
                buildWallColumn(level, c, x, maxZ - t, passage);
            }
        }

        // Oeste y este: dos bloques de grosor hacia el interior.
        for (int z = minZ + WALL_THICKNESS; z <= maxZ - WALL_THICKNESS; z++) {
            boolean passage = Math.abs(z - c.getZ()) <= PASSAGE_HALF_WIDTH;
            for (int t = 0; t < WALL_THICKNESS; t++) {
                buildWallColumn(level, c, minX + t, z, passage);
                buildWallColumn(level, c, maxX - t, z, passage);
            }
        }

        // Torres de esquina macizas, como pequeños bastiones.
        buildCornerTower(level, minX + 1, minZ + 1);
        buildCornerTower(level, maxX - 1, minZ + 1);
        buildCornerTower(level, minX + 1, maxZ - 1);
        buildCornerTower(level, maxX - 1, maxZ - 1);
    }

    private static void ensureChunk(ServerLevel level, int x, int z) {
        // Fuerza a cargar/generar el chunk necesario para que no queden huecos.
        level.getChunk(x >> 4, z >> 4);
    }

    private static int groundY(ServerLevel level, int x, int z) {
        ensureChunk(level, x, z);
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
    }

    private static void buildWallColumn(ServerLevel level, BlockPos c, int x, int z, boolean passage) {
        int y = groundY(level, x, z);
        if (y <= level.getMinBuildHeight()) return;

        if (passage) {
            // Paso completamente libre: sin puertas, vallas ni rejas.
            for (int dy = 1; dy <= WALL_HEIGHT + 3; dy++) {
                level.setBlock(new BlockPos(x, y + dy, z), Blocks.AIR.defaultBlockState(), 3);
            }
            return;
        }

        // Pequeña cimentación para evitar que se vea flotando en desniveles.
        level.setBlock(new BlockPos(x, y, z), Blocks.STONE_BRICKS.defaultBlockState(), 3);
        if (y - 1 > level.getMinBuildHeight()
                && level.getBlockState(new BlockPos(x, y - 1, z)).isAir()) {
            level.setBlock(new BlockPos(x, y - 1, z), Blocks.COBBLESTONE.defaultBlockState(), 3);
        }

        // Cuerpo: zócalo de ladrillo de piedra y centro de adoquín.
        level.setBlock(new BlockPos(x, y + 1, z), Blocks.STONE_BRICKS.defaultBlockState(), 3);
        for (int dy = 2; dy <= WALL_HEIGHT - 1; dy++) {
            level.setBlock(new BlockPos(x, y + dy, z), Blocks.COBBLESTONE.defaultBlockState(), 3);
        }
        level.setBlock(new BlockPos(x, y + WALL_HEIGHT, z), Blocks.STONE_BRICKS.defaultBlockState(), 3);

        // Almenas alternadas.
        if (((x + z) & 1) == 0) {
            level.setBlock(
                    new BlockPos(x, y + WALL_HEIGHT + 1, z),
                    Blocks.STONE_BRICK_WALL.defaultBlockState(),
                    3
            );
        }
    }

    private static void buildCornerTower(ServerLevel level, int cx, int cz) {
        final int radius = 2;
        final int towerHeight = 7;

        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int z = cz - radius; z <= cz + radius; z++) {
                int y = groundY(level, x, z);
                if (y <= level.getMinBuildHeight()) continue;

                boolean edge = x == cx - radius || x == cx + radius
                        || z == cz - radius || z == cz + radius;

                if (!edge) {
                    // Interior transitable.
                    for (int dy = 1; dy <= towerHeight; dy++) {
                        level.setBlock(new BlockPos(x, y + dy, z), Blocks.AIR.defaultBlockState(), 3);
                    }
                    continue;
                }

                level.setBlock(new BlockPos(x, y, z), Blocks.STONE_BRICKS.defaultBlockState(), 3);
                for (int dy = 1; dy <= towerHeight; dy++) {
                    boolean trim = dy == 1 || dy == towerHeight;
                    level.setBlock(
                            new BlockPos(x, y + dy, z),
                            (trim ? Blocks.STONE_BRICKS : Blocks.COBBLESTONE).defaultBlockState(),
                            3
                    );
                }

                if (((x + z) & 1) == 0) {
                    level.setBlock(
                            new BlockPos(x, y + towerHeight + 1, z),
                            Blocks.STONE_BRICK_WALL.defaultBlockState(),
                            3
                    );
                }
            }
        }
    }
}
