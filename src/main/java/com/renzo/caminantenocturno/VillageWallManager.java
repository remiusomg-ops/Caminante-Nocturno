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
    private static final int CHECK_INTERVAL = 200;
    private static final int SEARCH_RADIUS_CHUNKS = 6;
    private static final int WALL_RADIUS = 52;
    private static final int WALL_HEIGHT = 5;
    private static final int PASSAGE_HALF_WIDTH = 2; // hueco de 5 bloques, sin puerta
    private static final Set<Long> WALLED = new HashSet<>();

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        for (ServerLevel level : event.getServer().getAllLevels()) {
            if (!level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) continue;
            if (level.getGameTime() % CHECK_INTERVAL != 0) continue;
            for (ServerPlayer player : level.players()) {
                BlockPos center=level.findNearestMapStructure(StructureTags.VILLAGE,player.blockPosition(),SEARCH_RADIUS_CHUNKS,false);
                if(center==null) continue;
                long key=(((long)center.getX())&0xffffffffL)<<32|(((long)center.getZ())&0xffffffffL);
                if(WALLED.add(key)) buildWall(level,center);
            }
        }
    }

    private static void buildWall(ServerLevel level,BlockPos c){
        int minX=c.getX()-WALL_RADIUS,maxX=c.getX()+WALL_RADIUS;
        int minZ=c.getZ()-WALL_RADIUS,maxZ=c.getZ()+WALL_RADIUS;
        for(int x=minX;x<=maxX;x++){
            buildColumn(level,c,x,minZ,Math.abs(x-c.getX())<=PASSAGE_HALF_WIDTH);
            buildColumn(level,c,x,maxZ,Math.abs(x-c.getX())<=PASSAGE_HALF_WIDTH);
        }
        for(int z=minZ+1;z<maxZ;z++){
            buildColumn(level,c,minX,z,Math.abs(z-c.getZ())<=PASSAGE_HALF_WIDTH);
            buildColumn(level,c,maxX,z,Math.abs(z-c.getZ())<=PASSAGE_HALF_WIDTH);
        }
    }

    private static void buildColumn(ServerLevel level,BlockPos c,int x,int z,boolean passage){
        BlockPos sample=new BlockPos(x,level.getMinBuildHeight(),z);
        if(!level.hasChunkAt(sample)) return;
        int y=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z)-1;
        if(y<=level.getMinBuildHeight()) return;

        // Acceso completamente abierto: no hay puerta, reja ni fence gate.
        if(passage){
            for(int dy=1;dy<=WALL_HEIGHT+2;dy++) level.setBlock(new BlockPos(x,y+dy,z),Blocks.AIR.defaultBlockState(),3);
            return;
        }

        // Muralla más maciza como la referencia: base de piedra, cuerpo de adoquín y remate almenado.
        level.setBlock(new BlockPos(x,y+1,z),Blocks.STONE_BRICKS.defaultBlockState(),3);
        for(int dy=2;dy<=WALL_HEIGHT;dy++)
            level.setBlock(new BlockPos(x,y+dy,z),Blocks.COBBLESTONE.defaultBlockState(),3);

        // Almenas alternadas.
        if(((x+z)&1)==0)
            level.setBlock(new BlockPos(x,y+WALL_HEIGHT+1,z),Blocks.STONE_BRICK_WALL.defaultBlockState(),3);

        // Luz discreta hacia el perímetro.
        if((Math.abs(x-c.getX())+Math.abs(z-c.getZ()))%14==0)
            level.setBlock(new BlockPos(x,y+WALL_HEIGHT+1,z),Blocks.TORCH.defaultBlockState(),3);
    }
}
