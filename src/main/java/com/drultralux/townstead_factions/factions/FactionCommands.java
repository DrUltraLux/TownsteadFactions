package com.drultralux.townstead_factions.factions;

import com.drultralux.townstead_factions.roots.OriginManager;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import com.aetherianartificer.townstead.root.PlayerRoot;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.MinecraftServer;

public class FactionCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("factions")
                // Accessible to all active players
                .requires(source -> source.getEntity() instanceof ServerPlayer)

                //factions status
                .then(Commands.literal("status")
                        .executes(context -> executeStatus(context.getSource()))
                )

                //factions update
                .then(Commands.literal("update")
                        .executes(context -> executeForceUpdate(context.getSource()))
                )

                // Admin resource additions: /factions add <resource> <factionName> <amount>
                .then(Commands.literal("add")
                        .requires(source -> source.hasPermission(2)) // Restricts command to OP Level 2+
                        .then(Commands.argument("resource", StringArgumentType.word())
                                .then(Commands.argument("factionName", StringArgumentType.string())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(context -> executeResourceModification(
                                                        context.getSource(),
                                                        "add",
                                                        StringArgumentType.getString(context, "resource"),
                                                        StringArgumentType.getString(context, "factionName"),
                                                        IntegerArgumentType.getInteger(context, "amount")
                                                ))
                                        )
                                )
                        )
                )

                // Admin resource subtractions: /factions sub <resource> <factionName> <amount>
                .then(Commands.literal("sub")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("resource", StringArgumentType.word())
                                .then(Commands.argument("factionName", StringArgumentType.string())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(context -> executeResourceModification(
                                                        context.getSource(),
                                                        "sub",
                                                        StringArgumentType.getString(context, "resource"),
                                                        StringArgumentType.getString(context, "factionName"),
                                                        IntegerArgumentType.getInteger(context, "amount")
                                                ))
                                        )
                                )
                        )
                )

                // Admin resource set to value: /factions set <resource> <factionName> <amount>
                .then(Commands.literal("set")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("resource", StringArgumentType.word())
                                .then(Commands.argument("factionName", StringArgumentType.string())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0)) // Allows 0 for complete wipes
                                                .executes(context -> executeResourceModification(
                                                        context.getSource(),
                                                        "set",
                                                        StringArgumentType.getString(context, "resource"),
                                                        StringArgumentType.getString(context, "factionName"),
                                                        IntegerArgumentType.getInteger(context, "amount")
                                                ))
                                        )
                                )
                        )
                )

                .then(Commands.literal("dump")
                        .requires(source -> source.hasPermission(2)) // Restricts to OP Level 2+
                        .then(Commands.argument("factionName", StringArgumentType.string())
                                .executes(context -> executeDumpFactionData(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "factionName")
                                ))
                        )
                )
        );
    }

    private static int executeStatus(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            String faction = FactionManager.getPlayerFaction(player.getUUID());

            String cleanOriginName = "None Chosen";
            if (PlayerRoot.hasRoot(player)) {
                String rawRootID = PlayerRoot.getRootId(player);
                cleanOriginName = OriginManager.getCleanNameForRoot(rawRootID);
            }

            player.sendSystemMessage(Component.literal("§6§l[Factions Status]§r"));
            player.sendSystemMessage(Component.literal("§7• Current Faction: §e" + faction));
            player.sendSystemMessage(Component.literal("§7• Selected Origin: §b" + cleanOriginName));
            return 1;
        }
        return 0;
    }

    private static int executeForceUpdate(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            String activeRootID = null;

            if (PlayerRoot.hasRoot(player)) {
                activeRootID = PlayerRoot.getRootId(player);
            }

            FactionManager.reconcilePlayerFaction(player, activeRootID);

            FactionManager.syncFactionDataToClient(player);

            player.sendSystemMessage(Component.literal("§a§l[Factions]§r §7Successfully re-evaluated your root properties and forced a client network cache synchronization update!"));
            return 1;
        }
        return 0;
    }

    private static int executeResourceModification(CommandSourceStack source, String operation, String resource, String factionName, int amount) {
        MinecraftServer server = source.getServer();
        FactionSavedData data = FactionSavedData.get(source.getLevel());
        Faction faction = data.getOrCreateFaction(factionName, factionName, source.getEntity().getUUID());

        if (faction == null) {
            source.sendFailure(Component.literal("§cCould not find or create a registered faction named: " + factionName));
            return 0;
        }

        String resType = resource.toLowerCase();

        int currentVal = 0;
        switch (resType) {
            case "cogs", "cog" -> currentVal = faction.getCogs();
            case "food" -> currentVal = faction.getFood();
            case "mana" -> currentVal = faction.getMana();
            default -> {
                source.sendFailure(Component.literal("§cUnknown resource type: '" + resource + "'. Use cogs, food, or mana."));
                return 0;
            }
        }

        int newVal = currentVal;
        switch (operation) {
            case "add" -> newVal = Math.min(10, currentVal + amount);
            case "sub" -> newVal = Math.max(0, currentVal - amount);
            case "set" -> newVal = Math.max(0, Math.min(10, amount));
        }

        switch (resType) {
            case "cogs", "cog" -> faction.setCogs(newVal);
            case "food" -> faction.setFood(newVal);
            case "mana" -> faction.setMana(newVal);
        }

        data.setDirty(); // Flag system to save files directly to world directories

        String actionMessage = operation.equals("add") ? "Added " + amount : operation.equals("set") ? "Set " + newVal : "Subtracted " + amount;
        String fullResponse = "§a" + actionMessage + " " + resource + " to " + faction.getCleanName() + " (New Total: " + newVal + "/10)";

        source.sendSuccess(() -> Component.literal(fullResponse), true);

        //Instantly force client screen panel interfaces to synchronize real-time variables
        for (ServerPlayer serverPlayer : server.getPlayerList().getPlayers()) {
            String checkFaction = FactionManager.getPlayerFaction(serverPlayer.getUUID());
            if (factionName.equalsIgnoreCase(checkFaction)) {
                FactionManager.syncFactionDataToClient(serverPlayer);
            }
        }

        return 1;
    }

    private static int executeDumpFactionData(CommandSourceStack source, String factionName) {
        FactionSavedData data = FactionSavedData.get(source.getLevel());
        Faction faction = data.getFaction(factionName);

        if (faction == null) {
            source.sendFailure(Component.literal("§c[Factions Dump] Could not find a registered database entry for: " + factionName));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("§6§l=== Factions Database Dump: " + faction.getCleanName() + " ==="), false);
        source.sendSuccess(() -> Component.literal("§7• Lowercase System ID: §e" + faction.getFactionID()), false);
        source.sendSuccess(() -> Component.literal("§7• Registered Leaders/Members Size: §b" + faction.getMembersMap().size()), false);
        source.sendSuccess(() -> Component.literal("§7• Stored Treasury Cogs Count: §6" + faction.getCogs()), false);
        source.sendSuccess(() -> Component.literal("§7• Stored Stockpile Food Count: §a" + faction.getFood()), false);
        source.sendSuccess(() -> Component.literal("§7• Stored Mana Core Count: §9" + faction.getMana()), false);
        source.sendSuccess(() -> Component.literal("§7• Custom Banner Pattern NBT Length: §d" + (faction.getBannerPattern() != null ? faction.getBannerPattern().toString().length() : "0") + " bytes"), false);
        source.sendSuccess(() -> Component.literal("§6§l===================================="), false);

        return 1;
    }
}