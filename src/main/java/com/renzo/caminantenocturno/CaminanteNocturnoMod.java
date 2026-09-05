package com.renzo.caminantenocturno;

import com.renzo.caminantenocturno.entity.CaminanteNocturnoEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.common.ForgeSpawnEggItem;

@Mod(CaminanteNocturnoMod.MODID)
public class CaminanteNocturnoMod {
    public static final String MODID = "caminantenocturno";
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MODID);
    public static final RegistryObject<EntityType<CaminanteNocturnoEntity>> CAMINANTE_NOCTURNO = ENTITY_TYPES.register(
        "caminante_nocturno",
        () -> EntityType.Builder.of(CaminanteNocturnoEntity::new, MobCategory.MONSTER)
            .sized(0.6F, 1.95F).clientTrackingRange(10).build("caminantenocturno:caminante_nocturno")
    );
    public static final RegistryObject<SoundEvent> AULLIDO = SOUNDS.register(
        "aullido", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MODID, "aullido"))
    );
    public static final RegistryObject<Item> CAMINANTE_NOCTURNO_SPAWN_EGG = ITEMS.register(
        "caminante_nocturno_spawn_egg",
        () -> new ForgeSpawnEggItem(CAMINANTE_NOCTURNO, 0x201B1B, 0x793030, new Item.Properties())
    );
    public CaminanteNocturnoMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ENTITY_TYPES.register(modBus); ITEMS.register(modBus); SOUNDS.register(modBus);
        modBus.addListener(this::addCreative);
        MinecraftForge.EVENT_BUS.register(this);
    }
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) event.accept(CAMINANTE_NOCTURNO_SPAWN_EGG);
    }
}
