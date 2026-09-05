package com.renzo.caminantenocturno.client;
import com.renzo.caminantenocturno.CaminanteNocturnoMod;
import com.renzo.caminantenocturno.entity.CaminanteNocturnoEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
public class CaminanteNocturnoRenderer extends MobRenderer<CaminanteNocturnoEntity,HumanoidModel<CaminanteNocturnoEntity>>{
 private static final ResourceLocation TEXTURE=new ResourceLocation(CaminanteNocturnoMod.MODID,"textures/entity/caminante_nocturno.png");
 public CaminanteNocturnoRenderer(EntityRendererProvider.Context c){super(c,new HumanoidModel<>(c.bakeLayer(ModelLayers.ZOMBIE)),0.5F);}
 @Override public ResourceLocation getTextureLocation(CaminanteNocturnoEntity e){return TEXTURE;}
}
