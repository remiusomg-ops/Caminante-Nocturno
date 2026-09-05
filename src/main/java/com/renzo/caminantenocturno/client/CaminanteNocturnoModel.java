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
                CubeListBuilder.create().texOffs(0,0).addBox(-4,-8,-4,8,8,8,new CubeDeformation(0.0F)),
                PartPose.offset(0.0F,0.0F,0.0F));
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);

        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(16,16).addBox(-4,0,-2,8,12,4,new CubeDeformation(-0.25F)),
                PartPose.offsetAndRotation(0.0F,0.0F,0.0F,0.18F,0.0F,0.0F));

        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(40,16).addBox(-3,-2,-2,4,18,4,new CubeDeformation(-0.15F)),
                PartPose.offsetAndRotation(-5.0F,2.0F,0.0F,0.18F,0.0F,0.12F));
        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().mirror().texOffs(40,16).addBox(-1,-2,-2,4,18,4,new CubeDeformation(-0.15F)),
                PartPose.offsetAndRotation(5.0F,2.0F,0.0F,0.18F,0.0F,-0.12F));

        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0,16).addBox(-2,0,-2,4,12,4,new CubeDeformation(-0.1F)),
                PartPose.offset(-2.0F,12.0F,0.0F));
        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().mirror().texOffs(0,16).addBox(-2,0,-2,4,12,4,new CubeDeformation(-0.1F)),
                PartPose.offset(2.0F,12.0F,0.0F));

        return LayerDefinition.create(mesh,64,64);
    }
}
