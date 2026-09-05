package com.renzo.caminantenocturno.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.renzo.caminantenocturno.CaminanteNocturnoMod;
import com.renzo.caminantenocturno.entity.CaminanteNocturnoEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class CaminanteNocturnoRenderer extends MobRenderer<CaminanteNocturnoEntity,CaminanteNocturnoModel<CaminanteNocturnoEntity>> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(CaminanteNocturnoMod.MODID,"caminante_nocturno"),"main");
    private static final ResourceLocation TEXTURE = new ResourceLocation(CaminanteNocturnoMod.MODID,"textures/entity/caminante_nocturno.png");

    public CaminanteNocturnoRenderer(EntityRendererProvider.Context context) {
        super(context,new CaminanteNocturnoModel<>(context.bakeLayer(LAYER)),0.5F);
    }

    @Override protected void scale(CaminanteNocturnoEntity entity,PoseStack pose,float partialTick) {
        pose.scale(0.95F,1.13F,0.95F);
    }
    @Override public ResourceLocation getTextureLocation(CaminanteNocturnoEntity entity) { return TEXTURE; }
}
