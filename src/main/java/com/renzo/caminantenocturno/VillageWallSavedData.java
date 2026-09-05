package com.renzo.caminantenocturno;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

public class VillageWallSavedData extends SavedData {
    private static final String DATA_NAME = "caminantenocturno_village_walls";
    private final Set<Long> walledVillages = new HashSet<>();

    public static VillageWallSavedData get(net.minecraft.server.level.ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                VillageWallSavedData::load,
                VillageWallSavedData::new,
                DATA_NAME
        );
    }

    public boolean isWalled(long key) {
        return walledVillages.contains(key);
    }

    public void markWalled(long key) {
        if (walledVillages.add(key)) {
            setDirty();
        }
    }

    public static VillageWallSavedData load(CompoundTag tag) {
        VillageWallSavedData data = new VillageWallSavedData();
        ListTag list = tag.getList("villages", Tag.TAG_LONG);
        for (Tag value : list) {
            data.walledVillages.add(((LongTag) value).getAsLong());
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (long key : walledVillages) {
            list.add(LongTag.valueOf(key));
        }
        tag.put("villages", list);
        return tag;
    }
}
