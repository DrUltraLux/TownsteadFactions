package com.drultralux.townsteadfactions.factions;

import com.drultralux.townsteadfactions.LogManager;
import com.drultralux.townsteadfactions.roots.OriginManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.Map;
import java.util.UUID;

/**
 * Centrally coordinates registration and routing for player and administrative command trees.
 * Enforces strict permission splits and unified resource math operations natively.
 */
public class FactionCommands {

    /**
     * Registers the total /factions and short-form /f branch commands onto the gameplay bus dispatcher.
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Build the root nodes for both long-form and short-form variants concurrently
        var factionsRoot = Commands.literal("factions");
        var shortRoot = Commands.literal("f");

        // ==========================================
        //         PLAYER BRANCH (ALL LEVELS)
        // ==========================================

        var displayNode = Commands.literal("display")
                .executes(context -> executeDisplay(context.getSource()));

        var updateNode = Commands.literal("update")
                .executes(context -> executeUpdate(context.getSource()));

        factionsRoot.then(displayNode).then(updateNode);
        shortRoot.then(displayNode).then(updateNode);

        // ==========================================
        //         ADMIN BRANCH (OP PERM LEVEL 2)
        // ==========================================

        // 1. /factions add <resource> <faction> <amount>
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

        // 2. /factions sub <resource> <faction> <amount>
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

        // 3. /factions set <resource> <faction> <amount>
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

        // 4. /factions showall <faction>
        var showAllNode = Commands.literal("showall")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("faction", StringArgumentType.string())
                        .executes(context -> executeShowAll(context.getSource(), StringArgumentType.getString(context, "faction")))
                );

        // 5. /factions listall
        var listAllNode = Commands.literal("listall")
                .requires(source -> source.hasPermission(2))
                .executes(context -> executeListAll(context.getSource()));

        // Wire the admin commands straight onto the root literal extensions cleanly
        factionsRoot.then(addNode).then(subNode).then(setNode).then(showAllNode).then(listAllNode);
        shortRoot.then(addNode).then(subNode).then(setNode).then(showAllNode).then(listAllNode);

        // Register both base endpoints to the master server dispatcher bus context
        dispatcher.register(factionsRoot);
        dispatcher.register(shortRoot);
    }

    // ==========================================
    //         EXECUTION HANDLER METHODS
    // ==========================================

    private static int executeDisplay(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            UUID playerUUID = player.getUUID();

            String playerRootId = com.aetherianartificer.townstead.root.PlayerRoot.getRootId(player);
            String cleanOriginName = OriginManager.getCleanName(playerRootId);

            Faction assignedFaction = null;
            Map<String, Faction> activeMap = FactionManager.getInstance().getActiveFactions();
            for (Faction faction : activeMap.values()) {
                if (faction.getMembers() != null && faction.getMembers().contains(playerUUID)) {
                    assignedFaction = faction;
                    break;
                }
            }

            String factionDisplay = (assignedFaction != null) ? assignedFaction.getDisplayName() : "None Assigned";

            source.sendSuccess(() -> Component.literal("§b--- Your Profile Parameters ---" +
                    "\n§7Selected rootID: §f" + playerRootId +
                    "\n§7Clean Origin: §f" + cleanOriginName +
                    "\n§7Assigned Faction: §a" + factionDisplay), false);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("This command can only be executed by a live player in-game."));
            return 0;
        }
    }

    private static int executeUpdate(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            LogManager.info("Forcing runtime origin profile reconciliation sync for player: " + player.getName().getString());

            OriginManager.fetchInitialRootID(player);
            com.drultralux.townsteadfactions.network.FactionPacketManager.sendFactionDataToClient(player);

            source.sendSuccess(() -> Component.literal("§aSuccessfully updated and re-synchronized faction alignments against config states."), false);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to complete forced profile update checkpoint maps."));
            return 0;
        }
    }

    private static int executeResourceMath(CommandSourceStack source, String rawResource, String factionId, int amount, MathOp operation) {
        if (factionId == null || rawResource == null) return 0;

        Faction faction = FactionManager.getInstance().getActiveFactions().get(factionId.trim());
        if (faction == null) {
            source.sendFailure(Component.literal("Error: Faction identifier '" + factionId + "' does not match active live setups."));
            return 0;
        }

        String targetResource = rawResource.toLowerCase().trim();
        int currentBalance;

        if (targetResource.equals("cogs") || targetResource.equals("cog")) {
            currentBalance = faction.getCogs();
            faction.setCogs(calculateTargetValue(currentBalance, amount, operation));
        } else if (targetResource.equals("food")) {
            currentBalance = faction.getFood();
            faction.setFood(calculateTargetValue(currentBalance, amount, operation));
        } else if (targetResource.equals("mana")) {
            currentBalance = faction.getMana();
            faction.setMana(calculateTargetValue(currentBalance, amount, operation));
        } else {
            source.sendFailure(Component.literal("Invalid resource target. Supported metrics: cogs, food, mana"));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("§a[Admin] Successfully processed " + operation.name() + " operation on faction §f" + faction.getDisplayName() + " §a" + targetResource), true);
        return 1;
    }

    private static int executeShowAll(CommandSourceStack source, String factionId) {
        if (factionId == null) return 0;

        Faction faction = FactionManager.getInstance().getActiveFactions().get(factionId.trim());
        if (faction == null) {
            source.sendFailure(Component.literal("Specified Faction ID could not be matched against active arrays."));
            return 0;
        }

        StringBuilder originsBuilder = new StringBuilder();
        for (String rootId : faction.getValidOrigins()) {
            originsBuilder.append("\n §7- ").append(rootId).append(" (§e").append(OriginManager.getCleanName(rootId)).append("§7)");
        }

        source.sendSuccess(() -> Component.literal("§6=== Faction Comprehensive Review: " + faction.getDisplayName() + " ===" +
                "\n§7Internal Unique ID: §f" + faction.getId() +
                "\n§7System LeaderUUID: §f" + faction.getLeaderUUID() +
                "\n§bBalances Matrix:" +
                "\n §7• Cogs: §f" + faction.getCogs() + "  §7• Food: §f" + faction.getFood() + "  §7• Mana: §f" + faction.getMana() +
                "\n§dRegistered Allowed Origins:" + originsBuilder.toString() +
                "\n§7Active Live Members Loaded: §b" + faction.getMembers().size()), true);
        return 1;
    }

    private static int executeListAll(CommandSourceStack source) {
        Map<String, Faction> activeMap = FactionManager.getInstance().getActiveFactions();
        if (activeMap.isEmpty()) {
            source.sendSuccess(() -> Component.literal("There are currently zero active live factions registered in environment mappings."), true);
            return 1;
        }

        StringBuilder listBuilder = new StringBuilder("§6=== Active Registered Server Factions (" + activeMap.size() + ") ===");
        for (Faction faction : activeMap.values()) {
            listBuilder.append("\n §7• §f").append(faction.getId()).append(" §7-> Title: §a").append(faction.getDisplayName()).append(" §7(Members: §b").append(faction.getMembers().size()).append("§7)");
        }

        source.sendSuccess(() -> Component.literal(listBuilder.toString()), true);
        return 1;
    }

    private static int calculateTargetValue(int current, int modifier, MathOp op) {
        switch (op) {
            case ADD: return current + modifier;
            case SUB: return current - modifier;
            case SET: return modifier;
            default: return current;
        }
    }

    private enum MathOp {
        ADD, SUB, SET
    }
}