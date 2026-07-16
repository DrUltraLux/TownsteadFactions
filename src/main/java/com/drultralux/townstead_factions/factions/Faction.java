package com.drultralux.townstead_factions.factions;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import java.util.*;

public class Faction {
    private final String factionID;
    private String cleanName;
    private final Map<UUID, String> members = new HashMap<>(); // UUID -> Rank string ("LEADER", "MEMBER")
    private CompoundTag bannerPattern = new CompoundTag();
    private UUID treasuryAccountUUID;

    // Resource Placeholders
    private int cogsTreasury = 0;
    private int foodSupplies = 0;
    private int manaStockpile = 0;

    public Faction(String factionID, String cleanName, UUID creatorUUID) {
        this.factionID = factionID;
        this.cleanName = cleanName;
        this.members.put(creatorUUID, "LEADER"); // First player always defaults to a leader
        this.treasuryAccountUUID = UUID.randomUUID();
    }

    public Faction(CompoundTag tag) {
        this.factionID = tag.getString("FactionID");
        this.cleanName = tag.getString("CleanName");
        this.cogsTreasury = tag.getInt("CogsTreasury");
        this.foodSupplies = tag.getInt("FoodSupplies");
        this.manaStockpile = tag.getInt("ManaStockpile");
        if (tag.contains("BannerPattern")) {
            this.bannerPattern = tag.getCompound("BannerPattern");
        }

        ListTag memberList = tag.getList("MembersList", Tag.TAG_COMPOUND);
        for (int i = 0; i < memberList.size(); i++) {
            CompoundTag mTag = memberList.getCompound(i);
            this.members.put(mTag.getUUID("UUID"), mTag.getString("Rank"));
        }
    }

    public CompoundTag saveToNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("FactionID", factionID);
        tag.putString("CleanName", cleanName);
        tag.putInt("CogsTreasury", cogsTreasury);
        tag.putInt("FoodSupplies", foodSupplies);
        tag.putInt("ManaStockpile", manaStockpile);
        tag.put("BannerPattern", bannerPattern);

        ListTag memberList = new ListTag();
        for (Map.Entry<UUID, String> entry : members.entrySet()) {
            CompoundTag mTag = new CompoundTag();
            mTag.putUUID("UUID", entry.getKey());
            mTag.putString("Rank", entry.getValue());
            memberList.add(mTag);
        }
        tag.put("MembersList", memberList);
        return tag;
    }

    public boolean isLeader(UUID uuid) {
        return "LEADER".equals(members.get(uuid));
    }

    public void addLeader(UUID operator, UUID target) {
        if (isLeader(operator)) {
            members.put(target, "LEADER");
        }
    }

    public void resignToMember(UUID operator) {
        if (isLeader(operator)) {
            long leaderCount = members.values().stream().filter(r -> r.equals("LEADER")).count();
            if (leaderCount > 1) { // Prevent leaving a faction with no leaders
                members.put(operator, "MEMBER");
            }
        }
    }

    public void updateBanner(UUID operator, CompoundTag patternTag) {
        if (isLeader(operator)) {
            this.bannerPattern = patternTag.copy();
        }
    }

    public java.util.UUID getTreasuryAccountUUID() {
        if (this.factionID == null || this.factionID.isEmpty()) {
            return java.util.UUID.fromString("00000000-0000-0000-0000-000000000000");
        }
        return java.util.UUID.nameUUIDFromBytes(("townstead_faction:" + this.factionID).getBytes());
    }

    public void setTreasuryAccountUUID(UUID uuid) {
        this.treasuryAccountUUID = uuid;
    }

    // --- Getters & Setters ---
    public String getFactionID() { return factionID; }
    public String getCleanName() { return cleanName; }
    public Map<UUID, String> getMembersMap() { return members; }
    public CompoundTag getBannerPattern() { return bannerPattern; }
    public int getCogs() { return cogsTreasury; }
    public void setCogs(int val) { this.cogsTreasury = val; }
    public int getFood() { return foodSupplies; }
    public void setFood(int val) { this.foodSupplies = val; }
    public int getMana() { return manaStockpile; }
    public void setMana(int val) { this.manaStockpile = val; }
}