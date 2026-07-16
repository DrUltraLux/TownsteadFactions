package com.drultralux.townsteadfactions.factions;

import com.drultralux.townsteadfactions.LogManager;
import com.drultralux.townsteadfactions.network.FactionPacketManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Centrally manages brigadier administrative slash commands for the Factions engine.
 * Provides granular economy overrides and social hierarchy modification pipelines.
 */
public class FactionCommands {

    /**
     * Registers unified command argument structures straight into the server dispatcher tree.
     *
     * @param dispatcher the root command dispatcher context supplied by the server engine
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LogManager.info("Registering administrative faction command routing nodes...");

        dispatcher.register(Commands.literal("f")
                .requires(source -> source.hasPermission(2)) // Restrict access to Level 2+ Server Operators
                .then(Commands.literal("admin")
                        // Core Asset Resource Modification Node Layer: /f admin resource <faction_id> <cogs|food|mana> <value>
                        .then(Commands.literal("resource")
                                .then(Commands.argument("factionId", StringArgumentType.string())
                                        .then(Commands.argument("resourceType", StringArgumentType.string())
                                                .then(Commands.argument("newValue", IntegerArgumentType.integer(0))
                                                        .executes(context -> executeResourceChange(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "factionId"),
                                                                StringArgumentType.getString(context, "resourceType"),
                                                                IntegerArgumentType.getInteger(context, "newValue")
                                                        ))
                                                )
                                        )
                                )
                        )
                        // Social Hierarchy Promotion Node Layer: /f admin roster <faction_id> <player> <title_enum_name>
                        .then(Commands.literal("roster")
                                .then(Commands.argument("factionId", StringArgumentType.string())
                                        .then(Commands.argument("targetPlayer", EntityArgument.player())
                                                .then(Commands.argument("titleRole", StringArgumentType.string())
                                                        .executes(context -> executeRosterChange(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "factionId"),
                                                                EntityArgument.getPlayer(context, "targetPlayer"),
                                                                StringArgumentType.getString(context, "titleRole")
                                                        ))
                                                )
                                        )
                                )
                        )
                )
        );
    }

    /**
     * Mutates raw currency states inside isolated faction targets and flushes updates down network streams.
     */
    private static int executeResourceChange(CommandSourceStack source, String factionId, String resType, int value) {
        LogManager.debug("Processing admin resource command request: [" + factionId + "] Type: " + resType + " -> " + value);

        Faction faction = FactionManager.getInstance().getFaction(factionId);
        if (faction == null) {
            source.sendFailure(Component.literal("Error: Specified faction ID is not registered in config."));
            return 0;
        }

        String targetType = resType.toLowerCase().trim();
        switch (targetType) {
            case "cogs", "cog" -> faction.setCogs(value);
            case "food" -> faction.setFood(value);
            case "mana" -> faction.setMana(value);
            default -> {
                source.sendFailure(Component.literal("Invalid resource target. Choose either: cogs, food, or mana."));
                return 0;
            }
        }

        source.sendSuccess(() -> Component.literal("§aSuccessfully updated " + targetType + " balance to " + value + " for " + faction.getDisplayName()), true);

        // Dynamic Pipeline Flush: Instantly sync all connected player profiles with live states
        for (ServerPlayer networkPlayer : source.getServer().getPlayerList().getPlayers()) {
            FactionPacketManager.sendFactionSyncPacket(networkPlayer);
        }
        return 1;
    }

    /**
     * Alters individual profile authority tiers securely within encapsulated faction objects.
     */
    private static int executeRosterChange(CommandSourceStack source, String factionId, ServerPlayer target, String roleString) {
        LogManager.debug("Processing admin roster modifier tracking: [" + factionId + "] Player: " + target.getName().getString() + " -> " + roleString);

        Faction faction = FactionManager.getInstance().getFaction(factionId);
        if (faction == null) {
            source.sendFailure(Component.literal("Error: Target faction ID does not exist in cached config collections."));
            return 0;
        }

        FactionTitle matchingTitle;
        try {
            matchingTitle = FactionTitle.valueOf(roleString.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal("Invalid title tier. Valid selections: LEADER, SOLDIER, MEMBER, VILLAGER."));
            return 0;
        }

        // Object-Oriented Assignment Injection
        faction.addOrUpdateMember(target.getUUID(), matchingTitle);

        // Sync Player's Local Lookup Index State to match the assignment
        FactionManager.getInstance().assignPlayerToFaction(target.getUUID(), factionId);

        source.sendSuccess(() -> Component.literal("§aSuccessfully updated roster profile for " + target.getName().getString() + " to " + matchingTitle.getDisplayName()), true);

        // Pipeline Flush: Distribute updated NBT arrays right into active client views
        for (ServerPlayer networkPlayer : source.getServer().getPlayerList().getPlayers()) {
            FactionPacketManager.sendFactionSyncPacket(networkPlayer);
        }
        return 1;
    }
}