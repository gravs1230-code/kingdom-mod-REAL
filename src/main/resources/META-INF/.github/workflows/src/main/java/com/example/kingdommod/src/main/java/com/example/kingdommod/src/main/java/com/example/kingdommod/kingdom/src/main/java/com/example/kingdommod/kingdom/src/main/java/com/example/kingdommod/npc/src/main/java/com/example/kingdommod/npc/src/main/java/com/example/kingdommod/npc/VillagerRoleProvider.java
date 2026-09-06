package com.example.kingdommod.npc;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VillagerRoleProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    private KingdomRole role = KingdomRole.VILLAGER;
    private final LazyOptional<KingdomRole> optional = LazyOptional.of(() -> role);

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == VillagerRoleCapability.ROLE) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("role", role.name());
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        if (nbt.contains("role")) {
            role = KingdomRole.valueOf(nbt.getString("role"));
        }
    }

    // Add setter
    public void setRole(KingdomRole newRole) {
        this.role = newRole;
    }
}
