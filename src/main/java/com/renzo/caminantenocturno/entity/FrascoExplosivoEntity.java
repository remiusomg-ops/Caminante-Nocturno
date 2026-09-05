package com.renzo.caminantenocturno.entity;

import com.renzo.caminantenocturno.CaminanteNocturnoMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

public class FrascoExplosivoEntity extends ThrowableItemProjectile {
    public FrascoExplosivoEntity(EntityType<? extends FrascoExplosivoEntity> type, Level level) {
        super(type, level);
    }

    public FrascoExplosivoEntity(Level level, LivingEntity owner) {
        super(CaminanteNocturnoMod.FRASCO_EXPLOSIVO_ENTITY.get(), owner, level);
    }

    @Override
    protected Item getDefaultItem() {
        return CaminanteNocturnoMod.FRASCO_EXPLOSIVO.get();
    }

    @Override
    protected float getGravity() {
        return 0.05F;
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            level().explode(getOwner(), getX(), getY(), getZ(), 4.0F, false, Level.ExplosionInteraction.NONE);
            ((net.minecraft.server.level.ServerLevel) level()).sendParticles(
                    ParticleTypes.EXPLOSION, getX(), getY(), getZ(), 2, 0.15D, 0.15D, 0.15D, 0.0D);
            discard();
        }
    }
}
