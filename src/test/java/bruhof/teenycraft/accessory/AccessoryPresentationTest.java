package bruhof.teenycraft.accessory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessoryPresentationTest {
    @Test
    void everyRegisteredAccessoryHasScreenCopyForImplementedTiers() {
        for (String accessoryId : AccessoryRegistry.getIds()) {
            assertFalse(AccessoryPresentation.description(accessoryId).isBlank(), accessoryId);
            assertFalse(AccessoryPresentation.role(accessoryId).isBlank(), accessoryId);
            for (int tier = 1; tier <= 4; tier++) {
                assertFalse(AccessoryPresentation.tierUpgrade(accessoryId, tier).isBlank(),
                        accessoryId + " tier " + tier);
            }
        }
    }

    @Test
    void tierTwoMilestoneCopyUsesItsBalanceTarget() {
        AccessoryMilestoneDefinition definition = AccessoryMilestoneRegistry.get("mother_box_tier_2");

        String milestone = AccessoryPresentation.milestone(definition);

        assertTrue(milestone.contains(Long.toString(definition.target())));
        assertTrue(milestone.toLowerCase().contains("activate"));
    }

    @Test
    void unresolvedMilestonesAreClearlyUnavailable() {
        assertTrue(AccessoryPresentation.milestone(null).contains("not been designed"));
    }
}
