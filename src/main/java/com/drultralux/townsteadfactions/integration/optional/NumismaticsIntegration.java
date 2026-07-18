package com.drultralux.townsteadfactions.integration.optional;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.fml.ModList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Provides optional, reflection-based integration with the Create:
 * Numismatics mod, allowing faction treasuries to be backed by a real
 * Numismatics bank account when that mod is installed.
 *
 * <p>All operations degrade gracefully (returning a fallback value, or
 * doing nothing) if Numismatics is not present, or if its API doesn't
 * match what this class expects.</p>
 */
public class NumismaticsIntegration {

    /** The mod ID used to detect whether Create: Numismatics is loaded. */
    private static final String MOD_ID = "create_numismatics";

    /** Cached result of the mod-presence check, or {@code null} if not yet checked. */
    private static Boolean isLoaded = null;

    /** The reflectively loaded Numismatics bank accounts class, or {@code null} if unavailable. */
    private static Class<?> bankAccountsClass = null;

    /** Reflective handle for reading a bank account's balance. */
    private static Method getBalanceMethod = null;

    /** Reflective handle for adjusting a bank account's balance. */
    private static Method modifyBalanceMethod = null;

    /** Whether {@link #initReflection()} has already run. */
    private static boolean reflectionInitialized = false;

    /**
     * Checks whether the Create: Numismatics mod is currently loaded. The
     * result is cached after the first call.
     *
     * @return {@code true} if Numismatics is loaded
     */
    public static boolean isModPresent() {
        if (isLoaded == null) {
            isLoaded = ModList.get().isLoaded(MOD_ID);
        }
        return isLoaded;
    }

    /**
     * Resolves the Numismatics bank accounts class and its methods via
     * reflection, if the mod is present. Safe to call multiple times; only
     * runs once. On any failure, all cached method handles are reset to
     * {@code null} so lookups fail closed rather than throw.
     */
    private static void initReflection() {
        if (reflectionInitialized) return;
        reflectionInitialized = true;

        if (!isModPresent()) return;

        try {
            bankAccountsClass = Class.forName("com.layersofrailways.createnumismatics.common.data.BankAccounts");

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
     * Reads a faction treasury's balance from its linked Numismatics bank
     * account, if available.
     *
     * @param treasuryUUID the UUID of the faction's treasury bank account
     * @param fallbackValue the value to return if Numismatics isn't present
     *                      or the lookup fails
     * @return the account balance, or {@code fallbackValue} if unavailable
     */
    public static int getFactionCogsBalance(UUID treasuryUUID, int fallbackValue) {
        if (!isModPresent()) return fallbackValue;
        initReflection();

        if (getBalanceMethod == null) return fallbackValue;

        try {
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
     * Sets a faction treasury's linked Numismatics bank account balance to
     * the given amount, if Numismatics is present. Does nothing if the mod
     * isn't loaded or the current balance can't be read.
     *
     * @param treasuryUUID the UUID of the faction's treasury bank account
     * @param newAmount the balance to set
     */
    public static void setFactionCogsBalance(UUID treasuryUUID, int newAmount) {
        if (!isModPresent()) return;
        initReflection();

        if (getBalanceMethod == null || modifyBalanceMethod == null) return;

        try {
            long currentBalance = (Long) getBalanceMethod.invoke(null, treasuryUUID);
            long adjustment = (long) newAmount - currentBalance;

            if (adjustment != 0) {
                modifyBalanceMethod.invoke(null, treasuryUUID, adjustment);
            }
        } catch (Exception e) {
            // Fail safely without disrupting adjacent logic
        }
    }

    /**
     * Gives a player a Numismatics bank card linked to the given faction
     * treasury UUID, if Numismatics is present and its bank card item is
     * registered. Does nothing otherwise.
     *
     * @param player the player to give the card to
     * @param treasuryUUID the UUID of the faction's treasury bank account
     */
    public static void giveFactionBankCard(ServerPlayer player, UUID treasuryUUID) {
        if (!isModPresent()) return;

        ResourceLocation cardItemId = ResourceLocation.fromNamespaceAndPath(MOD_ID, "bank_card");
        Item bankCardItem = BuiltInRegistries.ITEM.get(cardItemId);

        if (bankCardItem != Items.AIR) {
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