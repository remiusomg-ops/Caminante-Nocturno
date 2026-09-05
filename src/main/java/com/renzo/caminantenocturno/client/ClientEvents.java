package com.renzo.caminantenocturno.client;
import com.renzo.caminantenocturno.CaminanteNocturnoMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=CaminanteNocturnoMod.MODID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ClientEvents {
 @SubscribeEvent public static void registerLayer(EntityRenderersEvent.RegisterLayerDefinitions e){
   e.registerLayerDefinition(CaminanteNocturnoRenderer.LAYER,CaminanteNocturnoModel::createBodyLayer);
 }
 @SubscribeEvent public static void registerRenderers(EntityRenderersEvent.RegisterRenderers e){
   e.registerEntityRenderer(CaminanteNocturnoMod.CAMINANTE_NOCTURNO.get(),CaminanteNocturnoRenderer::new);
 }
}
