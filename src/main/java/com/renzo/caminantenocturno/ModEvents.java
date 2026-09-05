package com.renzo.caminantenocturno;
import com.renzo.caminantenocturno.entity.CaminanteNocturnoEntity;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
@Mod.EventBusSubscriber(modid=CaminanteNocturnoMod.MODID,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class ModEvents {
 @SubscribeEvent public static void attributes(EntityAttributeCreationEvent e){e.put(CaminanteNocturnoMod.CAMINANTE_NOCTURNO.get(),CaminanteNocturnoEntity.createAttributes().build());}
 @SubscribeEvent public static void commonSetup(FMLCommonSetupEvent e){e.enqueueWork(()->SpawnPlacements.register(CaminanteNocturnoMod.CAMINANTE_NOCTURNO.get(),SpawnPlacements.Type.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,CaminanteNocturnoEntity::canSpawn));}
}
