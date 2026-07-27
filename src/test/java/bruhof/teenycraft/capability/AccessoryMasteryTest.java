package bruhof.teenycraft.capability;

import bruhof.teenycraft.TeenyBalance;
import org.junit.jupiter.api.Test;
import net.minecraft.nbt.CompoundTag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessoryMasteryTest {
    @Test
    void newAccessoryStartsAtTierOneWithTierTwoMilestone() {
        AccessoryMastery mastery = new AccessoryMastery();

        assertEquals(1, mastery.getTier("mother_box"));
        assertEquals("mother_box_tier_2", mastery.getCurrentMilestoneId("mother_box"));
        assertEquals(0, mastery.getMilestoneProgress("mother_box"));
        assertFalse(mastery.isCurrentMilestoneComplete("mother_box"));
    }

    @Test
    void completedMilestoneRequiresSeparateTierUnlock() {
        AccessoryMastery mastery = new AccessoryMastery();
        String milestoneId = mastery.getCurrentMilestoneId("mother_box");

        assertTrue(mastery.addMilestoneProgress("mother_box", milestoneId, 3));
        assertTrue(mastery.completeMilestone("mother_box", milestoneId));
        assertEquals(1, mastery.getTier("mother_box"));
        assertTrue(mastery.isCurrentMilestoneComplete("mother_box"));

        assertTrue(mastery.unlockNextTier("mother_box"));
        assertEquals(2, mastery.getTier("mother_box"));
        assertEquals("mother_box_tier_3", mastery.getCurrentMilestoneId("mother_box"));
        assertEquals(0, mastery.getMilestoneProgress("mother_box"));
        assertFalse(mastery.isCurrentMilestoneComplete("mother_box"));
    }

    @Test
    void persistenceRetainsTierAndCurrentMilestoneState() {
        AccessoryMastery original = new AccessoryMastery();
        original.setTier("violet_lantern_battery", 3);
        String milestoneId = original.getCurrentMilestoneId("violet_lantern_battery");
        original.setMilestoneProgress("violet_lantern_battery", milestoneId, 37);
        original.completeMilestone("violet_lantern_battery", milestoneId);

        CompoundTag tag = new CompoundTag();
        original.saveNBTData(tag);

        AccessoryMastery restored = new AccessoryMastery();
        restored.loadNBTData(tag);

        assertEquals(3, restored.getTier("violet_lantern_battery"));
        assertEquals(milestoneId, restored.getCurrentMilestoneId("violet_lantern_battery"));
        assertEquals(37, restored.getMilestoneProgress("violet_lantern_battery"));
        assertTrue(restored.isCurrentMilestoneComplete("violet_lantern_battery"));
    }

    @Test
    void tierCostsMatchPlannedCurve() {
        assertEquals(250, TeenyBalance.getAccessoryTierUpgradeCost(2));
        assertEquals(500, TeenyBalance.getAccessoryTierUpgradeCost(3));
        assertEquals(1000, TeenyBalance.getAccessoryTierUpgradeCost(4));
        assertEquals(2000, TeenyBalance.getAccessoryTierUpgradeCost(5));
        assertEquals(0, TeenyBalance.getAccessoryTierUpgradeCost(1));
    }

    @Test
    void tierFivePurchasesAreEnabledPerImplementedAccessory() {
        assertTrue(bruhof.teenycraft.accessory.AccessoryMasteryService
                .isTierPurchaseImplemented("mother_box", 4));
        assertTrue(bruhof.teenycraft.accessory.AccessoryMasteryService
                .isTierPurchaseImplemented("mother_box", 5));
        assertTrue(bruhof.teenycraft.accessory.AccessoryMasteryService
                .isTierPurchaseImplemented("red_lantern_battery", 5));
        assertTrue(bruhof.teenycraft.accessory.AccessoryMasteryService
                .isTierPurchaseImplemented("violet_lantern_battery", 5));
        assertTrue(bruhof.teenycraft.accessory.AccessoryMasteryService
                .isTierPurchaseImplemented("cyborgs_waffle_shooter", 5));
        assertTrue(bruhof.teenycraft.accessory.AccessoryMasteryService
                .isTierPurchaseImplemented("kryptonite", 5));
        assertTrue(bruhof.teenycraft.accessory.AccessoryMasteryService
                .isTierPurchaseImplemented("birdarang", 5));
        assertFalse(bruhof.teenycraft.accessory.AccessoryMasteryService
                .isTierPurchaseImplemented("lil_penguin", 5));
    }
}
