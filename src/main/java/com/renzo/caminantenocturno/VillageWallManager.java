package com.renzo.caminantenocturno;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

/**
 * Village fortification system adapted from Astryxion's Witchery-Villages
 * (Forge 1.20.1), GPL-3.0. Modified for Caminante Nocturno on 2026-09-05:
 * guards/keeps/towers/config were removed and only the terrain-following
 * village perimeter logic was retained.
 */
@Mod.EventBusSubscriber(modid = CaminanteNocturnoMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class VillageWallManager {
    private static final int DELAY_TICKS = 100;
    private static final int DEDUP_RADIUS = 80;
    private static final int BLOCKS_PER_TICK = 600;
    private static final int MAX_SPAN = 384;
    private static final int SOLID_THRESHOLD = 9;
    private static final int SHALLOW_WATER_PROBE = 4;

    private static final List<PendingVillage> PENDING = new ArrayList<>();
    private static final LinkedHashMap<PlacementKey, PendingBlock> BLOCK_QUEUE = new LinkedHashMap<>();
    private static ActiveVillage ACTIVE_VILLAGE = null;

    private VillageWallManager() {}

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getChunk() instanceof LevelChunk chunk)) return;

        var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (StructureStart start : chunk.getAllStarts().values()) {
            if (!start.isValid()) continue;
            boolean village = registry.getResourceKey(start.getStructure())
                    .flatMap(registry::getHolder)
                    .map(holder -> holder.is(StructureTags.VILLAGE))
                    .orElse(false);
            if (!village) continue;

            BlockPos origin = start.getPieces().isEmpty()
                    ? start.getBoundingBox().getCenter()
                    : start.getPieces().get(0).getBoundingBox().getCenter();
            schedule(level, origin);
            return;
        }
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        MinecraftServer server = event.getServer();
        tickPlacements();

        // Solo se confirma una aldea cuando la ultima pieza de su muralla ya fue colocada.
        if (ACTIVE_VILLAGE != null && BLOCK_QUEUE.isEmpty()) {
            VillageWallSavedData.get(ACTIVE_VILLAGE.level).markWalled(ACTIVE_VILLAGE.center.asLong());
            ACTIVE_VILLAGE = null;
        }

        long now = server.getTickCount();
        if (!BLOCK_QUEUE.isEmpty() || ACTIVE_VILLAGE != null) return;

        Iterator<PendingVillage> it = PENDING.iterator();
        while (it.hasNext()) {
            PendingVillage pending = it.next();
            if (now < pending.executeTick) continue;
            it.remove();

            if (VillageWallSavedData.get(pending.level).isNearWalled(pending.center, DEDUP_RADIUS)) continue;
            if (generateForVillage(pending.level, pending.center)) {
                if (BLOCK_QUEUE.isEmpty()) {
                    VillageWallSavedData.get(pending.level).markWalled(pending.center.asLong());
                } else {
                    ACTIVE_VILLAGE = new ActiveVillage(pending.level, pending.center);
                }
            }
            break; // one village start per tick
        }
    }

    private static void schedule(ServerLevel level, BlockPos center) {
        if (VillageWallSavedData.get(level).isNearWalled(center, DEDUP_RADIUS)) return;
        long r2 = (long)DEDUP_RADIUS * DEDUP_RADIUS;
        if (ACTIVE_VILLAGE != null && ACTIVE_VILLAGE.level == level
                && ACTIVE_VILLAGE.center.distSqr(center) <= r2) return;
        for (PendingVillage p : PENDING) {
            if (p.level == level && p.center.distSqr(center) <= r2) return;
        }
        PENDING.add(new PendingVillage(level, center.immutable(), level.getServer().getTickCount() + DELAY_TICKS));
    }

    private static boolean generateForVillage(ServerLevel level, BlockPos center) {
        StructureStart start = findVillageStart(level, center);
        if (!start.isValid() || start.getPieces().isEmpty()) return false;

        List<Bounds> streets = new ArrayList<>();
        List<Bounds> buildings = new ArrayList<>();
        long ySum = 0;
        int yCount = 0;

        for (StructurePiece piece : start.getPieces()) {
            BoundingBox box = piece.getBoundingBox();
            ySum += box.minY();
            yCount++;
            if (isStreetPiece(piece)) streets.add(new Bounds(box, 20, 7));
            else buildings.add(new Bounds(box, 10, 7));
        }

        // Usar calles + edificios evita que el contorno pase por una casa o granja periférica.
        List<Bounds> bounds = new ArrayList<>();
        bounds.addAll(streets);
        bounds.addAll(buildings);
        if (bounds.isEmpty()) bounds = List.of(new Bounds(start.getBoundingBox(), 10, 7));

        int yCoord = yCount == 0 ? center.getY() : (int)(ySum / yCount);
        return buildPlan(level, bounds, center.getX(), yCoord, center.getZ());
    }

    private static StructureStart findVillageStart(ServerLevel level, BlockPos center) {
        StructureStart start = level.structureManager().getStructureWithPieceAt(center, StructureTags.VILLAGE);
        if (start.isValid()) return start;

        Optional<HolderSet.Named<Structure>> villages = level.registryAccess()
                .registryOrThrow(Registries.STRUCTURE).getTag(StructureTags.VILLAGE);
        if (villages.isEmpty()) return StructureStart.INVALID_START;

        for (Holder<Structure> holder : villages.get()) {
            start = level.structureManager().getStructureAt(center, holder.value());
            if (start.isValid()) return start;
        }
        return StructureStart.INVALID_START;
    }

    private static boolean isStreetPiece(StructurePiece piece) {
        String text = (piece instanceof PoolElementStructurePiece pool
                ? pool.getElement().toString() : piece.toString()).toLowerCase(Locale.ROOT);
        return text.contains("/streets/") || text.contains("/terminators/") || text.contains("street");
    }

    private static boolean buildPlan(ServerLevel level, List<Bounds> boundsList, int xCoord, int yCoord, int zCoord) {
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        for (Bounds b : boundsList) {
            minX = Math.min(minX, b.minX); minZ = Math.min(minZ, b.minZ);
            maxX = Math.max(maxX, b.maxX); maxZ = Math.max(maxZ, b.maxZ);
        }
        if (minX == Integer.MAX_VALUE) return false;

        if (maxX - minX > MAX_SPAN || maxZ - minZ > MAX_SPAN) {
            int half = MAX_SPAN / 2;
            minX = Math.max(minX, xCoord - half); maxX = Math.min(maxX, xCoord + half);
            minZ = Math.max(minZ, zCoord - half); maxZ = Math.min(maxZ, zCoord + half);
        }
        if (maxX - minX < 8 || maxZ - minZ < 8) return false;

        byte[][] grid = new byte[maxX - minX + 3][maxZ - minZ + 3];

        for (Bounds b : boundsList) {
            int wMid = (b.maxX - b.minX + 1) / 2 + b.minX - 1;
            int hMid = (b.maxZ - b.minZ + 1) / 2 + b.minZ - 1;
            for (int x = b.minX; x <= b.maxX; x++) {
                for (int z = b.minZ; z <= b.maxZ; z++) {
                    int gx = x - minX + 1, gz = z - minZ + 1;
                    if (gx <= 0 || gz <= 0 || gx >= grid.length - 1 || gz >= grid[gx].length - 1) continue;
                    if (!b.ew && (z == b.minZ || z == b.maxZ) && x >= wMid - 1 && x <= wMid + 1) grid[gx][gz] = 3;
                    else if (b.ew && (x == b.minX || x == b.maxX) && z >= hMid - 1 && z <= hMid + 1) grid[gx][gz] = 3;
                    else grid[gx][gz] = 2;
                }
            }
        }

        connectSmallGaps(grid);
        fillEnclosedHoles(grid);

        for (int x = 1; x < grid.length - 1; x++) {
            for (int z = 1; z < grid[x].length - 1; z++) {
                boolean surrounded = true;
                for (int dx = -1; dx <= 1 && surrounded; dx++)
                    for (int dz = -1; dz <= 1; dz++)
                        if ((dx != 0 || dz != 0) && grid[x + dx][z + dz] == 0) { surrounded = false; break; }
                if (surrounded) grid[x][z] = 1;
            }
        }

        int[][] foundations = new int[grid.length][grid[0].length];
        int[][] heights = new int[grid.length][grid[0].length];

        for (int x = 1; x < grid.length - 1; x++) {
            for (int z = 1; z < grid[x].length - 1; z++) {
                if (grid[x][z] < 2) continue;
                int wx = minX + x, wz = minZ + z;
                level.getChunk(wx >> 4, wz >> 4);
                int foundation = findFoundationY(level, wx, wz, yCoord);
                foundations[x][z] = foundation;
                heights[x][z] = foundation + 9;
            }
        }

        for (int pass = 0; pass < 6; pass++) {
            int[][] next = new int[heights.length][heights[0].length];
            for (int x = 0; x < heights.length; x++) System.arraycopy(heights[x], 0, next[x], 0, heights[x].length);
            for (int x = 1; x < grid.length - 1; x++) {
                for (int z = 1; z < grid[x].length - 1; z++) {
                    if (grid[x][z] < 2 || heights[x][z] == 0) continue;
                    int near = maxNeighborHeight(grid, heights, x, z);
                    if (near == Integer.MIN_VALUE) continue;
                    if (near > heights[x][z]) next[x][z] = near - 1;
                    else if (near < heights[x][z]) next[x][z] = near + 1;
                }
            }
            heights = next;
        }

        for (int x = 1; x < grid.length - 1; x++) {
            for (int z = 1; z < grid[x].length - 1; z++) {
                if (grid[x][z] < 2) continue;
                placeWallColumn(level, grid, foundations, heights, minX, minZ, x, z);
            }
        }
        return true;
    }

    private static void connectSmallGaps(byte[][] grid) {
        int range = 7;
        for (int x = 1; x < grid.length - range; x++) {
            for (int z = 1; z < grid[x].length - range; z++) {
                if (grid[x][z] != 2) continue;
                for (int p = 1; p < range; p++) {
                    if (grid[x + p][z] == 2 && grid[x + p - 1][z] == 0)
                        for (int q = p; q > 0; q--) grid[x + q][z] = 2;
                    if (grid[x][z + p] == 2 && grid[x][z + p - 1] == 0)
                        for (int q = p; q > 0; q--) grid[x][z + q] = 2;
                }
            }
        }
    }

    private static void fillEnclosedHoles(byte[][] grid) {
        int w = grid.length, d = grid[0].length;
        boolean[][] outside = new boolean[w][d];
        ArrayDeque<int[]> q = new ArrayDeque<>();
        for (int x = 0; x < w; x++) { markOutside(grid, outside, q, x, 0); markOutside(grid, outside, q, x, d - 1); }
        for (int z = 0; z < d; z++) { markOutside(grid, outside, q, 0, z); markOutside(grid, outside, q, w - 1, z); }
        while (!q.isEmpty()) {
            int[] p = q.removeFirst();
            markOutside(grid, outside, q, p[0] + 1, p[1]); markOutside(grid, outside, q, p[0] - 1, p[1]);
            markOutside(grid, outside, q, p[0], p[1] + 1); markOutside(grid, outside, q, p[0], p[1] - 1);
        }
        for (int x = 1; x < w - 1; x++) for (int z = 1; z < d - 1; z++)
            if (grid[x][z] == 0 && !outside[x][z]) grid[x][z] = 2;
    }

    private static void markOutside(byte[][] grid, boolean[][] outside, ArrayDeque<int[]> q, int x, int z) {
        if (x < 0 || z < 0 || x >= grid.length || z >= grid[x].length || grid[x][z] != 0 || outside[x][z]) return;
        outside[x][z] = true; q.add(new int[]{x,z});
    }

    private static void placeWallColumn(ServerLevel level, byte[][] grid, int[][] foundations, int[][] heights,
                                        int minX, int minZ, int k, int z) {
        int wx = minX + k, wz = minZ + z;
        int top = heights[k][z];
        int bottom = getWallBottomY(level, wx, wz, foundations[k][z]);
        if (top <= bottom) return;

        boolean n = grid[k][z-1] >= 2, s = grid[k][z+1] >= 2, e = grid[k+1][z] >= 2, w = grid[k-1][z] >= 2;
        boolean gate = grid[k][z] == 3;

        for (int y = top; y > bottom; y--) {
            if (gate && y <= bottom + 3) continue; // open passage, no wooden gate

            queue(level, wx, y, wz, Blocks.STONE_BRICKS.defaultBlockState());
            if (!n) queue(level, wx, y, wz-1, Blocks.STONE_BRICKS.defaultBlockState());
            if (!s) queue(level, wx, y, wz+1, Blocks.STONE_BRICKS.defaultBlockState());
            if (!e) queue(level, wx+1, y, wz, Blocks.STONE_BRICKS.defaultBlockState());
            if (!w) queue(level, wx-1, y, wz, Blocks.STONE_BRICKS.defaultBlockState());

            if (!n && !e) queue(level, wx+1, y, wz-1, Blocks.STONE_BRICKS.defaultBlockState());
            if (!n && !w) queue(level, wx-1, y, wz-1, Blocks.STONE_BRICKS.defaultBlockState());
            if (!s && !e) queue(level, wx+1, y, wz+1, Blocks.STONE_BRICKS.defaultBlockState());
            if (!s && !w) queue(level, wx-1, y, wz+1, Blocks.STONE_BRICKS.defaultBlockState());
        }

        // Witchery-style upper lip / crenellation.
        if (!n) {
            queue(level, wx, top+1, wz-2, stairState(Direction.EAST));
            queue(level, wx, top, wz-2, Blocks.STONE_BRICKS.defaultBlockState());
        }
        if (!s) {
            queue(level, wx, top+1, wz+2, stairState(Direction.EAST));
            queue(level, wx, top, wz+2, Blocks.STONE_BRICKS.defaultBlockState());
        }
        if (!e) {
            queue(level, wx+2, top+1, wz, stairState(Direction.SOUTH));
            queue(level, wx+2, top, wz, Blocks.STONE_BRICKS.defaultBlockState());
        }
        if (!w) {
            queue(level, wx-2, top+1, wz, stairState(Direction.SOUTH));
            queue(level, wx-2, top, wz, Blocks.STONE_BRICKS.defaultBlockState());
        }
    }

    private static BlockState stairState(Direction facing) {
        return Blocks.STONE_BRICK_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, facing)
                .setValue(StairBlock.HALF, Half.BOTTOM);
    }

    private static int getWallBottomY(Level level, int x, int z, int foundation) {
        int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
        int ocean = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
        return surface - ocean <= 1 ? foundation : ocean;
    }

    private static int findFoundationY(Level level, int x, int z, int yCoord) {
        int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
        int top = Math.min(surface + 2, yCoord + 16);
        int bottom = Math.max(level.getMinBuildHeight() + 1, Math.min(yCoord, surface) - 40);
        if (top < bottom) { top = surface; bottom = Math.max(level.getMinBuildHeight() + 1, surface - 16); }

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = top; y >= bottom; y--) {
            if (hasFluidAt(level, x, z, y, cursor)) {
                for (int probe = y; probe > y - SHALLOW_WATER_PROBE && probe >= bottom; probe--)
                    if (!hasFluidAt(level, x, z, probe, cursor) && countSolidAt(level, x, z, probe, cursor) >= SOLID_THRESHOLD) return probe;
                return y;
            }
            if (countSolidAt(level, x, z, y, cursor) >= SOLID_THRESHOLD) return y;
        }
        return Math.max(bottom, surface - 1);
    }

    private static int maxNeighborHeight(byte[][] grid, int[][] heights, int x, int z) {
        int max = Integer.MIN_VALUE;
        if (grid[x-1][z] >= 2 && heights[x-1][z] != 0) max = Math.max(max, heights[x-1][z]);
        if (grid[x+1][z] >= 2 && heights[x+1][z] != 0) max = Math.max(max, heights[x+1][z]);
        if (grid[x][z-1] >= 2 && heights[x][z-1] != 0) max = Math.max(max, heights[x][z-1]);
        if (grid[x][z+1] >= 2 && heights[x][z+1] != 0) max = Math.max(max, heights[x][z+1]);
        return max;
    }

    private static boolean hasFluidAt(Level level, int x, int z, int y, BlockPos.MutableBlockPos cursor) {
        for (int dx=x-1; dx<=x+1; dx++) for (int dz=z-1; dz<=z+1; dz++) {
            FluidState fluid = level.getFluidState(cursor.set(dx,y,dz));
            if (!fluid.isEmpty()) return true;
        }
        return false;
    }

    private static int countSolidAt(Level level, int x, int z, int y, BlockPos.MutableBlockPos cursor) {
        int count = 0;
        for (int dx=x-1; dx<=x+1; dx++) for (int dz=z-1; dz<=z+1; dz++) {
            BlockState state = level.getBlockState(cursor.set(dx,y,dz));
            if (!state.isAir() && !canReplaceForWall(state)) count++;
        }
        return count;
    }

    private static boolean canReplaceForWall(BlockState state) {
        return state.isAir() || state.canBeReplaced() || !state.getFluidState().isEmpty()
                || state.is(BlockTags.LEAVES) || state.is(BlockTags.REPLACEABLE_BY_TREES);
    }

    private static void queue(ServerLevel level, int x, int y, int z, BlockState state) {
        BlockPos pos = new BlockPos(x,y,z);
        BLOCK_QUEUE.put(new PlacementKey(level, pos.asLong()), new PendingBlock(level, pos, state));
    }

    private static void tickPlacements() {
        int placed = 0;
        Iterator<PendingBlock> it = BLOCK_QUEUE.values().iterator();
        while (it.hasNext() && placed < BLOCKS_PER_TICK) {
            PendingBlock p = it.next();
            p.level.getChunk(p.pos.getX() >> 4, p.pos.getZ() >> 4);
            BlockState existing = p.level.getBlockState(p.pos);
            if (!isProtected(existing)) p.level.setBlock(p.pos, p.state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            it.remove();
            placed++;
        }
    }

    private static boolean isProtected(BlockState state) {
        return state.is(BlockTags.PLANKS) || state.is(BlockTags.LOGS) || state.is(BlockTags.WOODEN_STAIRS)
                || state.is(BlockTags.WOODEN_SLABS) || state.is(BlockTags.DOORS) || state.is(BlockTags.BEDS)
                || state.is(BlockTags.FENCES) || state.is(BlockTags.TRAPDOORS)
                || state.is(BlockTags.WOODEN_PRESSURE_PLATES)
                || state.is(Blocks.GLASS) || state.is(Blocks.GLASS_PANE)
                || state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST) || state.is(Blocks.BARREL)
                || state.is(Blocks.BELL) || state.is(Blocks.FARMLAND)
                || state.is(Blocks.WHEAT) || state.is(Blocks.CARROTS)
                || state.is(Blocks.POTATOES) || state.is(Blocks.BEETROOTS)
                || state.is(Blocks.COMPOSTER) || state.is(Blocks.CRAFTING_TABLE)
                || state.is(Blocks.FURNACE) || state.is(Blocks.BLAST_FURNACE) || state.is(Blocks.SMOKER)
                || state.is(Blocks.LECTERN) || state.is(Blocks.GRINDSTONE)
                || state.is(Blocks.SMITHING_TABLE) || state.is(Blocks.STONECUTTER)
                || state.is(Blocks.LOOM) || state.is(Blocks.CARTOGRAPHY_TABLE)
                || state.is(Blocks.FLETCHING_TABLE) || state.is(Blocks.LADDER)
                || state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH)
                || state.is(Blocks.LANTERN) || state.is(Blocks.SOUL_LANTERN);
    }

    private static final class Bounds {
        final int minX,minZ,maxX,maxZ; final boolean ew;
        Bounds(BoundingBox box, int expansionX, int expansionZ) {
            ew = box.maxX() - box.minX() > box.maxZ() - box.minZ();
            if (ew) {
                minX = box.minX() - expansionZ; maxX = box.maxX() + expansionZ;
                minZ = box.minZ() - expansionX; maxZ = box.maxZ() + expansionX;
            } else {
                minX = box.minX() - expansionX; maxX = box.maxX() + expansionX;
                minZ = box.minZ() - expansionZ; maxZ = box.maxZ() + expansionZ;
            }
        }
    }

    private record PendingVillage(ServerLevel level, BlockPos center, long executeTick) {}
    private record ActiveVillage(ServerLevel level, BlockPos center) {}
    private record PlacementKey(ServerLevel level, long pos) {}
    private record PendingBlock(ServerLevel level, BlockPos pos, BlockState state) {}
}
