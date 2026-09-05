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
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerLevel;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class CaminanteNocturnoEntity extends Monster {
 private static final double RADIO_AULLIDO=512.0D;
 private static final int TIEMPO_REUNION=1200; // 60 segundos
 private static final int COOLDOWN_GLOBAL_AULLIDO=160; // 8 segundos
 private static final Map<ServerLevel, GlobalHowl> GLOBAL_HOWLS = new WeakHashMap<>();
 private static final Map<ServerLevel, Long> LAST_HOWL_TICK = new WeakHashMap<>();

 private int rallyTicks=0;
 private BlockPos rallyPos=null;
 @Nullable private LivingEntity rallyTarget=null;

 private record GlobalHowl(BlockPos pos, @Nullable LivingEntity attacker, long expiresAt) {}

 public CaminanteNocturnoEntity(EntityType<? extends Monster> type,Level level){super(type,level);}
 public static AttributeSupplier.Builder createAttributes(){
   return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,20).add(Attributes.ATTACK_DAMAGE,3).add(Attributes.MOVEMENT_SPEED,0.30).add(Attributes.FOLLOW_RANGE,64);
 }
 public static boolean canSpawn(EntityType<CaminanteNocturnoEntity> type,ServerLevelAccessor level,MobSpawnType reason,BlockPos pos,net.minecraft.util.RandomSource random){
   if(reason==MobSpawnType.SPAWN_EGG||reason==MobSpawnType.COMMAND)return true;
   if(!level.canSeeSky(pos.above()))return false;
   return Monster.checkMonsterSpawnRules(type,level,reason,pos,random);
 }
 @Override protected void registerGoals(){
   goalSelector.addGoal(1,new FloatGoal(this));
   goalSelector.addGoal(2,new MeleeAttackGoal(this,1.30D,false));
   goalSelector.addGoal(6,new WaterAvoidingRandomStrollGoal(this,1.0D));
   goalSelector.addGoal(7,new LookAtPlayerGoal(this,Player.class,16));
   goalSelector.addGoal(8,new RandomLookAroundGoal(this));
   targetSelector.addGoal(1,new HurtByTargetGoal(this).setAlertOthers(CaminanteNocturnoEntity.class));
   targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,LivingEntity.class,10,true,false,
      t->t!=this&&!(t instanceof CaminanteNocturnoEntity)&&t.isAlive()&&!t.isInvulnerable()));
 }
 @Override public void aiStep(){
   super.aiStep();

   if(!level().isClientSide && level() instanceof ServerLevel sl){
      GlobalHowl gh=GLOBAL_HOWLS.get(sl);
      if(gh!=null){
         if(sl.getGameTime()<=gh.expiresAt()){
            if(rallyTicks<=0 || (rallyPos!=null && rallyPos.distSqr(gh.pos())>16.0D)){
               rallyTo(gh.pos(), gh.attacker());
            }
         }else{
            GLOBAL_HOWLS.remove(sl);
         }
      }
   }

   if(!level().isClientSide && rallyTicks>0){
      rallyTicks--;
      if(rallyTarget!=null && rallyTarget.isAlive() && !(rallyTarget instanceof CaminanteNocturnoEntity)){
         setTarget(rallyTarget);
         if(tickCount%10==0)getNavigation().moveTo(rallyTarget,1.35D);
      }else if(rallyPos!=null){
         if(tickCount%10==0)getNavigation().moveTo(rallyPos.getX()+0.5D,rallyPos.getY(),rallyPos.getZ()+0.5D,1.30D);
         if(blockPosition().distSqr(rallyPos)<25.0D){rallyTicks=0;rallyPos=null;}
      }
      if(rallyTicks==0){rallyPos=null;rallyTarget=null;}
   }
 }
 @Override public boolean hurt(DamageSource source,float amount){
   boolean h=super.hurt(source,amount);
   if(h&&!level().isClientSide && level() instanceof ServerLevel sl){
      long now=sl.getGameTime();
      long last=LAST_HOWL_TICK.getOrDefault(sl,Long.MIN_VALUE/2);
      if(now-last>=COOLDOWN_GLOBAL_AULLIDO){
         LAST_HOWL_TICK.put(sl,now);
         playSound(CaminanteNocturnoMod.AULLIDO.get(),4.0F,1.0F);
         LivingEntity a=source.getEntity() instanceof LivingEntity l?l:null;
         alertHorde(a);
      }
   }
   return h;
 }
 public void rallyTo(BlockPos pos,@Nullable LivingEntity attacker){
   rallyPos=pos.immutable();
   rallyTarget=attacker;
   rallyTicks=TIEMPO_REUNION;
   if(attacker!=null&&attacker.isAlive()&&!(attacker instanceof CaminanteNocturnoEntity)){
      setTarget(attacker);
      getNavigation().moveTo(attacker,1.35D);
   }else{
      getNavigation().moveTo(pos.getX()+0.5D,pos.getY(),pos.getZ()+0.5D,1.30D);
   }
 }
 private void alertHorde(@Nullable LivingEntity attacker){
   BlockPos source=blockPosition();
   if(level() instanceof ServerLevel sl){
      GLOBAL_HOWLS.put(sl,new GlobalHowl(source,attacker,sl.getGameTime()+TIEMPO_REUNION));
   }
   List<CaminanteNocturnoEntity> hs=level().getEntitiesOfClass(CaminanteNocturnoEntity.class,getBoundingBox().inflate(RADIO_AULLIDO),w->w!=this&&w.isAlive());
   for(CaminanteNocturnoEntity w:hs)w.rallyTo(source,attacker);
 }
 @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recentlyHit){
   super.dropCustomDeathLoot(source,looting,recentlyHit);
   int count=3+random.nextInt(3);
   spawnAtLocation(Items.IRON_NUGGET,count);
 }
 @Override public boolean canAttack(LivingEntity t){return !(t instanceof CaminanteNocturnoEntity)&&super.canAttack(t);}
 @Nullable @Override protected SoundEvent getAmbientSound(){return SoundEvents.ZOMBIE_AMBIENT;}
 @Override protected SoundEvent getHurtSound(DamageSource s){return SoundEvents.ZOMBIE_HURT;}
 @Override protected SoundEvent getDeathSound(){return SoundEvents.ZOMBIE_DEATH;}
}
