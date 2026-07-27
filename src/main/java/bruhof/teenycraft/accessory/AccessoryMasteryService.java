package bruhof.teenycraft.accessory;

import bruhof.teenycraft.TeenyBalance;
import bruhof.teenycraft.capability.AccessoryMasteryProvider;
import bruhof.teenycraft.capability.IAccessoryMastery;
import bruhof.teenycraft.capability.ITeenyCoins;
import bruhof.teenycraft.capability.TeenyCoinsProvider;
import bruhof.teenycraft.networking.ModMessages;
import bruhof.teenycraft.networking.PacketSyncAccessoryMastery;
import bruhof.teenycraft.networking.PacketSyncTeenyCoins;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;

public final class AccessoryMasteryService {
    private static final Set<String> IMPLEMENTED_TIER_FIVE_ACCESSORIES = Set.of(
            "titans_coin",
            "mother_box",
            "bat_signal",
            "red_lantern_battery",
            "green_lantern_battery",
            "violet_lantern_battery",
            "ravens_spellbook",
            "cyborgs_waffle_shooter",
            "kryptonite",
            "birdarang",
            "supermans_underpants",
            "krypto_the_superdog"
    );

    public enum PurchaseResult {
        SUCCESS,
        UNKNOWN_ACCESSORY,
        MAX_TIER,
        MILESTONE_INCOMPLETE,
        NOT_ENOUGH_COINS,
        MASTERY_UPGRADE_UNAVAILABLE,
        MISSING_CAPABILITY
    }

    private AccessoryMasteryService() {
    }

    public static PurchaseResult purchaseNextTier(ServerPlayer player, String accessoryId) {
        if (AccessoryRegistry.get(accessoryId) == null) {
            return PurchaseResult.UNKNOWN_ACCESSORY;
        }

        IAccessoryMastery mastery = player.getCapability(AccessoryMasteryProvider.ACCESSORY_MASTERY).orElse(null);
        ITeenyCoins coins = player.getCapability(TeenyCoinsProvider.TEENY_COINS).orElse(null);
        if (mastery == null || coins == null) {
            return PurchaseResult.MISSING_CAPABILITY;
        }

        int currentTier = mastery.getTier(accessoryId);
        if (currentTier >= AccessoryProgression.MAX_TIER) {
            return PurchaseResult.MAX_TIER;
        }
        if (!isTierPurchaseImplemented(accessoryId, currentTier + 1)) {
            return PurchaseResult.MASTERY_UPGRADE_UNAVAILABLE;
        }
        if (!mastery.isCurrentMilestoneComplete(accessoryId)) {
            return PurchaseResult.MILESTONE_INCOMPLETE;
        }

        int cost = TeenyBalance.getAccessoryTierUpgradeCost(currentTier + 1);
        if (!coins.trySpendCoins(cost)) {
            return PurchaseResult.NOT_ENOUGH_COINS;
        }
        if (!mastery.unlockNextTier(accessoryId)) {
            coins.addCoins(cost);
            return PurchaseResult.MILESTONE_INCOMPLETE;
        }

        sync(player, mastery, coins);
        return PurchaseResult.SUCCESS;
    }

    public static boolean isTierPurchaseImplemented(String accessoryId, int targetTier) {
        if (targetTier >= 2 && targetTier < AccessoryProgression.MAX_TIER) {
            return true;
        }
        return targetTier == AccessoryProgression.MAX_TIER
                && IMPLEMENTED_TIER_FIVE_ACCESSORIES.contains(accessoryId);
    }

    public static void syncMastery(ServerPlayer player) {
        player.getCapability(AccessoryMasteryProvider.ACCESSORY_MASTERY)
                .ifPresent(mastery -> syncMastery(player, mastery));
    }

    public static void syncMastery(ServerPlayer player, IAccessoryMastery mastery) {
        CompoundTag tag = new CompoundTag();
        mastery.saveNBTData(tag);
        ModMessages.sendToPlayer(new PacketSyncAccessoryMastery(tag), player);
    }

    private static void sync(ServerPlayer player, IAccessoryMastery mastery, ITeenyCoins coins) {
        syncMastery(player, mastery);
        ModMessages.sendToPlayer(new PacketSyncTeenyCoins(coins.getCoins()), player);
    }
}
