package com.example.kingdommod.kingdom;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import java.util.*;

public class Kingdom {
    private UUID id;
    private String name;
    private BlockPos capital;
    private int treasury;
    private int armySize;
    private Map<UUID, Integer> relations = new HashMap<>();
    private List<UUID> members = new ArrayList<>();
    private UUID kingUUID;
    private List<BlockPos> villages = new ArrayList<>();

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", id);
        tag.putString("name", name);
        tag.put("capital", NbtUtils.writeBlockPos(capital));
        tag.putInt("treasury", treasury);
        tag.putInt("armySize", armySize);
        ListTag relList = new ListTag();
        for (Map.Entry<UUID, Integer> entry : relations.entrySet()) {
            CompoundTag relTag = new CompoundTag();
            relTag.putUUID("other", entry.getKey());
            relTag.putInt("value", entry.getValue());
            relList.add(relTag);
        }
        tag.put("relations", relList);
        ListTag memList = new ListTag();
        for (UUID uuid : members) {
            CompoundTag memTag = new CompoundTag();
            memTag.putUUID("uuid", uuid);
            memList.add(memTag);
        }
        tag.put("members", memList);
        if (kingUUID != null) tag.putUUID("king", kingUUID);
        ListTag villageList = new ListTag();
        for (BlockPos pos : villages) {
            villageList.add(NbtUtils.writeBlockPos(pos));
        }
        tag.put("villages", villageList);
        return tag;
    }

    public static Kingdom deserializeNBT(CompoundTag tag) {
        Kingdom k = new Kingdom();
        k.id = tag.getUUID("id");
        k.name = tag.getString("name");
        k.capital = NbtUtils.readBlockPos(tag.getCompound("capital"));
        k.treasury = tag.getInt("treasury");
        k.armySize = tag.getInt("armySize");
        ListTag relList = tag.getList("relations", Tag.TAG_COMPOUND);
        for (int i = 0; i < relList.size(); i++) {
            CompoundTag relTag = relList.getCompound(i);
            UUID other = relTag.getUUID("other");
            int value = relTag.getInt("value");
            k.relations.put(other, value);
        }
        ListTag memList = tag.getList("members", Tag.TAG_COMPOUND);
        for (int i = 0; i < memList.size(); i++) {
            k.members.add(memList.getCompound(i).getUUID("uuid"));
        }
        if (tag.hasUUID("king")) k.kingUUID = tag.getUUID("king");
        ListTag villageList = tag.getList("villages", Tag.TAG_COMPOUND);
        for (int i = 0; i < villageList.size(); i++) {
            k.villages.add(NbtUtils.readBlockPos(villageList.getCompound(i)));
        }
        return k;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BlockPos getCapital() { return capital; }
    public void setCapital(BlockPos capital) { this.capital = capital; }
    public int getTreasury() { return treasury; }
    public void setTreasury(int treasury) { this.treasury = treasury; }
    public int getArmySize() { return armySize; }
    public void setArmySize(int armySize) { this.armySize = armySize; }
    public Map<UUID, Integer> getRelations() { return relations; }
    public List<UUID> getMembers() { return members; }
    public UUID getKingUUID() { return kingUUID; }
    public void setKingUUID(UUID kingUUID) { this.kingUUID = kingUUID; }
    public List<BlockPos> getVillages() { return villages; }
}
