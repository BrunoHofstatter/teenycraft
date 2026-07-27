package bruhof.teenycraft.gametest;

import bruhof.teenycraft.TeenyBalance;
import bruhof.teenycraft.TeenyCraft;
import bruhof.teenycraft.accessory.AccessoryExecutor;
import bruhof.teenycraft.accessory.AccessoryRegistry;
import bruhof.teenycraft.accessory.AccessorySpec;
import bruhof.teenycraft.battle.AbilityExecutor;
import bruhof.teenycraft.battle.BattleFigure;
import bruhof.teenycraft.chip.ChipExecutor;
import bruhof.teenycraft.battle.damage.DamagePipeline;
import bruhof.teenycraft.battle.effect.EffectCalculator;
import bruhof.teenycraft.battle.effect.EffectApplierRegistry;
import bruhof.teenycraft.battle.effect.EffectInstance;
import bruhof.teenycraft.capability.BattleState;
import bruhof.teenycraft.capability.AccessoryMasteryProvider;
import bruhof.teenycraft.capability.BattleStateProvider;
import bruhof.teenycraft.capability.IBattleState;
import bruhof.teenycraft.capability.ITitanManager;
import bruhof.teenycraft.capability.TitanManagerProvider;
import bruhof.teenycraft.entity.ModEntities;
import bruhof.teenycraft.entity.custom.EntityTeenyDummy;
import bruhof.teenycraft.item.ModItems;
import bruhof.teenycraft.item.custom.ItemFigure;
import bruhof.teenycraft.item.custom.ItemChip;
import bruhof.teenycraft.item.custom.battle.ItemAbility;
import bruhof.teenycraft.util.AbilityLoader;
import bruhof.teenycraft.util.FigureLoader;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(TeenyCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BattleGameTests {
    private static final Vec3 PLAYER_POS = new Vec3(10.5, 1.0, 18.5);
    private static final Vec3 OPPONENT_POS = new Vec3(13.5, 1.0, 18.5);
    private static final Vec3 DISTRACTOR_POS = new Vec3(11.5, 1.0, 18.5);
    private static final float PLAYER_YAW = -90.0f;
    private static final float OPPONENT_YAW = 90.0f;

    private BattleGameTests() {
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void battleStartInitializesParticipantsAndInventory(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper, List.of(figure("robin"), figure("batman")), List.of(figure("joker")), ItemStack.EMPTY);

        helper.assertTrue(battle.playerState.isBattling(), "player battle state should be active");
        helper.assertTrue(battle.opponentState.isBattling(), "opponent battle state should be active");
        helper.assertTrue("robin".equals(battle.playerState.getActiveFigureId()), "player active figure should be robin");
        helper.assertTrue("joker".equals(battle.opponentState.getActiveFigureId()), "opponent active figure should be joker");
        helper.assertTrue(battle.opponent.getUUID().equals(battle.playerState.getOpponentEntityId()), "player state should store the paired opponent identity");
        helper.assertTrue(battle.player.getUUID().equals(battle.opponentState.getOpponentEntityId()), "opponent state should store the paired player identity");

        ItemStack firstAbility = battle.player.getInventory().getItem(0);
        helper.assertTrue(firstAbility.is(ModItems.ABILITY_1.get()), "slot 0 should contain the first battle ability item");
        helper.assertTrue("heroic_pose".equals(firstAbility.getOrCreateTag().getString(ItemAbility.TAG_ID)), "slot 0 should be Robin's first ability");
        helper.assertTrue(!battle.player.getInventory().getItem(6).isEmpty(), "slot 6 should contain a bench figure icon");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void swapClearsFlightAndAppliesCooldown(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper, List.of(figure("superman"), figure("robin")), List.of(figure("joker")), ItemStack.EMPTY);

        battle.playerState.applyEffect("flight", 60, 1, 60.0f);
        battle.playerState.swapFigure(1, battle.player);

        helper.assertTrue("robin".equals(battle.playerState.getActiveFigureId()), "swap should move to the bench figure");
        helper.assertFalse(battle.playerState.hasEffect("flight"), "swap should clear flight");
        helper.assertTrue(battle.playerState.getSwapCooldown() == TeenyBalance.SWAP_COOLDOWN * 20, "swap should apply the configured cooldown");
        helper.assertTrue("heroic_pose".equals(battle.player.getInventory().getItem(0).getOrCreateTag().getString(ItemAbility.TAG_ID)), "inventory should refresh to the new active figure");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void figureOwnedEffectsAndProgressStayWithOriginalFigureAcrossSwap(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper, List.of(figure("robin"), figure("batman")), List.of(figure("joker")), ItemStack.EMPTY);

        battle.playerState.applyEffect("power_up", 120, 5);
        battle.playerState.setSlotProgress(0, 2);
        battle.playerState.setInternalCooldown("pet_fire_1", 40);

        battle.playerState.swapFigure(1, battle.player);

        helper.assertTrue("batman".equals(battle.playerState.getActiveFigureId()), "swap should move to the second figure");
        helper.assertFalse(battle.playerState.hasEffect("power_up"), "the new active figure should not inherit the previous figure's effect bucket");
        helper.assertTrue(battle.playerState.getSlotProgress(0) == 0, "activate progress should not leak to the new active figure");
        helper.assertTrue(battle.playerState.getInternalCooldown("pet_fire_1") == 0, "figure-owned internal cooldowns should not leak to the new active figure");

        battle.playerState.setActiveFigure(0);

        helper.assertTrue("robin".equals(battle.playerState.getActiveFigureId()), "reactivating the original figure should restore its state");
        helper.assertTrue(battle.playerState.hasEffect("power_up"), "the original figure should keep its effect bucket");
        helper.assertTrue(battle.playerState.getSlotProgress(0) == 2, "activate progress should stay with the original figure");
        helper.assertTrue(battle.playerState.getInternalCooldown("pet_fire_1") == 40, "internal cooldowns should stay with the original figure");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void duplicateBenchItemTargetsTaggedDuplicateFigure(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper, List.of(figure("robin"), figure("robin")), List.of(figure("joker")), ItemStack.EMPTY);

        battle.playerState.applyEffect("power_up", 120, 5);
        ItemStack benchIcon = battle.player.getInventory().getItem(6).copy();

        helper.assertTrue(benchIcon.getOrCreateTag().getInt(BattleState.TAG_BATTLE_FIGURE_INDEX) == 1, "bench icon should carry the authoritative duplicate figure index");
        helper.assertTrue(battle.playerState.trySwapFigureFromItem(benchIcon, battle.player), "duplicate bench item should resolve through its tagged figure index");
        helper.assertTrue(battle.playerState.getActiveFigureIndex() == 1, "duplicate bench targeting should swap to the tagged duplicate figure");
        helper.assertFalse(battle.playerState.hasEffect("power_up"), "the second duplicate figure should not inherit the first duplicate's effects");

        battle.playerState.setActiveFigure(0);

        helper.assertTrue(battle.playerState.hasEffect("power_up"), "swapping back should restore the original duplicate's effect state");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void chargeStateDoesNotLeakAcrossActiveFigureChanges(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper, List.of(figure("joker"), figure("robin")), List.of(figure("batman")), ItemStack.EMPTY);

        battle.playerState.addMana(80);
        AbilityExecutor.executeAction(battle.player, battle.playerState.getActiveFigure(), 1);

        helper.assertTrue(battle.playerState.isCharging(), "joker should enter charge state before the active figure changes");
        helper.assertTrue(battle.playerState.getLockedSlot() == 1, "charge state should lock the casting slot on the charging figure");

        battle.playerState.setActiveFigure(1);

        helper.assertFalse(battle.playerState.isCharging(), "charge state should not follow a different active figure");
        helper.assertTrue(battle.playerState.getLockedSlot() == -1, "slot lock should clear when the charging figure is no longer active");

        battle.playerState.setActiveFigure(0);

        helper.assertFalse(battle.playerState.isCharging(), "reactivating the old figure should not resume a canceled pending charge");
        helper.assertTrue(battle.playerState.getPendingAbility() == null, "pending charge data should be cleared when ownership changes away from the figure");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void opponentChargeUpStartsChargingBeforeApplyingEffect(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper, List.of(figure("robin")), List.of(figure("raven")), ItemStack.EMPTY);

        BattleFigure active = battle.opponentState.getActiveFigure();
        battle.opponentState.addMana(manaCost(active, 0) + 20);

        AbilityExecutor.executeAction(battle.opponent, active, 0);

        helper.assertTrue(battle.opponentState.isCharging(), "opponent charge-up abilities should enter charge state first");
        helper.assertTrue(battle.opponentState.getLockedSlot() == 0, "opponent charge-up should lock the casting slot");
        helper.assertFalse(battle.playerState.hasEffect("curse"), "charge-up should not apply the effect immediately");

        helper.runAfterDelay(35, () -> {
            helper.assertFalse(battle.opponentState.isCharging(), "opponent charge state should end after the delay");
            helper.assertTrue(battle.playerState.hasEffect("curse"), "the effect should apply after charge completes");
            helper.succeed();
        });
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void faintRoundResetOnlyTouchesPairedOpponent(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper, List.of(figure("robin"), figure("batman")), List.of(figure("joker")), ItemStack.EMPTY);
        NearbyBattler distractor = spawnNearbyBattler(helper, DISTRACTOR_POS, figure("superman"));

        battle.playerState.applyEffect("power_up", -1, 5);
        battle.playerState.applyResolvedCombatFigureDelta(battle.playerState.getActiveFigure(), -battle.playerState.getActiveFigure().getMaxHp());

        helper.assertTrue("batman".equals(battle.playerState.getActiveFigureId()), "faint should swap to the next living figure");
        helper.assertTrue(battle.playerState.hasEffect("reset_lock"), "round reset should apply reset_lock to the fainted side");
        helper.assertFalse(battle.playerState.hasEffect("power_up"), "round reset should clear previous effects");
        helper.assertTrue(battle.opponentState.hasEffect("reset_lock"), "round reset should apply reset_lock to the paired opponent");
        helper.assertFalse(distractor.state.hasEffect("reset_lock"), "round reset should not touch an unpaired nearby battler");
        helper.assertTrue("bat_mine".equals(battle.player.getInventory().getItem(0).getOrCreateTag().getString(ItemAbility.TAG_ID)), "inventory should refresh after a faint swap");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void defeatCleanupEndsBattleAfterTimer(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper, List.of(figure("robin")), List.of(figure("joker")), ItemStack.EMPTY);

        battle.playerState.applyResolvedCombatFigureDelta(battle.playerState.getActiveFigure(), -battle.playerState.getActiveFigure().getMaxHp());

        helper.runAfterDelay(45, () -> {
            helper.assertFalse(battle.playerState.isBattling(), "player battle state should clean itself up after the defeat timer");
            helper.assertTrue(battle.playerState.getTeam().isEmpty(), "defeat cleanup should clear the team");
            helper.succeed();
        });
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void poisonTickKillResolvesDefeatCleanup(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper, List.of(figure("robin")), List.of(figure("joker")), ItemStack.EMPTY);
        BattleFigure active = battle.playerState.getActiveFigure();

        int guaranteedTickKill = active.getMaxHp() + active.getDodgeStat();
        battle.playerState.applyEffect("poison", 1, 1, guaranteedTickKill, battle.opponent.getUUID());

        helper.runAfterDelay(2, () -> helper.assertTrue(active.getCurrentHp() == 0, "poison tick should reduce the active figure to 0 HP"));

        helper.runAfterDelay(45, () -> {
            helper.assertFalse(battle.playerState.isBattling(), "poison kill should resolve defeat cleanup after the timer");
            helper.assertTrue(battle.playerState.getTeam().isEmpty(), "poison defeat cleanup should clear the team");
            helper.succeed();
        });
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void standardHitKillTriggersVampireChipHeal(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper, List.of(figureWithChip("robin", chip(ModItems.CHIP_VAMPIRE.get()))), List.of(figure("joker")), ItemStack.EMPTY);

        BattleFigure attackerFigure = battle.playerState.getActiveFigure();
        battle.playerState.applyResolvedCombatFigureDelta(attackerFigure, -10);
        int damagedHp = attackerFigure.getCurrentHp();

        BattleFigure victimFigure = battle.opponentState.getActiveFigure();
        DamagePipeline.DamageResult lethalHit = new DamagePipeline.DamageResult(victimFigure.getMaxHp() + 100, 1, false, false);
        lethalHit.undodgeable = true;

        AbilityExecutor.applyDamageToFigure(
                battle.playerState,
                battle.player,
                battle.opponent,
                battle.opponentState,
                victimFigure,
                lethalHit,
                null,
                0,
                false,
                false,
                false,
                AbilityExecutor.nextAccessoryReactionId(),
                true
        );

        helper.assertTrue(attackerFigure.getCurrentHp() > damagedHp, "standard-hit kills should trigger the vampire chip heal hook");
        helper.assertTrue(attackerFigure.getCurrentHp() <= attackerFigure.getMaxHp(), "vampire chip healing should still respect max HP");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void statChipsModifyBattleSnapshotAsExpected(GameTestHelper helper) {
        ItemStack baseRobin = figure("robin");
        int baseHp = ItemFigure.getHealth(baseRobin);
        int basePower = ItemFigure.getPower(baseRobin);
        int baseLuck = ItemFigure.getLuck(baseRobin);
        int baseDodge = ItemFigure.getDodge(baseRobin);

        BattleFigure powerFigure = new BattleFigure(figureWithChip("robin", chip(ModItems.CHIP_POWER.get())));
        BattleFigure healthFigure = new BattleFigure(figureWithChip("robin", chip(ModItems.CHIP_HEALTH.get())));
        BattleFigure luckFigure = new BattleFigure(figureWithChip("robin", chip(ModItems.CHIP_LUCK.get())));
        BattleFigure ninjaFigure = new BattleFigure(figureWithChip("robin", chip(ModItems.CHIP_NINJA_SKILLS.get())));
        BattleFigure loadedDiceFigure = new BattleFigure(figureWithChip("robin", chip(ModItems.CHIP_LOADED_DICE.get())));

        helper.assertTrue(powerFigure.getPowerStat() == basePower + TeenyBalance.CHIP_POWER_POWER_BY_RANK[0], "power chip should add flat power");
        helper.assertTrue(healthFigure.getMaxHp() == baseHp + TeenyBalance.CHIP_HEALTH_HP_BY_RANK[0], "health chip should add flat max HP");
        helper.assertTrue(luckFigure.getLuckStat() == baseLuck + TeenyBalance.CHIP_LUCK_LUCK_BY_RANK[0], "luck chip should add flat luck");
        helper.assertTrue(ninjaFigure.getDodgeStat() == baseDodge + TeenyBalance.CHIP_NINJA_SKILLS_DODGE_BY_RANK[0], "ninja skills should add dodge");
        helper.assertTrue(ninjaFigure.getMaxHp() == baseHp + TeenyBalance.CHIP_NINJA_SKILLS_HP_BY_RANK[0], "ninja skills should reduce max HP");
        helper.assertTrue(loadedDiceFigure.getLuckStat() == baseLuck + TeenyBalance.CHIP_LOADED_DICE_LUCK_BY_RANK[0], "loaded dice should add luck");
        helper.assertTrue(loadedDiceFigure.getPowerStat() == basePower + TeenyBalance.CHIP_LOADED_DICE_POWER_BY_RANK[0], "loaded dice should reduce power");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void toughStuffOverridesDodgeToZeroAndAddsHp(GameTestHelper helper) {
        ItemStack baseRobin = figure("robin");
        BattleFigure figure = new BattleFigure(figureWithChip("robin", chip(ModItems.CHIP_TOUGH_STUFF.get())));

        helper.assertTrue(figure.getMaxHp() == ItemFigure.getHealth(baseRobin) + TeenyBalance.CHIP_TOUGH_STUFF_HP_BY_RANK[0], "tough stuff should add max HP");
        helper.assertTrue(figure.getDodgeStat() == 0, "tough stuff should override dodge to 0");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void firstAppearanceChipsApplyOwnerEffects(GameTestHelper helper) {
        BattleHarness battle = startBattle(
                helper,
                List.of(figureWithChip("robin", chip(ModItems.CHIP_CLEAN_ENTRY.get()))),
                List.of(figureWithChip("joker", chip(ModItems.CHIP_BEASTLY_ENTRY.get()))),
                ItemStack.EMPTY
        );

        helper.assertTrue(battle.playerState.hasEffect("cleanse_immunity"), "clean entry should apply cleanse immunity on first appearance");
        helper.assertTrue(battle.opponentState.hasEffect("power_up"), "beastly entry should apply power_up on first appearance");
        helper.assertTrue(battle.opponentState.getEffectMagnitude("power_up") == TeenyBalance.CHIP_BEASTLY_ENTRY_POWER_UP_BY_RANK[0], "beastly entry should use the configured power_up magnitude");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void curseEntryAppliesCurseToOpponentOnFirstAppearance(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper, List.of(figureWithChip("robin", chip(ModItems.CHIP_CURSE_ENTRY.get()))), List.of(figure("joker")), ItemStack.EMPTY);

        helper.assertTrue(battle.opponentState.hasEffect("curse"), "curse entry should apply curse to the opposing active figure");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void flightDodgeTriggersHealthyDodgeChipHeal(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper, List.of(figureWithChip("robin", chip(ModItems.CHIP_HEALTHY_DODGE.get()))), List.of(figure("joker")), ItemStack.EMPTY);

        BattleFigure victim = battle.playerState.getActiveFigure();
        battle.playerState.applyResolvedCombatFigureDelta(victim, -10);
        int hpBeforeHit = victim.getCurrentHp();
        battle.playerState.applyEffect("flight", 60, 1, 60.0f);

        DamagePipeline.DamageResult hit = undodgeableHit(10, false);
        hit.undodgeable = false;
        AbilityExecutor.applyDamageToFigure(
                battle.opponentState,
                battle.opponent,
                battle.player,
                battle.playerState,
                victim,
                hit,
                null,
                0,
                false,
                false,
                false,
                AbilityExecutor.nextAccessoryReactionId(),
                true
        );

        helper.assertTrue(victim.getCurrentHp() > hpBeforeHit, "healthy dodge should heal when the figure dodges");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void speedyDodgeAppliesSpeedUpEffectOnDodge(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper, List.of(figureWithChip("robin", chip(ModItems.CHIP_SPEEDY_DODGE.get()))), List.of(figure("joker")), ItemStack.EMPTY);

        ChipExecutor.onDodge(battle.playerState, battle.player, battle.playerState.getActiveFigure(), battle.opponentState, battle.opponent);

        helper.assertTrue(battle.playerState.hasEffect("speed_up"), "speedy dodge should apply a speed_up effect");
        helper.assertTrue(battle.playerState.getEffectMagnitude("speed_up") == TeenyBalance.CHIP_SPEEDY_DODGE_SPEED_PCT_BY_RANK[0], "speedy dodge should use the configured speed magnitude");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void secondChancePreventsFaintClearsEffectsAndDebuffsSwapIn(GameTestHelper helper) {
        BattleHarness battle = startBattle(
                helper,
                List.of(figureWithChip("robin", chip(ModItems.CHIP_SECOND_CHANCE.get())), figure("batman")),
                List.of(figureWithChip("joker", chip(ModItems.CHIP_VAMPIRE.get()))),
                ItemStack.EMPTY
        );

        BattleFigure rescuedFigure = battle.playerState.getActiveFigure();
        BattleFigure attackerFigure = battle.opponentState.getActiveFigure();
        battle.playerState.applyEffect("power_up", -1, 5);
        battle.opponentState.applyResolvedCombatFigureDelta(attackerFigure, -10);
        int attackerHpBefore = attackerFigure.getCurrentHp();

        AbilityExecutor.applyDamageToFigure(
                battle.opponentState,
                battle.opponent,
                battle.player,
                battle.playerState,
                rescuedFigure,
                undodgeableHit(rescuedFigure.getMaxHp() + 50, false),
                null,
                0,
                false,
                false,
                false,
                AbilityExecutor.nextAccessoryReactionId(),
                true
        );

        EffectInstance slowDown = battle.playerState.getEffectInstance("speed_down");
        EffectInstance curse = battle.playerState.getEffectInstance("curse");
        helper.assertTrue("batman".equals(battle.playerState.getActiveFigureId()), "second chance should force a swap to the next living figure");
        helper.assertTrue(rescuedFigure.getCurrentHp() == TeenyBalance.CHIP_SECOND_CHANCE_SURVIVE_HP_BY_RANK[0], "second chance should leave the rescued figure at its configured survive HP");
        helper.assertTrue(rescuedFigure.getActiveEffects().isEmpty(), "second chance should clear the rescued figure's active effects like a normal faint reset");
        helper.assertTrue(slowDown != null
                        && slowDown.magnitude == TeenyBalance.CHIP_SECOND_CHANCE_SLOW_MAGNITUDE_BY_RANK[0]
                        && slowDown.duration == TeenyBalance.CHIP_SECOND_CHANCE_DEBUFF_DURATION_BY_RANK[0],
                "second chance should apply the configured slow debuff to the swapped-in figure");
        helper.assertTrue(curse != null && curse.duration == TeenyBalance.CHIP_SECOND_CHANCE_DEBUFF_DURATION_BY_RANK[0],
                "second chance should apply curse to the swapped-in figure for the configured duration");
        helper.assertTrue(attackerFigure.getCurrentHp() == attackerHpBefore, "prevented lethal damage should not count as a kill for attacker chips");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void secondChanceOnlyTriggersOncePerBattle(GameTestHelper helper) {
        BattleHarness battle = startBattle(
                helper,
                List.of(figureWithChip("robin", chip(ModItems.CHIP_SECOND_CHANCE.get())), figure("batman")),
                List.of(figure("joker")),
                ItemStack.EMPTY
        );

        BattleFigure rescuedFigure = battle.playerState.getTeam().get(0);
        AbilityExecutor.applyDamageToFigure(
                battle.opponentState,
                battle.opponent,
                battle.player,
                battle.playerState,
                rescuedFigure,
                undodgeableHit(rescuedFigure.getMaxHp() + 50, false),
                null,
                0,
                false,
                false,
                false,
                AbilityExecutor.nextAccessoryReactionId(),
                true
        );

        battle.playerState.applyResolvedCombatFigureDelta(rescuedFigure, rescuedFigure.getMaxHp());
        battle.playerState.removeEffect("reset_lock");
        battle.opponentState.removeEffect("reset_lock");
        battle.playerState.setActiveFigure(0);

        AbilityExecutor.applyDamageToFigure(
                battle.opponentState,
                battle.opponent,
                battle.player,
                battle.playerState,
                rescuedFigure,
                undodgeableHit(rescuedFigure.getMaxHp() + 50, false),
                null,
                0,
                false,
                false,
                false,
                AbilityExecutor.nextAccessoryReactionId(),
                true
        );

        helper.assertTrue(rescuedFigure.getCurrentHp() == 0, "second chance should not rescue the same figure a second time in the same battle");
        helper.assertTrue("batman".equals(battle.playerState.getActiveFigureId()), "after the one allowed rescue, the next lethal hit should resolve the normal faint swap");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void healthySecondChanceUsesConfiguredRescueHpWithoutAddingMaxHp(GameTestHelper helper) {
        BattleFigure baseFigure = new BattleFigure(figure("robin"));
        BattleFigure hybridFigure = new BattleFigure(figureWithChip("robin", chip(ModItems.CHIP_HEALTHY_SECOND_CHANCE.get())));

        helper.assertTrue(hybridFigure.getMaxHp() == baseFigure.getMaxHp(), "healthy second chance should not raise the figure's max HP stat");

        BattleHarness battle = startBattle(
                helper,
                List.of(figureWithChip("robin", chip(ModItems.CHIP_HEALTHY_SECOND_CHANCE.get())), figure("batman")),
                List.of(figure("joker")),
                ItemStack.EMPTY
        );

        BattleFigure rescuedFigure = battle.playerState.getTeam().get(0);
        AbilityExecutor.applyDamageToFigure(
                battle.opponentState,
                battle.opponent,
                battle.player,
                battle.playerState,
                rescuedFigure,
                undodgeableHit(rescuedFigure.getMaxHp() + 50, false),
                null,
                0,
                false,
                false,
                false,
                AbilityExecutor.nextAccessoryReactionId(),
                true
        );

        EffectInstance slowDown = battle.playerState.getEffectInstance("speed_down");
        helper.assertTrue(rescuedFigure.getCurrentHp() == TeenyBalance.CHIP_HEALTHY_SECOND_CHANCE_SURVIVE_HP_BY_RANK[0], "healthy second chance should use its authored survive HP instead of the base chip's 1 HP rescue");
        helper.assertTrue(slowDown != null
                        && slowDown.magnitude == TeenyBalance.CHIP_HEALTHY_SECOND_CHANCE_SLOW_MAGNITUDE_BY_RANK[0]
                        && slowDown.duration == TeenyBalance.CHIP_HEALTHY_SECOND_CHANCE_DEBUFF_DURATION_BY_RANK[0],
                "healthy second chance should apply its stronger authored swap-in slow values");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void standardHitKillTriggersMomentumChipManaGain(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper, List.of(figureWithChip("robin", chip(ModItems.CHIP_MOMENTUM.get()))), List.of(figure("joker")), ItemStack.EMPTY);

        float manaBefore = battle.playerState.getCurrentMana();
        BattleFigure victimFigure = battle.opponentState.getActiveFigure();
        DamagePipeline.DamageResult lethalHit = undodgeableHit(victimFigure.getMaxHp() + 100, true);

        AbilityExecutor.applyDamageToFigure(
                battle.playerState,
                battle.player,
                battle.opponent,
                battle.opponentState,
                victimFigure,
                lethalHit,
                null,
                0,
                false,
                false,
                false,
                AbilityExecutor.nextAccessoryReactionId(),
                true
        );

        helper.assertTrue(battle.playerState.getCurrentMana() > manaBefore, "momentum should grant mana on kill");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void standardHitKillTriggersVictoryDanceChipEffect(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper, List.of(figureWithChip("robin", chip(ModItems.CHIP_VICTORY_DANCE.get()))), List.of(figure("joker")), ItemStack.EMPTY);

        BattleFigure victimFigure = battle.opponentState.getActiveFigure();
        DamagePipeline.DamageResult lethalHit = undodgeableHit(victimFigure.getMaxHp() + 100, true);

        AbilityExecutor.applyDamageToFigure(
                battle.playerState,
                battle.player,
                battle.opponent,
                battle.opponentState,
                victimFigure,
                lethalHit,
                null,
                0,
                false,
                false,
                false,
                AbilityExecutor.nextAccessoryReactionId(),
                true
        );

        helper.assertTrue(battle.playerState.hasEffect("dance"), "victory dance should apply dance on kill");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void manaStealDuckCritTransfersActualOpponentMana(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper, List.of(figureWithChip("robin", chip(ModItems.CHIP_MANA_STEAL_DUCK.get()))), List.of(figure("joker")), ItemStack.EMPTY);

        battle.opponentState.addMana(20);
        float ownerManaBefore = battle.playerState.getCurrentMana();
        float opponentManaBefore = battle.opponentState.getCurrentMana();

        ChipExecutor.onCritHit(battle.playerState, battle.player, battle.playerState.getActiveFigure(), battle.opponentState, battle.opponent);

        int expectedTransfer = Math.min(TeenyBalance.CHIP_MANA_STEAL_DUCK_MANA_BY_RANK[0], (int) opponentManaBefore);
        helper.assertTrue(battle.playerState.getCurrentMana() == ownerManaBefore + expectedTransfer, "mana steal duck should grant the actual mana stolen");
        helper.assertTrue(battle.opponentState.getCurrentMana() == opponentManaBefore - expectedTransfer, "mana steal duck should drain the same amount from the opponent");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void poisonKillTriggersVampireChipHeal(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper, List.of(figureWithChip("robin", chip(ModItems.CHIP_VAMPIRE.get()))), List.of(figure("joker")), ItemStack.EMPTY);

        BattleFigure attackerFigure = battle.playerState.getActiveFigure();
        battle.playerState.applyResolvedCombatFigureDelta(attackerFigure, -10);
        int damagedHp = attackerFigure.getCurrentHp();

        BattleFigure victimFigure = battle.opponentState.getActiveFigure();
        int guaranteedTickKill = victimFigure.getMaxHp() + victimFigure.getDodgeStat();
        battle.opponentState.applyEffect("poison", 1, 1, guaranteedTickKill, battle.player.getUUID());

        helper.runAfterDelay(2, () -> {
            helper.assertTrue(victimFigure.getCurrentHp() == 0, "poison tick should still reduce the victim to 0 HP");
            helper.assertTrue(attackerFigure.getCurrentHp() > damagedHp, "poison kills should trigger the same vampire chip heal hook");
            helper.assertTrue(attackerFigure.getCurrentHp() <= attackerFigure.getMaxHp(), "poison kill healing should still respect max HP");
            helper.succeed();
        });
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void poisonKillAfterSourceSwapCreditsOriginalFigure(GameTestHelper helper) {
        BattleHarness battle = startBattle(
                helper,
                List.of(figureWithChip("robin", chip(ModItems.CHIP_VAMPIRE.get())), figure("batman")),
                List.of(figure("joker")),
                ItemStack.EMPTY
        );

        BattleFigure originalSourceFigure = battle.playerState.getActiveFigure();
        battle.playerState.applyResolvedCombatFigureDelta(originalSourceFigure, -10);
        int damagedHp = originalSourceFigure.getCurrentHp();

        BattleFigure swappedFigure = battle.playerState.getTeam().get(1);
        int swappedFigureHp = swappedFigure.getCurrentHp();

        BattleFigure victimFigure = battle.opponentState.getActiveFigure();
        int guaranteedTickKill = victimFigure.getMaxHp() + victimFigure.getDodgeStat();
        battle.opponentState.applyEffect("poison", 1, 1, guaranteedTickKill, battle.player.getUUID(), originalSourceFigure);
        battle.playerState.swapFigure(1, battle.player);

        helper.runAfterDelay(2, () -> {
            helper.assertTrue("batman".equals(battle.playerState.getActiveFigureId()), "the player should remain swapped after the poison is applied");
            helper.assertTrue(victimFigure.getCurrentHp() == 0, "poison tick should still reduce the victim to 0 HP");
            helper.assertTrue(originalSourceFigure.getCurrentHp() > damagedHp, "poison kill credit should heal the original source figure even after a swap");
            helper.assertTrue(swappedFigure.getCurrentHp() == swappedFigureHp, "the later active figure should not receive the original source figure's kill hook");
            helper.succeed();
        });
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void poisonKillAfterSourceSwapAppliesVictoryDanceToOriginalSourceFigure(GameTestHelper helper) {
        BattleHarness battle = startBattle(
                helper,
                List.of(figureWithChip("robin", chip(ModItems.CHIP_VICTORY_DANCE.get())), figure("batman")),
                List.of(figure("joker")),
                ItemStack.EMPTY
        );

        BattleFigure originalSourceFigure = battle.playerState.getActiveFigure();
        BattleFigure swappedFigure = battle.playerState.getTeam().get(1);
        BattleFigure victimFigure = battle.opponentState.getActiveFigure();
        int guaranteedTickKill = victimFigure.getMaxHp() + victimFigure.getDodgeStat();

        battle.opponentState.applyEffect("poison", 1, 1, guaranteedTickKill, battle.player.getUUID(), originalSourceFigure);
        battle.playerState.swapFigure(1, battle.player);

        helper.runAfterDelay(2, () -> {
            helper.assertTrue("batman".equals(battle.playerState.getActiveFigureId()), "the player should remain swapped after the poison is applied");
            helper.assertTrue(victimFigure.getCurrentHp() == 0, "poison tick should still reduce the victim to 0 HP");
            helper.assertTrue(battle.playerState.hasEffect(originalSourceFigure, "dance"),
                    "victory dance should apply to the figure that earned the kill credit");
            helper.assertFalse(battle.playerState.hasEffect(swappedFigure, "dance"),
                    "the later active figure should not receive the original source figure's kill buff");
            helper.succeed();
        });
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void poisonKillAfterSourceSwapSummonsNecromancerPetOnOriginalSourceFigure(GameTestHelper helper) {
        BattleHarness battle = startBattle(
                helper,
                List.of(figureWithChip("robin", chip(ModItems.CHIP_NECROMANCER.get())), figure("batman")),
                List.of(figure("joker")),
                ItemStack.EMPTY
        );

        BattleFigure originalSourceFigure = battle.playerState.getActiveFigure();
        BattleFigure swappedFigure = battle.playerState.getTeam().get(1);
        BattleFigure victimFigure = battle.opponentState.getActiveFigure();
        int guaranteedTickKill = victimFigure.getMaxHp() + victimFigure.getDodgeStat();

        battle.opponentState.applyEffect("poison", 1, 1, guaranteedTickKill, battle.player.getUUID(), originalSourceFigure);
        battle.playerState.swapFigure(1, battle.player);

        helper.runAfterDelay(2, () -> {
            helper.assertTrue(victimFigure.getCurrentHp() == 0, "poison tick should still reduce the victim to 0 HP");
            helper.assertTrue(
                    battle.playerState.hasEffect(originalSourceFigure, "pet_slot_1")
                            || battle.playerState.hasEffect(originalSourceFigure, "pet_slot_2"),
                    "necromancer should summon onto the figure that earned the kill credit"
            );
            helper.assertFalse(battle.playerState.hasEffect(swappedFigure, "pet_slot_1")
                            || battle.playerState.hasEffect(swappedFigure, "pet_slot_2"),
                    "the swapped-in figure should not inherit the original source figure's summoned pet");
            helper.succeed();
        });
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void selfShockKillResolvesDefeatCleanup(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper, List.of(figure("cyborg")), List.of(figure("joker")), ItemStack.EMPTY);
        BattleFigure active = battle.playerState.getActiveFigure();

        battle.playerState.applyEffect("self_shock", 0, active.getCurrentHp());

        helper.assertTrue(active.getCurrentHp() == 0, "self_shock should reduce the active figure to 0 HP immediately");

        helper.runAfterDelay(45, () -> {
            helper.assertFalse(battle.playerState.isBattling(), "self_shock kill should resolve defeat cleanup after the timer");
            helper.assertTrue(battle.playerState.getTeam().isEmpty(), "self_shock defeat cleanup should clear the team");
            helper.succeed();
        });
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void selfShockFaintTriggersDeathEnergyChipHook(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper, List.of(figureWithChip("cyborg", chip(ModItems.CHIP_DEATH_ENERGY.get()))), List.of(figure("joker")), ItemStack.EMPTY);
        BattleFigure active = battle.playerState.getActiveFigure();
        float batteryBefore = battle.playerState.getBatteryCharge();

        battle.playerState.applyEffect("self_shock", 0, active.getCurrentHp());

        helper.assertTrue(active.getCurrentHp() == 0, "self_shock should still reduce the active figure to 0 HP");
        helper.assertTrue(battle.playerState.getBatteryCharge() > batteryBefore, "self_shock faints should now trigger death_energy battery gain");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void chargeUpLocksSlotAndFinishesHealing(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper, List.of(figure("joker")), List.of(figure("robin")), ItemStack.EMPTY);

        BattleFigure active = battle.playerState.getActiveFigure();
        int hpBefore = active.getCurrentHp();
        battle.playerState.applyResolvedCombatFigureDelta(active, -10);
        int damagedHp = active.getCurrentHp();
        battle.playerState.addMana(80);

        AbilityExecutor.executeAction(battle.player, active, 1);

        helper.assertTrue(battle.playerState.isCharging(), "charge_up abilities should enter the charging state");
        helper.assertTrue(battle.playerState.getLockedSlot() == 1, "charge_up should lock the casting slot");
        helper.assertTrue(battle.playerState.getCurrentMana() == 80, "charge_up should reserve mana visually and spend it only on resolution");

        helper.runAfterDelay(14, () -> {
            helper.assertFalse(battle.playerState.isCharging(), "charge_up should finish after its delay");
            helper.assertTrue(battle.playerState.getLockedSlot() == -1, "charge_up should release the slot lock after resolving");
            helper.assertTrue(battle.playerState.getCurrentMana() < 80, "charge_up should spend mana when the charged cast resolves");
            helper.assertTrue(active.getCurrentHp() > damagedHp, "evil_laugh should heal after charge completion");
            helper.assertTrue(active.getCurrentHp() <= hpBefore, "healing should still respect max HP");
            helper.succeed();
        });
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void chargeUpRaycastDelayQueuesProjectileAfterCharge(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper, List.of(figure("starfire")), List.of(figure("joker")), ItemStack.EMPTY);

        BattleFigure active = battle.playerState.getActiveFigure();
        AbilityLoader.AbilityData data = abilityData(active, 0);
        int originalDelayTier = data.raycastDelayTier;
        data.raycastDelayTier = 3;

        battle.playerState.addMana(manaCost(active, 0) + 20);
        float manaBefore = battle.playerState.getCurrentMana();
        int chargeTicks = chargeUpBaseTicks(active, 0);

        AbilityExecutor.executeAction(battle.player, active, 0);

        helper.assertTrue(battle.playerState.isCharging(), "charged delayed raycasts should start charge-up first");
        helper.assertTrue(battle.playerState.getProjectiles().isEmpty(), "charged delayed raycasts should not queue a projectile before charge completes");
        helper.assertTrue(battle.playerState.getCurrentMana() == manaBefore, "charged delayed raycasts should reserve mana until charge completion");

        helper.runAfterDelay(chargeTicks + 2, () -> {
            try {
                helper.assertFalse(battle.playerState.isCharging(), "charge should complete before the projectile is fired");
                helper.assertTrue(battle.playerState.getProjectiles().size() == 1, "charged delayed raycasts should queue a projectile after charge completion");
                helper.assertTrue(battle.playerState.getCurrentMana() < manaBefore, "charged delayed raycasts should spend mana when the projectile is fired");
                helper.succeed();
            } finally {
                data.raycastDelayTier = originalDelayTier;
            }
        });
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void fastCastReducesChargeUpDelay(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper, List.of(figureWithChip("joker", chip(ModItems.CHIP_FAST_CAST.get()))), List.of(figure("batman")), ItemStack.EMPTY);

        BattleFigure active = battle.playerState.getActiveFigure();
        battle.playerState.addMana(80);
        int baseChargeTicks = chargeUpBaseTicks(active, 1);

        AbilityExecutor.executeAction(battle.player, active, 1);

        helper.assertTrue(battle.playerState.isCharging(), "fast cast test ability should still enter charge state");
        helper.assertTrue(battle.playerState.getChargeTicks() < baseChargeTicks, "fast cast should reduce charge-up delay");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 420)
    public static void blueChannelStartsTicksAndClearsLock(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper, List.of(figure("superman")), List.of(figure("joker")), ItemStack.EMPTY);

        battle.playerState.addMana(manaCost(battle.playerState.getActiveFigure(), 2) + 8);
        float manaBefore = battle.playerState.getCurrentMana();
        int hpBefore = battle.opponentState.getActiveFigure().getCurrentHp();

        AbilityExecutor.executeAction(battle.player, battle.playerState.getActiveFigure(), 2);

        helper.assertTrue(battle.playerState.isBlueChanneling(), "blue abilities should enter the channel state");
        helper.assertTrue(battle.playerState.getLockedSlot() == 2, "blue channel should lock the slot while active");
        int scheduledTicks = battle.playerState.getBlueChannelTicks();

        helper.runAfterDelay(5, () -> helper.assertTrue(battle.playerState.getCurrentMana() < manaBefore, "blue channel should drain mana over time"));
        helper.runAfterDelay(scheduledTicks + 5, () -> {
            helper.assertFalse(battle.playerState.isBlueChanneling(), "blue channel should end after its scheduled duration");
            helper.assertTrue(battle.playerState.getLockedSlot() == -1, "blue channel should clear the slot lock after finishing");
            helper.assertTrue(battle.opponentState.getActiveFigure().getCurrentHp() <= hpBefore, "blue channel should not heal the opponent");
            helper.succeed();
        });
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void delayedProjectileSpendsManaOnFireAndCancelsFlightOnResolution(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper, List.of(figure("cyborg")), List.of(figure("joker")), ItemStack.EMPTY);

        BattleFigure active = battle.playerState.getActiveFigure();
        battle.playerState.applyEffect("flight", 60, 1, 60.0f);
        battle.playerState.addMana(manaCost(active, 1) + 20);
        float manaBefore = battle.playerState.getCurrentMana();
        int hpBefore = battle.opponentState.getActiveFigure().getCurrentHp();

        AbilityExecutor.executeAction(battle.player, active, 1);

        helper.assertTrue(battle.playerState.getProjectiles().size() == 1, "delayed raycast abilities should queue a projectile");
        helper.assertTrue(battle.playerState.getCurrentMana() < manaBefore, "delayed projectiles should spend mana immediately when fired");
        helper.assertTrue(battle.playerState.hasEffect("flight"), "flight should still be active until the projectile resolves");

        IBattleState.PendingProjectile projectile = battle.playerState.getProjectiles().get(0);
        projectile.castPosition = battle.opponent.getEyePosition();
        AbilityExecutor.resolveProjectile(battle.playerState, battle.player, projectile);
        battle.playerState.getProjectiles().clear();

        helper.assertFalse(battle.playerState.hasEffect("flight"), "flight should cancel when resolveProjectile applies the delayed hit");
        helper.assertTrue(battle.opponentState.getActiveFigure().getCurrentHp() <= hpBefore, "resolveProjectile should not heal the opponent");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void fastDrawReducesProjectileDelay(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper, List.of(figureWithChip("cyborg", chip(ModItems.CHIP_FAST_DRAW.get()))), List.of(figure("joker")), ItemStack.EMPTY);

        BattleFigure active = battle.playerState.getActiveFigure();
        battle.playerState.addMana(manaCost(active, 1) + 20);
        int baseDelayTicks = projectileBaseDelayTicks(battle.player, battle.opponent, active, 1);

        AbilityExecutor.executeAction(battle.player, active, 1);

        helper.assertTrue(battle.playerState.getProjectiles().size() == 1, "fast draw test ability should still queue a projectile");
        helper.assertTrue(battle.playerState.getProjectiles().get(0).ticksRemaining < baseDelayTicks, "fast draw should reduce projectile delay");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void energeticBatteryBoostsPassiveChargePerTick(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper, List.of(figureWithChip("robin", chip(ModItems.CHIP_ENERGETIC_BATTERY.get()))), List.of(figure("joker")), ItemStack.EMPTY);

        float batteryBefore = battle.playerState.getBatteryCharge();
        battle.playerState.tick();

        helper.assertTrue(battle.playerState.getBatteryCharge() > batteryBefore + TeenyBalance.BATTERY_PASSIVE_CHARGE_RATE, "energetic battery should increase passive battery gain per tick");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void lastLaughAppliesOneConfiguredOpponentEffectOnFaint(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper, List.of(figureWithChip("robin", chip(ModItems.CHIP_LAST_LAUGH.get()))), List.of(figure("joker")), ItemStack.EMPTY);

        ChipExecutor.onFaint(battle.playerState, battle.player, battle.playerState.getActiveFigure(), battle.opponentState, battle.opponent);

        boolean appliedExpectedEffect = battle.opponentState.hasEffect("curse")
                || battle.opponentState.hasEffect("shock")
                || battle.opponentState.hasEffect("poison")
                || battle.opponentState.hasEffect("freeze_movement")
                || battle.opponentState.hasEffect("waffle");
        helper.assertTrue(appliedExpectedEffect, "last laugh should apply one configured random opponent effect");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void selfExplosionCountsAsGroupDamageAndBreaksFlight(GameTestHelper helper) {
        BattleHarness battle = startBattle(
                helper,
                List.of(figureWithChip("robin", chip(ModItems.CHIP_SELF_EXPLOSION.get()))),
                List.of(figure("joker"), figure("batman")),
                ItemStack.EMPTY
        );

        BattleFigure opponentActive = battle.opponentState.getActiveFigure();
        int activeHpBefore = opponentActive.getCurrentHp();
        int benchHpBefore = battle.opponentState.getTeam().get(1).getCurrentHp();
        battle.opponentState.applyEffect("flight", 60, 1, 60.0f);

        ChipExecutor.onFaint(battle.playerState, battle.player, battle.playerState.getActiveFigure(), battle.opponentState, battle.opponent);

        helper.assertFalse(battle.opponentState.hasEffect("flight"), "self explosion group damage should break flight before resolving damage");
        helper.assertTrue(opponentActive.getCurrentHp() < activeHpBefore, "self explosion should damage the opposing active figure");
        helper.assertTrue(battle.opponentState.getTeam().get(1).getCurrentHp() < benchHpBefore, "self explosion should also damage the opposing bench figure");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void regenerativeCutenessHealsAtConfiguredActiveInterval(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper, List.of(figureWithChip("robin", chip(ModItems.CHIP_REGENERATIVE_CUTENESS.get()))), List.of(figure("joker")), ItemStack.EMPTY);

        BattleFigure active = battle.playerState.getActiveFigure();
        battle.playerState.applyResolvedCombatFigureDelta(active, -10);
        int hpAfterDamage = active.getCurrentHp();

        for (int i = 0; i < TeenyBalance.CHIP_REGENERATIVE_CUTENESS_INTERVAL_BY_RANK[0] - 1; i++) {
            battle.playerState.tick();
        }

        helper.assertTrue(active.getCurrentHp() == hpAfterDamage, "regenerative cuteness should wait until its full interval before healing");
        battle.playerState.tick();
        helper.assertTrue(active.getCurrentHp() == hpAfterDamage + TeenyBalance.CHIP_REGENERATIVE_CUTENESS_HEAL_BY_RANK[0],
                "regenerative cuteness should heal the configured amount at its active interval");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void tofuLoverExposesHigherTofuChanceMultiplier(GameTestHelper helper) {
        BattleFigure figure = new BattleFigure(figureWithChip("robin", chip(ModItems.CHIP_TOFU_LOVER.get())));

        helper.assertTrue(ChipExecutor.getTofuChanceMultiplier(figure) > 1.0f,
                "tofu lover should raise the tofu spawn chance multiplier above the baseline");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void helloNurseBoostsAbilityHealMagnitude(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper, List.of(figureWithChip("joker", chip(ModItems.CHIP_HELLO_NURSE.get()))), List.of(figure("robin")), ItemStack.EMPTY);

        BattleFigure active = battle.playerState.getActiveFigure();
        battle.playerState.applyResolvedCombatFigureDelta(active, -40);
        int hpBefore = active.getCurrentHp();
        AbilityLoader.AbilityData healData = new AbilityLoader.AbilityData();
        int expectedHeal = Math.round(EffectCalculator.calculateHealMagnitude(active, 10, 1.0f)
                * ChipExecutor.getAbilityHealMultiplier(active));

        EffectApplierRegistry.getValidated("heal").apply(
                battle.playerState,
                battle.player,
                active,
                healData,
                10,
                List.of(1.0f),
                battle.player
        );

        helper.assertTrue(active.getCurrentHp() - hpBefore == expectedHeal,
                "hello nurse should scale direct healing ability output");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void teamMedicSplashesBenchHealOnSelfHeal(GameTestHelper helper) {
        BattleHarness battle = startBattle(
                helper,
                List.of(figureWithChip("robin", chip(ModItems.CHIP_TEAM_MEDIC.get())), figure("batman")),
                List.of(figure("joker")),
                ItemStack.EMPTY
        );

        BattleFigure active = battle.playerState.getActiveFigure();
        BattleFigure bench = battle.playerState.getTeam().get(1);
        battle.playerState.applyResolvedCombatFigureDelta(active, -30);
        battle.playerState.applyResolvedCombatFigureDelta(bench, -20);
        int benchBeforeHeal = bench.getCurrentHp();

        battle.playerState.applyResolvedCombatFigureDelta(active, 20,
                new IBattleState.CombatMutationSource(battle.playerState, battle.player, active));

        int expectedBenchHeal = Math.max(1, Math.round(20 * TeenyBalance.CHIP_TEAM_MEDIC_BENCH_HEAL_PCT_BY_RANK[0]));
        helper.assertTrue(bench.getCurrentHp() - benchBeforeHeal == expectedBenchHeal,
                "team medic should heal bench allies for the configured fraction of a self-heal");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void pointyRetaliatesOnlyOncePerReactionId(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper, List.of(figure("joker")), List.of(figureWithChip("robin", chip(ModItems.CHIP_POINTY.get()))), ItemStack.EMPTY);

        BattleFigure attackerFigure = battle.playerState.getActiveFigure();
        BattleFigure defenderFigure = battle.opponentState.getActiveFigure();
        int attackerHpBefore = attackerFigure.getCurrentHp();
        int reactionId = AbilityExecutor.nextAccessoryReactionId();

        AbilityExecutor.applyDamageToFigure(
                battle.playerState,
                battle.player,
                battle.opponent,
                battle.opponentState,
                defenderFigure,
                undodgeableHit(5, false),
                null,
                0,
                false,
                false,
                false,
                reactionId,
                true
        );

        int attackerHpAfterFirstHit = attackerFigure.getCurrentHp();

        AbilityExecutor.applyDamageToFigure(
                battle.playerState,
                battle.player,
                battle.opponent,
                battle.opponentState,
                defenderFigure,
                undodgeableHit(5, false),
                null,
                0,
                false,
                false,
                false,
                reactionId,
                true
        );

        helper.assertTrue(attackerHpBefore - attackerHpAfterFirstHit == TeenyBalance.CHIP_POINTY_DAMAGE_BY_RANK[0],
                "pointy should retaliate for its configured flat damage");
        helper.assertTrue(attackerFigure.getCurrentHp() == attackerHpAfterFirstHit,
                "pointy should not retaliate more than once for the same multi-hit reaction id");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void stunResisterOnlyReducesDirectStunDuration(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper, List.of(figureWithChip("robin", chip(ModItems.CHIP_STUN_RESISTER.get()))), List.of(figure("joker")), ItemStack.EMPTY);

        BattleFigure defender = battle.playerState.getActiveFigure();
        AbilityLoader.AbilityData directStunData = new AbilityLoader.AbilityData();
        int baseDuration = EffectCalculator.calculateStunDuration(battle.opponentState.getActiveFigure(), battle.opponentState, 20, 1.0f);

        EffectApplierRegistry.getValidated("stun").apply(
                battle.opponentState,
                battle.opponent,
                battle.opponentState.getActiveFigure(),
                directStunData,
                20,
                List.of(1.0f),
                battle.player
        );

        EffectInstance directStun = battle.playerState.getEffectInstance("stun");
        int expectedReducedDuration = Math.max(1, Math.round(baseDuration * ChipExecutor.getDirectStunDurationMultiplier(defender)));
        helper.assertTrue(directStun != null && directStun.duration == expectedReducedDuration,
                "stun resister should reduce direct stun duration");

        battle.playerState.removeEffect("stun");
        battle.playerState.applyEffect("shock", 1, 1, 40.0f);
        battle.playerState.tick();

        EffectInstance shockStun = battle.playerState.getEffectInstance("stun");
        helper.assertTrue(shockStun != null && shockStun.duration == 40,
                "stun resister should not reduce stun applied indirectly by shock");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void finisherChecksVictimHpOnEachHit(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper, List.of(figureWithChip("robin", chip(ModItems.CHIP_FINISHER.get()))), List.of(figure("joker")), ItemStack.EMPTY);

        BattleFigure victim = battle.opponentState.getActiveFigure();
        int thresholdHp = Math.round(victim.getMaxHp() * TeenyBalance.CHIP_FINISHER_HP_THRESHOLD_BY_RANK[0]);
        battle.opponentState.applyResolvedCombatFigureDelta(victim, -(victim.getMaxHp() - (thresholdHp + 5)));

        int hpBeforeFirstHit = victim.getCurrentHp();
        AbilityExecutor.applyDamageToFigure(
                battle.playerState,
                battle.player,
                battle.opponent,
                battle.opponentState,
                victim,
                undodgeableHit(10, false),
                null,
                0,
                false,
                false,
                false,
                AbilityExecutor.nextAccessoryReactionId(),
                true
        );

        int firstHitDamage = hpBeforeFirstHit - victim.getCurrentHp();
        int hpBeforeSecondHit = victim.getCurrentHp();

        AbilityExecutor.applyDamageToFigure(
                battle.playerState,
                battle.player,
                battle.opponent,
                battle.opponentState,
                victim,
                undodgeableHit(10, false),
                null,
                0,
                false,
                false,
                false,
                AbilityExecutor.nextAccessoryReactionId(),
                true
        );

        int secondHitDamage = hpBeforeSecondHit - victim.getCurrentHp();
        helper.assertTrue(firstHitDamage == 10, "finisher should not boost hits while the victim is above the low-health threshold");
        helper.assertTrue(secondHitDamage > firstHitDamage, "finisher should boost later hits once the victim drops below the threshold");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void opponentDelayedProjectileResolvesUsingOpponentOwnerEntity(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper, List.of(figure("joker")), List.of(figure("cyborg")), ItemStack.EMPTY);

        BattleFigure active = battle.opponentState.getActiveFigure();
        battle.opponentState.applyEffect("flight", 60, 1, 60.0f);
        battle.opponentState.addMana(manaCost(active, 1) + 20);
        float manaBefore = battle.opponentState.getCurrentMana();
        int hpBefore = battle.playerState.getActiveFigure().getCurrentHp();

        AbilityExecutor.executeAction(battle.opponent, active, 1);

        helper.assertTrue(battle.opponentState.getProjectiles().size() == 1, "opponent delayed raycast abilities should queue a projectile");
        helper.assertTrue(battle.opponentState.getCurrentMana() < manaBefore, "opponent delayed projectiles should spend mana immediately when fired");
        helper.assertTrue(battle.opponentState.hasEffect("flight"), "opponent flight should still be active until the projectile resolves");

        IBattleState.PendingProjectile projectile = battle.opponentState.getProjectiles().get(0);
        projectile.castPosition = battle.player.getEyePosition();
        AbilityExecutor.resolveProjectile(battle.opponentState, battle.opponent, projectile);
        battle.opponentState.getProjectiles().clear();

        helper.assertFalse(battle.opponentState.hasEffect("flight"), "opponent flight should cancel when the projectile resolves");
        helper.assertTrue(battle.playerState.getActiveFigure().getCurrentHp() <= hpBefore, "opponent projectile resolution should not heal the player");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void opponentPresentationTracksDamageAndSwap(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper, List.of(figure("joker")), List.of(figure("robin"), figure("batman")), ItemStack.EMPTY);

        BattleFigure openingFigure = battle.opponentState.getActiveFigure();
        helper.assertTrue(Math.abs(battle.opponent.getMaxHealth() - openingFigure.getMaxHp()) < 0.01f,
                "dummy max health should sync to the opening active figure");

        battle.opponentState.applyResolvedCombatFigureDelta(openingFigure, -5);
        helper.assertTrue(Math.abs(battle.opponent.getHealth() - openingFigure.getCurrentHp()) < 0.01f,
                "dummy health should follow active-figure HP changes");

        battle.opponentState.swapFigure(1, null);
        BattleFigure swappedFigure = battle.opponentState.getActiveFigure();

        helper.assertTrue("batman".equals(swappedFigure.getFigureId()), "opponent swap should activate the next figure even without a player owner");
        helper.assertTrue(Math.abs(battle.opponent.getMaxHealth() - swappedFigure.getMaxHp()) < 0.01f,
                "dummy max health should sync after an opponent-side swap");
        helper.assertTrue(Math.abs(battle.opponent.getHealth() - swappedFigure.getCurrentHp()) < 0.01f,
                "dummy health should sync after an opponent-side swap");
        helper.assertTrue(battle.opponent.getCustomName() != null && battle.opponent.getCustomName().getString().contains(swappedFigure.getNickname()),
                "dummy name should reflect the active opponent figure after a swap");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void accessoryActivationAppliesAndRemovesTitansCoinBonus(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper, List.of(figure("robin")), List.of(figure("joker")), new ItemStack(ModItems.ACCESSORY_TITANS_COIN.get()));

        BattleFigure active = battle.playerState.getActiveFigure();
        int baseMaxHp = active.getBaseMaxHp();

        battle.playerState.addBatteryCharge(TeenyBalance.ACCESSORY_ACTIVATION_MIN_CHARGE);
        helper.assertTrue(battle.playerState.tryActivateAccessory(), "battery threshold should allow accessory activation");
        helper.assertTrue(battle.playerState.isAccessoryActive(), "accessory should be active after a successful activation");
        helper.assertTrue("titans_coin".equals(battle.playerState.getActiveAccessoryId()), "active accessory id should match the equipped accessory");
        helper.assertTrue(active.getMaxHp() > baseMaxHp, "titans coin should grant a max HP bonus while active");

        battle.playerState.forceDeactivateAccessory();
        helper.assertFalse(battle.playerState.isAccessoryActive(), "forced deactivation should clear the accessory active flag");
        helper.assertTrue(active.getMaxHp() == baseMaxHp, "titans coin HP bonus should be removed on deactivation");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void accessoryActivationMilestoneBecomesPurchaseReady(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper, List.of(figure("robin")), List.of(figure("joker")),
                new ItemStack(ModItems.ACCESSORY_TITANS_COIN.get()));
        var mastery = battle.player.getCapability(AccessoryMasteryProvider.ACCESSORY_MASTERY)
                .orElseThrow(IllegalStateException::new);
        mastery.reset("titans_coin");

        battle.playerState.addBatteryCharge(TeenyBalance.ACCESSORY_ACTIVATION_MIN_CHARGE);
        for (int i = 0; i < TeenyBalance.ACCESSORY_TIER_2_ACTIVATIONS_REQUIRED; i++) {
            helper.assertTrue(battle.playerState.tryActivateAccessory(), "activation should start");
            battle.playerState.forceDeactivateAccessory();
        }

        helper.assertTrue(mastery.getTier("titans_coin") == 1, "milestone completion must not auto-purchase Tier 2");
        helper.assertTrue(mastery.getMilestoneProgress("titans_coin") == TeenyBalance.ACCESSORY_TIER_2_ACTIVATIONS_REQUIRED,
                "three activations should reach the Tier 2 target");
        helper.assertTrue(mastery.isCurrentMilestoneComplete("titans_coin"),
                "three activations should make Tier 2 purchase-ready");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void titansCoinFinalMilestoneLeavesAllFiguresAtOneHp(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper,
                List.of(figure("robin"), figure("raven"), figure("cyborg")),
                List.of(figure("joker")), new ItemStack(ModItems.ACCESSORY_TITANS_COIN.get()));
        var mastery = battle.player.getCapability(AccessoryMasteryProvider.ACCESSORY_MASTERY)
                .resolve().orElseThrow(() -> new IllegalStateException("missing accessory mastery"));
        mastery.setTier("titans_coin", 4);
        battle.playerState.addBatteryCharge(100);

        helper.assertTrue(battle.playerState.tryActivateAccessory(), "Titan's Coin should activate");
        for (BattleFigure figure : battle.playerState.getTeam()) {
            int bonus = figure.getMaxHp() - figure.getBaseMaxHp();
            figure.modifyHp(-(figure.getCurrentHp() - bonus));
        }
        battle.playerState.forceDeactivateAccessory();

        helper.assertTrue(battle.playerState.getTeam().stream().allMatch(figure -> figure.getCurrentHp() == 1),
                "removing Titan's Coin should preserve each living figure at 1 HP");
        helper.assertTrue(mastery.getMilestoneProgress("titans_coin") == 1,
                "the Tier 5 survival milestone should gain one completion");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void accessoryPowerUpDamageRetainsSourceContribution(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper, List.of(figure("robin")), List.of(figure("joker")),
                new ItemStack(ModItems.ACCESSORY_RED_LANTERN_BATTERY.get()));
        battle.playerState.addBatteryCharge(100);
        helper.assertTrue(battle.playerState.tryActivateAccessory(), "Red Lantern Battery should activate");

        DamagePipeline.DamageResult hit = undodgeableHit(30, false);
        hit.accessoryBonusDamage.put("red_lantern_battery", 10);
        AbilityExecutor.applyDamageToFigure(
                battle.playerState, battle.player, battle.opponent, battle.opponentState,
                battle.opponentState.getActiveFigure(), hit, null, 0, false, false, false,
                AbilityExecutor.nextAccessoryReactionId(), true);

        long attributed = battle.playerState.getAccessoryBattleProgressTracker().battleSnapshot()
                .total(bruhof.teenycraft.accessory.AccessoryContribution.Type.POWER_UP_BONUS_DAMAGE);
        helper.assertTrue(attributed > 0, "actual damage should retain its Red Lantern accessory contribution");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void accessoryOpponentEffectUsesPairedOpponent(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper, List.of(figure("robin")), List.of(figure("joker")), new ItemStack(ModItems.ACCESSORY_RAVENS_SPELLBOOK.get()));
        NearbyBattler distractor = spawnNearbyBattler(helper, DISTRACTOR_POS, figure("superman"));

        AccessorySpec spec = AccessoryRegistry.get("ravens_spellbook");
        if (spec == null) {
            throw new IllegalStateException("missing ravens_spellbook accessory spec");
        }

        AccessoryExecutor.onTick(battle.playerState, battle.player, spec, spec.getIntervalTicks());

        helper.assertTrue(battle.opponentState.hasEffect("curse"), "accessory effect should apply to the paired opponent");
        helper.assertTrue("ravens_spellbook".equals(battle.opponentState.getEffectSourceAccessoryId("curse")),
                "accessory-applied effects should retain their source accessory id");
        helper.assertFalse(distractor.state.hasEffect("curse"), "accessory effect should not apply to an unpaired nearby battler");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void masteredTitansCoinHealsEveryLivingTeamFigureOnActivation(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper,
                List.of(figure("robin"), figure("raven"), figure("cyborg")),
                List.of(figure("joker")), new ItemStack(ModItems.ACCESSORY_TITANS_COIN.get()));
        setMasteryTier(battle, "titans_coin", 5);

        int[] hpBefore = new int[battle.playerState.getTeam().size()];
        for (int i = 0; i < hpBefore.length; i++) {
            BattleFigure figure = battle.playerState.getTeam().get(i);
            figure.modifyHp(-50);
            hpBefore[i] = figure.getCurrentHp();
        }
        battle.playerState.addBatteryCharge(100);
        helper.assertTrue(battle.playerState.tryActivateAccessory(), "mastered Titan's Coin should activate");

        for (int i = 0; i < hpBefore.length; i++) {
            BattleFigure figure = battle.playerState.getTeam().get(i);
            int expectedHeal = Math.round(figure.getBaseMaxHp() * TeenyBalance.ACCESSORY_TITANS_COIN_TIER_5_HEAL_PCT);
            helper.assertTrue(figure.getCurrentHp() - hpBefore[i] == expectedHeal,
                    "mastered Titan's Coin should heal from original max HP");
        }
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void masteredGreenLanternFillsManaOnActivation(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper, List.of(figure("robin")), List.of(figure("joker")),
                new ItemStack(ModItems.ACCESSORY_GREEN_LANTERN_BATTERY.get()));
        setMasteryTier(battle, "green_lantern_battery", 5);
        battle.playerState.addBatteryCharge(100);

        helper.assertTrue(battle.playerState.tryActivateAccessory(), "mastered Green Lantern Battery should activate");
        helper.assertTrue(battle.playerState.getCurrentMana() == TeenyBalance.BATTLE_MANA_MAX,
                "mastered Green Lantern Battery should immediately fill mana");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void masteredRavensSpellbookSlowsItsCursedTarget(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper, List.of(figure("robin")), List.of(figure("joker")),
                new ItemStack(ModItems.ACCESSORY_RAVENS_SPELLBOOK.get()));
        setMasteryTier(battle, "ravens_spellbook", 5);
        double speedBefore = battle.opponent.getAttributeValue(Attributes.MOVEMENT_SPEED);
        battle.playerState.addBatteryCharge(100);

        helper.assertTrue(battle.playerState.tryActivateAccessory(), "mastered Raven's Spellbook should activate");
        double speedAfter = battle.opponent.getAttributeValue(Attributes.MOVEMENT_SPEED);
        helper.assertTrue(battle.opponentState.hasEffect("ravens_slow"), "the cursed target should receive Raven's slow");
        helper.assertTrue(Math.abs(speedAfter - speedBefore * TeenyBalance.ACCESSORY_RAVENS_SPELLBOOK_TIER_5_SPEED_MULT) < 0.00001d,
                "Raven's slow should multiply final movement speed by 0.8");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void masteredUnderpantsOnlySpendBatteryWhenBlockingDamage(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper, List.of(figure("superman")), List.of(figure("joker")),
                new ItemStack(ModItems.ACCESSORY_SUPERMANS_UNDERPANTS.get()));
        setMasteryTier(battle, "supermans_underpants", 5);
        battle.playerState.addBatteryCharge(100);
        helper.assertTrue(battle.playerState.tryActivateAccessory(), "mastered Superman's Underpants should activate");

        for (int i = 0; i < 20; i++) {
            battle.playerState.tick();
        }
        helper.assertTrue(battle.playerState.getBatteryCharge() == 100.0f,
                "mastered underpants should not drain battery passively");

        int blocked = AccessoryExecutor.onIncomingDamage(battle.playerState, battle.player,
                battle.playerState.getActiveFigure(), battle.opponent, 10,
                AbilityExecutor.nextAccessoryReactionId(), true);
        helper.assertTrue(blocked == 0, "underpants should still block incoming damage");
        helper.assertTrue(battle.playerState.getBatteryCharge() == 97.0f,
                "underpants should spend battery for blocked damage");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void masteredKryptoGrantsTwoDifferentEffectsPerPulse(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper, List.of(figure("robin")), List.of(figure("joker")),
                new ItemStack(ModItems.ACCESSORY_KRYPTO_THE_SUPERDOG.get()));
        setMasteryTier(battle, "krypto_the_superdog", 5);
        battle.playerState.getActiveFigure().modifyHp(-20);
        battle.playerState.addBatteryCharge(100);

        helper.assertTrue(battle.playerState.tryActivateAccessory(), "mastered Krypto should activate");
        long granted = battle.playerState.getAccessoryBattleProgressTracker().battleSnapshot()
                .total(bruhof.teenycraft.accessory.AccessoryContribution.Type.KRYPTO_EFFECT_GRANTED);
        helper.assertTrue(granted == TeenyBalance.ACCESSORY_KRYPTO_TIER_5_EFFECT_COUNT,
                "mastered Krypto should grant two useful, different outcomes per pulse");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void masteredBatSignalPiercesShieldAndFlight(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper, List.of(figure("robin")), List.of(figure("joker")),
                new ItemStack(ModItems.ACCESSORY_BAT_SIGNAL.get()));
        setMasteryTier(battle, "bat_signal", 5);
        battle.opponentState.applyEffect("shield", 100, 1);
        battle.opponentState.applyEffect("flight", 100, 1, 100.0f);
        int hpBefore = battle.opponentState.getActiveFigure().getCurrentHp();
        battle.playerState.addBatteryCharge(100);
        helper.assertTrue(battle.playerState.tryActivateAccessory(), "mastered Bat Signal should activate");

        AccessorySpec spec = AccessoryRegistry.get("bat_signal");
        AccessoryExecutor.onTick(battle.playerState, battle.player,
                bruhof.teenycraft.accessory.AccessoryTierResolver.resolve(spec, 5),
                bruhof.teenycraft.accessory.AccessoryTierResolver.resolve(spec, 5).intervalTicks());
        helper.assertFalse(battle.opponentState.hasEffect("shield"), "Bat Signal should consume and pierce the shield");
        helper.assertTrue(battle.opponentState.getActiveFigure().getCurrentHp() < hpBefore,
                "Bat Signal should damage a flying target through its shield");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void masteredVioletOverhealPersistsAndOnlyVioletCanIncreaseIt(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper, List.of(figure("raven")), List.of(figure("joker")),
                new ItemStack(ModItems.ACCESSORY_VIOLET_LANTERN_BATTERY.get()));
        setMasteryTier(battle, "violet_lantern_battery", 5);
        BattleFigure active = battle.playerState.getActiveFigure();
        int normalMaxHp = active.getMaxHp();
        active.modifyHp(-5);
        battle.playerState.addBatteryCharge(100);

        helper.assertTrue(battle.playerState.tryActivateAccessory(), "mastered Violet Lantern Battery should activate");
        helper.assertTrue(active.getCurrentHp() > normalMaxHp, "Violet's own pulse should overheal above normal max HP");
        helper.assertTrue(active.getCurrentHp() <= Math.round(normalMaxHp
                        * (1.0f + TeenyBalance.ACCESSORY_VIOLET_LANTERN_TIER_5_OVERHEAL_PCT)),
                "Violet overheal should respect its 30% cap");

        battle.playerState.forceDeactivateAccessory();
        int overhealedHp = active.getCurrentHp();
        battle.playerState.applyResolvedCombatFigureDelta(active, 20);
        helper.assertTrue(active.getCurrentHp() == overhealedHp,
                "ordinary healing should neither increase nor erase existing overheal");
        battle.playerState.applyResolvedCombatFigureDelta(active, -1);
        helper.assertTrue(active.getCurrentHp() == overhealedHp - 1,
                "damage should remove overheal normally after Violet deactivates");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void masteredRedLanternRetainsHalfItsPowerUpAfterConsumption(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper, List.of(figure("robin")), List.of(figure("joker")),
                new ItemStack(ModItems.ACCESSORY_RED_LANTERN_BATTERY.get()));
        setMasteryTier(battle, "red_lantern_battery", 5);
        battle.playerState.addBatteryCharge(100);
        helper.assertTrue(battle.playerState.tryActivateAccessory(), "mastered Red Lantern Battery should activate");

        int granted = battle.playerState.getEffectAccessoryMagnitude("power_up", "red_lantern_battery");
        battle.playerState.triggerOnAttack(battle.playerState.getActiveFigure());
        int retained = battle.playerState.getEffectAccessoryMagnitude("power_up", "red_lantern_battery");
        helper.assertTrue(retained == Math.round(granted
                        * TeenyBalance.ACCESSORY_RED_LANTERN_TIER_5_POWER_UP_RETENTION),
                "consuming Power Up should retain half of the Red Lantern-owned magnitude");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 300)
    public static void masteredWaffleShooterAddsDifferentSecondarySlotEveryFifthHit(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper, List.of(figure("cyborg")), List.of(figure("joker")),
                new ItemStack(ModItems.ACCESSORY_CYBORGS_WAFFLE_SHOOTER.get()));
        setMasteryTier(battle, "cyborgs_waffle_shooter", 5);
        battle.playerState.addBatteryCharge(100);
        helper.assertTrue(battle.playerState.tryActivateAccessory(), "mastered Waffle Shooter should activate");

        AccessorySpec base = AccessoryRegistry.get("cyborgs_waffle_shooter");
        bruhof.teenycraft.accessory.ResolvedAccessorySpec spec =
                bruhof.teenycraft.accessory.AccessoryTierResolver.resolve(base, 5);
        for (int pulse = 2; pulse <= TeenyBalance.ACCESSORY_WAFFLE_SHOOTER_TIER_5_SECONDARY_INTERVAL; pulse++) {
            AccessoryExecutor.onTick(battle.playerState, battle.player, spec, spec.intervalTicks() * pulse);
        }

        helper.assertTrue(battle.opponentState.hasEffect("waffle_secondary"),
                "the fifth successful waffle should add a secondary waffle");
        helper.assertTrue(battle.opponentState.getEffectMagnitude("waffle")
                        != battle.opponentState.getEffectMagnitude("waffle_secondary"),
                "primary and secondary waffles should block different slots");
        helper.assertTrue(battle.opponentState.getEffectInstance("waffle_secondary").duration
                        == Math.round(spec.effectDurationTicks()
                        * TeenyBalance.ACCESSORY_WAFFLE_SHOOTER_TIER_5_SECONDARY_DURATION_MULT),
                "the secondary waffle should use half duration");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void masteredKryptoniteReducesDamageSpeedAndManaRegeneration(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper, List.of(figure("robin")), List.of(figure("joker")),
                new ItemStack(ModItems.ACCESSORY_KRYPTONITE.get()));
        setMasteryTier(battle, "kryptonite", 5);
        AbilityLoader.AbilityData damageData = new AbilityLoader.AbilityData();
        damageData.damageTier = 1;
        DamagePipeline.DamageResult normalDamage = DamagePipeline.calculateOutput(
                battle.opponentState, battle.opponentState.getActiveFigure(), damageData, 20, false);
        double speedBefore = battle.opponent.getAttributeValue(Attributes.MOVEMENT_SPEED);
        battle.playerState.addBatteryCharge(100);

        helper.assertTrue(battle.playerState.tryActivateAccessory(), "mastered Kryptonite should activate");
        DamagePipeline.DamageResult reducedDamage = DamagePipeline.calculateOutput(
                battle.opponentState, battle.opponentState.getActiveFigure(), damageData, 20, false);
        helper.assertTrue(reducedDamage.baseDamagePerHit == Math.round(normalDamage.baseDamagePerHit
                        * TeenyBalance.ACCESSORY_KRYPTONITE_TIER_5_PENALTY_MULT),
                "Kryptonite should multiply outgoing damage by 0.9");
        helper.assertTrue(Math.abs(battle.opponent.getAttributeValue(Attributes.MOVEMENT_SPEED)
                        - speedBefore * TeenyBalance.ACCESSORY_KRYPTONITE_TIER_5_PENALTY_MULT) < 0.00001d,
                "Kryptonite should multiply final movement speed by 0.9");

        float manaBefore = battle.opponentState.getCurrentMana();
        battle.opponentState.regenMana();
        float expectedRegen = (TeenyBalance.BATTLE_MANA_REGEN_PER_SEC / 20.0f)
                * TeenyBalance.ACCESSORY_KRYPTONITE_TIER_5_PENALTY_MULT;
        helper.assertTrue(Math.abs((battle.opponentState.getCurrentMana() - manaBefore) - expectedRegen) < 0.0001f,
                "Kryptonite should multiply mana regeneration by 0.9");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void masteredBirdarangRetaliatesWhileInactive(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper, List.of(figure("robin")), List.of(figure("joker")),
                new ItemStack(ModItems.ACCESSORY_BIRDARANG.get()));
        setMasteryTier(battle, "birdarang", 5);
        int attackerHpBefore = battle.opponentState.getActiveFigure().getCurrentHp();

        for (int attempt = 0; attempt < 10
                && battle.opponentState.getActiveFigure().getCurrentHp() == attackerHpBefore; attempt++) {
            AccessoryExecutor.onIncomingDamage(battle.playerState, battle.player,
                    battle.playerState.getActiveFigure(), battle.opponent, 1,
                    AbilityExecutor.nextAccessoryReactionId(), true);
        }

        helper.assertFalse(battle.playerState.isAccessoryActive(), "passive Birdarang should not require activation");
        helper.assertTrue(battle.opponentState.getActiveFigure().getCurrentHp() < attackerHpBefore,
                "a mastered inactive Birdarang should retaliate against the attacker");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void remoteMinePathUsesPairedOpponentWhenDistractorIsNearby(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper, List.of(figure("batman")), List.of(figure("joker")), ItemStack.EMPTY);
        NearbyBattler distractor = spawnNearbyBattler(helper, DISTRACTOR_POS, figure("superman"));

        battle.playerState.addMana(100);
        AbilityExecutor.executeAction(battle.player, battle.playerState.getActiveFigure(), 0);

        helper.assertTrue(battle.opponentState.hasEffect("remote_mine_0"), "first bat_mine use should place a mine on the paired opponent");
        helper.assertFalse(distractor.state.hasEffect("remote_mine_0"), "first bat_mine use should not place a mine on an unpaired nearby battler");
        int hpBeforeDetonation = battle.opponentState.getActiveFigure().getCurrentHp();
        int distractorHpBeforeDetonation = distractor.state.getActiveFigure().getCurrentHp();

        battle.playerState.addMana(100);
        AbilityExecutor.executeAction(battle.player, battle.playerState.getActiveFigure(), 0);

        helper.assertFalse(battle.opponentState.hasEffect("remote_mine_0"), "second bat_mine use should detonate and remove the existing mine from the paired opponent");
        helper.assertTrue(battle.opponentState.getActiveFigure().getCurrentHp() <= hpBeforeDetonation, "detonation should not increase paired opponent HP");
        helper.assertTrue(distractor.state.getActiveFigure().getCurrentHp() == distractorHpBeforeDetonation, "detonation should not hit an unpaired nearby battler");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 40)
    public static void multiHitOnlyFlagsLastHitForClassBonus(GameTestHelper helper) {
        DamagePipeline.DamageResult attack = new DamagePipeline.DamageResult(11, 3, false, false);
        attack.classBonusEligible = true;

        DamagePipeline.DamageResult[] hits = DamagePipeline.splitIntoHitResults(attack);

        helper.assertTrue(hits.length == 3, "multi-hit damage should split into one result per hit");
        helper.assertFalse(hits[0].classBonusEligible, "class bonus should not apply on the first multi-hit split");
        helper.assertFalse(hits[1].classBonusEligible, "class bonus should not apply on middle multi-hit splits");
        helper.assertTrue(hits[2].classBonusEligible, "class bonus should only apply on the last multi-hit split");
        helper.assertTrue(hits[0].baseDamagePerHit + hits[1].baseDamagePerHit + hits[2].baseDamagePerHit == 11,
                "multi-hit splitting should preserve the original damage pool");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 80)
    public static void classAdvantageAddsMinimumOneDamageToDirectHit(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper, List.of(figure("harley_quinn")), List.of(figure("raven")), ItemStack.EMPTY);

        BattleFigure victim = battle.opponentState.getActiveFigure();
        int hpBefore = victim.getCurrentHp();

        AbilityExecutor.applyDamageToFigure(
                battle.playerState,
                battle.player,
                battle.opponent,
                battle.opponentState,
                victim,
                undodgeableHit(1, true),
                null,
                0,
                false,
                false,
                false,
                AbilityExecutor.nextAccessoryReactionId(),
                true
        );

        helper.assertTrue(hpBefore - victim.getCurrentHp() == 2,
                "class advantage should add at least 1 extra damage to a direct hit");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 80)
    public static void classAdvantageIsCalculatedPerVictimFigure(GameTestHelper helper) {
        BattleHarness battle = startBattle(
                helper,
                List.of(figure("harley_quinn")),
                List.of(figure("raven"), figure("superman")),
                ItemStack.EMPTY
        );

        BattleFigure darkArtsVictim = battle.opponentState.getTeam().get(0);
        BattleFigure superVictim = battle.opponentState.getTeam().get(1);
        int darkArtsHpBefore = darkArtsVictim.getCurrentHp();
        int superHpBefore = superVictim.getCurrentHp();

        AbilityExecutor.applyDamageToFigure(
                battle.playerState,
                battle.player,
                battle.opponent,
                battle.opponentState,
                darkArtsVictim,
                undodgeableHit(10, true),
                null,
                0,
                false,
                false,
                false,
                AbilityExecutor.nextAccessoryReactionId(),
                true
        );

        AbilityExecutor.applyDamageToFigure(
                battle.playerState,
                battle.player,
                battle.opponent,
                battle.opponentState,
                superVictim,
                undodgeableHit(10, true),
                null,
                0,
                false,
                false,
                false,
                AbilityExecutor.nextAccessoryReactionId(),
                true
        );

        helper.assertTrue(darkArtsHpBefore - darkArtsVictim.getCurrentHp() == 12,
                "Cute damage should gain the configured 20% bonus against Dark Arts figures");
        helper.assertTrue(superHpBefore - superVictim.getCurrentHp() == 10,
                "the same hit should not gain bonus damage against a figure outside the class matchup");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void goldenBonusAddsExtraPowerUpMagnitude(GameTestHelper helper) {
        ItemStack goldenRobin = figure("robin");
        ItemFigure.setGolden(goldenRobin, 1, true);
        BattleHarness battle = startBattle(helper, List.of(goldenRobin), List.of(figure("joker")), ItemStack.EMPTY);

        BattleFigure active = battle.playerState.getActiveFigure();
        int manaCost = manaCost(active, 0);
        int expectedMagnitude = EffectCalculator.calculatePowerUpMagnitude(active, manaCost, 1.0f)
                + EffectCalculator.calculatePowerUpMagnitude(active, manaCost, 0.3f);

        battle.playerState.addMana(100);
        AbilityExecutor.executeAction(battle.player, active, 0);

        helper.assertTrue(battle.playerState.getEffectMagnitude("power_up") == expectedMagnitude, "golden heroic_pose should apply both the base and golden power_up magnitudes");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void goldenSelfBonusSupportsMultiParamParsedContract(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper, List.of(figure("robin")), List.of(figure("joker")), ItemStack.EMPTY);

        AbilityLoader.AbilityData data = new AbilityLoader.AbilityData();
        data.id = "phase7_self_bonus_test";
        data.name = "Phase 7 Self Bonus Test";
        data.hitType = "none";
        data.damageTier = 0;
        data.goldenBonus.add("self:health_radio:1.0,0.3");
        data.parsedGoldenBonus.add(AbilityLoader.parseGoldenBonus("self:health_radio:1.0,0.3"));

        AbilityExecutor.executeDebugCast(
                battle.playerState,
                battle.player,
                battle.playerState.getActiveFigure(),
                data,
                10,
                true,
                battle.opponent
        );

        helper.assertTrue(battle.playerState.hasEffect("health_radio"), "parsed golden self bonuses should preserve multi-param effect payloads");
        helper.succeed();
    }

    @GameTest(template = "arenas/arena1", timeoutTicks = 200)
    public static void goldenOpponentBonusUsesParsedContract(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper, List.of(figure("robin")), List.of(figure("joker")), ItemStack.EMPTY);

        AbilityLoader.AbilityData data = new AbilityLoader.AbilityData();
        data.id = "phase7_opponent_bonus_test";
        data.name = "Phase 7 Opponent Bonus Test";
        data.hitType = "melee";
        data.damageTier = 1;
        data.traits.add(trait("undodgeable"));
        data.goldenBonus.add("opponent:stun:1.0");
        data.parsedGoldenBonus.add(AbilityLoader.parseGoldenBonus("opponent:stun:1.0"));

        AbilityExecutor.executeDebugCast(
                battle.playerState,
                battle.player,
                battle.playerState.getActiveFigure(),
                data,
                10,
                true,
                battle.opponent
        );

        helper.assertTrue(battle.opponentState.hasEffect("stun"), "parsed golden opponent bonuses should still apply through the validated effect contract");
        helper.succeed();
    }

    private static BattleHarness startBattle(GameTestHelper helper,
                                             List<ItemStack> playerTeam,
                                             List<ItemStack> opponentTeam,
                                             ItemStack accessory) {
        Player player = helper.makeMockSurvivalPlayer();
        placePlayer(helper, player);
        if (!helper.getLevel().addFreshEntity(player)) {
            throw new IllegalStateException("failed to add mock player to the GameTest level");
        }

        ITitanManager titanManager = player.getCapability(TitanManagerProvider.TITAN_MANAGER)
                .resolve()
                .orElseThrow(() -> new IllegalStateException("mock player is missing the Titan Manager capability"));
        if (!accessory.isEmpty()) {
            titanManager.getEquippedAccessoryHandler().setStackInSlot(0, accessory.copy());
        }

        EntityTeenyDummy opponent = spawnDummy(helper, OPPONENT_POS);

        BattleState playerState = getBattleState(player);
        BattleState opponentState = getBattleState(opponent);
        playerState.initializeBattle(playerTeam, player, opponent);
        opponentState.initializeBattle(opponentTeam, opponent, player);
        playerState.setActiveFigure(0);
        opponentState.setActiveFigure(0);
        playerState.refreshPlayerInventory(player);

        return new BattleHarness(player, playerState, opponent, opponentState);
    }

    private static void setMasteryTier(BattleHarness battle, String accessoryId, int tier) {
        battle.player.getCapability(AccessoryMasteryProvider.ACCESSORY_MASTERY)
                .resolve()
                .orElseThrow(() -> new IllegalStateException("missing accessory mastery"))
                .setTier(accessoryId, tier);
    }

    private static NearbyBattler spawnNearbyBattler(GameTestHelper helper, Vec3 relativePos, ItemStack figureStack) {
        EntityTeenyDummy dummy = spawnDummy(helper, relativePos);
        BattleState state = getBattleState(dummy);
        state.initializeBattle(List.of(figureStack.copy()), dummy, null);
        state.setActiveFigure(0);
        return new NearbyBattler(dummy, state);
    }

    private static EntityTeenyDummy spawnDummy(GameTestHelper helper, Vec3 relativePos) {
        EntityTeenyDummy dummy = ModEntities.TEENY_DUMMY.get().create(helper.getLevel());
        if (dummy == null) {
            throw new IllegalStateException("failed to create Teeny Dummy for GameTest");
        }

        Vec3 absolutePos = helper.absoluteVec(relativePos);
        dummy.moveTo(absolutePos.x, absolutePos.y, absolutePos.z, OPPONENT_YAW, 0.0f);
        helper.getLevel().addFreshEntity(dummy);
        return dummy;
    }

    private static void placePlayer(GameTestHelper helper, Player player) {
        Vec3 playerPos = helper.absoluteVec(PLAYER_POS);
        player.moveTo(playerPos.x, playerPos.y, playerPos.z, PLAYER_YAW, 0.0f);
        player.setYRot(PLAYER_YAW);
        player.setYHeadRot(PLAYER_YAW);
        player.setYBodyRot(PLAYER_YAW);
        player.setXRot(0.0f);
    }

    private static BattleState getBattleState(net.minecraft.world.entity.LivingEntity entity) {
        return entity.getCapability(BattleStateProvider.BATTLE_STATE)
                .resolve()
                .filter(BattleState.class::isInstance)
                .map(BattleState.class::cast)
                .orElseThrow(() -> new IllegalStateException("entity is missing the BattleState capability"));
    }

    private static ItemStack figure(String id) {
        ItemStack stack = FigureLoader.getFigureStack(id);
        if (stack.isEmpty()) {
            throw new IllegalStateException("missing figure test fixture '" + id + "'");
        }
        return stack.copy();
    }

    private static ItemStack figureWithChip(String id, ItemStack chipStack) {
        ItemStack stack = figure(id);
        ItemFigure.installChip(stack, chipStack);
        return stack;
    }

    private static ItemStack chip(net.minecraft.world.item.Item item) {
        return ItemChip.createStack(item, 1);
    }

    private static int manaCost(BattleFigure figure, int slotIndex) {
        List<String> tiers = ItemFigure.getAbilityTiers(figure.getOriginalStack());
        String tierLetter = slotIndex < tiers.size() ? tiers.get(slotIndex) : "a";
        return TeenyBalance.getManaCost(slotIndex + 1, tierLetter);
    }

    private static int chargeUpBaseTicks(BattleFigure figure, int slotIndex) {
        AbilityLoader.AbilityData data = abilityData(figure, slotIndex);
        if (data == null || data.traits == null) {
            return 0;
        }

        for (AbilityLoader.TraitData trait : data.traits) {
            if ("charge_up".equals(trait.id)) {
                float param = trait.params.isEmpty() ? 1.0f : trait.params.get(0);
                return (int) (TeenyBalance.BASE_CHARGE_DELAY * param);
            }
        }

        return 0;
    }

    private static int projectileBaseDelayTicks(LivingEntity attacker, LivingEntity target, BattleFigure figure, int slotIndex) {
        AbilityLoader.AbilityData data = abilityData(figure, slotIndex);
        if (data == null) {
            return 0;
        }

        double distance = attacker.getEyePosition().distanceTo(target.getEyePosition());
        return Math.max(1, Math.round((float) (distance * TeenyBalance.getRaycastDelay(data.raycastDelayTier))));
    }

    private static AbilityLoader.AbilityData abilityData(BattleFigure figure, int slotIndex) {
        List<String> order = ItemFigure.getAbilityOrder(figure.getOriginalStack());
        if (slotIndex < 0 || slotIndex >= order.size()) {
            return null;
        }
        return AbilityLoader.getAbility(order.get(slotIndex));
    }

    private static AbilityLoader.TraitData trait(String id) {
        AbilityLoader.TraitData trait = new AbilityLoader.TraitData();
        trait.id = id;
        return trait;
    }

    private static DamagePipeline.DamageResult undodgeableHit(int damage, boolean classBonusEligible) {
        DamagePipeline.DamageResult hit = new DamagePipeline.DamageResult(damage, 1, false, false);
        hit.classBonusEligible = classBonusEligible;
        hit.undodgeable = true;
        return hit;
    }

    private record BattleHarness(Player player, BattleState playerState, EntityTeenyDummy opponent, BattleState opponentState) {
    }

    private record NearbyBattler(EntityTeenyDummy entity, BattleState state) {
    }
}
