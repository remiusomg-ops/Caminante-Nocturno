package com.renzo.caminantenocturno.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.renzo.caminantenocturno.CaminanteNocturnoMod;
import com.renzo.caminantenocturno.entity.CaminanteNocturnoEntity;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class CaminanteNocturnoRenderer extends MobRenderer<CaminanteNocturnoEntity, ZombieModel<CaminanteNocturnoEntity>> {
    private static final ResourceLocation TEXTURE =
        new ResourceLocation(CaminanteNocturnoMod.MODID, "textures/entity/caminante_nocturno.png");

    public CaminanteNocturnoRenderer(EntityRendererProvider.Context context) {
        super(context, new ZombieModel<>(context.bakeLayer(ModelLayers.HUSK)), 0.5F);
    }

    @Override
    protected void scale(CaminanteNocturnoEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(0.92F, 1.12F, 0.92F);
    }

    @Override
    public ResourceLocation getTextureLocation(CaminanteNocturnoEntity entity) {
        return TEXTURE;
    }
}
