package bruhof.teenycraft.capability;

import bruhof.teenycraft.battle.effect.EffectInstance;
import bruhof.teenycraft.battle.effect.EffectRegistry;
import bruhof.teenycraft.battle.BattleFigure;
import bruhof.teenycraft.TeenyBalance;
import bruhof.teenycraft.entity.custom.EntityTeenyDummy;
import bruhof.teenycraft.item.ModItems;
import bruhof.teenycraft.item.custom.ItemFigure;
import bruhof.teenycraft.networking.ModMessages;
import bruhof.teenycraft.networking.PacketSyncBattleData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BattleState implements IBattleState {

    private static final UUID DODGE_SPEED_UUID = UUID.fromString("6f6c94a5-6a5e-4c7b-8b9a-7a5d1b6e2f4c");

    private boolean isBattling = false;
    private final List<BattleFigure> team = new ArrayList<>();
    private int activeFigureIndex = 0;
    private int swapCooldown = 0;
    private int lockedSlot = -1;
    private ServerPlayer player;
    
    // Participant State
    private float currentMana = 0;
    private float currentTofuMana = 0;
    private final Map<String, EffectInstance> activeEffects = new HashMap<>();

    // Charge Up Data
    private int chargeTicks = 0;
    private bruhof.teenycraft.util.AbilityLoader.AbilityData pendingAbility = null;
    private int pendingSlot = -1;
    private boolean pendingIsGolden = false;
    private UUID pendingTargetUUID = null;

    // Victory/Persistence Logic (Still tracked per state for now)
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
    public void initializeBattle(List<ItemStack> teamStacks, ServerPlayer player) {
        this.player = player;
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
            this.activeFigureIndex = index;
        }
    }

    @Override
    public void swapFigure(int newIndex, Player player) {
        if (hasEffect("stun")) {
            player.sendSystemMessage(Component.literal("§cYou are Stunned! Cannot swap!"));
            return;
        }
        if (hasEffect("root")) {
            player.sendSystemMessage(Component.literal("§c§lROOTED! §7Cannot swap figures."));
            return;
        }
        if (newIndex < 0 || newIndex >= team.size()) return;
        if (newIndex == activeFigureIndex) return; 

        if (team.get(newIndex).getCurrentHp() <= 0) {
            player.sendSystemMessage(Component.literal("§cThat figure has fainted!"));
            return;
        }

        if (hasEffect("disable_" + newIndex)) {
            player.sendSystemMessage(Component.literal("§c§lDISABLED! §7That figure is currently locked."));
            return;
        }

        if (swapCooldown > 0) {
            player.sendSystemMessage(Component.literal("§cSwap Cooldown: " + (swapCooldown / 20) + "s"));
            return;
        }

        this.activeFigureIndex = newIndex;
        this.swapCooldown = TeenyBalance.SWAP_COOLDOWN * 20;

        BattleFigure newActive = getActiveFigure();
        player.sendSystemMessage(Component.literal("§aSwapped to: " + newActive.getNickname()));
        
        refreshPlayerInventory(player);
    }

    @Override
    public void disableFigure(int index, int duration, Player player) {
        if (hasEffect("cleanse_immunity")) return;
        if (index < 0 || index >= team.size()) return;

        List<String> activeDisables = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            if (hasEffect("disable_" + i)) activeDisables.add("disable_" + i);
        }

        if (activeDisables.size() >= 2 && !hasEffect("disable_" + index)) {
            String oldest = null;
            int minDur = Integer.MAX_VALUE;
            for (String id : activeDisables) {
                EffectInstance inst = getEffectInstance(id);
                if (inst != null && inst.duration < minDur) {
                    minDur = inst.duration;
                    oldest = id;
                }
            }
            if (oldest != null) removeEffect(oldest);
        }

        applyEffect("disable_" + index, duration, 0);

        if (index == activeFigureIndex) {
            player.sendSystemMessage(Component.literal("§c§lDISABLED! §7Current figure locked. Swapping..."));
            int nextIndex = -1;
            for (int i = 1; i < team.size(); i++) {
                int check = (activeFigureIndex + i) % team.size();
                if (team.get(check).getCurrentHp() > 0 && !hasEffect("disable_" + check)) {
                    nextIndex = check;
                    break;
                }
            }

            if (nextIndex != -1) {
                this.activeFigureIndex = nextIndex;
                this.swapCooldown = TeenyBalance.SWAP_COOLDOWN * 20;
                refreshPlayerInventory(player);
                player.sendSystemMessage(Component.literal("§aForce swapped to: " + getActiveFigure().getNickname()));
            }
        }
    }

    @Override
    public int getSwapCooldown() {
        return swapCooldown;
    }

    @Override
    public void updatePlayerSpeed(Player player) {
        AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr == null) return;
        speedAttr.removeModifier(DODGE_SPEED_UUID);

        if (isBattling) {
            BattleFigure active = getActiveFigure();
            if (active != null) {
                int dodgeStat = active.getDodgeStat();
                double multiplierValue = TeenyBalance.SPEED_PER_DODGE * dodgeStat;
                speedAttr.addTransientModifier(new AttributeModifier(DODGE_SPEED_UUID, "Teeny Dodge Speed", multiplierValue, AttributeModifier.Operation.MULTIPLY_BASE));
            }
        }
    }

    @Override
    public void refreshPlayerInventory(Player player) {
        player.getInventory().clearContent();
        BattleFigure active = getActiveFigure();
        if (active == null) return;
        
        updatePlayerSpeed(player);
        ItemStack figureStack = active.getOriginalStack();
        java.util.ArrayList<String> abilityOrder = ItemFigure.getAbilityOrder(figureStack);

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

        int benchSlot = 6;
        for (int i = 0; i < team.size(); i++) {
            if (i == activeFigureIndex) continue;
            if (benchSlot <= 8) {
                player.getInventory().setItem(benchSlot, team.get(i).getOriginalStack().copy());
                benchSlot++;
            }
        }
        
        if (currentTofuMana > 0) {
            player.getInventory().setItem(4, new ItemStack(ModItems.TOFU.get()));
        }
        
        player.inventoryMenu.broadcastChanges();
    }
    
    @Override
    public void tick() {
        if (!isBattling) return;
        
        // Victory Timer
        if (battleWon) {
            if (victoryTimer > 0) {
                victoryTimer--;
            } else {
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

        if (swapCooldown > 0) swapCooldown--;
        regenMana();

        if (isCharging()) {
            if (TeenyBalance.CHARGE_CANCEL_ON_STUN && hasEffect("stun")) {
                cancelCharge();
            } else {
                chargeTicks--;
            }
        }

        for (BattleFigure figure : team) {
            figure.tick();
        }
        
        BattleFigure active = getActiveFigure();
        // Use a snapshot to avoid ConcurrentModificationException if onRemove modifies the map
        List<Map.Entry<String, EffectInstance>> snapshot = new ArrayList<>(activeEffects.entrySet());
        for (Map.Entry<String, EffectInstance> entry : snapshot) {
            String id = entry.getKey();
            EffectInstance instance = entry.getValue();
            
            // Re-verify existence in case a previous effect in this loop removed it
            if (!activeEffects.containsKey(id)) continue;

            bruhof.teenycraft.battle.effect.BattleEffect effect = bruhof.teenycraft.battle.effect.EffectRegistry.get(id);
            if (effect != null && active != null) {
                effect.onTick(this, active, instance.duration);
            }
            
            if (instance.duration > 0) {
                instance.duration--;
            }
            
            if (instance.duration == 0) {
                removeEffect(id);
            }
        }
    }

    @Override
    public void checkFaint(LivingEntity entity) {
        BattleFigure active = getActiveFigure();
        if (active == null || active.getCurrentHp() > 0) return;

        // Search for next alive figure
        int nextIndex = -1;
        for (int i = 1; i < team.size(); i++) {
            int check = (activeFigureIndex + i) % team.size();
            if (team.get(check).getCurrentHp() > 0) {
                nextIndex = check;
                break;
            }
        }

        if (nextIndex != -1) {
            // SWAP CASE (Round Reset)
            this.activeFigureIndex = nextIndex;
            BattleFigure newActive = getActiveFigure();
            
            // 1. Clear all effects (Fresh start)
            this.activeEffects.clear();
            
            // 2. Apply Reset Lock to self
            applyEffect("reset_lock", TeenyBalance.DEATH_SWAP_RESET_TICKS, 1);
            
            // 3. Visuals for self
            if (entity instanceof EntityTeenyDummy dummy) {
                dummy.setCustomName(Component.literal("§c" + newActive.getNickname()));
            }
            entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.GLOWING, TeenyBalance.DEATH_SWAP_RESET_TICKS, 0));
            entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_BLAST, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
            if (entity.level() instanceof ServerLevel sl) {
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE, entity.getX(), entity.getY() + 1, entity.getZ(), 600, 0.5, 0.5, 0.5, 0.05);
            }

            // 4. Find opponent and apply reset visuals to them too
            entity.level().getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(20),
                e -> e != entity && e.getCapability(BattleStateProvider.BATTLE_STATE).isPresent())
                .forEach(opponent -> {
                    opponent.getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(oppState -> {
                        oppState.applyEffect("reset_lock", TeenyBalance.DEATH_SWAP_RESET_TICKS, 1);
                    });
                    opponent.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.GLOWING, TeenyBalance.DEATH_SWAP_RESET_TICKS, 0));
                    opponent.level().playSound(null, opponent.getX(), opponent.getY(), opponent.getZ(), net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_BLAST, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
                    if (opponent.level() instanceof ServerLevel sl) {
                        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, opponent.getX(), opponent.getY() + 1, opponent.getZ(), 60, 0.5, 0.5, 0.5, 0.05);
                    }
                });

            if (entity instanceof ServerPlayer sp) {
                sp.sendSystemMessage(Component.literal("§c" + active.getNickname() + " Fainted! §e--- ROUND RESET ---"));
                refreshPlayerInventory(sp);
            }
        } else {
            // TEAM DEFEATED (Victory/Defeat logic)
            if (entity instanceof EntityTeenyDummy dummy) {
                dummy.kill();
                ServerPlayer winner = entity.level().getEntitiesOfClass(ServerPlayer.class, entity.getBoundingBox().inflate(30))
                    .stream().findFirst().orElse(null);
                
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
    public int getLockedSlot() { return lockedSlot; }

    @Override
    public void setLockedSlot(int slot) { this.lockedSlot = slot; }

    @Override
    public void spawnTofu(float power) {
        if (power > 0 && this.currentTofuMana > 0) return; 
        this.currentTofuMana = power; 
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
        if (hasEffect("stun") || isCharging()) return;
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
        applyEffect(effectId, duration, magnitude, 0, null);
    }

    @Override
    public void applyEffect(String effectId, int duration, int magnitude, float power) {
        applyEffect(effectId, duration, magnitude, power, null);
    }

    @Override
    public void applyEffect(String effectId, int duration, int magnitude, float power, UUID caster) {
        bruhof.teenycraft.battle.effect.BattleEffect effect = EffectRegistry.get(effectId);
        BattleFigure active = getActiveFigure();
        if (effect != null && active != null) {
            if (!effect.onApply(this, active, duration, magnitude)) {
                return; 
            }
        }
        
        if (activeEffects.containsKey(effectId)) {
            EffectInstance existing = activeEffects.get(effectId);
            if (effect != null && effect.canStackMagnitude()) {
                existing.magnitude += magnitude;
            } else {
                existing.magnitude = magnitude; 
            }
            existing.power = Math.max(existing.power, power); 
            existing.casterUUID = caster;
            if (duration == -1) {
                existing.duration = -1;
            } else if (duration > existing.duration && existing.duration != -1) {
                existing.duration = duration;
            }
        } else {
            activeEffects.put(effectId, new EffectInstance(duration, magnitude, power, caster));
        }
    }
    
    @Override
    public boolean hasEffect(String effectId) {
        return activeEffects.containsKey(effectId);
    }
    
    private boolean isRemovingEffect = false;

    @Override
    public void removeEffect(String effectId) {
        if (isRemovingEffect) {
            activeEffects.remove(effectId);
            return;
        }

        EffectInstance inst = activeEffects.remove(effectId);
        if (inst != null) {
            isRemovingEffect = true;
            try {
                bruhof.teenycraft.battle.effect.BattleEffect effect = bruhof.teenycraft.battle.effect.EffectRegistry.get(effectId);
                BattleFigure active = getActiveFigure();
                if (effect != null && active != null) {
                    effect.onRemove(this, active);
                }
            } finally {
                isRemovingEffect = false;
            }
        }
    }
    
    @Override
    public int getEffectMagnitude(String effectId) {
        EffectInstance instance = activeEffects.get(effectId);
        return (instance != null) ? instance.magnitude : 0;
    }
    
    @Override
    public EffectInstance getEffectInstance(String effectId) {
        return activeEffects.get(effectId);
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
    public boolean isCharging() { return chargeTicks > 0; }

    @Override
    public int getChargeTicks() { return chargeTicks; }

    @Override
    public void startCharge(int ticks, bruhof.teenycraft.util.AbilityLoader.AbilityData data, int slot, boolean isGolden, UUID targetUUID) {
        this.chargeTicks = ticks;
        this.pendingAbility = data;
        this.pendingSlot = slot;
        this.lockedSlot = slot;
        this.pendingIsGolden = isGolden;
        this.pendingTargetUUID = targetUUID;
    }

    @Override
    public void cancelCharge() {
        this.chargeTicks = 0;
        this.pendingAbility = null;
        this.pendingSlot = -1;
        this.lockedSlot = -1;
        this.pendingIsGolden = false;
        this.pendingTargetUUID = null;
    }

    @Override
    public bruhof.teenycraft.util.AbilityLoader.AbilityData getPendingAbility() { return pendingAbility; }

    @Override
    public int getPendingSlot() { return pendingSlot; }

    @Override
    public boolean isPendingGolden() { return pendingIsGolden; }

    @Override
    public UUID getPendingTargetUUID() { return pendingTargetUUID; }

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
        this.activeFigureIndex = 0;
        
        if (this.player != null) {
            updatePlayerSpeed(this.player);
        }

        this.currentMana = 0;
        this.currentTofuMana = 0;
        this.activeEffects.clear();
    }
}
