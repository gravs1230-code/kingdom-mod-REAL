package com.example.kingdommod;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

public class KingdomData extends SavedData {
    private static final String DATA_NAME = "kingdommod_data";

    private Map<UUID, Kingdom> kingdoms = new HashMap<>();
    private Map<UUID, PlayerProgress> playerData = new HashMap<>();

    public static KingdomData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage()
                .computeIfAbsent(KingdomData::load, KingdomData::new, DATA_NAME);
    }

    public static KingdomData load(CompoundTag tag) {
        KingdomData data = new KingdomData();
        ListTag kList = tag.getList("kingdoms", Tag.TAG_COMPOUND);
        for (int i = 0; i < kList.size(); i++) {
            Kingdom k = Kingdom.fromNBT(kList.getCompound(i));
            data.kingdoms.put(k.id, k);
        }
        ListTag pList = tag.getList("players", Tag.TAG_COMPOUND);
        for (int i = 0; i < pList.size(); i++) {
            PlayerProgress pp = PlayerProgress.fromNBT(pList.getCompound(i));
            data.playerData.put(pp.uuid, pp);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag kList = new ListTag();
        for (Kingdom k : kingdoms.values()) kList.add(k.toNBT());
        tag.put("kingdoms", kList);

        ListTag pList = new ListTag();
        for (PlayerProgress pp : playerData.values()) pList.add(pp.toNBT());
        tag.put("players", pList);
        return tag;
    }

    // Kingdom management
    public Kingdom createKingdom(String name, BlockPos capital) {
        Kingdom k = new Kingdom(UUID.randomUUID(), name, capital);
        // Initialize random relations with existing kingdoms
        for (Kingdom other : kingdoms.values()) {
            int rel = new Random().nextInt(201) - 100; // -100..100
            k.relations.put(other.id, rel);
            other.relations.put(k.id, rel);
        }
        kingdoms.put(k.id, k);
        setDirty();
        return k;
    }

    public Kingdom getKingdom(UUID id) { return kingdoms.get(id); }
    public Kingdom getKingdomByName(String name) {
        for (Kingdom k : kingdoms.values()) if (k.name.equalsIgnoreCase(name)) return k;
        return null;
    }
    public Collection<Kingdom> getAllKingdoms() { return kingdoms.values(); }
    public void removeKingdom(UUID id) { kingdoms.remove(id); setDirty(); }

    // Player progress
    public PlayerProgress getOrCreatePlayerProgress(UUID playerId) {
        return playerData.computeIfAbsent(playerId, PlayerProgress::new);
    }

    public void setPlayerProgress(UUID playerId, PlayerProgress pp) {
        playerData.put(playerId, pp);
        setDirty();
    }

    // Inner classes
    public static class Kingdom {
        public UUID id;
        public String name;
        public BlockPos capital;
        public int treasury;
        public int armySize;
        public Map<UUID, Integer> relations = new HashMap<>();
        public UUID kingUUID; // UUID of villager king if any

        public Kingdom(UUID id, String name, BlockPos capital) {
            this.id = id;
            this.name = name;
            this.capital = capital;
            this.treasury = 1000;
            this.armySize = 10;
        }

        public CompoundTag toNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("id", id);
            tag.putString("name", name);
            tag.put("capital", NbtUtils.writeBlockPos(capital));
            tag.putInt("treasury", treasury);
            tag.putInt("armySize", armySize);
            ListTag relList = new ListTag();
            for (Map.Entry<UUID, Integer> e : relations.entrySet()) {
                CompoundTag relTag = new CompoundTag();
                relTag.putUUID("other", e.getKey());
                relTag.putInt("value", e.getValue());
                relList.add(relTag);
            }
            tag.put("relations", relList);
            if (kingUUID != null) tag.putUUID("king", kingUUID);
            return tag;
        }

        public static Kingdom fromNBT(CompoundTag tag) {
            Kingdom k = new Kingdom(tag.getUUID("id"), tag.getString("name"),
                    NbtUtils.readBlockPos(tag.getCompound("capital")));
            k.treasury = tag.getInt("treasury");
            k.armySize = tag.getInt("armySize");
            ListTag relList = tag.getList("relations", Tag.TAG_COMPOUND);
            for (int i = 0; i < relList.size(); i++) {
                CompoundTag relTag = relList.getCompound(i);
                k.relations.put(relTag.getUUID("other"), relTag.getInt("value"));
            }
            if (tag.hasUUID("king")) k.kingUUID = tag.getUUID("king");
            return k;
        }
    }

    public static class PlayerProgress {
        public UUID uuid;
        public UUID joinedKingdomId;
        public Map<UUID, Integer> reputation = new HashMap<>();
        public Map<UUID, String> titles = new HashMap<>();

        public PlayerProgress(UUID uuid) {
            this.uuid = uuid;
        }

        public int getReputation(UUID kingdomId) {
            return reputation.getOrDefault(kingdomId, 0);
        }

        public void setReputation(UUID kingdomId, int value) {
            reputation.put(kingdomId, value);
        }

        public String getTitle(UUID kingdomId) {
            return titles.getOrDefault(kingdomId, "Peasant");
        }

        public void setTitle(UUID kingdomId, String title) {
            titles.put(kingdomId, title);
        }

        public CompoundTag toNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("uuid", uuid);
            if (joinedKingdomId != null) tag.putUUID("joinedKingdom", joinedKingdomId);
            CompoundTag repTag = new CompoundTag();
            for (Map.Entry<UUID, Integer> e : reputation.entrySet()) repTag.putInt(e.getKey().toString(), e.getValue());
            tag.put("reputation", repTag);
            CompoundTag titleTag = new CompoundTag();
            for (Map.Entry<UUID, String> e : titles.entrySet()) titleTag.putString(e.getKey().toString(), e.getValue());
            tag.put("titles", titleTag);
            return tag;
        }

        public static PlayerProgress fromNBT(CompoundTag tag) {
            PlayerProgress pp = new PlayerProgress(tag.getUUID("uuid"));
            if (tag.hasUUID("joinedKingdom")) pp.joinedKingdomId = tag.getUUID("joinedKingdom");
            CompoundTag repTag = tag.getCompound("reputation");
            for (String key : repTag.getAllKeys()) pp.reputation.put(UUID.fromString(key), repTag.getInt(key));
            CompoundTag titleTag = tag.getCompound("titles");
            for (String key : titleTag.getAllKeys()) pp.titles.put(UUID.fromString(key), titleTag.getString(key));
            return pp;
        }
    }
}