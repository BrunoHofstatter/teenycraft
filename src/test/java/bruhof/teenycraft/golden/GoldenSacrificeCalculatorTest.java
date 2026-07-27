package bruhof.teenycraft.golden;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoldenSacrificeCalculatorTest {
    private static final GoldenSacrificeCalculator.FigureData ROBIN = figure("robin", "Martial Arts");

    @Test
    void exactDuplicateTotalsRewardLargerBatches() {
        assertEquals(5, exactTotal(1));
        assertEquals(12, exactTotal(2));
        assertEquals(20, exactTotal(3));
        assertEquals(27, exactTotal(4));
        assertEquals(35, exactTotal(5));
    }

    @Test
    void classComboCountsAtMostTwoCopiesOfOneFigureId() {
        var result = GoldenSacrificeCalculator.calculate(ROBIN, List.of(
                figure("speedy", "Martial Arts"),
                figure("speedy", "Martial Arts"),
                figure("speedy", "Martial Arts"),
                figure("batman", "Martial Arts"),
                figure("nightwing", "Martial Arts")
        ), groups());

        assertEquals(5, result.donors().size());
        assertEquals(4, result.classComboCount());
        assertEquals(2, result.classBonus());
        assertEquals(12, result.totalPoints());
    }

    @Test
    void duplicateGroupMembersOnlyCountOnceTowardGroupCombo() {
        GroupStub groups = groups().add("titans", "Titans", "robin", "raven", "starfire", "cyborg");
        var result = GoldenSacrificeCalculator.calculate(ROBIN, List.of(
                figure("raven", "Dark Arts"),
                figure("raven", "Dark Arts"),
                figure("starfire", "Super")
        ), groups);

        assertEquals(9, result.basePoints());
        assertEquals(0, result.totalPoints() - result.basePoints());
        assertEquals(2, result.groupBreakdowns().get(0).distinctSacrificeCount());
        assertFalse(result.groupBreakdowns().get(0).complete());
    }

    @Test
    void completingSmallGroupMakesEachRequiredDonorWorthExactBase() {
        GroupStub groups = groups().add("dynamic_duo", "Dynamic Duo", "robin", "batman");
        var result = GoldenSacrificeCalculator.calculate(ROBIN,
                List.of(figure("batman", "Martial Arts")), groups);

        assertEquals(3, result.basePoints());
        assertNotNull(result.awardedGroup());
        assertTrue(result.awardedGroup().complete());
        assertEquals(1, result.awardedGroup().awardedBonus());
        assertEquals(4, result.totalPoints());
    }

    @Test
    void threeDistinctGroupMembersEarnNormalBonus() {
        GroupStub groups = groups().add("titans", "Titans", "robin", "raven", "starfire", "cyborg", "beast_boy");
        var result = GoldenSacrificeCalculator.calculate(ROBIN, List.of(
                figure("raven", "Dark Arts"),
                figure("starfire", "Super"),
                figure("cyborg", "Tech")
        ), groups);

        assertEquals(9, result.basePoints());
        assertEquals(3, result.awardedGroup().awardedBonus());
        assertEquals(12, result.totalPoints());
    }

    @Test
    void independentExactAndCompletedGroupBonusesStack() {
        GroupStub groups = groups().add("trio", "Trio", "robin", "raven", "starfire");
        var result = GoldenSacrificeCalculator.calculate(ROBIN, List.of(
                ROBIN, ROBIN, ROBIN,
                figure("raven", "Dark Arts"),
                figure("starfire", "Super")
        ), groups);

        assertEquals(21, result.basePoints());
        assertEquals(5, result.exactBonus());
        assertEquals(2, result.awardedGroup().awardedBonus());
        assertEquals(28, result.totalPoints());
    }

    private int exactTotal(int count) {
        return GoldenSacrificeCalculator.calculate(ROBIN,
                java.util.Collections.nCopies(count, ROBIN), groups()).totalPoints();
    }

    private static GoldenSacrificeCalculator.FigureData figure(String id, String figureClass) {
        return new GoldenSacrificeCalculator.FigureData(id, figureClass);
    }

    private static GroupStub groups() {
        return new GroupStub();
    }

    private static final class GroupStub implements GoldenSacrificeCalculator.GroupAccess {
        private final Map<String, Set<String>> members = new HashMap<>();
        private final Map<String, String> names = new HashMap<>();
        private final Map<String, Set<String>> reverse = new HashMap<>();

        GroupStub add(String id, String name, String... figureIds) {
            Set<String> memberSet = Set.of(figureIds);
            members.put(id, memberSet);
            names.put(id, name);
            for (String figureId : memberSet) {
                reverse.computeIfAbsent(figureId, ignored -> new java.util.HashSet<>()).add(id);
            }
            return this;
        }

        @Override
        public Set<String> groupsForFigure(String figureId) {
            return reverse.getOrDefault(figureId, Set.of());
        }

        @Override
        public Set<String> membersForGroup(String groupId) {
            return members.getOrDefault(groupId, Set.of());
        }

        @Override
        public String nameForGroup(String groupId) {
            return names.getOrDefault(groupId, groupId);
        }
    }
}
