package com.example.kingdommod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.core.BlockPos;
import net.minecraftforge.server.command.EnumArgument;
import java.util.Collection;

public class KingdomCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("kingdom")
            .then(Commands.literal("create")
                .then(Commands.argument("name", StringArgumentType.string())
                    .executes(ctx -> create(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
            .then(Commands.literal("list")
                .executes(ctx -> list(ctx.getSource())))
            .then(Commands.literal("join")
                .then(Commands.argument("name", StringArgumentType.string())
                    .executes(ctx -> join(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
            .then(Commands.literal("leave")
                .executes(ctx -> leave(ctx.getSource())))
            .then(Commands.literal("setrole")
                .then(Commands.argument("role", StringArgumentType.string())
                    .executes(ctx -> setRole(ctx.getSource(), StringArgumentType.getString(ctx, "role")))))
            .then(Commands.literal("reputation")
                .then(Commands.argument("player", StringArgumentType.string())
                    .then(Commands.argument("kingdom", StringArgumentType.string())
                        .then(Commands.argument("amount", IntegerArgumentType.integer())
                            .executes(ctx -> setReputation(ctx.getSource(), StringArgumentType.getString(ctx, "player"), StringArgumentType.getString(ctx, "kingdom"), IntegerArgumentType.getInteger(ctx, "amount")))))))
            .then(Commands.literal("paytaxes")
                .executes(ctx -> payTaxes(ctx.getSource())))
        );
    }

    private static int create(CommandSourceStack source, String name) {
        if (source.getEntity() instanceof ServerPlayer player) {
            ServerLevel level = (ServerLevel) player.level();
            KingdomData data = KingdomData.get(level);
            if (data.getKingdomByName(name) != null) {
                source.sendFailure(Component.literal("Kingdom already exists!"));
                return 0;
            }
            KingdomData.Kingdom kingdom = data.createKingdom(name, player.blockPosition());
            source.sendSuccess(() -> Component.literal("Kingdom " + name + " created!"), true);
            return 1;
        }
        source.sendFailure(Component.literal("Only players can use this command."));
        return 0;
    }

    private static int list(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        KingdomData data = KingdomData.get(level);
        Collection<KingdomData.Kingdom> kingdoms = data.getAllKingdoms();
        if (kingdoms.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No kingdoms exist."), false);
        } else {
            source.sendSuccess(() -> Component.literal("Kingdoms:"), false);
            for (KingdomData.Kingdom k : kingdoms) {
                source.sendSuccess(() -> Component.literal("- " + k.name + " | Treasury: " + k.treasury + " | Army: " + k.armySize), false);
            }
        }
        return 1;
    }

    private static int join(CommandSourceStack source, String name) {
        if (source.getEntity() instanceof ServerPlayer player) {
            ServerLevel level = (ServerLevel) player.level();
            KingdomData data = KingdomData.get(level);
            KingdomData.Kingdom kingdom = data.getKingdomByName(name);
            if (kingdom == null) {
                source.sendFailure(Component.literal("Kingdom not found."));
                return 0;
            }
            KingdomData.PlayerProgress pp = data.getOrCreatePlayerProgress(player.getUUID());
            pp.joinedKingdomId = kingdom.id;
            pp.setReputation(kingdom.id, 0);
            pp.setTitle(kingdom.id, "Peasant");
            data.setPlayerProgress(player.getUUID(), pp);
            source.sendSuccess(() -> Component.literal("Joined kingdom " + kingdom.name + " as Peasant."), true);
            return 1;
        }
        return 0;
    }

    private static int leave(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            ServerLevel level = (ServerLevel) player.level();
            KingdomData data = KingdomData.get(level);
            KingdomData.PlayerProgress pp = data.getOrCreatePlayerProgress(player.getUUID());
            if (pp.joinedKingdomId == null) {
                source.sendFailure(Component.literal("You are not in a kingdom."));
                return 0;
            }
            pp.joinedKingdomId = null;
            data.setPlayerProgress(player.getUUID(), pp);
            source.sendSuccess(() -> Component.literal("You left your kingdom."), true);
            return 1;
        }
        return 0;
    }

    private static int setRole(CommandSourceStack source, String roleName) {
        if (source.getEntity() instanceof ServerPlayer player) {
            Villager villager = null;
            double closest = 5.0;
            for (Villager v : player.level().getEntitiesOfClass(Villager.class, player.getBoundingBox().inflate(5.0))) {
                double dist = player.distanceToSqr(v);
                if (dist < closest) {
                    closest = dist;
                    villager = v;
                }
            }
            if (villager == null) {
                source.sendFailure(Component.literal("No villager nearby."));
                return 0;
            }
            KingdomRoles role = KingdomRoles.fromString(roleName);
            KingdomRoles.setRole(villager, role);
            source.sendSuccess(() -> Component.literal("Set role to " + role.name()), true);
            return 1;
        }
        return 0;
    }

    private static int setReputation(CommandSourceStack source, String playerName, String kingdomName, int amount) {
        ServerPlayer target = source.getServer().getPlayerList().getPlayerByName(playerName);
        if (target == null) {
            source.sendFailure(Component.literal("Player not found."));
            return 0;
        }
        ServerLevel level = (ServerLevel) target.level();
        KingdomData data = KingdomData.get(level);
        KingdomData.Kingdom kingdom = data.getKingdomByName(kingdomName);
        if (kingdom == null) {
            source.sendFailure(Component.literal("Kingdom not found."));
            return 0;
        }
        KingdomData.PlayerProgress pp = data.getOrCreatePlayerProgress(target.getUUID());
        int current = pp.getReputation(kingdom.id);
        pp.setReputation(kingdom.id, current + amount);
        updateTitle(pp, kingdom.id);
        data.setPlayerProgress(target.getUUID(), pp);
        source.sendSuccess(() -> Component.literal("Reputation updated."), true);
        return 1;
    }

    private static int payTaxes(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            ServerLevel level = (ServerLevel) player.level();
            KingdomData data = KingdomData.get(level);
            KingdomData.PlayerProgress pp = data.getOrCreatePlayerProgress(player.getUUID());
            if (pp.joinedKingdomId == null) {
                source.sendFailure(Component.literal("You are not in a kingdom."));
                return 0;
            }
            KingdomData.Kingdom kingdom = data.getKingdom(pp.joinedKingdomId);
            if (kingdom == null) {
                source.sendFailure(Component.literal("Your kingdom no longer exists."));
                return 0;
            }
            // Pay 10 emeralds (or any currency) - we just add reputation and treasury
            kingdom.treasury += 10;
            int newRep = pp.getReputation(kingdom.id) + 5;
            pp.setReputation(kingdom.id, newRep);
            updateTitle(pp, kingdom.id);
            data.setPlayerProgress(player.getUUID(), pp);
            data.setDirty();
            source.sendSuccess(() -> Component.literal("Paid taxes. Reputation increased. New reputation: " + newRep), true);
            return 1;
        }
        return 0;
    }

    private static void updateTitle(KingdomData.PlayerProgress pp, UUID kingdomId) {
        int rep = pp.getReputation(kingdomId);
        String title;
        if (rep >= 1000) title = "Royal Advisor";
        else if (rep >= 600) title = "Noble";
        else if (rep >= 300) title = "Knight";
        else if (rep >= 100) title = "Citizen";
        else title = "Peasant";
        pp.setTitle(kingdomId, title);
    }
}