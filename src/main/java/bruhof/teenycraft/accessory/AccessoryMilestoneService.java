package bruhof.teenycraft.accessory;

import bruhof.teenycraft.capability.AccessoryMasteryProvider;
import bruhof.teenycraft.capability.IAccessoryMastery;
import bruhof.teenycraft.capability.IBattleState;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class AccessoryMilestoneService {
    private AccessoryMilestoneService() {
    }

    public static void beginBattle(IBattleState state) {
        state.getAccessoryBattleProgressTracker().beginBattle();
    }

    public static void beginActivation(IBattleState state, Player owner, ResolvedAccessorySpec spec) {
        state.getAccessoryBattleProgressTracker().beginActivation(spec.id(), spec.tier());
        record(state, owner, AccessoryContribution.count(spec.id(), AccessoryContribution.Type.ACTIVATION));
    }

    public static void record(IBattleState state, Player owner, AccessoryContribution contribution) {
        state.getAccessoryBattleProgressTracker().record(contribution);
        if (!(owner instanceof ServerPlayer serverPlayer)) {
            return;
        }
        serverPlayer.getCapability(AccessoryMasteryProvider.ACCESSORY_MASTERY).ifPresent(mastery ->
                evaluateContribution(serverPlayer, mastery, contribution));
    }

    public static void endActivation(IBattleState state, Player owner) {
        AccessoryProgressSnapshot activation = state.getAccessoryBattleProgressTracker().endActivation();
        if (!(owner instanceof ServerPlayer serverPlayer) || activation.accessoryId().isEmpty()) {
            return;
        }
        serverPlayer.getCapability(AccessoryMasteryProvider.ACCESSORY_MASTERY).ifPresent(mastery -> {
            AccessoryMilestoneDefinition definition = currentDefinition(mastery, activation.accessoryId());
            if (definition == null || definition.trigger() != AccessoryMilestoneDefinition.Trigger.ACTIVATION_END) {
                return;
            }
            long increment = definition.evaluator().applyAsLong(
                    new AccessoryMilestoneDefinition.Evaluation(null, activation, null, false));
            applyProgress(serverPlayer, mastery, definition, increment);
        });
    }

    public static void finishBattle(IBattleState state, Player owner, boolean won, boolean abandoned) {
        AccessoryBattleProgressTracker tracker = state.getAccessoryBattleProgressTracker();
        AccessoryProgressSnapshot battle = tracker.battleSnapshot();
        try {
            if (abandoned || !(owner instanceof ServerPlayer serverPlayer) || battle.accessoryId().isEmpty()) {
                return;
            }
            serverPlayer.getCapability(AccessoryMasteryProvider.ACCESSORY_MASTERY).ifPresent(mastery -> {
                AccessoryMilestoneDefinition definition = currentDefinition(mastery, battle.accessoryId());
                if (definition == null || definition.trigger() != AccessoryMilestoneDefinition.Trigger.BATTLE_END
                        || definition.requiresWin() && !won) {
                    return;
                }
                long increment = definition.evaluator().applyAsLong(
                        new AccessoryMilestoneDefinition.Evaluation(null, null, battle, won));
                applyProgress(serverPlayer, mastery, definition, increment);
            });
        } finally {
            tracker.reset();
        }
    }

    private static void evaluateContribution(ServerPlayer player, IAccessoryMastery mastery,
                                               AccessoryContribution contribution) {
        AccessoryMilestoneDefinition definition = currentDefinition(mastery, contribution.accessoryId());
        if (definition == null || definition.trigger() != AccessoryMilestoneDefinition.Trigger.CONTRIBUTION) {
            return;
        }
        long increment = definition.evaluator().applyAsLong(
                new AccessoryMilestoneDefinition.Evaluation(contribution, null, null, false));
        applyProgress(player, mastery, definition, increment);
    }

    private static AccessoryMilestoneDefinition currentDefinition(IAccessoryMastery mastery, String accessoryId) {
        if (mastery.isCurrentMilestoneComplete(accessoryId)) {
            return null;
        }
        return AccessoryMilestoneRegistry.get(mastery.getCurrentMilestoneId(accessoryId));
    }

    private static void applyProgress(ServerPlayer player, IAccessoryMastery mastery,
                                      AccessoryMilestoneDefinition definition, long increment) {
        if (increment <= 0) {
            return;
        }
        long current = mastery.getMilestoneProgress(definition.accessoryId());
        long next = current > Long.MAX_VALUE - increment ? Long.MAX_VALUE : current + increment;
        next = Math.min(definition.target(), next);
        mastery.setMilestoneProgress(definition.accessoryId(), definition.id(), next);

        boolean completedNow = next >= definition.target()
                && mastery.completeMilestone(definition.accessoryId(), definition.id());
        AccessoryMasteryService.syncMastery(player, mastery);
        if (completedNow) {
            player.sendSystemMessage(Component.literal(
                    "Accessory milestone complete: " + definition.id() + ". The next tier can now be purchased."));
        }
    }
}
