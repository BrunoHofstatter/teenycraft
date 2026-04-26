package bruhof.teenycraft.capability;

import bruhof.teenycraft.battle.BattleFigure;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public interface IBattleState {

    // Is the player currently in a battle context?
    boolean isBattling();
    void setBattling(boolean battling);

    // The team of 3 figures
    List<BattleFigure> getTeam();
    
    /**
     * Initializes hot battle state for one participant in a paired battle.
     * @param teamStacks List of 3 ItemStacks from the manager.
     * @param ownerEntity The owning battle participant.
     * @param opponentEntity The authoritative paired opponent, if known.
     */
    void initializeBattle(List<ItemStack> teamStacks, LivingEntity ownerEntity, LivingEntity opponentEntity);

    // The currently active figure (0-2)
    BattleFigure getActiveFigure();
    int getActiveFigureIndex();
    void setActiveFigure(int index);

    UUID getOpponentEntityId();
    LivingEntity getOpponentEntity();
    IBattleState getOpponentBattleState();
    
    // Swapping Logic
    void swapFigure(int newIndex, net.minecraft.world.entity.player.Player player);
    boolean trySwapFigureFromItem(ItemStack stack, net.minecraft.world.entity.player.Player player);
    int getSwapCooldown();
    void refreshPlayerInventory(net.minecraft.world.entity.player.Player player);
    void updatePlayerSpeed(net.minecraft.world.entity.player.Player player);

    // Core Loop
    void tick();
    void checkFaint(LivingEntity entity);
    CombatMutationResult applyCombatFigureDelta(BattleFigure figure, int amount, @Nullable CombatMutationSource source);
    void resolveCombatFigureDelta(CombatMutationResult mutation);
    @Nullable CombatMutationSource resolveCombatMutationSource(@Nullable UUID entityId, int figureIndex);

    default CombatMutationResult applyCombatFigureDelta(BattleFigure figure, int amount) {
        return applyCombatFigureDelta(figure, amount, null);
    }

    default CombatMutationResult applyResolvedCombatFigureDelta(BattleFigure figure, int amount) {
        return applyResolvedCombatFigureDelta(figure, amount, null);
    }

    default CombatMutationResult applyResolvedCombatFigureDelta(BattleFigure figure, int amount, @Nullable CombatMutationSource source) {
        CombatMutationResult mutation = applyCombatFigureDelta(figure, amount, source);
        resolveCombatFigureDelta(mutation);
        return mutation;
    }

    default @Nullable CombatMutationSource resolveCombatMutationSource(@Nullable UUID entityId) {
        return resolveCombatMutationSource(entityId, bruhof.teenycraft.battle.effect.EffectInstance.NO_CASTER_FIGURE_INDEX);
    }
    
    // Player State (Mana & Effects)
    float getCurrentMana();
    void consumeMana(int amount);
    void consumeMana(float amount);
    void addMana(int amount);
    void regenMana();
    
    // Tofu Logic
    float getCurrentTofuMana();
    void spawnTofu(float power);
    
    // Battery / Accessory Logic
    float getBatteryCharge();
    void addBatteryCharge(float amount);
    float getBatterySpawnPct();
    int getBatterySpawnTimer();
    boolean isAccessoryActive();
    String getActiveAccessoryId();
    boolean tryActivateAccessory();
    void forceDeactivateAccessory();

    int getLockedSlot();
    void setLockedSlot(int slot);
    
    int getInternalCooldown(String key);
    void setInternalCooldown(String key, int ticks);
    
    int getSlotProgress(int slot);
    void setSlotProgress(int slot, int val);
    
    boolean hasActiveMine(int slot, java.util.UUID targetUUID);
    
    // Swap Logic Extensions
    void disableFigure(int index, int duration, net.minecraft.world.entity.player.Player player);
    
    // Effect Management
    void applyEffect(String effectId, int duration, int magnitude);
    void applyEffect(String effectId, int duration, int magnitude, float power);
    void applyEffect(String effectId, int duration, int magnitude, float power, java.util.UUID caster, @Nullable BattleFigure casterFigure);
    boolean hasEffect(String effectId);
    void removeEffect(String effectId);
    int getEffectMagnitude(String effectId);
    bruhof.teenycraft.battle.effect.EffectInstance getEffectInstance(String effectId);
    void removeEffectsByCategory(bruhof.teenycraft.battle.effect.BattleEffect.EffectCategory category);
    void triggerOnAttack(bruhof.teenycraft.battle.BattleFigure attacker); // Pass attacker context

    default void applyEffect(String effectId, int duration, int magnitude, float power, java.util.UUID caster) {
        applyEffect(effectId, duration, magnitude, power, caster, null);
    }
    
    // Charge Up logic
    boolean isCharging();
    int getChargeTicks();
    void startCharge(int ticks, bruhof.teenycraft.util.AbilityLoader.AbilityData data, int slot, boolean isGolden, java.util.UUID targetUUID);
    void cancelCharge();
    bruhof.teenycraft.util.AbilityLoader.AbilityData getPendingAbility();
    int getPendingSlot();
    boolean isPendingGolden();
    java.util.UUID getPendingTargetUUID();
    
    // Blue Channel logic
    boolean isBlueChanneling();
    void startBlueChannel(int totalTicks, int interval, bruhof.teenycraft.util.AbilityLoader.AbilityData data, int slot, boolean isGolden, float totalDamage, float totalHeal, float totalMana);
    void cancelBlueChannel();
    void decrementBlueChannelTicks();
    int getBlueChannelTicks();
    int getBlueChannelTotalTicks();
    int getBlueChannelInterval();
    bruhof.teenycraft.util.AbilityLoader.AbilityData getBlueChannelAbility();
    int getBlueChannelSlot();
    boolean isBlueChannelGolden();
    float getBlueChannelTotalDamage();
    float getBlueChannelTotalHeal();
    float getBlueChannelTotalMana();

    // Projectile logic
    class PendingProjectile {
        public bruhof.teenycraft.util.AbilityLoader.AbilityData data;
        public int slotIndex;
        public boolean isGolden;
        public int manaCost;
        public java.util.UUID targetUUID;
        public net.minecraft.world.phys.Vec3 castPosition;
        public int ticksRemaining;
        public bruhof.teenycraft.battle.BattleFigure attackerFigure;

        public PendingProjectile(bruhof.teenycraft.util.AbilityLoader.AbilityData data, int slotIndex, boolean isGolden, int manaCost, java.util.UUID targetUUID, net.minecraft.world.phys.Vec3 castPosition, int ticksRemaining, bruhof.teenycraft.battle.BattleFigure attackerFigure) {
            this.data = data;
            this.slotIndex = slotIndex;
            this.isGolden = isGolden;
            this.manaCost = manaCost;
            this.targetUUID = targetUUID;
            this.castPosition = castPosition;
            this.ticksRemaining = ticksRemaining;
            this.attackerFigure = attackerFigure;
        }
    }

    void addProjectile(PendingProjectile projectile);
    List<PendingProjectile> getProjectiles();

    // UI Helpers
    List<String> getEffectList();
    List<String> getBenchInfoList();
    List<Integer> getBenchIndicesList();
    List<String> getBenchFigureIds();
    String getActiveFigureId();
    int getBasePower();
    int[] getCooldowns();
    List<String> getAbilityIds();
    List<String> getAbilityTiers();
    List<Boolean> getAbilityGoldenStatus();

    // Data persistence (Minimal, mainly for reconnecting during battle if we support that)
    void saveNBTData(CompoundTag tag);
    void loadNBTData(CompoundTag tag);
    
    // Clean up
    void endBattle();

    record CombatMutationSource(@Nullable IBattleState state, @Nullable LivingEntity entity, @Nullable BattleFigure figure) {
        public boolean hasAttackerFigure() {
            return state != null && entity != null && figure != null;
        }
    }

    record CombatMutationResult(@Nullable BattleFigure figure, int requestedDelta, int previousHp, int currentHp,
                                @Nullable CombatMutationSource source) {
        public boolean isDamage() {
            return requestedDelta < 0;
        }

        public boolean fainted() {
            return isDamage() && previousHp > 0 && currentHp <= 0;
        }
    }
}
