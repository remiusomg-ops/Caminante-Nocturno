package com.renzo.caminantenocturno.client;

import com.renzo.caminantenocturno.entity.CaminanteNocturnoEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

public class CaminanteNocturnoModel<T extends CaminanteNocturnoEntity> extends HumanoidModel<T> {
    public CaminanteNocturnoModel(ModelPart root) { super(root); }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0,0)
                        .addBox(-4,-8,-4,8,8,8),
                PartPose.offset(0.0F,0.0F,0.0F));

        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);

        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(16,16)
                        .addBox(-3.5F,0,-2,7,12,4),
                PartPose.offsetAndRotation(0.0F,0.0F,0.0F,0.10F,0.0F,0.0F));

        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(40,16)
                        .addBox(-2.5F,-2,-2,4,14,4),
                PartPose.offsetAndRotation(-4.5F,2.0F,0.0F,0.15F,0.0F,0.08F));

        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().mirror().texOffs(40,16)
                        .addBox(-1.5F,-2,-2,4,14,4),
                PartPose.offsetAndRotation(4.5F,2.0F,0.0F,0.15F,0.0F,-0.08F));

        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0,16)
                        .addBox(-2,0,-2,4,12,4),
                PartPose.offset(-1.9F,12.0F,0.0F));

        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().mirror().texOffs(0,16)
                        .addBox(-2,0,-2,4,12,4),
                PartPose.offset(1.9F,12.0F,0.0F));

        return LayerDefinition.create(mesh,64,64);
    }
}
