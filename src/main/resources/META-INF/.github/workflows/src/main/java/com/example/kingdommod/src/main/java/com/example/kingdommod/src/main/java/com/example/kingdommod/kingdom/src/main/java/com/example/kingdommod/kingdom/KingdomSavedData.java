package com.example.kingdommod.kingdom;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

public class KingdomSavedData extends SavedData {
    private static final String DATA_NAME = "kingdommod_kingdoms";
    private Map<UUID, Kingdom> kingdoms = new HashMap<>();

    public static KingdomSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage()
                .computeIfAbsent(KingdomSavedData::load, KingdomSavedData::new, DATA_NAME);
    }

    public static KingdomSavedData load(CompoundTag tag) {
        KingdomSavedData data = new KingdomSavedData();
        ListTag list = tag.getList("kingdoms", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            Kingdom k = Kingdom.deserializeNBT(list.getCompound(i));
            data.kingdoms.put(k.getId(), k);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Kingdom k : kingdoms.values()) {
            list.add(k.serializeNBT());
        }
        tag.put("kingdoms", list);
        return tag;
    }

    public Kingdom createKingdom(String name, BlockPos capital) {
        Kingdom k = new Kingdom();
        k.setId(UUID.randomUUID());
        k.setName(name);
        k.setCapital(capital);
        k.setTreasury(1000);
        k.setArmySize(10);
        kingdoms.put(k.getId(), k);
        setDirty();
        return k;
    }

    public Kingdom getKingdom(UUID id) { return kingdoms.get(id); }
    public Collection<Kingdom> getAllKingdoms() { return kingdoms.values(); }
    public void removeKingdom(UUID id) { kingdoms.remove(id); setDirty(); }
}
