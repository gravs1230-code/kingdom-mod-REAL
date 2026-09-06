package com.example.kingdommod;

import com.example.kingdommod.kingdom.Kingdom;
import com.example.kingdommod.kingdom.KingdomSavedData;
import com.example.kingdommod.npc.KingdomRole;
import com.example.kingdommod.npc.VillagerRoleCapability;
import com.example.kingdommod.player.PlayerKingdomCapability;
import com.example.kingdommod.player.PlayerKingdomData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraftforge.common.util.LazyOptional;

public class KingdomCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("kingdom")
            .then(Commands.literal("create")
                .then(Commands.argument("name", StringArgumentType.string())
                    .executes(ctx -> createKingdom(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
            .then(Commands.literal("setrole")
                .then(Commands.argument("role", StringArgumentType.string())
                    .executes(ctx -> setRole(ctx.getSource(), StringArgumentType.getString(ctx, "role")))))
            .then(Commands.literal("reputation")
                .then(Commands.argument("player", StringArgumentType.string())
                    .then(Commands.argument("kingdom", StringArgumentType.string())
                        .then(Commands.argument("amount", IntegerArgumentType.integer())
                            .executes(ctx -> setReputation(ctx.getSource(), StringArgumentType.getString(ctx, "player"), StringArgumentType.getString(ctx, "kingdom"), IntegerArgumentType.getInteger(ctx, "amount")))))))
        );
    }

    private static int createKingdom(CommandSourceStack source, String name) {
        if (source.getEntity() instanceof ServerPlayer player) {
            ServerLevel level = (ServerLevel) player.level();
            KingdomSavedData data = KingdomSavedData.get(level);
            BlockPos pos = player.blockPosition();
            Kingdom kingdom = data.createKingdom(name, pos);
            source.sendSuccess(() -> Component.literal("Kingdom " + name + " created with ID " + kingdom.getId().toString().substring(0, 8)), true);
            return 1;
        }
        return 0;
    }

    private static int setRole(CommandSourceStack source, String roleName) {
        if (source.getEntity() instanceof ServerPlayer player) {
            // Find nearest villager
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
            KingdomRole role = KingdomRole.valueOf(roleName.toUpperCase());
            LazyOptional<KingdomRole> cap = villager.getCapability(VillagerRoleCapability.ROLE);
            cap.ifPresent(r -> {
                // Cannot set value on immutable enum? We'll use a mutable holder later.
                // For now, we'll just send a message.
            });
            // Since our capability provider stores a mutable field, we need to set it.
            // This is a simplified version; actual code would need a setter.
            source.sendSuccess(() -> Component.literal("Role set to " + role + " (but currently not saved due to capability design)"), true);
            return 1;
        }
        return 0;
    }

    private static int setReputation(CommandSourceStack source, String playerName, String kingdomName, int amount) {
        // Simplified: find player by name and modify reputation
        ServerPlayer target = source.getServer().getPlayerList().getPlayerByName(playerName);
        if (target == null) {
            source.sendFailure(Component.literal("Player not found."));
            return 0;
        }
        LazyOptional<PlayerKingdomData> cap = target.getCapability(PlayerKingdomCapability.PLAYER_DATA);
        cap.ifPresent(data -> {
            // For demo, we'll just print a message. Actual code would store by kingdom ID.
        });
        source.sendSuccess(() -> Component.literal("Reputation set (not fully implemented in this simplified version)."), true);
        return 1;
    }
}
