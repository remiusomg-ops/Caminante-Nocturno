package com.renzo.caminantenocturno.entity;

import com.renzo.caminantenocturno.CaminanteNocturnoMod;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import javax.annotation.Nullable;
import java.util.List;

public class CaminanteNocturnoEntity extends Monster {
 private static final double RADIO_AULLIDO=96.0D; private int howlCooldown=0;
 public CaminanteNocturnoEntity(EntityType<? extends Monster> type,Level level){super(type,level);}
 public static AttributeSupplier.Builder createAttributes(){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,20).add(Attributes.ATTACK_DAMAGE,3).add(Attributes.MOVEMENT_SPEED,0.30).add(Attributes.FOLLOW_RANGE,40);}
 public static boolean canSpawn(EntityType<CaminanteNocturnoEntity> type,ServerLevelAccessor level,MobSpawnType reason,BlockPos pos,net.minecraft.util.RandomSource random){
   if(reason==MobSpawnType.SPAWN_EGG||reason==MobSpawnType.COMMAND)return true;
   if(!level.canSeeSky(pos.above()))return false;
   return Monster.checkMonsterSpawnRules(type,level,reason,pos,random);
 }
 @Override protected void registerGoals(){
   goalSelector.addGoal(1,new FloatGoal(this));
   goalSelector.addGoal(2,new MeleeAttackGoal(this,1.25D,false));
   goalSelector.addGoal(6,new WaterAvoidingRandomStrollGoal(this,1.0D));
   goalSelector.addGoal(7,new LookAtPlayerGoal(this,Player.class,12));
   goalSelector.addGoal(8,new RandomLookAroundGoal(this));
   targetSelector.addGoal(1,new HurtByTargetGoal(this).setAlertOthers(CaminanteNocturnoEntity.class));
   targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,LivingEntity.class,10,true,false,t->t!=this&&!(t instanceof CaminanteNocturnoEntity)&&t.isAlive()&&!t.isInvulnerable()));
 }
 @Override public void aiStep(){super.aiStep();if(howlCooldown>0)howlCooldown--;}
 @Override public boolean hurt(DamageSource source,float amount){
   boolean h=super.hurt(source,amount);
   if(h&&!level().isClientSide&&howlCooldown<=0){howlCooldown=100;playSound(CaminanteNocturnoMod.AULLIDO.get(),2.5F,0.95F+random.nextFloat()*0.10F);LivingEntity a=source.getEntity() instanceof LivingEntity l?l:null;alertHorde(a);}
   return h;
 }
 private void alertHorde(@Nullable LivingEntity attacker){
   List<CaminanteNocturnoEntity> hs=level().getEntitiesOfClass(CaminanteNocturnoEntity.class,getBoundingBox().inflate(RADIO_AULLIDO),w->w!=this&&w.isAlive());
   for(CaminanteNocturnoEntity w:hs){if(attacker!=null&&attacker.isAlive()&&!(attacker instanceof CaminanteNocturnoEntity)){w.setTarget(attacker);w.getNavigation().moveTo(attacker,1.25D);}else w.getNavigation().moveTo(getX(),getY(),getZ(),1.2D);}
 }
 @Override public boolean canAttack(LivingEntity t){return !(t instanceof CaminanteNocturnoEntity)&&super.canAttack(t);}
 @Nullable @Override protected SoundEvent getAmbientSound(){return SoundEvents.ZOMBIE_AMBIENT;}
 @Override protected SoundEvent getHurtSound(DamageSource s){return SoundEvents.ZOMBIE_HURT;}
 @Override protected SoundEvent getDeathSound(){return SoundEvents.ZOMBIE_DEATH;}
}
