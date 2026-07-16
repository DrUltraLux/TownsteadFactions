package com.drultralux.townsteadfactions.integration;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.fml.ModList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Method;
import java.util.UUID;

public class NumismaticsIntegration {
    private static final String MOD_ID = "create_numismatics";
    private static Boolean isLoaded = null;

    // Cached Reflection Targets
    private static Class<?> bankAccountsClass = null;
    private static Method getBalanceMethod = null;
    private static Method modifyBalanceMethod = null;
    private static boolean reflectionInitialized = false;

    public static boolean isModPresent() {
        if (isLoaded == null) {
            isLoaded = ModList.get().isLoaded(MOD_ID);
        }
        return isLoaded;
    }

    /**
     * Lazy-loads Numismatics classes at runtime via reflection.
     * Prevents compile-time dependency crashes.
     */
    private static void initReflection() {
        if (reflectionInitialized) return;
        reflectionInitialized = true;

        if (!isModPresent()) return;

        try {
            // Target the official Numismatics account ledger manager class
            bankAccountsClass = Class.forName("com.layersofrailways.createnumismatics.common.data.BankAccounts");

            // Resolve target methods dynamically
            getBalanceMethod = bankAccountsClass.getMethod("getBalance", UUID.class);
            modifyBalanceMethod = bankAccountsClass.getMethod("modifyBalance", UUID.class, long.class);
        } catch (Exception e) {
            // Graceful fallback if the API signature shifts across versions
            bankAccountsClass = null;
            getBalanceMethod = null;
            modifyBalanceMethod = null;
        }
    }

    /**
     * Reads the live balance of the Faction's dummy/synthetic bank account
     */
    public static int getFactionCogsBalance(UUID treasuryUUID, int fallbackValue) {
        if (!isModPresent()) return fallbackValue;
        initReflection();

        if (getBalanceMethod == null) return fallbackValue;

        try {
            // Invokes: long balance = BankAccounts.getBalance(treasuryUUID);
            Object result = getBalanceMethod.invoke(null, treasuryUUID);
            if (result instanceof Long) {
                return ((Long) result).intValue();
            }
        } catch (Exception e) {
            // Fall back to local cogs configuration on failure
        }
        return fallbackValue;
    }

    /**
     * Force syncs updates back to the Numismatics account data ledger
     */
    public static void setFactionCogsBalance(UUID treasuryUUID, int newAmount) {
        if (!isModPresent()) return;
        initReflection();

        if (getBalanceMethod == null || modifyBalanceMethod == null) return;

        try {
            // Determine dynamic delta modification step
            long currentBalance = (Long) getBalanceMethod.invoke(null, treasuryUUID);
            long adjustment = (long) newAmount - currentBalance;

            if (adjustment != 0) {
                // Invokes: BankAccounts.modifyBalance(treasuryUUID, adjustment);
                modifyBalanceMethod.invoke(null, treasuryUUID, adjustment);
            }
        } catch (Exception e) {
            // Fail safely without disrupting adjacent logic
        }
    }

    /**
     * Generates a physical Numismatics bank card hard-coded to the Faction's shared UUID
     */
    public static void giveFactionBankCard(ServerPlayer player, UUID treasuryUUID) {
        if (!isModPresent()) return;

        ResourceLocation cardItemId = ResourceLocation.fromNamespaceAndPath(MOD_ID, "bank_card");
        Item bankCardItem = BuiltInRegistries.ITEM.get(cardItemId);

        if (bankCardItem != net.minecraft.world.item.Items.AIR) {
            ItemStack cardStack = new ItemStack(bankCardItem);

            CustomData.update(
                    DataComponents.CUSTOM_DATA,
                    cardStack,
                    tag -> tag.putUUID("uuid", treasuryUUID)
            );

            cardStack.set(
                    DataComponents.CUSTOM_NAME,
                    Component.literal("§dFaction Treasury Card")
            );

            if (!player.getInventory().add(cardStack)) {
                player.drop(cardStack, false);
            }
        }
    }
}