package bruhof.teenycraft.capability;

import bruhof.teenycraft.battle.BattleFigure;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface IBattleState {

    // Is the player currently in a battle context?
    boolean isBattling();
    void setBattling(boolean battling);

    // The team of 3 figures
    List<BattleFigure> getTeam();
    
    /**
     * Initializes the team from the TitanManager inventory.
     * @param teamStacks List of 3 ItemStacks from the manager.
     * @param player The player instance.
     */
    void initializeBattle(List<ItemStack> teamStacks, net.minecraft.server.level.ServerPlayer player);

    // The currently active figure (0-2)
    BattleFigure getActiveFigure();
    int getActiveFigureIndex();
    void setActiveFigure(int index);
    
    // Swapping Logic
    void swapFigure(int newIndex, net.minecraft.world.entity.player.Player player);
    int getSwapCooldown();
    void refreshPlayerInventory(net.minecraft.world.entity.player.Player player);
    void updatePlayerSpeed(net.minecraft.world.entity.player.Player player);

    // Core Loop
    void tick();
    void checkFaint(net.minecraft.world.entity.LivingEntity entity);
    
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
    boolean tryActivateAccessory();

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
    void applyEffect(String effectId, int duration, int magnitude, float power, java.util.UUID caster);
    boolean hasEffect(String effectId);
    void removeEffect(String effectId);
    int getEffectMagnitude(String effectId);
    bruhof.teenycraft.battle.effect.EffectInstance getEffectInstance(String effectId);
    void removeEffectsByCategory(bruhof.teenycraft.battle.effect.BattleEffect.EffectCategory category);
    void triggerOnAttack(bruhof.teenycraft.battle.BattleFigure attacker); // Pass attacker context
    
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
}
