package com.drultralux.townstead_factions.factions;

import com.drultralux.townstead_factions.roots.OriginManager;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import com.aetherianartificer.townstead.root.PlayerRoot;

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
        );
    }

    private static int executeStatus(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            String faction = FactionManager.getPlayerFaction(player.getUUID());

            // Pull their clean origin name from our existing data mapping tools
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

            //Re-extract their actual active root string from Townstead's core metadata layer
            if (PlayerRoot.hasRoot(player)) {
                activeRootID = PlayerRoot.getRootId(player);
            }

            //Force the server to re-evaluate their faction based on the active JSON lists
            FactionManager.reconcilePlayerFaction(player, activeRootID);

            //Immediately broadcast a fresh network sync packet to fix their UI cache data
            FactionManager.syncFactionDataToClient(player);

            player.sendSystemMessage(Component.literal("§a§l[Factions]§r §7Successfully re-evaluated your root properties and forced a client network cache synchronization update!"));
            return 1;
        }
        return 0;
    }
}