package com.example.kingdommod;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.EnumSet;
import java.util.Random;

@Mod.EventBusSubscriber(modid = KingdomMod.MODID)
public class KingdomEventHandler {

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Villager villager && !event.getLevel().isClientSide()) {
            // Add schedule and soldier goals
            villager.goalSelector.addGoal(1, new ScheduleGoal(villager));
            if (KingdomRoles.hasRole(villager, KingdomRoles.SOLDIER)) {
                villager.goalSelector.addGoal(2, new SoldierAttackGoal(villager, 1.0, true));
            }
            // If this villager is a king, link to kingdom data (set kingUUID)
            if (KingdomRoles.hasRole(villager, KingdomRoles.KING)) {
                ServerLevel serverLevel = (ServerLevel) villager.level();
                KingdomData data = KingdomData.get(serverLevel);
                // Find nearest kingdom and assign kingUUID
                BlockPos pos = villager.blockPosition();
                double minDist = Double.MAX_VALUE;
                KingdomData.Kingdom nearest = null;
                for (KingdomData.Kingdom k : data.getAllKingdoms()) {
                    double dist = pos.distSqr(k.capital);
                    if (dist < minDist) {
                        minDist = dist;
                        nearest = k;
                    }
                }
                if (nearest != null) {
                    nearest.kingUUID = villager.getUUID();
                    data.setDirty();
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getTarget() instanceof Villager villager && event.getEntity() instanceof Player player) {
            if (KingdomRoles.hasRole(villager, KingdomRoles.KING)) {
                if (!player.level().isClientSide()) {
                    ServerLevel serverLevel = (ServerLevel) player.level();
                    KingdomData data = KingdomData.get(serverLevel);
                    KingdomData.PlayerProgress pp = data.getOrCreatePlayerProgress(player.getUUID());
                    if (pp.joinedKingdomId != null) {
                        KingdomData.Kingdom k = data.getKingdom(pp.joinedKingdomId);
                        if (k != null) {
                            player.sendSystemMessage(Component.literal("Kingdom: " + k.name +
                                    " | Treasury: " + k.treasury +
                                    " | Army: " + k.armySize +
                                    " | Your reputation: " + pp.getReputation(k.id) +
                                    " | Title: " + pp.getTitle(k.id)));
                        }
                    } else {
                        player.sendSystemMessage(Component.literal("You are not in a kingdom. Use /kingdom join <name> to join."));
                    }
                    event.setCanceled(true);
                    event.setCancellationResult(InteractionResult.SUCCESS);
                }
            }
        }
    }

    // War simulation every 5 minutes
    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide()) return;
        ServerLevel serverLevel = (ServerLevel) event.level;
        if (++tickCounter < 6000) return;
        tickCounter = 0;
        KingdomData data = KingdomData.get(serverLevel);
        processWars(data);
    }

    private static void processWars(KingdomData data) {
        KingdomData.Kingdom[] kingdoms = data.getAllKingdoms().toArray(new KingdomData.Kingdom[0]);
        Random random = new Random();
        for (int i = 0; i < kingdoms.length; i++) {
            for (int j = i + 1; j < kingdoms.length; j++) {
                KingdomData.Kingdom a = kingdoms[i];
                KingdomData.Kingdom b = kingdoms[j];
                int relation = a.relations.getOrDefault(b.id, 0);
                if (relation < -50 && a.armySize > 0 && b.armySize > 0) {
                    // Simulate battle
                    int aPower = a.armySize + random.nextInt(10);
                    int bPower = b.armySize + random.nextInt(10);
                    if (aPower > bPower) {
                        // a wins
                        int loot = b.treasury / 2;
                        a.treasury += loot;
                        b.treasury -= loot;
                        b.armySize = Math.max(0, b.armySize - random.nextInt(5));
                        a.armySize = Math.max(0, a.armySize - random.nextInt(3));
                    } else if (bPower > aPower) {
                        int loot = a.treasury / 2;
                        b.treasury += loot;
                        a.treasury -= loot;
                        a.armySize = Math.max(0, a.armySize - random.nextInt(5));
                        b.armySize = Math.max(0, b.armySize - random.nextInt(3));
                    } else {
                        // draw
                        a.armySize = Math.max(0, a.armySize - 2);
                        b.armySize = Math.max(0, b.armySize - 2);
                    }
                    data.setDirty();
                }
            }
        }
    }

    // Custom goals
    public static class ScheduleGoal extends Goal {
        private final Villager villager;
        private int timer;

        public ScheduleGoal(Villager villager) {
            this.villager = villager;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return villager.getRandom().nextInt(100) == 0;
        }

        @Override
        public void tick() {
            long time = villager.level().getDayTime() % 24000;
            if (time < 6000) {
                // morning: go to work (if has job site)
                var jobSite = villager.getVillagerData().getJobSite();
                if (jobSite != null) {
                    villager.getNavigation().moveTo(jobSite.getX(), jobSite.getY(), jobSite.getZ(), 0.6);
                }
            } else if (time > 12000 && time < 13000) {
                // evening: go to bell
                // find bell nearby
            } else if (time > 18000) {
                // night: go to bed
                // find bed
            }
        }
    }

    public static class SoldierAttackGoal extends Goal {
        private final Villager soldier;
        private final double speedModifier;
        private final boolean followingTargetEvenIfNotSeen;

        public SoldierAttackGoal(Villager villager, double speed, boolean followEvenIfNotSeen) {
            this.soldier = villager;
            this.speedModifier = speed;
            this.followingTargetEvenIfNotSeen = followEvenIfNotSeen;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return soldier.getTarget() != null && soldier.getTarget().isAlive();
        }

        @Override
        public void tick() {
            var target = soldier.getTarget();
            if (target != null) {
                soldier.getNavigation().moveTo(target, speedModifier);
                if (soldier.distanceToSqr(target) < 4.0) {
                    soldier.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                    target.hurt(target.damageSources().mobAttack(soldier), 2.0f);
                }
            }
        }
    }
}