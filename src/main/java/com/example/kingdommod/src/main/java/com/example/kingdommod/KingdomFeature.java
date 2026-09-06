package com.example.kingdommod;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class KingdomFeature extends Feature<NoneFeatureConfiguration> {
    public KingdomFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        // Rare chance: 0.1% per chunk (1 in 1000)
        if (context.random().nextFloat() > 0.001f) return false;

        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        if (!(level instanceof ServerLevel serverLevel)) return false;

        // Build main castle
        buildCastle(level, origin);

        // Build two houses
        buildHouse(level, origin.offset(10, 0, 10));
        buildHouse(level, origin.offset(-10, 0, 10));

        // Build farm
        buildFarm(level, origin.offset(0, 0, 15));

        // Spawn NPCs with roles
        spawnVillager(serverLevel, origin.offset(0, 1, 0), KingdomRoles.KING);
        spawnVillager(serverLevel, origin.offset(10, 1, 10), KingdomRoles.FARMER);
        spawnVillager(serverLevel, origin.offset(-10, 1, 10), KingdomRoles.SOLDIER);
        spawnVillager(serverLevel, origin.offset(5, 1, 5), KingdomRoles.MERCHANT);

        // Create kingdom data
        KingdomData data = KingdomData.get(serverLevel);
        KingdomData.Kingdom kingdom = data.createKingdom("Kingdom of " + origin.toShortString(), origin);
        // Set king UUID (the first villager's UUID, but we don't have it yet, so we set later on entity join)
        // For now leave null; we'll link in event handler when villager with KING role spawns.
        data.setDirty();

        return true;
    }

    private void buildCastle(WorldGenLevel level, BlockPos center) {
        // 11x11 cobblestone tower, 8 blocks high, hollow
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                for (int y = 0; y < 8; y++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (x == -5 || x == 5 || z == -5 || z == 5 || y == 0 || y == 7) {
                        level.setBlock(pos, Blocks.COBBLESTONE.defaultBlockState(), 3);
                    } else {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
        // Door
        level.setBlock(center.offset(0, 1, -5), Blocks.OAK_DOOR.defaultBlockState(), 3);
    }

    private void buildHouse(WorldGenLevel level, BlockPos center) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = 0; y < 5; y++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (x == -2 || x == 2 || z == -2 || z == 2 || y == 0 || y == 4) {
                        level.setBlock(pos, Blocks.OAK_PLANKS.defaultBlockState(), 3);
                    } else {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
        level.setBlock(center.offset(0, 1, -2), Blocks.OAK_DOOR.defaultBlockState(), 3);
    }

    private void buildFarm(WorldGenLevel level, BlockPos center) {
        for (int x = -3; x <= 3; x++) {
            for (int z = -1; z <= 1; z++) {
                level.setBlock(center.offset(x, 0, z), Blocks.FARMLAND.defaultBlockState(), 3);
            }
        }
    }

    private void spawnVillager(ServerLevel level, BlockPos pos, KingdomRoles role) {
        Villager villager = EntityType.VILLAGER.create(level);
        if (villager != null) {
            villager.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
            KingdomRoles.setRole(villager, role);
            level.addFreshEntity(villager);
        }
    }
}