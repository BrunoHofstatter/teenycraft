package bruhof.teenycraft.accessory;

import java.util.function.ToLongFunction;
import java.util.function.Predicate;

public record AccessoryMilestoneDefinition(
        String id,
        String accessoryId,
        int unlockTier,
        Trigger trigger,
        long target,
        boolean requiresWin,
        String description,
        ToLongFunction<Evaluation> evaluator
) {
    public enum Trigger {
        CONTRIBUTION,
        ACTIVATION_END,
        BATTLE_END
    }

    public record Evaluation(
            AccessoryContribution contribution,
            AccessoryProgressSnapshot activation,
            AccessoryProgressSnapshot battle,
            boolean won
    ) {
    }

    public static AccessoryMilestoneDefinition lifetimeContribution(String accessoryId, int unlockTier,
                                                                     AccessoryContribution.Type type, long target) {
        return lifetimeContribution(accessoryId, unlockTier, type, target, "Accessory milestone.",
                contribution -> contribution.amount());
    }

    public static AccessoryMilestoneDefinition lifetimeContribution(String accessoryId, int unlockTier,
                                                                     AccessoryContribution.Type type, long target,
                                                                     String description,
                                                                     ToLongFunction<AccessoryContribution> contributionValue) {
        return new AccessoryMilestoneDefinition(
                AccessoryProgression.milestoneIdForUnlock(accessoryId, unlockTier),
                accessoryId,
                unlockTier,
                Trigger.CONTRIBUTION,
                target,
                false,
                description,
                evaluation -> evaluation.contribution() != null && evaluation.contribution().type() == type
                        ? contributionValue.applyAsLong(evaluation.contribution())
                        : 0
        );
    }

    public static AccessoryMilestoneDefinition activationChallenge(String accessoryId, int unlockTier, long target,
                                                                    Predicate<AccessoryProgressSnapshot> condition) {
        return activationChallenge(accessoryId, unlockTier, target, "Accessory activation challenge.", condition);
    }

    public static AccessoryMilestoneDefinition activationChallenge(String accessoryId, int unlockTier, long target,
                                                                    String description,
                                                                    Predicate<AccessoryProgressSnapshot> condition) {
        return new AccessoryMilestoneDefinition(
                AccessoryProgression.milestoneIdForUnlock(accessoryId, unlockTier),
                accessoryId,
                unlockTier,
                Trigger.ACTIVATION_END,
                target,
                false,
                description,
                evaluation -> evaluation.activation() != null && condition.test(evaluation.activation()) ? 1 : 0
        );
    }

    public static AccessoryMilestoneDefinition qualifyingBattles(String accessoryId, int unlockTier, long target,
                                                                  boolean requiresWin,
                                                                  Predicate<AccessoryProgressSnapshot> condition) {
        return qualifyingBattles(accessoryId, unlockTier, target, requiresWin,
                "Accessory battle challenge.", condition);
    }

    public static AccessoryMilestoneDefinition qualifyingBattles(String accessoryId, int unlockTier, long target,
                                                                  boolean requiresWin, String description,
                                                                  Predicate<AccessoryProgressSnapshot> condition) {
        return new AccessoryMilestoneDefinition(
                AccessoryProgression.milestoneIdForUnlock(accessoryId, unlockTier),
                accessoryId,
                unlockTier,
                Trigger.BATTLE_END,
                target,
                requiresWin,
                description,
                evaluation -> evaluation.battle() != null && condition.test(evaluation.battle()) ? 1 : 0
        );
    }
}
