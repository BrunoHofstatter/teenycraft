package bruhof.teenycraft.gametest;

import bruhof.teenycraft.TeenyBalance;
import bruhof.teenycraft.TeenyCraft;
import bruhof.teenycraft.accessory.AccessoryExecutor;
import bruhof.teenycraft.accessory.AccessoryRegistry;
import bruhof.teenycraft.accessory.AccessorySpec;
import bruhof.teenycraft.battle.AbilityExecutor;
import bruhof.teenycraft.battle.BattleFigure;
import bruhof.teenycraft.battle.damage.DamagePipeline;
import bruhof.teenycraft.battle.effect.EffectCalculator;
import bruhof.teenycraft.capability.BattleState;
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
        helper.assertTrue(battle.playerState.getCurrentMana() < 80, "charge_up should spend mana immediately");

        helper.runAfterDelay(14, () -> {
            helper.assertFalse(battle.playerState.isCharging(), "charge_up should finish after its delay");
            helper.assertTrue(battle.playerState.getLockedSlot() == -1, "charge_up should release the slot lock after resolving");
            helper.assertTrue(active.getCurrentHp() > damagedHp, "evil_laugh should heal after charge completion");
            helper.assertTrue(active.getCurrentHp() <= hpBefore, "healing should still respect max HP");
            helper.succeed();
        });
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
    public static void accessoryOpponentEffectUsesPairedOpponent(GameTestHelper helper) {
        BattleHarness battle = startBattle(helper, List.of(figure("robin")), List.of(figure("joker")), new ItemStack(ModItems.ACCESSORY_RAVENS_SPELLBOOK.get()));
        NearbyBattler distractor = spawnNearbyBattler(helper, DISTRACTOR_POS, figure("superman"));

        AccessorySpec spec = AccessoryRegistry.get("ravens_spellbook");
        if (spec == null) {
            throw new IllegalStateException("missing ravens_spellbook accessory spec");
        }

        AccessoryExecutor.onTick(battle.playerState, battle.player, spec, spec.getIntervalTicks());

        helper.assertTrue(battle.opponentState.hasEffect("curse"), "accessory effect should apply to the paired opponent");
        helper.assertFalse(distractor.state.hasEffect("curse"), "accessory effect should not apply to an unpaired nearby battler");
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
