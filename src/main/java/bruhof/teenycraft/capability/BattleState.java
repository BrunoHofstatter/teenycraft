package bruhof.teenycraft.capability;

import bruhof.teenycraft.battle.effect.EffectInstance;
import bruhof.teenycraft.battle.effect.EffectRegistry;
import bruhof.teenycraft.battle.BattleFigure;
import bruhof.teenycraft.TeenyBalance;
import bruhof.teenycraft.item.ModItems;
import bruhof.teenycraft.item.custom.ItemFigure;
import bruhof.teenycraft.networking.ModMessages;
import bruhof.teenycraft.networking.PacketSyncBattleData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BattleState implements IBattleState {

    private boolean isBattling = false;
    private final List<BattleFigure> team = new ArrayList<>();
    private final List<BattleFigure> opponentTeam = new ArrayList<>();
    private UUID opponentEntityUUID;
    private int activeFigureIndex = 0;
    private int swapCooldown = 0;
    
    // Player State
    private float currentMana = 0;
    private float currentTofuMana = 0;
    private final Map<String, EffectInstance> activeEffects = new HashMap<>();

    // Victory Logic
    private boolean battleWon = false;
    private int victoryTimer = 0;
    private ServerPlayer winnerPlayer = null;

    @Override
    public boolean isBattling() {
        return isBattling;
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
    public void initializeBattle(List<ItemStack> teamStacks) {
        this.team.clear();
        for (ItemStack stack : teamStacks) {
            if (!stack.isEmpty()) {
                this.team.add(new BattleFigure(stack));
            }
        }
        this.activeFigureIndex = 0;
        this.isBattling = true;
        this.swapCooldown = 0;
        this.currentMana = 0;
        this.activeEffects.clear();
        
        // Reset Opponent
        this.opponentTeam.clear();
        this.opponentEntityUUID = null;
        
        // Reset Victory
        this.battleWon = false;
        this.victoryTimer = 0;
        this.winnerPlayer = null;
    }

    @Override
    public void setOpponentTeam(List<BattleFigure> team) {
        this.opponentTeam.clear();
        this.opponentTeam.addAll(team);
    }

    @Override
    public List<BattleFigure> getOpponentTeam() {
        return opponentTeam;
    }

    @Override
    public BattleFigure getActiveOpponent() {
        if (opponentTeam.isEmpty()) return null;
        return opponentTeam.get(0);
    }

    @Override
    public void setOpponentEntityUUID(UUID uuid) {
        this.opponentEntityUUID = uuid;
    }

    @Override
    public UUID getOpponentEntityUUID() {
        return opponentEntityUUID;
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
            this.activeFigureIndex = index;
        }
    }

    @Override
    public void swapFigure(int newIndex, Player player) {
        if (hasEffect("stun")) {
            player.sendSystemMessage(Component.literal("§cYou are Stunned! Cannot swap!"));
            return;
        }
        if (newIndex < 0 || newIndex >= team.size()) return;
        if (newIndex == activeFigureIndex) return; // Already active

        if (swapCooldown > 0) {
            player.sendSystemMessage(Component.literal("§cSwap Cooldown: " + (swapCooldown / 20) + "s"));
            return;
        }

        // Perform Swap
        this.activeFigureIndex = newIndex;
        this.swapCooldown = TeenyBalance.SWAP_COOLDOWN * 20;

        BattleFigure newActive = getActiveFigure();
        player.sendSystemMessage(Component.literal("§aSwapped to: " + newActive.getNickname()));
        
        refreshPlayerInventory(player);
    }

    @Override
    public int getSwapCooldown() {
        return swapCooldown;
    }

    @Override
    public void refreshPlayerInventory(Player player) {
        player.getInventory().clearContent();

        BattleFigure active = getActiveFigure();
        if (active == null) return;
        ItemStack figureStack = active.getOriginalStack();
        java.util.ArrayList<String> abilityOrder = ItemFigure.getAbilityOrder(figureStack);

        // 1. Give Abilities (Slots 0-2)
        Item[] abilityItems = { ModItems.ABILITY_1.get(), ModItems.ABILITY_2.get(), ModItems.ABILITY_3.get() };
        for (int i = 0; i < 3; i++) {
            if (i < abilityOrder.size()) {
                String id = abilityOrder.get(i);
                bruhof.teenycraft.util.AbilityLoader.AbilityData data = bruhof.teenycraft.util.AbilityLoader.getAbility(id);
                if (data != null) {
                    ItemStack stack = new ItemStack(abilityItems[i]);
                    int damage = ItemFigure.calculateAbilityDamage(figureStack, i);
                    boolean golden = ItemFigure.isAbilityGolden(figureStack, id);
                    
                    bruhof.teenycraft.item.custom.battle.ItemAbility.initializeAbility(stack, id, data.name, damage, golden);
                    player.getInventory().setItem(i, stack);
                }
            }
        }

        // 2. Give Bench Figures (Slots 6-8)
        int benchSlot = 6;
        for (int i = 0; i < team.size(); i++) {
            if (i == activeFigureIndex) continue;
            
            if (benchSlot <= 8) {
                player.getInventory().setItem(benchSlot, team.get(i).getOriginalStack().copy());
                benchSlot++;
            }
        }
        
        // 3. Give Tofu (Slot 4)
        if (currentTofuMana > 0) {
            player.getInventory().setItem(4, new ItemStack(ModItems.TOFU.get()));
        }
        
        player.inventoryMenu.broadcastChanges();
    }
    
    @Override
    public void triggerVictory(ServerPlayer player) {
        this.battleWon = true;
        this.victoryTimer = 40; // 2 seconds
        this.winnerPlayer = player;
        player.sendSystemMessage(Component.literal("§6§lVICTORY! Teleporting in 2 seconds..."));
    }

    @Override
    public void tick() {
        if (!isBattling) return;
        
        // Victory Timer
        if (battleWon) {
            if (victoryTimer > 0) {
                victoryTimer--;
            } else {
                // Time's up -> End Battle & Teleport
                ServerPlayer playerToTeleport = this.winnerPlayer;
                endBattle();
                
                if (playerToTeleport != null) {
                    ModMessages.sendToPlayer(new PacketSyncBattleData(
                        false, "", 0, 0, 0, 0, 
                        new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                        "", 0, 0
                    ), playerToTeleport);
                    ServerLevel overworld = playerToTeleport.serverLevel().getServer().getLevel(Level.OVERWORLD);
                    if (overworld != null) {
                        playerToTeleport.teleportTo(overworld, playerToTeleport.getX(), playerToTeleport.getY(), playerToTeleport.getZ(), Set.of(), playerToTeleport.getYRot(), playerToTeleport.getXRot());
                    }
                }
                return; 
            }
        }

        // Cooldowns (Global or Active Only?)
        if (swapCooldown > 0) swapCooldown--;
        
        // Regen Mana
        regenMana();

        // Update ALL figures (Cooldowns)
        for (BattleFigure figure : team) {
            figure.tick();
        }
        
        // Update Effects
        BattleFigure active = getActiveFigure();
        activeEffects.entrySet().removeIf(entry -> {
            String id = entry.getKey();
            EffectInstance instance = entry.getValue();
            
            // Call OnTick (Pass active figure as context?)
            bruhof.teenycraft.battle.effect.BattleEffect effect = EffectRegistry.get(id);
            if (effect != null && active != null) {
                effect.onTick(this, active, instance.duration);
            }

            // Decrement Duration
            if (instance.duration > 0) {
                instance.duration--;
            }

            // Remove if expired
            if (instance.duration == 0) {
                if (effect != null && active != null) effect.onRemove(this, active);
                return true;
            }
            return false;
        });
    }
    
    // ==========================================
    // MANA & EFFECTS
    // ==========================================
    
    @Override
    public float getCurrentMana() { return currentMana; }
    
    @Override
    public float getCurrentTofuMana() { return currentTofuMana; }
    
    @Override
    public void spawnTofu(float power) {
        if (power > 0 && this.currentTofuMana > 0) return; // Prevent overwriting existing Tofu
        this.currentTofuMana = power; // Allow clearing (0) or initial spawn
    }

    @Override
    public void consumeMana(int amount) {
        this.currentMana -= amount;
        if (this.currentMana < 0) this.currentMana = 0;
    }

    @Override
    public void addMana(int amount) {
        this.currentMana += amount;
        if (this.currentMana > TeenyBalance.BATTLE_MANA_MAX) {
            this.currentMana = TeenyBalance.BATTLE_MANA_MAX;
        } else if (this.currentMana < 0) {
            this.currentMana = 0;
        }
    }
    
    @Override
    public void regenMana() {
        if (hasEffect("stun")) return;
        
        if (currentMana < TeenyBalance.BATTLE_MANA_MAX) {
            float regenRate = (float) TeenyBalance.BATTLE_MANA_REGEN_PER_SEC / 20.0f;
            
            if (hasEffect("curse")) {
                regenRate *= TeenyBalance.CURSE_EFFICIENCY;
            } else if (hasEffect("dance")) {
                regenRate *= TeenyBalance.DANCE_MANA_REGEN_MULTIPLIER;
            }
            
            currentMana += regenRate;
            if (currentMana > TeenyBalance.BATTLE_MANA_MAX) {
                currentMana = TeenyBalance.BATTLE_MANA_MAX;
            }
        }
    }
    
    @Override
    public void applyEffect(String effectId, int duration, int magnitude) {
        bruhof.teenycraft.battle.effect.BattleEffect effect = EffectRegistry.get(effectId);
        BattleFigure active = getActiveFigure();
        
        // 1. Logic Hook
        if (effect != null && active != null) {
            if (!effect.onApply(this, active, duration, magnitude)) {
                return; // Blocked
            }
        }
        
        // 2. Apply/Stack
        if (activeEffects.containsKey(effectId)) {
            EffectInstance existing = activeEffects.get(effectId);
            existing.magnitude += magnitude;
            if (duration > existing.duration && existing.duration != -1) {
                existing.duration = duration;
            }
        } else {
            activeEffects.put(effectId, new EffectInstance(duration, magnitude));
        }
    }
    
    @Override
    public boolean hasEffect(String effectId) {
        return activeEffects.containsKey(effectId);
    }
    
    @Override
    public void removeEffect(String effectId) {
        activeEffects.remove(effectId);
    }
    
    @Override
    public int getEffectMagnitude(String effectId) {
        EffectInstance instance = activeEffects.get(effectId);
        return (instance != null) ? instance.magnitude : 0;
    }
    
    @Override
    public void removeEffectsByCategory(bruhof.teenycraft.battle.effect.BattleEffect.EffectCategory category) {
        BattleFigure active = getActiveFigure();
        activeEffects.entrySet().removeIf(entry -> {
             bruhof.teenycraft.battle.effect.BattleEffect effect = EffectRegistry.get(entry.getKey());
             if (effect != null && effect.getCategory() == category) {
                 if (active != null) effect.onRemove(this, active);
                 return true;
             }
             return false;
        });
    }
    
    @Override
    public void triggerOnAttack(BattleFigure attacker) {
        activeEffects.entrySet().removeIf(entry -> {
             bruhof.teenycraft.battle.effect.BattleEffect effect = EffectRegistry.get(entry.getKey());
             if (effect != null && effect.onAttack(this, attacker)) {
                 effect.onRemove(this, attacker);
                 return true;
             }
             return false;
        });
    }

    @Override
    public List<String> getEffectList() {
        List<String> list = new ArrayList<>();
        for (Map.Entry<String, EffectInstance> entry : activeEffects.entrySet()) {
            bruhof.teenycraft.battle.effect.BattleEffect effect = EffectRegistry.get(entry.getKey());
            if (effect != null && effect.getType() == bruhof.teenycraft.battle.effect.BattleEffect.EffectType.INFINITE) {
                list.add(entry.getKey() + " (Mag: " + entry.getValue().magnitude + ")");
            } else {
                float seconds = entry.getValue().duration / 20.0f;
                list.add(entry.getKey() + " (" + String.format("%.1fs", seconds) + ")");
            }
        }
        return list;
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
    public void saveNBTData(CompoundTag tag) {
        tag.putBoolean("IsBattling", isBattling);
        tag.putInt("ActiveIndex", activeFigureIndex);
        tag.putFloat("Mana", currentMana);
        // Save Effects? For now, we assume simple reconnect logic or data loss on crash is acceptable for effects.
        // Or implement serialization for EffectInstance.
    }

    @Override
    public void loadNBTData(CompoundTag tag) {
        this.isBattling = tag.getBoolean("IsBattling");
        this.activeFigureIndex = tag.getInt("ActiveIndex");
        this.currentMana = tag.getFloat("Mana");
    }

    @Override
    public void endBattle() {
        this.isBattling = false;
        this.team.clear();
        this.opponentTeam.clear();
        this.activeFigureIndex = 0;
        this.battleWon = false;
        this.victoryTimer = 0;
        this.winnerPlayer = null;
        this.currentMana = 0;
        this.currentTofuMana = 0;
        this.activeEffects.clear();
    }
}
