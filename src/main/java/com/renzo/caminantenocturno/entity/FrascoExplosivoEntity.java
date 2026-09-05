package com.renzo.caminantenocturno.entity;

import com.renzo.caminantenocturno.CaminanteNocturnoMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;

public class FrascoExplosivoEntity extends ThrowableItemProjectile {
    private static final double RADIO_DANO = 5.0D;
    private static final float DANO_MAXIMO = 18.0F;

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
        if (level().isClientSide) return;

        ServerLevel server = (ServerLevel) level();

        // Efecto de explosión sin crear una explosión vanilla real:
        // así NO destruye bloques y NO elimina ItemEntity del suelo.
        server.playSound(null, getX(), getY(), getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0F, 1.0F);

        server.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                getX(), getY(), getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);

        AABB area = getBoundingBox().inflate(RADIO_DANO);
        for (LivingEntity target : server.getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive)) {
            if (target == getOwner()) continue;

            double distance = target.distanceTo(this);
            if (distance > RADIO_DANO) continue;

            double factor = 1.0D - (distance / RADIO_DANO);
            float damage = (float)(DANO_MAXIMO * factor);
            if (damage < 2.0F) damage = 2.0F;

            target.hurt(server.damageSources().explosion(this, getOwner()), damage);
        }

        discard();
    }
}
