package com.renzo.caminantenocturno;

import com.renzo.caminantenocturno.entity.CaminanteNocturnoEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=CaminanteNocturnoMod.MODID,bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class NightHordeSpawner {
 private static final int INTERVALO_TICKS=400; // 20 segundos
 private static final int LIMITE_LOCAL=180;
 private static final int RADIO_CONTEO=160;

 @SubscribeEvent
 public static void serverTick(TickEvent.ServerTickEvent event){
   if(event.phase!=TickEvent.Phase.END)return;
   for(ServerLevel level:event.getServer().getAllLevels()){
      if(!level.dimension().equals(LevelKeys.OVERWORLD))continue;
      if(level.isDay())continue;
      if(level.getGameTime()%INTERVALO_TICKS!=0)continue;
      for(ServerPlayer player:level.players()){
         if(player.isSpectator())continue;
         int existing=level.getEntitiesOfClass(CaminanteNocturnoEntity.class,player.getBoundingBox().inflate(RADIO_CONTEO)).size();
         if(existing>=LIMITE_LOCAL)continue;
         int wanted=Math.min(LIMITE_LOCAL-existing,20+level.random.nextInt(16)); // 20-35
         spawnPack(level,player,wanted);
      }
   }
 }

 private static void spawnPack(ServerLevel level,ServerPlayer player,int count){
   int angle=level.random.nextInt(360);
   double rad=Math.toRadians(angle);
   int distance=36+level.random.nextInt(29); // 36-64 bloques
   int baseX=Mth.floor(player.getX()+Math.cos(rad)*distance);
   int baseZ=Mth.floor(player.getZ()+Math.sin(rad)*distance);
   int spawned=0;
   for(int i=0;i<count*3 && spawned<count;i++){
      int x=baseX+level.random.nextInt(17)-8;
      int z=baseZ+level.random.nextInt(17)-8;
      int y=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
      BlockPos pos=new BlockPos(x,y,z);
      if(!level.canSeeSky(pos))continue;
      if(player.blockPosition().distSqr(pos)<24*24)continue;
      CaminanteNocturnoEntity mob=CaminanteNocturnoMod.CAMINANTE_NOCTURNO.get().create(level);
      if(mob==null)continue;
      mob.moveTo(x+0.5D,y,z+0.5D,level.random.nextFloat()*360F,0);
      if(!level.noCollision(mob))continue;
      if(!level.isUnobstructed(mob))continue;
      mob.finalizeSpawn(level,level.getCurrentDifficultyAt(pos),MobSpawnType.EVENT,null,null);
      level.addFreshEntityWithPassengers(mob);
      spawned++;
   }
 }
 private static final class LevelKeys {
   private static final net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> OVERWORLD=net.minecraft.world.level.Level.OVERWORLD;
 }
}
