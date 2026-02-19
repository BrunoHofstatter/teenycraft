package bruhof.teenycraft.capability;

import bruhof.teenycraft.battle.BattleFigure;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;

import java.util.List;

@AutoRegisterCapability
public interface IBattleState {

    // Is the player currently in a battle context?
    boolean isBattling();
    void setBattling(boolean battling);

    // The team of 3 figures
    List<BattleFigure> getTeam();
    
    /**
     * Initializes the team from the TitanManager inventory.
     * @param teamStacks List of 3 ItemStacks from the manager.
     */
    void initializeBattle(List<ItemStack> teamStacks);

    // The currently active figure (0-2)
    BattleFigure getActiveFigure();
    int getActiveFigureIndex();
    void setActiveFigure(int index);
    
    // Swapping Logic
    void swapFigure(int newIndex, net.minecraft.world.entity.player.Player player);
    int getSwapCooldown();
    void refreshPlayerInventory(net.minecraft.world.entity.player.Player player);

    // Opponent Logic
    void setOpponentTeam(List<BattleFigure> team);
    List<BattleFigure> getOpponentTeam();
    BattleFigure getActiveOpponent();
    void setOpponentEntityUUID(java.util.UUID uuid);
    java.util.UUID getOpponentEntityUUID();
    
    // Victory
    void triggerVictory(net.minecraft.server.level.ServerPlayer player);

    // Core Loop
    void tick();
    
    // Player State (Mana & Effects)
    float getCurrentMana();
    void consumeMana(int amount);
    void addMana(int amount);
    void regenMana();
    
    // Tofu Logic
    float getCurrentTofuMana();
    void spawnTofu(float power);
    
    // Effect Management
    void applyEffect(String effectId, int duration, int magnitude);
    boolean hasEffect(String effectId);
    void removeEffect(String effectId);
    int getEffectMagnitude(String effectId);
    void removeEffectsByCategory(bruhof.teenycraft.battle.effect.BattleEffect.EffectCategory category);
    void triggerOnAttack(bruhof.teenycraft.battle.BattleFigure attacker); // Pass attacker context
    
    // UI Helpers
    List<String> getEffectList();
    List<String> getBenchInfoList();
    List<Integer> getBenchIndicesList();

    // Data persistence (Minimal, mainly for reconnecting during battle if we support that)
    void saveNBTData(CompoundTag tag);
    void loadNBTData(CompoundTag tag);
    
    // Clean up
    void endBattle();
}
