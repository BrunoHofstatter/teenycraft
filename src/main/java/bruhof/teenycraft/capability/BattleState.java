package bruhof.teenycraft.capability;

import bruhof.teenycraft.accessory.AccessoryExecutor;
import bruhof.teenycraft.accessory.AccessoryRegistry;
import bruhof.teenycraft.accessory.AccessorySpec;
import bruhof.teenycraft.accessory.AccessoryTierResolver;
import bruhof.teenycraft.accessory.ResolvedAccessorySpec;
import bruhof.teenycraft.accessory.AccessoryBattleProgressTracker;
import bruhof.teenycraft.accessory.AccessoryMilestoneService;
import bruhof.teenycraft.battle.effect.EffectInstance;
import bruhof.teenycraft.battle.effect.EffectRegistry;
import bruhof.teenycraft.battle.BattleFigure;
import bruhof.teenycraft.battle.presentation.BattleHudEffectSnapshot;
import bruhof.teenycraft.battle.presentation.BattleUiEventPayload;
import bruhof.teenycraft.battle.presentation.BattleUiFeedbackBroadcaster;
import bruhof.teenycraft.chip.ChipExecutor;
import bruhof.teenycraft.TeenyBalance;
import bruhof.teenycraft.entity.custom.EntityTeenyDummy;
import bruhof.teenycraft.item.custom.ItemFigure;
import bruhof.teenycraft.group.FigureGroupDefinition;
import bruhof.teenycraft.group.FigureGroupResolver;
import bruhof.teenycraft.group.GroupComboExecutor;
import bruhof.teenycraft.group.GroupComboStatBonus;
import bruhof.teenycraft.world.arena.ArenaBattleManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public class BattleState implements IBattleState {

    private static final UUID DODGE_SPEED_UUID = UUID.fromString("6f6c94a5-6a5e-4c7b-8b9a-7a5d1b6e2f4c");
    private static final UUID ARENA_SPEED_UUID = UUID.fromString("0fc2d7f0-2b52-4ed4-b1f5-8b54c7a1cc2f");
    private static final UUID CHIP_SPEED_UUID = UUID.fromString("454c4db1-b1d3-4dfc-a4a7-4a71fce31f20");
    private static final UUID RAVENS_SLOW_UUID = UUID.fromString("589549df-af4d-4cc0-ad51-94f422def7ba");
    private static final UUID KRYPTONITE_SLOW_UUID = UUID.fromString("52dd6be4-3175-48f5-925b-dbc9994fbb6e");
    static final int BATTLE_ACCESSORY_SLOT = 5;
    public static final String TAG_BATTLE_FIGURE_INDEX = "BattleFigureIndex";

    private boolean isBattling = false;
    private final List<BattleFigure> team = new ArrayList<>();
    private int activeFigureIndex = 0;
    private int swapCooldown = 0;
    private Player player;
    private LivingEntity ownerEntity;
    private UUID opponentEntityId;
    
    // Participant State
    private float currentMana = 0;
    private float currentTofuMana = 0;
    private String currentTofuPreviewEffectId = "";
    private boolean currentTofuPreviewSelfTarget = true;
    
    // Battery State
    private float batteryCharge = 0;
    private float batterySpawnPct = -1.0f;
    private int batterySpawnTimer = TeenyBalance.BATTERY_SPAWN_MIN_TICKS;
    private boolean accessoryActive = false;
    private int accessoryActiveTicks = 0;
    private String activeAccessoryId = null;
    private int activeAccessoryTier = 1;
    private final AccessoryBattleProgressTracker accessoryBattleProgressTracker = new AccessoryBattleProgressTracker();

    private final Map<String, EffectInstance> participantEffects = new HashMap<>();
    private final Map<BattleFigure, Integer> activeChipTickCounters = new java.util.IdentityHashMap<>();
    private BattleFigure effectContextFigure;

    public Player getPlayer() { return this.player; }

    public void setOwnerEntity(LivingEntity ownerEntity) {
        this.ownerEntity = ownerEntity;
        if (ownerEntity instanceof Player ownerPlayer) {
            this.player = ownerPlayer;
        } else {
            this.player = null;
        }
    }

    // Victory/Persistence Logic (Still tracked per state for now)
    private boolean battleWon = false;
    private int victoryTimer = 0;
    private ServerPlayer winnerPlayer = null;

    @Override
    public boolean isBattling() {
        return isBattling;
    }

    @Override
    public boolean didWinBattle() {
        return battleWon;
    }

    @Override
    public void setBattling(boolean battling) {
        this.isBattling = battling;
        if (!battling) {
            endBattle();
        }
    }

    @Override
    public List<BattleFigure> getTeam() {
        return team;
    }

    @Override
    public void initializeBattle(List<ItemStack> teamStacks, LivingEntity ownerEntity, LivingEntity opponentEntity) {
        clearAllEffects();
        this.ownerEntity = ownerEntity;
        this.player = ownerEntity instanceof Player ownerPlayer ? ownerPlayer : null;
        this.opponentEntityId = opponentEntity != null ? opponentEntity.getUUID() : null;
        this.team.clear();
        this.activeChipTickCounters.clear();
        String selectedGroupId = this.player == null ? "" : this.player.getCapability(TitanManagerProvider.TITAN_MANAGER)
                .map(ITitanManager::getSelectedComboGroupId).orElse("");
        FigureGroupDefinition activeCombo = resolveComboForTeam(teamStacks, selectedGroupId).orElse(null);
        GroupComboStatBonus comboBonus = GroupComboExecutor.resolveStatBonus(activeCombo);
        for (int teamSlot = 0; teamSlot < teamStacks.size(); teamSlot++) {
            ItemStack stack = teamStacks.get(teamSlot);
            if (!stack.isEmpty()) {
                GroupComboStatBonus figureBonus = teamSlot < 2 && activeCombo != null
                        ? comboBonus
                        : GroupComboStatBonus.NONE;
                this.team.add(new BattleFigure(stack, figureBonus, teamSlot));
            }
        }
        this.activeFigureIndex = 0;
        this.isBattling = true;
        this.swapCooldown = 0;
        this.currentMana = 0;
        this.currentTofuMana = 0;
        this.currentTofuPreviewEffectId = "";
        this.currentTofuPreviewSelfTarget = true;
        this.effectContextFigure = null;
        
        this.batteryCharge = 0;
        this.batterySpawnPct = -1.0f;
        this.batterySpawnTimer = TeenyBalance.BATTERY_SPAWN_MIN_TICKS;
        this.accessoryActive = false;
        this.accessoryActiveTicks = 0;
        this.activeAccessoryId = null;
        this.activeAccessoryTier = 1;
        AccessoryMilestoneService.beginBattle(this);
        
        // Reset Victory
        this.battleWon = false;
        this.victoryTimer = 0;
        this.winnerPlayer = null;
    }

    @Override
    public BattleFigure getActiveFigure() {
        if (team.isEmpty()) return null;
        if (activeFigureIndex < 0 || activeFigureIndex >= team.size()) return team.get(0);
        return team.get(activeFigureIndex);
    }

    @Override
    public int getActiveFigureIndex() {
        return activeFigureIndex;
    }

    @Override
    public void setActiveFigure(int index) {
        if (index >= 0 && index < team.size()) {
            activateFigureInternal(index);
        }
    }

    public void setActiveFigureByTeamSlot(int teamSlot) {
        for (int i = 0; i < team.size(); i++) {
            if (team.get(i).getTeamSlotIndex() == teamSlot) {
                setActiveFigure(i);
                return;
            }
        }
        setActiveFigure(0);
    }

    private static Optional<FigureGroupDefinition> resolveComboForTeam(List<ItemStack> teamStacks, String selectedGroupId) {
        if (teamStacks.size() < 2 || teamStacks.get(0).isEmpty() || teamStacks.get(1).isEmpty()) {
            return Optional.empty();
        }
        return FigureGroupResolver.resolve(
                ItemFigure.getFigureID(teamStacks.get(0)),
                ItemFigure.getFigureID(teamStacks.get(1)),
                selectedGroupId
        );
    }

    @Override
    public UUID getOpponentEntityId() {
        return opponentEntityId;
    }

    @Override
    public LivingEntity getOpponentEntity() {
        if (ownerEntity == null || opponentEntityId == null || !(ownerEntity.level() instanceof ServerLevel serverLevel)) {
            return null;
        }

        Entity entity = serverLevel.getEntity(opponentEntityId);
        return entity instanceof LivingEntity livingEntity ? livingEntity : null;
    }

    @Override
    public IBattleState getOpponentBattleState() {
        IBattleState opponentState = getBattleState(getOpponentEntity());
        if (opponentState == null || !opponentState.isBattling()) {
            return null;
        }
        return opponentState;
    }

    private boolean isParticipantEffect(String effectId) {
        bruhof.teenycraft.battle.effect.BattleEffect effect = EffectRegistry.get(effectId);
        return effect == null
                || effect.getScope() == bruhof.teenycraft.battle.effect.BattleEffect.EffectScope.PARTICIPANT;
    }

    private BattleFigure getScopedFigure() {
        if (effectContextFigure != null) {
            return effectContextFigure;
        }
        return getActiveFigure();
    }

    private <T> T withFigureContext(BattleFigure figure, Supplier<T> supplier) {
        BattleFigure previous = effectContextFigure;
        effectContextFigure = figure;
        try {
            return supplier.get();
        } finally {
            effectContextFigure = previous;
        }
    }

    private void withFigureContext(BattleFigure figure, Runnable action) {
        withFigureContext(figure, () -> {
            action.run();
            return null;
        });
    }

    private boolean hasFigureEffect(BattleFigure figure, String effectId) {
        return figure != null && figure.getActiveEffects().containsKey(effectId);
    }

    private EffectInstance getFigureEffectInstance(BattleFigure figure, String effectId) {
        return figure != null ? figure.getActiveEffects().get(effectId) : null;
    }

    @Override
    public LivingEntity getBattleEntity() {
        return ownerEntity != null ? ownerEntity : player;
    }

    private void updateOwnerSpeedInternal() {
        LivingEntity battleEntity = getBattleEntity();
        if (battleEntity == null) {
            return;
        }

        AttributeInstance speedAttr = battleEntity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr == null) {
            return;
        }

        speedAttr.removeModifier(DODGE_SPEED_UUID);
        speedAttr.removeModifier(ARENA_SPEED_UUID);
        speedAttr.removeModifier(CHIP_SPEED_UUID);
        speedAttr.removeModifier(RAVENS_SLOW_UUID);
        speedAttr.removeModifier(KRYPTONITE_SLOW_UUID);

        if (!isBattling) {
            return;
        }

        BattleFigure active = getActiveFigure();
        if (active != null) {
            int dodgeStat = active.getDodgeStat();
            double multiplierValue = TeenyBalance.SPEED_PER_DODGE * dodgeStat;
            speedAttr.addTransientModifier(new AttributeModifier(
                    DODGE_SPEED_UUID,
                    "Teeny Dodge Speed",
                    multiplierValue,
                    AttributeModifier.Operation.MULTIPLY_BASE
            ));

            int chipSpeedPercent = getEffectMagnitude("speed_up") - getEffectMagnitude("speed_down");
            if (chipSpeedPercent != 0) {
                speedAttr.addTransientModifier(new AttributeModifier(
                        CHIP_SPEED_UUID,
                        "Battle Speed Modifier",
                        chipSpeedPercent / 100.0d,
                        AttributeModifier.Operation.MULTIPLY_BASE
                ));
            }

            int ravenSlowPercent = getEffectMagnitude("ravens_slow");
            if (ravenSlowPercent > 0) {
                speedAttr.addTransientModifier(new AttributeModifier(
                        RAVENS_SLOW_UUID,
                        "Raven's Spellbook Slow",
                        -(ravenSlowPercent / 100.0d),
                        AttributeModifier.Operation.MULTIPLY_TOTAL
                ));
            }

            int kryptonitePenaltyPercent = getEffectMagnitude("kryptonite_mastery");
            if (kryptonitePenaltyPercent > 0) {
                speedAttr.addTransientModifier(new AttributeModifier(
                        KRYPTONITE_SLOW_UUID,
                        "Kryptonite Mastery Slow",
                        -(kryptonitePenaltyPercent / 100.0d),
                        AttributeModifier.Operation.MULTIPLY_TOTAL
                ));
            }
        }

        if (hasEffect("arena_speed")) {
            double arenaSpeedMultiplier = TeenyBalance.getArenaSpeedMultiplier(getEffectMagnitude("arena_speed"));
            if (arenaSpeedMultiplier > 0.0d) {
                speedAttr.addTransientModifier(new AttributeModifier(
                        ARENA_SPEED_UUID,
                        "Arena Pickup Speed",
                        arenaSpeedMultiplier,
                        AttributeModifier.Operation.MULTIPLY_BASE
                ));
            }
        }
    }

    private void syncOwnerPresentation() {
        if (!(ownerEntity instanceof EntityTeenyDummy dummy)) {
            return;
        }

        BattleFigure active = getActiveFigure();
        if (active == null) {
            return;
        }

        AttributeInstance maxHealth = dummy.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(Math.max(active.getMaxHp(), active.getCurrentHp()));
        }

        dummy.setHealth(Math.min(dummy.getMaxHealth(), active.getCurrentHp()));
        dummy.setCustomName(Component.literal("§c" + active.getNickname()));
        dummy.setCustomNameVisible(true);
    }

    private boolean isFigureDisabled(int index) {
        if (index < 0 || index >= team.size()) {
            return false;
        }
        return hasFigureEffect(team.get(index), "disable_" + index);
    }

    @Override
    public void swapFigure(int newIndex, Player player) {
        if (hasEffect("stun")) {
            if (player == null) {
                return;
            }
            player.sendSystemMessage(Component.literal("§cYou are Stunned! Cannot swap!"));
            return;
        }
        if (hasEffect("root")) {
            if (player == null) {
                return;
            }
            player.sendSystemMessage(Component.literal("§c§lROOTED! §7Cannot swap figures."));
            return;
        }
        if (hasEffect("arena_launch_lock")) {
            if (player == null) {
                return;
            }
            player.sendSystemMessage(Component.literal("Cannot swap while launching."));
            return;
        }
        if (newIndex < 0 || newIndex >= team.size()) return;
        if (newIndex == activeFigureIndex) return; 

        if (team.get(newIndex).getCurrentHp() <= 0) {
            if (player == null) {
                return;
            }
            player.sendSystemMessage(Component.literal("§cThat figure has fainted!"));
            return;
        }

        if (isFigureDisabled(newIndex)) {
            if (player == null) {
                return;
            }
            player.sendSystemMessage(Component.literal("§c§lDISABLED! §7That figure is currently locked."));
            return;
        }

        if (swapCooldown > 0) {
            if (player == null) {
                return;
            }
            player.sendSystemMessage(Component.literal("§cSwap Cooldown: " + (swapCooldown / 20) + "s"));
            return;
        }

        // Cancel flight on swap
        if (hasEffect("flight")) {
            removeEffect("flight");
        }

        activateFigureInternal(newIndex);
        this.swapCooldown = TeenyBalance.SWAP_COOLDOWN * 20;
        applySwapItemCooldown(player);

        BattleFigure newActive = getActiveFigure();
        if (player == null) {
            return;
        }
        player.sendSystemMessage(Component.literal("§aSwapped to: " + newActive.getNickname()));
        
        refreshPlayerInventory(player);
    }

    @Override
    public boolean trySwapFigureFromItem(ItemStack stack, Player player) {
        if (stack.isEmpty()) {
            return false;
        }

        CompoundTag tag = stack.getTag();
        int targetIndex = -1;
        if (tag != null && tag.contains(TAG_BATTLE_FIGURE_INDEX)) {
            targetIndex = tag.getInt(TAG_BATTLE_FIGURE_INDEX);
        }

        if (targetIndex < 0 || targetIndex >= team.size()) {
            String clickedId = ItemFigure.getFigureID(stack);
            for (int i = 0; i < team.size(); i++) {
                if (team.get(i).getFigureId().equals(clickedId)) {
                    targetIndex = i;
                    break;
                }
            }
        }

        if (targetIndex < 0 || targetIndex >= team.size()) {
            return false;
        }

        swapFigure(targetIndex, player);
        return true;
    }

    @Override
    public void disableFigure(int index, int duration, Player player) {
        if (index < 0 || index >= team.size()) return;
        BattleFigure targetFigure = team.get(index);
        if (hasFigureEffect(targetFigure, "cleanse_immunity")) return;

        List<String> activeDisables = new ArrayList<>();
        for (int i = 0; i < team.size(); i++) {
            if (isFigureDisabled(i)) activeDisables.add("disable_" + i);
        }

        if (activeDisables.size() >= 2 && !isFigureDisabled(index)) {
            BattleFigure oldestFigure = null;
            String oldestEffectId = null;
            int minDur = Integer.MAX_VALUE;
            for (String id : activeDisables) {
                int disabledIndex = Integer.parseInt(id.substring("disable_".length()));
                BattleFigure disabledFigure = team.get(disabledIndex);
                EffectInstance inst = getFigureEffectInstance(disabledFigure, id);
                if (inst != null && inst.duration < minDur) {
                    minDur = inst.duration;
                    oldestFigure = disabledFigure;
                    oldestEffectId = id;
                }
            }
            if (oldestFigure != null && oldestEffectId != null) {
                removeEffectFromFigure(oldestFigure, oldestEffectId);
            }
        }

        applyEffectToFigure(targetFigure, "disable_" + index, duration, 0, 0, null, EffectInstance.NO_CASTER_FIGURE_INDEX);

        if (index == activeFigureIndex) {
            if (player != null) player.sendSystemMessage(Component.literal("§c§lDISABLED! §7Current figure locked. Swapping..."));
            int nextIndex = -1;
            for (int i = 1; i < team.size(); i++) {
                int check = (activeFigureIndex + i) % team.size();
                if (team.get(check).getCurrentHp() > 0 && !isFigureDisabled(check)) {
                    nextIndex = check;
                    break;
                }
            }

            if (nextIndex != -1) {
                activateFigureInternal(nextIndex);
                this.swapCooldown = TeenyBalance.SWAP_COOLDOWN * 20;
                if (player != null) {
                    refreshPlayerInventory(player);
                    player.sendSystemMessage(Component.literal("§aForce swapped to: " + getActiveFigure().getNickname()));
                }
            }
        }
    }

    @Override
    public int getSwapCooldown() {
        return swapCooldown;
    }

    @Override
    public void updatePlayerSpeed(Player player) {
        updateOwnerSpeedInternal();
    }

    @Override
    public void refreshPlayerInventory(Player player) {
        if (getActiveFigure() == null) {
            return;
        }
        updatePlayerSpeed(player);
        BattleInventoryLoadoutBuilder.rebuild(this, player);
    }
    
    @Override
    public void tick() {
        if (!isBattling) return;
        
        // End-of-battle exit timer
        if (victoryTimer > 0) {
            victoryTimer--;
            if (victoryTimer == 0) {
                ServerPlayer playerToExit = this.winnerPlayer;
                if (playerToExit != null) {
                    ArenaBattleManager.finishBattleForPlayer(playerToExit);
                } else {
                    endBattle();
                }
            }
            return;
        }

        if (swapCooldown > 0) swapCooldown--;
        regenMana();
        tickActiveChipRuntime();

        // Battery Passive Charge
        if (batteryCharge < TeenyBalance.BATTERY_MAX_CHARGE) {
            batteryCharge += TeenyBalance.BATTERY_PASSIVE_CHARGE_RATE
                    * ChipExecutor.getPassiveBatteryChargeMultiplier(getActiveFigure());
            if (batteryCharge > TeenyBalance.BATTERY_MAX_CHARGE) {
                batteryCharge = TeenyBalance.BATTERY_MAX_CHARGE;
            }
        }
        
        // Battery Spawn Logic
        if (batterySpawnPct == -1.0f) {
            if (batterySpawnTimer > 0) {
                batterySpawnTimer--;
            } else {
                float range = TeenyBalance.BATTERY_SPAWN_MAX_PCT - TeenyBalance.BATTERY_SPAWN_MIN_PCT;
                batterySpawnPct = TeenyBalance.BATTERY_SPAWN_MIN_PCT + (float)(Math.random() * range);
                checkBatteryCollection();
            }
        }

        Optional<AccessorySpec> equippedAccessory = getEquippedAccessorySpec();
        if (accessoryActive && (equippedAccessory.isEmpty() || !equippedAccessory.get().getId().equals(activeAccessoryId))) {
            deactivateAccessory();
        }
        if (accessoryActive && equippedAccessory.isPresent()) {
            ResolvedAccessorySpec resolved = AccessoryTierResolver.resolve(equippedAccessory.get(), activeAccessoryTier);
            accessoryActiveTicks++;
            AccessoryExecutor.onTick(this, player, resolved, accessoryActiveTicks);
            boolean masteredUnderpants = "supermans_underpants".equals(resolved.id()) && resolved.tier() >= 5;
            if (!masteredUnderpants) {
                addBatteryChargeInternal(-resolved.batteryDrainPerTick(), false);
            }
            if (batteryCharge <= 0) {
                deactivateAccessory();
            }
        }

        for (BattleFigure figure : team) {
            figure.tickPersistentState();
        }

        if (isCharging()) {
            if (TeenyBalance.CHARGE_CANCEL_ON_STUN && hasEffect("stun")) {
                cancelCharge();
            } else {
                getActiveFigure().decrementChargeTicks();
            }
        }

        tickParticipantEffects();
        for (BattleFigure figure : team) {
            tickFigureEffects(figure);
        }

        // Tick projectiles
        java.util.Iterator<PendingProjectile> it = pendingProjectiles.iterator();
        while (it.hasNext()) {
            PendingProjectile proj = it.next();
            proj.ticksRemaining--;
            if (proj.ticksRemaining <= 0) {
                LivingEntity projectileCaster = getBattleEntity();
                if (projectileCaster != null) {
                    bruhof.teenycraft.battle.AbilityExecutor.resolveProjectile(this, projectileCaster, proj);
                }
                it.remove();
            }
        }
    }

    private void tickActiveChipRuntime() {
        BattleFigure active = getActiveFigure();
        if (active == null || active.getCurrentHp() <= 0) {
            return;
        }

        int regenInterval = ChipExecutor.getActiveRegenInterval(active);
        int regenHeal = ChipExecutor.getActiveRegenHeal(active);
        if (regenInterval <= 0 || regenHeal <= 0) {
            return;
        }

        int tickCount = activeChipTickCounters.getOrDefault(active, 0) + 1;
        if (tickCount >= regenInterval) {
            activeChipTickCounters.put(active, 0);
            applyResolvedCombatFigureDelta(active, regenHeal, new CombatMutationSource(this, ownerEntity, active));
        } else {
            activeChipTickCounters.put(active, tickCount);
        }
    }

    @Override
    public CombatMutationResult applyCombatFigureDelta(BattleFigure figure, int amount, @Nullable CombatMutationSource source) {
        if (figure == null || amount == 0) {
            int currentHp = figure != null ? figure.getCurrentHp() : 0;
            return new CombatMutationResult(figure, amount, currentHp, currentHp, source);
        }

        int hpBefore = figure.getCurrentHp();
        figure.modifyHp(amount);
        if (figure == getActiveFigure()) {
            syncOwnerPresentation();
        }
        return new CombatMutationResult(figure, amount, hpBefore, figure.getCurrentHp(), source);
    }

    @Override
    public CombatMutationResult applyAccessoryOverheal(BattleFigure figure, int amount, float overhealPct) {
        if (figure == null || amount == 0) {
            int currentHp = figure != null ? figure.getCurrentHp() : 0;
            return new CombatMutationResult(figure, amount, currentHp, currentHp, null);
        }

        int hpBefore = figure.getCurrentHp();
        figure.applyOverheal(amount, overhealPct);
        if (figure == getActiveFigure()) {
            syncOwnerPresentation();
        }
        return new CombatMutationResult(figure, amount, hpBefore, figure.getCurrentHp(), null);
    }

    @Override
    public void resolveCombatFigureDelta(CombatMutationResult mutation) {
        if (mutation == null || mutation.figure() == null) {
            return;
        }

        CombatMutationResult effectiveMutation = mutation;
        boolean rescuedBySecondChance = false;
        if (mutation.isDamage() && mutation.fainted()) {
            rescuedBySecondChance = tryResolveSecondChance(mutation);
            if (rescuedBySecondChance) {
                effectiveMutation = new CombatMutationResult(
                        mutation.figure(),
                        mutation.requestedDelta(),
                        mutation.previousHp(),
                        mutation.figure().getCurrentHp(),
                        mutation.source()
                );
            }
        }

        CombatMutationSource source = mutation.source();
        if (source == null || source.emitDefaultHpEvent()) {
            emitHpMutationEvent(effectiveMutation);
        }

        if (rescuedBySecondChance) {
            return;
        }

        if (!effectiveMutation.isDamage()) {
            triggerTeamMedicSplash(effectiveMutation);
            return;
        }

        LivingEntity targetEntity = getBattleEntity();
        if (effectiveMutation.fainted()) {
            if (targetEntity != null) {
                ChipExecutor.onFaint(this, targetEntity, effectiveMutation.figure(),
                        source != null ? source.state() : null,
                        source != null ? source.entity() : null);
            }
            if (targetEntity != null && source != null && source.hasAttackerFigure()) {
                ChipExecutor.onKill(source.state(), source.entity(), source.figure(), this, targetEntity);
            }
        }

        if (effectiveMutation.figure() == getActiveFigure() && effectiveMutation.currentHp() <= 0 && targetEntity != null) {
            checkFaint(targetEntity);
        }
    }

    private void triggerTeamMedicSplash(CombatMutationResult mutation) {
        int actualHeal = mutation.currentHp() - mutation.previousHp();
        if (actualHeal <= 0) {
            return;
        }

        CombatMutationSource source = mutation.source();
        if (source == null || source.figure() == null || source.figure() != mutation.figure()) {
            return;
        }

        float benchHealPct = ChipExecutor.getTeamMedicBenchHealPct(mutation.figure());
        if (benchHealPct <= 0.0f) {
            return;
        }

        int splashHeal = Math.max(1, Math.round(actualHeal * benchHealPct));
        for (BattleFigure ally : team) {
            if (ally == mutation.figure() || ally.getCurrentHp() <= 0) {
                continue;
            }
            applyResolvedCombatFigureDelta(ally, splashHeal,
                    new CombatMutationSource(source.state(), source.entity(), source.figure(), false));
        }
    }

    @Override
    public @Nullable CombatMutationSource resolveCombatMutationSource(@Nullable UUID entityId, int figureIndex) {
        if (entityId == null) {
            return null;
        }

        LivingEntity self = getBattleEntity();
        if (self != null && entityId.equals(self.getUUID())) {
            return new CombatMutationSource(this, self, resolveSourceFigure(this, figureIndex));
        }

        LivingEntity opponent = getOpponentEntity();
        IBattleState opponentState = getOpponentBattleState();
        if (opponent != null && opponentState != null && entityId.equals(opponent.getUUID())) {
            return new CombatMutationSource(opponentState, opponent, resolveSourceFigure(opponentState, figureIndex));
        }

        return null;
    }

    private boolean tryResolveSecondChance(CombatMutationResult mutation) {
        BattleFigure figure = mutation.figure();
        if (figure == null || !ChipExecutor.tryConsumeSecondChance(figure)) {
            return false;
        }

        int surviveHp = Math.max(1, ChipExecutor.getSecondChanceSurviveHp(figure));
        figure.modifyHp(surviveHp - figure.getCurrentHp());
        if (figure == getActiveFigure()) {
            syncOwnerPresentation();
        }

        clearAllEffects();

        if (figure != getActiveFigure()) {
            return true;
        }

        LivingEntity entity = getBattleEntity();
        if (entity != null) {
            entity.removeEffect(net.minecraft.world.effect.MobEffects.LEVITATION);
            entity.removeEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
        }

        int nextIndex = findNextLivingFigureIndex();
        if (nextIndex == -1) {
            return true;
        }

        applyEffect("reset_lock", TeenyBalance.DEATH_SWAP_RESET_TICKS, 1);
        activateFigureInternal(nextIndex);

        BattleFigure newActive = getActiveFigure();
        if (newActive != null) {
            int debuffDuration = ChipExecutor.getSecondChanceDebuffDuration(figure);
            int slowMagnitude = ChipExecutor.getSecondChanceSlowMagnitude(figure);
            if (debuffDuration > 0) {
                applyEffect(newActive, "speed_down", debuffDuration, slowMagnitude);
                applyEffect(newActive, "curse", debuffDuration, 1);
            }
        }

        if (entity != null) {
            if (entity instanceof EntityTeenyDummy dummy && newActive != null) {
                dummy.setCustomName(Component.literal("Â§c" + newActive.getNickname()));
            }
            entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.GLOWING,
                    TeenyBalance.DEATH_SWAP_RESET_TICKS,
                    0
            ));
            entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_BLAST,
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
            if (entity.level() instanceof ServerLevel sl) {
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        entity.getX(), entity.getY() + 1, entity.getZ(), 600, 0.5, 0.5, 0.5, 0.05);
            }
        }

        LivingEntity opponent = getOpponentEntity();
        IBattleState opponentState = getOpponentBattleState();
        if (opponent != null) {
            if (opponentState != null) {
                opponentState.applyEffect("reset_lock", TeenyBalance.DEATH_SWAP_RESET_TICKS, 1);
            }
            opponent.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.GLOWING,
                    TeenyBalance.DEATH_SWAP_RESET_TICKS,
                    0
            ));
            opponent.level().playSound(null, opponent.getX(), opponent.getY(), opponent.getZ(),
                    net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_BLAST,
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
            if (opponent.level() instanceof ServerLevel sl) {
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                        opponent.getX(), opponent.getY() + 1, opponent.getZ(), 60, 0.5, 0.5, 0.5, 0.05);
            }
        }

        if (entity instanceof ServerPlayer sp) {
            sp.sendSystemMessage(Component.literal(
                    "Â§e" + figure.getNickname() + " triggered Second Chance! Â§c--- ROUND RESET ---"
            ));
            refreshPlayerInventory(sp);
        }
        if (entity instanceof Player playerEntity && !(entity instanceof ServerPlayer)) {
            refreshPlayerInventory(playerEntity);
        }

        return true;
    }

    private int findNextLivingFigureIndex() {
        for (int i = 1; i < team.size(); i++) {
            int check = (activeFigureIndex + i) % team.size();
            if (team.get(check).getCurrentHp() > 0) {
                return check;
            }
        }
        return -1;
    }

    @Override
    public void checkFaint(LivingEntity entity) {
        BattleFigure active = getActiveFigure();
        if (active == null || active.getCurrentHp() > 0) return;

        int nextIndex = findNextLivingFigureIndex();

        if (nextIndex != -1) {
            // SWAP CASE (Round Reset)
            // 1. Clear the fainting side's temporary effects (fresh round)
            entity.removeEffect(net.minecraft.world.effect.MobEffects.LEVITATION);
            entity.removeEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
            clearAllEffects();
            
            // 2. Apply Reset Lock to self
            applyEffect("reset_lock", TeenyBalance.DEATH_SWAP_RESET_TICKS, 1);

            activateFigureInternal(nextIndex);
            BattleFigure newActive = getActiveFigure();
            
            // 3. Visuals for self
            if (entity instanceof EntityTeenyDummy dummy) {
                dummy.setCustomName(Component.literal("§c" + newActive.getNickname()));
            }
            entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.GLOWING, TeenyBalance.DEATH_SWAP_RESET_TICKS, 0));
            entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_BLAST, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
            if (entity.level() instanceof ServerLevel sl) {
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE, entity.getX(), entity.getY() + 1, entity.getZ(), 600, 0.5, 0.5, 0.5, 0.05);
            }

            // 4. Apply reset visuals to the authoritative paired opponent
            LivingEntity opponent = getOpponentEntity();
            IBattleState opponentState = getOpponentBattleState();
            if (opponent != null) {
                if (opponentState != null) {
                    opponentState.applyEffect("reset_lock", TeenyBalance.DEATH_SWAP_RESET_TICKS, 1);
                }
                opponent.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.GLOWING, TeenyBalance.DEATH_SWAP_RESET_TICKS, 0));
                opponent.level().playSound(null, opponent.getX(), opponent.getY(), opponent.getZ(), net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_BLAST, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
                if (opponent.level() instanceof ServerLevel sl) {
                    sl.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, opponent.getX(), opponent.getY() + 1, opponent.getZ(), 60, 0.5, 0.5, 0.5, 0.05);
                }
            }

            if (entity instanceof ServerPlayer sp) {
                sp.sendSystemMessage(Component.literal("§c" + active.getNickname() + " Fainted! §e--- ROUND RESET ---"));
                refreshPlayerInventory(sp);
            }
            if (entity instanceof Player playerEntity && !(entity instanceof ServerPlayer)) {
                refreshPlayerInventory(playerEntity);
            }
        } else {
            // TEAM DEFEATED (Victory/Defeat logic)
            if (entity instanceof EntityTeenyDummy dummy) {
                dummy.kill();

                ServerPlayer winner = getOpponentEntity() instanceof ServerPlayer pairedPlayer ? pairedPlayer : null;
                if (winner != null) {
                    winner.getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(s -> {
                        if (s instanceof BattleState bs) {
                            bs.battleWon = true;
                            bs.victoryTimer = 40;
                            bs.winnerPlayer = winner;
                            winner.sendSystemMessage(Component.literal("§6§lVICTORY! §fOpponent team defeated."));
                        }
                    });
                }
            } else if (entity instanceof Player playerEntity && !(entity instanceof ServerPlayer)) {
                playerEntity.sendSystemMessage(Component.literal("Defeat! All your figures fainted."));
                this.battleWon = false;
                this.victoryTimer = 40;
                this.winnerPlayer = null;
            } else if (entity instanceof ServerPlayer sp) {
                sp.sendSystemMessage(Component.literal("§c§lDEFEAT! §fAll your figures fainted."));
                this.battleWon = false;
                this.victoryTimer = 40;
                this.winnerPlayer = sp; 
            }
        }
    }
    
    @Override
    public float getCurrentMana() { return currentMana; }
    
    @Override
    public float getCurrentTofuMana() { return currentTofuMana; }

    @Override
    public String getCurrentTofuPreviewEffectId() {
        return currentTofuPreviewEffectId;
    }

    @Override
    public boolean isCurrentTofuPreviewSelfTarget() {
        return currentTofuPreviewSelfTarget;
    }
    
    @Override
    public int getLockedSlot() {
        BattleFigure active = getActiveFigure();
        return active != null ? active.getLockedSlot() : -1;
    }

    @Override
    public void setLockedSlot(int slot) {
        BattleFigure active = getActiveFigure();
        if (active != null) {
            active.setLockedSlot(slot);
        }
    }

    @Override
    public void spawnTofu(float power) {
        if (power > 0 && this.currentTofuMana > 0) return;
        boolean gainedTofu = power > 0 && this.currentTofuMana <= 0;
        this.currentTofuMana = power;
        if (power <= 0) {
            this.currentTofuPreviewEffectId = "";
            this.currentTofuPreviewSelfTarget = true;
        } else if (gainedTofu) {
            assignTofuPreview();
        }
        if (gainedTofu) {
            emitUiEvent(new BattleUiEventPayload(
                    BattleUiEventPayload.Type.TOFU_GAINED,
                    getBattleEntity() != null ? getBattleEntity().getId() : 0,
                    Math.round(power),
                    0,
                    "tofu",
                    false
            ));
        }
    }

    @Override
    public float getBatteryCharge() { return batteryCharge; }
    
    @Override
    public void addBatteryCharge(float amount) {
        addBatteryChargeInternal(amount, true);
    }

    private void addBatteryChargeInternal(float amount, boolean emitFeedback) {
        float before = this.batteryCharge;
        this.batteryCharge += amount;
        if (this.batteryCharge > TeenyBalance.BATTERY_MAX_CHARGE) {
            this.batteryCharge = TeenyBalance.BATTERY_MAX_CHARGE;
        } else if (this.batteryCharge < 0) {
            this.batteryCharge = 0;
        }
        if (emitFeedback) {
            int displayDelta = Math.round((this.batteryCharge - before) * 2.0f);
            if (displayDelta != 0) {
                emitUiEvent(new BattleUiEventPayload(
                        BattleUiEventPayload.Type.BATTERY,
                        getBattleEntity() != null ? getBattleEntity().getId() : 0,
                        displayDelta,
                        0,
                        "",
                        false
                ));
            }
        }
    }
    
    @Override
    public float getBatterySpawnPct() { return batterySpawnPct; }
    
    @Override
    public int getBatterySpawnTimer() { return batterySpawnTimer; }

    @Override
    public boolean isAccessoryActive() {
        return accessoryActive;
    }

    @Override
    public String getActiveAccessoryId() {
        return activeAccessoryId;
    }

    @Override
    public int getActiveAccessoryTier() {
        return activeAccessoryTier;
    }

    @Override
    public AccessoryBattleProgressTracker getAccessoryBattleProgressTracker() {
        return accessoryBattleProgressTracker;
    }

    @Override
    public String getEquippedAccessoryId() {
        return getEquippedAccessorySpec().map(AccessorySpec::getId).orElse("");
    }

    @Override
    public boolean tryActivateAccessory() {
        Optional<AccessorySpec> equippedAccessory = getEquippedAccessorySpec();
        if (equippedAccessory.isEmpty() || accessoryActive) {
            return false;
        }
        if (batteryCharge < TeenyBalance.ACCESSORY_ACTIVATION_MIN_CHARGE) {
            return false;
        }

        activateAccessory(equippedAccessory.get());
        return true;
    }

    @Override
    public void forceDeactivateAccessory() {
        deactivateAccessory();
    }

    @Override
    public boolean isBlueChanneling() {
        BattleFigure active = getActiveFigure();
        return active != null && active.isBlueChanneling();
    }

    @Override
    public void startBlueChannel(int totalTicks, int interval, bruhof.teenycraft.util.AbilityLoader.AbilityData data, int slot, boolean isGolden, float totalDamage, float totalHeal, float totalMana) {
        BattleFigure active = getActiveFigure();
        if (active != null) {
            active.startBlueChannel(totalTicks, interval, data, slot, isGolden, totalDamage, totalHeal, totalMana);
        }
    }

    @Override
    public void cancelBlueChannel() {
        BattleFigure active = getActiveFigure();
        if (active != null) {
            active.cancelBlueChannel();
        }
    }

    @Override
    public void decrementBlueChannelTicks() {
        BattleFigure active = getActiveFigure();
        if (active != null) {
            active.decrementBlueChannelTicks();
        }
    }

    @Override
    public int getBlueChannelTicks() {
        BattleFigure active = getActiveFigure();
        return active != null ? active.getBlueChannelTicks() : 0;
    }

    @Override
    public int getBlueChannelTotalTicks() {
        BattleFigure active = getActiveFigure();
        return active != null ? active.getBlueChannelTotalTicks() : 0;
    }

    @Override
    public int getBlueChannelInterval() {
        BattleFigure active = getActiveFigure();
        return active != null ? active.getBlueChannelInterval() : 0;
    }

    @Override
    public bruhof.teenycraft.util.AbilityLoader.AbilityData getBlueChannelAbility() {
        BattleFigure active = getActiveFigure();
        return active != null ? active.getBlueChannelAbility() : null;
    }

    @Override
    public int getBlueChannelSlot() {
        BattleFigure active = getActiveFigure();
        return active != null ? active.getBlueChannelSlot() : -1;
    }

    @Override
    public boolean isBlueChannelGolden() {
        BattleFigure active = getActiveFigure();
        return active != null && active.isBlueChannelGolden();
    }

    @Override
    public float getBlueChannelTotalDamage() {
        BattleFigure active = getActiveFigure();
        return active != null ? active.getBlueChannelTotalDamage() : 0;
    }

    @Override
    public float getBlueChannelTotalHeal() {
        BattleFigure active = getActiveFigure();
        return active != null ? active.getBlueChannelTotalHeal() : 0;
    }

    @Override
    public float getBlueChannelTotalMana() {
        BattleFigure active = getActiveFigure();
        return active != null ? active.getBlueChannelTotalMana() : 0;
    }

    @Override
    public void consumeMana(float amount) {
        float before = this.currentMana;
        this.currentMana -= amount;
        if (this.currentMana < 0) this.currentMana = 0;
        emitManaDelta(before, this.currentMana);
    }

    @Override
    public void consumeMana(int amount) {
        float before = this.currentMana;
        this.currentMana -= amount;
        if (this.currentMana < 0) this.currentMana = 0;
        emitManaDelta(before, this.currentMana);
    }

    private void checkBatteryCollection() {
        if (batterySpawnPct != -1.0f) {
            float targetMana = TeenyBalance.BATTLE_MANA_MAX * batterySpawnPct;
            if (currentMana >= targetMana) {
                addBatteryCharge(TeenyBalance.BATTERY_COLLECT_CHARGE);
                batterySpawnPct = -1.0f;
                float range = TeenyBalance.BATTERY_SPAWN_MAX_TICKS - TeenyBalance.BATTERY_SPAWN_MIN_TICKS;
                batterySpawnTimer = TeenyBalance.BATTERY_SPAWN_MIN_TICKS + (int)(Math.random() * range);
                if (this.player != null) {
                    this.player.level().playSound(null, this.player.getX(), this.player.getY(), this.player.getZ(), net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
                }
            }
        }
    }

    @Override
    public void addMana(int amount) {
        float before = this.currentMana;
        this.currentMana += amount;
        if (this.currentMana > TeenyBalance.BATTLE_MANA_MAX) {
            this.currentMana = TeenyBalance.BATTLE_MANA_MAX;
        } else if (this.currentMana < 0) {
            this.currentMana = 0;
        }
        emitManaDelta(before, this.currentMana);
        checkBatteryCollection();
    }

    private void emitManaDelta(float before, float after) {
        int displayDelta = Math.round(after) - Math.round(before);
        if (displayDelta == 0) {
            return;
        }

        emitUiEvent(new BattleUiEventPayload(
                BattleUiEventPayload.Type.MANA,
                getBattleEntity() != null ? getBattleEntity().getId() : 0,
                displayDelta,
                0,
                "",
                false
        ));
    }

    private void emitUiEvent(BattleUiEventPayload payload) {
        if (!isBattling || getBattleEntity() == null) {
            return;
        }
        BattleUiFeedbackBroadcaster.emitToViewers(this, payload);
    }

    private void emitHpMutationEvent(CombatMutationResult mutation) {
        int amount = Math.abs(mutation.currentHp() - mutation.previousHp());
        if (amount <= 0) {
            return;
        }

        emitUiEvent(new BattleUiEventPayload(
                mutation.isDamage() ? BattleUiEventPayload.Type.DAMAGE : BattleUiEventPayload.Type.HEAL,
                getBattleEntity() != null ? getBattleEntity().getId() : 0,
                amount,
                0,
                "",
                false
        ));
    }

    private void applySwapItemCooldown(Player player) {
        if (player == null || swapCooldown <= 0) {
            return;
        }

        for (BattleFigure figure : team) {
            if (figure == null || figure.getOriginalStack().isEmpty()) {
                continue;
            }
            player.getCooldowns().addCooldown(figure.getOriginalStack().getItem(), swapCooldown);
        }
    }
    
    @Override
    public void regenMana() {
        if (hasEffect("stun") || isCharging() || isBlueChanneling()) return;
        if (currentMana < TeenyBalance.BATTLE_MANA_MAX) {
            float regenRate = (float) TeenyBalance.BATTLE_MANA_REGEN_PER_SEC / 20.0f;
            if (hasEffect("curse")) {
                int reductionPct = getEffectMagnitude("curse");
                regenRate *= reductionPct > 1
                        ? Math.max(0.0f, 1.0f - reductionPct / 100.0f)
                        : TeenyBalance.CURSE_EFFICIENCY;
            } else if (hasEffect("dance")) {
                regenRate *= TeenyBalance.DANCE_MANA_REGEN_MULTIPLIER;
            }
            if (hasEffect("kryptonite_mastery")) {
                regenRate *= TeenyBalance.ACCESSORY_KRYPTONITE_TIER_5_PENALTY_MULT;
            }
            currentMana += regenRate;
            if (currentMana > TeenyBalance.BATTLE_MANA_MAX) {
                currentMana = TeenyBalance.BATTLE_MANA_MAX;
            }
            checkBatteryCollection();
        }
    }
    
    @Override
    public void applyEffect(String effectId, int duration, int magnitude) {
        applyEffect(effectId, duration, magnitude, 0, null);
    }

    @Override
    public void applyEffect(String effectId, int duration, int magnitude, float power) {
        applyEffect(effectId, duration, magnitude, power, null);
    }

    @Override
    public void applyEffect(String effectId, int duration, int magnitude, float power, UUID caster, @Nullable BattleFigure casterFigure) {
        int casterFigureIndex = resolveAppliedEffectSourceFigureIndex(caster, casterFigure);
        BattleFigure targetFigure = getScopedFigure();
        if (isParticipantEffect(effectId)) {
            applyParticipantEffect(targetFigure, effectId, duration, magnitude, power, caster, casterFigureIndex);
            return;
        }

        if (targetFigure != null) {
            applyEffectToFigure(targetFigure, effectId, duration, magnitude, power, caster, casterFigureIndex);
        }
    }

    @Override
    public boolean applyAccessoryEffect(String accessoryId, String effectId, int duration, int magnitude) {
        if (accessoryId == null || accessoryId.isBlank()) {
            return false;
        }
        BattleFigure targetFigure = getScopedFigure();
        if (isParticipantEffect(effectId)) {
            return applyParticipantEffect(targetFigure, effectId, duration, magnitude, 0, null,
                    EffectInstance.NO_CASTER_FIGURE_INDEX, accessoryId);
        }

        return targetFigure != null && applyEffectToFigure(targetFigure, effectId, duration, magnitude, 0, null,
                EffectInstance.NO_CASTER_FIGURE_INDEX, accessoryId);
    }

    @Override
    public void applyEffect(BattleFigure targetFigure, String effectId, int duration, int magnitude, float power, UUID caster,
                            @Nullable BattleFigure casterFigure) {
        int casterFigureIndex = resolveAppliedEffectSourceFigureIndex(caster, casterFigure);
        if (isParticipantEffect(effectId)) {
            applyParticipantEffect(targetFigure, effectId, duration, magnitude, power, caster, casterFigureIndex);
            return;
        }

        if (targetFigure != null) {
            applyEffectToFigure(targetFigure, effectId, duration, magnitude, power, caster, casterFigureIndex);
        }
    }
    
    @Override
    public boolean hasEffect(String effectId) {
        if (participantEffects.containsKey(effectId)) {
            return true;
        }
        return hasFigureEffect(getScopedFigure(), effectId);
    }

    @Override
    public boolean hasEffect(BattleFigure targetFigure, String effectId) {
        if (participantEffects.containsKey(effectId)) {
            return true;
        }
        return hasFigureEffect(targetFigure, effectId);
    }

    @Override
    public void removeEffect(String effectId) {
        if (isParticipantEffect(effectId)) {
            removeParticipantEffect(effectId);
            return;
        }
        BattleFigure targetFigure = getScopedFigure();
        if (targetFigure != null) {
            removeEffectFromFigure(targetFigure, effectId);
        }
    }
    
    @Override
    public int getEffectMagnitude(String effectId) {
        EffectInstance instance = participantEffects.get(effectId);
        if (instance == null) {
            instance = getFigureEffectInstance(getScopedFigure(), effectId);
        }
        return (instance != null) ? instance.magnitude : 0;
    }

    @Override
    public String getEffectSourceAccessoryId(String effectId) {
        EffectInstance instance = getEffectInstance(effectId);
        return instance != null ? instance.sourceAccessoryId : "";
    }

    @Override
    public int getEffectAccessoryMagnitude(String effectId, String accessoryId) {
        EffectInstance instance = getEffectInstance(effectId);
        return instance != null ? instance.getAccessoryMagnitude(accessoryId) : 0;
    }
    
    @Override
    public EffectInstance getEffectInstance(String effectId) {
        EffectInstance participantInstance = participantEffects.get(effectId);
        if (participantInstance != null) {
            return participantInstance;
        }
        return getFigureEffectInstance(getScopedFigure(), effectId);
    }

    @Override
    public EffectInstance getEffectInstance(BattleFigure targetFigure, String effectId) {
        EffectInstance participantInstance = participantEffects.get(effectId);
        if (participantInstance != null) {
            return participantInstance;
        }
        return getFigureEffectInstance(targetFigure, effectId);
    }
    
    @Override
    public void removeEffectsByCategory(bruhof.teenycraft.battle.effect.BattleEffect.EffectCategory category) {
        List<String> participantRemovals = new ArrayList<>();
        for (Map.Entry<String, EffectInstance> entry : participantEffects.entrySet()) {
            bruhof.teenycraft.battle.effect.BattleEffect effect = EffectRegistry.get(entry.getKey());
            if (effect != null && effect.getCategory() == category) {
                participantRemovals.add(entry.getKey());
            }
        }
        for (String effectId : participantRemovals) {
            removeParticipantEffect(effectId);
        }

        for (BattleFigure figure : team) {
            List<String> figureRemovals = new ArrayList<>();
            for (Map.Entry<String, EffectInstance> entry : figure.getActiveEffects().entrySet()) {
                bruhof.teenycraft.battle.effect.BattleEffect effect = EffectRegistry.get(entry.getKey());
                if (effect != null && effect.getCategory() == category) {
                    figureRemovals.add(entry.getKey());
                }
            }
            for (String effectId : figureRemovals) {
                removeEffectFromFigure(figure, effectId);
            }
        }
    }
    
    private boolean applyParticipantEffect(BattleFigure targetFigure, String effectId, int duration, int magnitude,
                                           float power, UUID caster, int casterFigureIndex) {
        return applyParticipantEffect(targetFigure, effectId, duration, magnitude, power, caster, casterFigureIndex, "");
    }

    private boolean applyParticipantEffect(BattleFigure targetFigure, String effectId, int duration, int magnitude,
                                           float power, UUID caster, int casterFigureIndex, String sourceAccessoryId) {
        bruhof.teenycraft.battle.effect.BattleEffect effect = EffectRegistry.get(effectId);
        if (effect != null && targetFigure != null
                && !withFigureContext(targetFigure, () -> effect.onApply(this, targetFigure, duration, magnitude))) {
            return false;
        }

        if (participantEffects.containsKey(effectId)) {
            EffectInstance existing = participantEffects.get(effectId);
            if (effect != null && effect.canStackMagnitude()) {
                existing.magnitude += magnitude;
                if (!sourceAccessoryId.isEmpty()) {
                    existing.addAccessoryMagnitude(sourceAccessoryId, magnitude);
                }
            } else {
                existing.magnitude = magnitude;
                if (sourceAccessoryId.isEmpty()) {
                    existing.clearAccessoryMagnitudes();
                } else {
                    existing.replaceAccessoryMagnitude(sourceAccessoryId, magnitude);
                }
            }
            existing.power = Math.max(existing.power, power);
            existing.casterUUID = caster;
            existing.casterFigureIndex = casterFigureIndex;
            if (duration == -1) {
                existing.duration = -1;
            } else if (duration > existing.duration && existing.duration != -1) {
                existing.duration = duration;
            }
        } else {
            participantEffects.put(effectId, new EffectInstance(duration, magnitude, power, caster, casterFigureIndex,
                    sourceAccessoryId));
        }

        updateOwnerSpeedInternal();
        return true;
    }

    private void applyEffectToFigure(BattleFigure figure, String effectId, int duration, int magnitude, float power, UUID caster, int casterFigureIndex) {
        applyEffectToFigure(figure, effectId, duration, magnitude, power, caster, casterFigureIndex, "");
    }

    private boolean applyEffectToFigure(BattleFigure figure, String effectId, int duration, int magnitude, float power,
                                        UUID caster, int casterFigureIndex, String sourceAccessoryId) {
        if (figure == null) {
            return false;
        }

        return withFigureContext(figure, () -> {
            bruhof.teenycraft.battle.effect.BattleEffect effect = EffectRegistry.get(effectId);
            if (effect != null && !effect.onApply(this, figure, duration, magnitude)) {
                return false;
            }

            Map<String, EffectInstance> figureEffects = figure.getActiveEffects();
            if (figureEffects.containsKey(effectId)) {
                EffectInstance existing = figureEffects.get(effectId);
                if (effect != null && effect.canStackMagnitude()) {
                    existing.magnitude += magnitude;
                    if (!sourceAccessoryId.isEmpty()) {
                        existing.addAccessoryMagnitude(sourceAccessoryId, magnitude);
                    }
                } else {
                    existing.magnitude = magnitude;
                    if (sourceAccessoryId.isEmpty()) {
                        existing.clearAccessoryMagnitudes();
                    } else {
                        existing.replaceAccessoryMagnitude(sourceAccessoryId, magnitude);
                    }
                }
                existing.power = Math.max(existing.power, power);
                existing.casterUUID = caster;
                existing.casterFigureIndex = casterFigureIndex;
                if (duration == -1) {
                    existing.duration = -1;
                } else if (duration > existing.duration && existing.duration != -1) {
                    existing.duration = duration;
                }
            } else {
                figureEffects.put(effectId, new EffectInstance(duration, magnitude, power, caster, casterFigureIndex,
                        sourceAccessoryId));
            }

            if (figure == getActiveFigure()) {
                updateOwnerSpeedInternal();
            }
            return true;
        });
    }

    private void removeParticipantEffect(String effectId) {
        EffectInstance instance = participantEffects.remove(effectId);
        if (instance == null) {
            return;
        }

        BattleFigure active = getActiveFigure();
        bruhof.teenycraft.battle.effect.BattleEffect effect = EffectRegistry.get(effectId);
        if (effect != null && active != null) {
            withFigureContext(active, () -> effect.onRemove(this, active));
        }

        updateOwnerSpeedInternal();
    }

    private void removeEffectFromFigure(BattleFigure figure, String effectId) {
        if (figure == null) {
            return;
        }

        withFigureContext(figure, () -> {
            Map<String, EffectInstance> figureEffects = figure.getActiveEffects();
            if (figure.isRemovingEffect()) {
                figureEffects.remove(effectId);
                return;
            }

            EffectInstance inst = figureEffects.remove(effectId);
            if (inst == null) {
                return;
            }

            figure.setRemovingEffect(true);
            try {
                bruhof.teenycraft.battle.effect.BattleEffect effect = EffectRegistry.get(effectId);
                if (effect != null) {
                    effect.onRemove(this, figure);
                }
            } finally {
                figure.setRemovingEffect(false);
            }

            if (figure == getActiveFigure()) {
                updateOwnerSpeedInternal();
            }
        });
    }

    private void tickParticipantEffects() {
        List<Map.Entry<String, EffectInstance>> snapshot = new ArrayList<>(participantEffects.entrySet());
        for (Map.Entry<String, EffectInstance> entry : snapshot) {
            String id = entry.getKey();
            EffectInstance instance = entry.getValue();

            if (!participantEffects.containsKey(id)) continue;

            bruhof.teenycraft.battle.effect.BattleEffect effect = EffectRegistry.get(id);
            BattleFigure active = getActiveFigure();
            if (effect != null && active != null) {
                withFigureContext(active, () -> effect.onTick(this, active, instance.duration));
            }

            if (instance.duration > 0) {
                instance.duration--;
            }

            if (instance.duration == 0) {
                removeParticipantEffect(id);
            }
        }
    }

    private void tickFigureEffects(BattleFigure figure) {
        if (figure == null) {
            return;
        }

        List<Map.Entry<String, EffectInstance>> snapshot = new ArrayList<>(figure.getActiveEffects().entrySet());
        for (Map.Entry<String, EffectInstance> entry : snapshot) {
            String id = entry.getKey();
            EffectInstance instance = entry.getValue();

            if (!hasFigureEffect(figure, id)) continue;

            withFigureContext(figure, () -> {
                bruhof.teenycraft.battle.effect.BattleEffect effect = EffectRegistry.get(id);
                if (effect != null) {
                    effect.onTick(this, figure, instance.duration);
                }
            });

            if (instance.duration > 0) {
                instance.duration--;
            }

            if (instance.duration == 0) {
                removeEffectFromFigure(figure, id);
            }
        }
    }

    private void clearFigureEffects(BattleFigure figure) {
        if (figure == null) {
            return;
        }

        List<String> removals = new ArrayList<>(figure.getActiveEffects().keySet());
        for (String effectId : removals) {
            removeEffectFromFigure(figure, effectId);
        }
    }

    private void clearParticipantEffects() {
        List<String> removals = new ArrayList<>(participantEffects.keySet());
        for (String effectId : removals) {
            removeParticipantEffect(effectId);
        }
    }

    private void clearAllEffects() {
        clearParticipantEffects();
        for (BattleFigure figure : team) {
            clearFigureEffects(figure);
        }
    }

    @Override
    public boolean isCharging() {
        BattleFigure active = getActiveFigure();
        return active != null && active.isCharging();
    }

    @Override
    public int getChargeTicks() {
        BattleFigure active = getActiveFigure();
        return active != null ? active.getChargeTicks() : 0;
    }

    @Override
    public int getChargeTotalTicks() {
        BattleFigure active = getActiveFigure();
        return active != null ? active.getChargeTotalTicks() : 0;
    }

    @Override
    public void startCharge(int ticks, bruhof.teenycraft.util.AbilityLoader.AbilityData data, int slot, boolean isGolden, UUID targetUUID) {
        BattleFigure active = getActiveFigure();
        if (active != null) {
            active.startCharge(ticks, data, slot, isGolden, targetUUID);
        }
    }

    @Override
    public void cancelCharge() {
        BattleFigure active = getActiveFigure();
        if (active != null) {
            active.cancelCharge();
        }
    }

    @Override
    public bruhof.teenycraft.util.AbilityLoader.AbilityData getPendingAbility() {
        BattleFigure active = getActiveFigure();
        return active != null ? active.getPendingAbility() : null;
    }

    @Override
    public int getPendingSlot() {
        BattleFigure active = getActiveFigure();
        return active != null ? active.getPendingSlot() : -1;
    }

    @Override
    public boolean isPendingGolden() {
        BattleFigure active = getActiveFigure();
        return active != null && active.isPendingGolden();
    }

    @Override
    public UUID getPendingTargetUUID() {
        BattleFigure active = getActiveFigure();
        return active != null ? active.getPendingTargetUUID() : null;
    }

    private final List<PendingProjectile> pendingProjectiles = new ArrayList<>();

    @Override
    public void addProjectile(PendingProjectile projectile) {
        pendingProjectiles.add(projectile);
    }

    @Override
    public List<PendingProjectile> getProjectiles() {
        return pendingProjectiles;
    }

    @Override
    public void triggerOnAttack(BattleFigure attacker) {
        BattleFigure active = getScopedFigure();
        if (active == null) {
            return;
        }

        int retainedRedPowerUp = 0;
        EffectInstance powerUp = participantEffects.get("power_up");
        if (powerUp != null && player != null
                && AccessoryTierResolver.getTier(player, "red_lantern_battery") >= 5) {
            retainedRedPowerUp = Math.round(powerUp.getAccessoryMagnitude("red_lantern_battery")
                    * TeenyBalance.ACCESSORY_RED_LANTERN_TIER_5_POWER_UP_RETENTION);
        }

        List<String> removals = new ArrayList<>();
        for (Map.Entry<String, EffectInstance> entry : participantEffects.entrySet()) {
            bruhof.teenycraft.battle.effect.BattleEffect effect = EffectRegistry.get(entry.getKey());
            if (effect != null && withFigureContext(active, () -> effect.onAttack(this, attacker))) {
                removals.add(entry.getKey());
            }
        }

        for (String effectId : removals) {
            removeParticipantEffect(effectId);
        }
        if (retainedRedPowerUp > 0 && removals.contains("power_up")) {
            applyAccessoryEffect("red_lantern_battery", "power_up", -1, retainedRedPowerUp);
        }
    }

    @Override
    public List<String> getEffectList() {
        List<String> list = new ArrayList<>();
        appendEffectDescriptions(list, participantEffects);
        BattleFigure active = getActiveFigure();
        if (active != null) {
            appendEffectDescriptions(list, active.getActiveEffects());
        }
        return list;
    }

    @Override
    public List<BattleHudEffectSnapshot> getEffectSnapshots() {
        List<BattleHudEffectSnapshot> list = new ArrayList<>();
        appendEffectSnapshots(list, participantEffects);
        BattleFigure active = getActiveFigure();
        if (active != null) {
            appendEffectSnapshots(list, active.getActiveEffects());
        }
        return list;
    }

    private void appendEffectDescriptions(List<String> list, Map<String, EffectInstance> effects) {
        for (Map.Entry<String, EffectInstance> entry : effects.entrySet()) {
            bruhof.teenycraft.battle.effect.BattleEffect effect = EffectRegistry.get(entry.getKey());
            if (effect != null && effect.getType() == bruhof.teenycraft.battle.effect.BattleEffect.EffectType.INFINITE) {
                list.add(entry.getKey() + " (Mag: " + entry.getValue().magnitude + ")");
            } else {
                float seconds = entry.getValue().duration / 20.0f;
                list.add(entry.getKey() + " (" + String.format("%.1fs", seconds) + ")");
            }
        }
    }

    private void appendEffectSnapshots(List<BattleHudEffectSnapshot> list, Map<String, EffectInstance> effects) {
        for (Map.Entry<String, EffectInstance> entry : effects.entrySet()) {
            bruhof.teenycraft.battle.effect.BattleEffect effect = EffectRegistry.get(entry.getKey());
            boolean infinite = effect != null
                    && effect.getType() == bruhof.teenycraft.battle.effect.BattleEffect.EffectType.INFINITE;
            list.add(new BattleHudEffectSnapshot(
                    entry.getKey(),
                    Math.max(0, entry.getValue().duration),
                    entry.getValue().magnitude,
                    infinite
            ));
        }
    }
    
    @Override
    public List<String> getBenchInfoList() {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < team.size(); i++) {
            if (i == activeFigureIndex) continue;
            BattleFigure fig = team.get(i);
            list.add(fig.getCurrentHp() + "/" + fig.getMaxHp());
        }
        return list;
    }

    @Override
    public List<Integer> getBenchIndicesList() {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < team.size(); i++) {
            if (i == activeFigureIndex) continue;
            list.add(i);
        }
        return list;
    }

    @Override
    public List<String> getBenchFigureIds() {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < team.size(); i++) {
            if (i == activeFigureIndex) continue;
            list.add(team.get(i).getFigureId());
        }
        return list;
    }

    @Override
    public String getActiveFigureId() {
        BattleFigure active = getActiveFigure();
        return (active != null) ? active.getFigureId() : "none";
    }

    @Override
    public int getBasePower() {
        BattleFigure active = getActiveFigure();
        return (active != null) ? active.getPowerStat() : 0;
    }

    @Override
    public int[] getCooldowns() {
        BattleFigure active = getActiveFigure();
        if (active == null) return new int[3];
        return new int[]{active.getCooldown(0), active.getCooldown(1), active.getCooldown(2)};
    }

    @Override
    public List<String> getAbilityIds() {
        BattleFigure active = getActiveFigure();
        if (active == null) return new ArrayList<>();
        List<String> ids = new ArrayList<>();
        for (int slotIndex = 0; slotIndex < 3; slotIndex++) {
            bruhof.teenycraft.battle.BattleAbilitySlot slot = bruhof.teenycraft.battle.BattleAbilitySlot.resolve(active, slotIndex);
            if (slot != null) ids.add(slot.effectiveAbilityId());
        }
        return ids;
    }

    @Override
    public List<String> getAbilityTiers() {
        BattleFigure active = getActiveFigure();
        if (active == null) return new ArrayList<>();
        List<String> tiers = new ArrayList<>();
        for (int slotIndex = 0; slotIndex < 3; slotIndex++) {
            bruhof.teenycraft.battle.BattleAbilitySlot slot = bruhof.teenycraft.battle.BattleAbilitySlot.resolve(active, slotIndex);
            if (slot != null) tiers.add(slot.costTier());
        }
        return tiers;
    }

    @Override
    public List<Boolean> getAbilityGoldenStatus() {
        BattleFigure active = getActiveFigure();
        if (active == null) return new ArrayList<>();
        List<Boolean> golden = new ArrayList<>();
        for (int slotIndex = 0; slotIndex < 3; slotIndex++) {
            bruhof.teenycraft.battle.BattleAbilitySlot slot = bruhof.teenycraft.battle.BattleAbilitySlot.resolve(active, slotIndex);
            if (slot != null) golden.add(slot.golden());
        }
        return golden;
    }

    @Override
    public int getInternalCooldown(String key) {
        BattleFigure active = getActiveFigure();
        return active != null ? active.getInternalCooldown(key) : 0;
    }

    @Override
    public void setInternalCooldown(String key, int ticks) {
        BattleFigure active = getActiveFigure();
        if (active != null) {
            active.setInternalCooldown(key, ticks);
        }
    }

    @Override
    public int getSlotProgress(int slot) {
        BattleFigure active = getActiveFigure();
        return active != null ? active.getSlotProgress(slot) : 0;
    }

    @Override
    public void setSlotProgress(int slot, int val) {
        BattleFigure active = getActiveFigure();
        if (active != null) {
            active.setSlotProgress(slot, val);
        }
    }

    @Override
    public boolean hasActiveMine(int slot, UUID targetUUID) {
        LivingEntity sourceEntity = ownerEntity != null ? ownerEntity : player;
        if (sourceEntity == null || targetUUID == null || !(sourceEntity.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        Entity target = serverLevel.getEntity(targetUUID);
        if (!(target instanceof LivingEntity living)) return false;
        
        return living.getCapability(BattleStateProvider.BATTLE_STATE).map(s -> {
            String effectId = "remote_mine_" + slot;
            EffectInstance inst = s.getEffectInstance(effectId);
            return inst != null && sourceEntity.getUUID().equals(inst.casterUUID);
        }).orElse(false);
    }

    @Override
    public void saveNBTData(CompoundTag tag) {
        tag.putBoolean("IsBattling", isBattling);
        tag.putInt("ActiveIndex", activeFigureIndex);
        tag.putFloat("Mana", currentMana);
    }

    @Override
    public void loadNBTData(CompoundTag tag) {
        this.isBattling = tag.getBoolean("IsBattling");
        this.activeFigureIndex = tag.getInt("ActiveIndex");
        this.currentMana = tag.getFloat("Mana");
    }

    @Override
    public void endBattle() {
        LivingEntity battleEntity = getBattleEntity();
        this.isBattling = false;
        clearAllEffects();
        this.team.clear();
        this.activeChipTickCounters.clear();
        this.activeFigureIndex = 0;
        this.ownerEntity = null;
        this.opponentEntityId = null;
        this.swapCooldown = 0;
        this.effectContextFigure = null;
        
        if (battleEntity != null) {
            AttributeInstance speedAttr = battleEntity.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speedAttr != null) {
                speedAttr.removeModifier(DODGE_SPEED_UUID);
                speedAttr.removeModifier(ARENA_SPEED_UUID);
                speedAttr.removeModifier(CHIP_SPEED_UUID);
                speedAttr.removeModifier(RAVENS_SLOW_UUID);
                speedAttr.removeModifier(KRYPTONITE_SLOW_UUID);
            }
        }

        this.currentMana = 0;
        this.currentTofuMana = 0;
        this.currentTofuPreviewEffectId = "";
        this.currentTofuPreviewSelfTarget = true;
        this.batteryCharge = 0;
        this.batterySpawnPct = -1.0f;
        this.batterySpawnTimer = TeenyBalance.BATTERY_SPAWN_MIN_TICKS;
        this.pendingProjectiles.clear();
        this.battleWon = false;
        this.victoryTimer = 0;
        this.winnerPlayer = null;
        deactivateAccessory();
        accessoryBattleProgressTracker.reset();
    }

    private void assignTofuPreview() {
        String[] selfEffects = {"power_up", "heal", "bar_fill", "cleanse", "dance"};
        String[] opponentEffects = {"freeze", "stun", "waffle"};
        boolean selfTarget = Math.random() < (5.0 / 8.0);
        String[] pool = selfTarget ? selfEffects : opponentEffects;
        this.currentTofuPreviewSelfTarget = selfTarget;
        this.currentTofuPreviewEffectId = pool[(int) (Math.random() * pool.length)];
    }

    private Optional<AccessorySpec> getEquippedAccessorySpec() {
        if (player == null) return Optional.empty();

        return player.getCapability(TitanManagerProvider.TITAN_MANAGER)
                .resolve()
                .map(ITitanManager::getEquippedAccessory)
                .flatMap(stack -> Optional.ofNullable(AccessoryRegistry.get(stack)));
    }

    private ItemStack getEquippedAccessoryStack() {
        if (player == null) return ItemStack.EMPTY;
        return player.getCapability(TitanManagerProvider.TITAN_MANAGER)
                .map(ITitanManager::getEquippedAccessory)
                .orElse(ItemStack.EMPTY);
    }

    ItemStack getEquippedAccessoryStackForBattleInventory() {
        return getEquippedAccessoryStack();
    }

    private void activateAccessory(AccessorySpec spec) {
        accessoryActive = true;
        accessoryActiveTicks = 0;
        activeAccessoryId = spec.getId();
        activeAccessoryTier = AccessoryTierResolver.getTier(player, spec.getId());
        ResolvedAccessorySpec resolved = AccessoryTierResolver.resolve(spec, activeAccessoryTier);
        AccessoryMilestoneService.beginActivation(this, player, resolved);
        AccessoryExecutor.onActivated(this, player, resolved, getEquippedAccessoryStack().getHoverName());
        if (player != null && isBattling && !team.isEmpty()) {
            refreshPlayerInventory(player);
        }
    }

    private void deactivateAccessory() {
        if (!accessoryActive) return;
        AccessorySpec currentSpec = AccessoryRegistry.get(activeAccessoryId);
        ResolvedAccessorySpec resolved = currentSpec != null
                ? AccessoryTierResolver.resolve(currentSpec, activeAccessoryTier)
                : null;
        Component accessoryName = getEquippedAccessoryStack().isEmpty() ? null : getEquippedAccessoryStack().getHoverName();
        AccessoryExecutor.onDeactivated(this, player, resolved, accessoryName);
        AccessoryMilestoneService.endActivation(this, player);
        accessoryActive = false;
        accessoryActiveTicks = 0;
        activeAccessoryId = null;
        activeAccessoryTier = 1;
        if (player != null && isBattling && !team.isEmpty()) {
            refreshPlayerInventory(player);
        }
    }

    private void onActiveFigureChanged() {
        if (!accessoryActive) return;

        AccessorySpec currentSpec = AccessoryRegistry.get(activeAccessoryId);
        if (currentSpec != null) {
            AccessoryExecutor.onActiveFigureChanged(this, AccessoryTierResolver.resolve(currentSpec, activeAccessoryTier));
        }
    }

    private void activateFigureInternal(int index) {
        if (index < 0 || index >= team.size()) {
            return;
        }

        BattleFigure previousActive = getActiveFigure();
        BattleFigure nextActive = team.get(index);
        if (previousActive != null && previousActive != nextActive) {
            if (hasEffect("flight")) {
                removeEffect("flight");
            }
            previousActive.cancelActiveOnlyRuntime();
        }

        this.activeFigureIndex = index;
        updateOwnerSpeedInternal();
        onActiveFigureChanged();
        syncOwnerPresentation();

        BattleFigure active = getActiveFigure();
        if (active != null && active.markAppearedThisBattle()) {
            LivingEntity opponentEntity = getOpponentEntity();
            IBattleState opponentState = getOpponentBattleState();
            ChipExecutor.onFirstAppearance(this, ownerEntity, active, opponentState, opponentEntity);
        }
    }

    private IBattleState getBattleState(LivingEntity entity) {
        if (entity == null) {
            return null;
        }
        return entity.getCapability(BattleStateProvider.BATTLE_STATE).orElse(null);
    }

    private int resolveAppliedEffectSourceFigureIndex(@Nullable UUID caster, @Nullable BattleFigure casterFigure) {
        if (caster == null) {
            return EffectInstance.NO_CASTER_FIGURE_INDEX;
        }

        LivingEntity self = getBattleEntity();
        if (self != null && caster.equals(self.getUUID())) {
            return resolveFigureIndex(this, casterFigure);
        }

        LivingEntity opponent = getOpponentEntity();
        IBattleState opponentState = getOpponentBattleState();
        if (opponent != null && opponentState != null && caster.equals(opponent.getUUID())) {
            return resolveFigureIndex(opponentState, casterFigure);
        }

        return EffectInstance.NO_CASTER_FIGURE_INDEX;
    }

    private int resolveFigureIndex(IBattleState state, @Nullable BattleFigure figure) {
        if (figure != null) {
            int index = state.getTeam().indexOf(figure);
            if (index >= 0) {
                return index;
            }
        }
        return state.getActiveFigureIndex();
    }

    private @Nullable BattleFigure resolveSourceFigure(IBattleState state, int figureIndex) {
        if (figureIndex >= 0 && figureIndex < state.getTeam().size()) {
            return state.getTeam().get(figureIndex);
        }
        return state.getActiveFigure();
    }
}
