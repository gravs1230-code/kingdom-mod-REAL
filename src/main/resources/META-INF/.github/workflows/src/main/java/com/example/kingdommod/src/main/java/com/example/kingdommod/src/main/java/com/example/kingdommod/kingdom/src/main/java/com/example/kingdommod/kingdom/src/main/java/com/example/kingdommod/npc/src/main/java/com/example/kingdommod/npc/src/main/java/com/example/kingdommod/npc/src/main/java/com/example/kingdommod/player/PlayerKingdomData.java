package com.example.kingdommod.player;

import net.minecraft.nbt.CompoundTag;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerKingdomData {
    private Map<UUID, Integer> reputation = new HashMap<>();
    private Map<UUID, String> titles = new HashMap<>();
    private UUID joinedKingdomId = null;

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

    public UUID getJoinedKingdomId() {
        return joinedKingdomId;
    }

    public void setJoinedKingdomId(UUID kingdomId) {
        this.joinedKingdomId = kingdomId;
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        CompoundTag repTag = new CompoundTag();
        for (Map.Entry<UUID, Integer> entry : reputation.entrySet()) {
            repTag.putInt(entry.getKey().toString(), entry.getValue());
        }
        tag.put("reputation", repTag);
        CompoundTag titlesTag = new CompoundTag();
        for (Map.Entry<UUID, String> entry : titles.entrySet()) {
            titlesTag.putString(entry.getKey().toString(), entry.getValue());
        }
        tag.put("titles", titlesTag);
        if (joinedKingdomId != null) tag.putUUID("joinedKingdom", joinedKingdomId);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        if (tag.contains("reputation")) {
            CompoundTag repTag = tag.getCompound("reputation");
            for (String key : repTag.getAllKeys()) {
                reputation.put(UUID.fromString(key), repTag.getInt(key));
            }
        }
        if (tag.contains("titles")) {
            CompoundTag titlesTag = tag.getCompound("titles");
            for (String key : titlesTag.getAllKeys()) {
                titles.put(UUID.fromString(key), titlesTag.getString(key));
            }
        }
        if (tag.hasUUID("joinedKingdom")) {
            joinedKingdomId = tag.getUUID("joinedKingdom");
        }
    }
}
