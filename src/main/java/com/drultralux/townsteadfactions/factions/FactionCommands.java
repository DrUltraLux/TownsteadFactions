package com.drultralux.townsteadfactions.factions;

import com.aetherianartificer.townstead.root.PlayerRoot;
import com.drultralux.townsteadfactions.utils.LogManager;
import com.drultralux.townsteadfactions.network.FactionPacketManager;
import com.drultralux.townsteadfactions.integration.required.OriginManager;
import com.drultralux.townsteadfactions.layout.LayoutResetManager;
import com.drultralux.townsteadfactions.network.FactionPacketActions;
import com.mojang.authlib.GameProfile;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.nbt.CompoundTag;
import java.util.Collection;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;

/**
 * Registers and handles the {@code /factions} and {@code /f} command
 * trees: player-facing profile commands, and OP-only administrative
 * resource and listing commands.
 */
public class FactionCommands {

    /**
     * Registers both the {@code /factions} and {@code /f} command trees
     * onto the given dispatcher.
     *
     * @param dispatcher the command dispatcher to register onto
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var factionsRoot = Commands.literal("factions");
        var shortRoot = Commands.literal("f");

        // --- PLAYER BRANCH (all permission levels) ---

        var displayNode = Commands.literal("display")
                .executes(context -> executeDisplay(context.getSource()));

        var updateNode = Commands.literal("update")
                .executes(context -> executeUpdate(context.getSource()));

        var resetLayoutNode = Commands.literal("resetlayout")
                .executes(context -> executeResetLayoutSelf(context.getSource()))
                .then(Commands.literal("all")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> executeResetLayoutAll(context.getSource()))
                )
                .then(Commands.argument("player", GameProfileArgument.gameProfile())
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> executeResetLayoutPlayer(context.getSource(), GameProfileArgument.getGameProfiles(context, "player")))
                );

        factionsRoot.then(resetLayoutNode);
        shortRoot.then(resetLayoutNode);

        // --- ADMIN BRANCH (OP permission level 2) ---

        // /factions add <resource> <faction> <amount>
        var addNode = Commands.literal("add")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("resource", StringArgumentType.string())
                        .then(Commands.argument("faction", StringArgumentType.string())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0, 100000))
                                        .executes(context -> executeResourceMath(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "resource"),
                                                StringArgumentType.getString(context, "faction"),
                                                IntegerArgumentType.getInteger(context, "amount"),
                                                MathOp.ADD
                                        ))
                                )
                        )
                );

        // /factions sub <resource> <faction> <amount>
        var subNode = Commands.literal("sub")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("resource", StringArgumentType.string())
                        .then(Commands.argument("faction", StringArgumentType.string())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0, 100000))
                                        .executes(context -> executeResourceMath(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "resource"),
                                                StringArgumentType.getString(context, "faction"),
                                                IntegerArgumentType.getInteger(context, "amount"),
                                                MathOp.SUB
                                        ))
                                )
                        )
                );

        // /factions set <resource> <faction> <amount>
        var setNode = Commands.literal("set")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("resource", StringArgumentType.string())
                        .then(Commands.argument("faction", StringArgumentType.string())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(-10000, 100000))
                                        .executes(context -> executeResourceMath(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "resource"),
                                                StringArgumentType.getString(context, "faction"),
                                                IntegerArgumentType.getInteger(context, "amount"),
                                                MathOp.SET
                                        ))
                                )
                        )
                );

        // /factions showall <faction>
        var showAllNode = Commands.literal("showall")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("faction", StringArgumentType.string())
                        .executes(context -> executeShowAll(context.getSource(), StringArgumentType.getString(context, "faction")))
                );

        // /factions listall
        var listAllNode = Commands.literal("listall")
                .requires(source -> source.hasPermission(2))
                .executes(context -> executeListAll(context.getSource()));

        factionsRoot.then(addNode).then(subNode).then(setNode).then(showAllNode).then(listAllNode);
        shortRoot.then(addNode).then(subNode).then(setNode).then(showAllNode).then(listAllNode);

        dispatcher.register(factionsRoot);
        dispatcher.register(shortRoot);
    }

    // --- EXECUTION HANDLER METHODS ---

    /**
     * Handles {@code /factions display}: shows the executing player their
     * origin and assigned faction.
     *
     * @param source the command source
     * @return {@code 1} on success, {@code 0} if not run by a player
     */
    private static int executeDisplay(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            UUID playerUUID = player.getUUID();
            String playerRootId = PlayerRoot.getRootId(player);
            String cleanOriginName = OriginManager.getCleanName(playerRootId);

            String factionDisplay = FactionManager.getPlayerFactionDisplayName(playerUUID);

            source.sendSuccess(() -> Component.literal("§b--- Your Profile Parameters ---" +
                    "\n §7Selected rootID: §f" + playerRootId +
                    "\n §7Clean Origin: §f" + cleanOriginName +
                    "\n §7Assigned Faction: §a" + factionDisplay), false);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("This command can only be executed by a live player in-game."));
            return 0;
        }
    }

    /**
     * Handles {@code /factions update}: re-resolves the executing player's
     * origin/faction assignment and re-syncs their client cache.
     *
     * @param source the command source
     * @return {@code 1} on success, {@code 0} if not run by a player
     */
    private static int executeUpdate(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            LogManager.info("Forcing runtime origin profile reconciliation sync for player: " + player.getName().getString());

            OriginManager.fetchInitialRootID(player);
            FactionPacketManager.sendFactionDataToClient(player);

            source.sendSuccess(() -> Component.literal("§aSuccessfully updated and re-synchronized faction alignments against config states."), false);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to complete forced profile update checkpoint maps."));
            return 0;
        }
    }

    /**
     * Handles {@code /factions add|sub|set}: applies a resource math
     * operation to a faction and syncs the executing player if applicable.
     *
     * @param source the command source
     * @param rawResource the resource name to modify (e.g. {@code "cogs"})
     * @param factionId the target faction's ID
     * @param amount the amount to apply
     * @param operation which math operation to perform
     * @return {@code 1} on success, {@code 0} on validation failure
     */
    private static int executeResourceMath(CommandSourceStack source, String rawResource, String factionId, int amount, MathOp operation) {
        if (factionId == null || rawResource == null || operation == null) {
            return 0;
        }

        int resultStatus = FactionManager.executeEncapsulatedAssetMath(
                factionId,
                rawResource,
                amount,
                operation.name()
        );

        if (resultStatus == 0) {
            source.sendFailure(Component.literal("Error: Faction identifier '" + factionId + "' or resource metrics could not be validated."));
            return 0;
        }

        FactionPacketManager.broadcastFactionDelta(factionId.trim(), source.getServer());

        String targetResource = rawResource.toLowerCase().trim();
        source.sendSuccess(() -> Component.literal("§a[Admin] Successfully processed " + operation.name() + " operation on faction §f" + factionId.trim() + " §a" + targetResource), true);
        return 1;
    }

    /**
     * Handles {@code /factions showall <faction>}: shows a full resource
     * summary for the given faction.
     *
     * @param source the command source
     * @param factionId the faction's ID to summarize
     * @return {@code 1} on success, {@code 0} if the faction isn't found
     */
    private static int executeShowAll(CommandSourceStack source, String factionId) {
        if (factionId == null) {
            return 0;
        }

        String comprehensiveSummary = FactionManager.getFactionSummaryString(factionId);

        if (comprehensiveSummary == null) {
            source.sendFailure(Component.literal("Specified Faction ID could not be matched against active arrays."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(comprehensiveSummary), true);
        return 1;
    }

    /**
     * Handles {@code /factions listall}: lists a summary of every active
     * faction on the server.
     *
     * @param source the command source
     * @return always {@code 1}
     */
    private static int executeListAll(CommandSourceStack source) {
        String globalListOutput = FactionManager.buildGlobalFactionListString();

        source.sendSuccess(() -> Component.literal(globalListOutput), true);
        return 1;
    }

    /**
     * Handles {@code /factions resetlayout}: queues a dashboard layout
     * reset for the executing player, delivered immediately since they're
     * online by definition.
     *
     * @param source the command source
     * @return {@code 1} on success, {@code 0} if not run by a player
     */
    private static int executeResetLayoutSelf(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            FactionPacketManager.sendToPlayer(player, FactionPacketActions.FACTION_LAYOUT_RESET, new CompoundTag());
            source.sendSuccess(() -> Component.literal("§aYour faction dashboard layout will reset to defaults next time you open it."), false);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("This command can only be executed by a live player in-game."));
            return 0;
        }
    }

    /**
     * Handles {@code /factions resetlayout <player>}: resets the named
     * player's dashboard layout, sent immediately if they're online, or
     * queued for their next login if not.
     *
     * @param source the command source
     * @param profiles the resolved target profile(s)
     * @return always {@code 1}
     */
    private static int executeResetLayoutPlayer(CommandSourceStack source, Collection<GameProfile> profiles) {
        int affected = 0;
        for (GameProfile profile : profiles) {
            UUID targetUUID = profile.getId();
            ServerPlayer online = source.getServer().getPlayerList().getPlayer(targetUUID);
            if (online != null) {
                FactionPacketManager.sendToPlayer(online, FactionPacketActions.FACTION_LAYOUT_RESET, new CompoundTag());
            } else {
                LayoutResetManager.markPlayerPendingReset(targetUUID);
            }
            affected++;
        }
        final int count = affected;
        source.sendSuccess(() -> Component.literal("§a[Admin] Queued a layout reset for " + count + " player(s)."), true);
        return 1;
    }

    /**
     * Handles {@code /factions resetlayout all}: triggers a global
     * dashboard layout reset, sent immediately to every online player and
     * automatically applied to everyone else the next time they log in.
     *
     * @param source the command source
     * @return always {@code 1}
     */
    private static int executeResetLayoutAll(CommandSourceStack source) {
        LayoutResetManager.triggerGlobalReset();
        for (ServerPlayer online : source.getServer().getPlayerList().getPlayers()) {
            FactionPacketManager.sendToPlayer(online, FactionPacketActions.FACTION_LAYOUT_RESET, new CompoundTag());
        }
        source.sendSuccess(() -> Component.literal("§a[Admin] Triggered a global faction dashboard layout reset for all players."), true);
        return 1;
    }

    /** The resource math operations supported by the admin commands. */
    private enum MathOp {
        /** Adds the given amount to the current value. */
        ADD,
        /** Subtracts the given amount from the current value. */
        SUB,
        /** Sets the value directly to the given amount. */
        SET
    }
}