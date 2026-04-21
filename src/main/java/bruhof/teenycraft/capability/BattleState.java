package bruhof.teenycraft.capability;

import bruhof.teenycraft.accessory.AccessoryExecutor;
import bruhof.teenycraft.accessory.AccessoryRegistry;
import bruhof.teenycraft.accessory.AccessorySpec;
import bruhof.teenycraft.battle.effect.EffectInstance;
import bruhof.teenycraft.battle.effect.EffectRegistry;
import bruhof.teenycraft.battle.BattleFigure;
import bruhof.teenycraft.chip.ChipExecutor;
import bruhof.teenycraft.TeenyBalance;
import bruhof.teenycraft.entity.custom.EntityTeenyDummy;
import bruhof.teenycraft.item.ModItems;
import bruhof.teenycraft.item.custom.ItemAccessory;
import bruhof.teenycraft.item.custom.ItemFigure;
import bruhof.teenycraft.world.arena.ArenaBattleManager;
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
import java.util.Optional;
import java.util.UUID;

public class BattleState implements IBattleState {

    private static final UUID DODGE_SPEED_UUID = UUID.fromString("6f6c94a5-6a5e-4c7b-8b9a-7a5d1b6e2f4c");
    private static final int BATTLE_ACCESSORY_SLOT = 5;

    private boolean isBattling = false;
    private final List<BattleFigure> team = new ArrayList<>();
    private int activeFigureIndex = 0;
    private int swapCooldown = 0;
    private int lockedSlot = -1;
    private ServerPlayer player;
    private LivingEntity ownerEntity;
    
    // Participant State
    private float currentMana = 0;
    private float currentTofuMana = 0;
    
    // Battery State
    private float batteryCharge = 0;
    private float batterySpawnPct = -1.0f;
    private int batterySpawnTimer = TeenyBalance.BATTERY_SPAWN_MIN_TICKS;
    private boolean accessoryActive = false;
    private int accessoryActiveTicks = 0;
    private String activeAccessoryId = null;
    
    private final Map<String, EffectInstance> activeEffects = new HashMap<>();
    private final Map<String, Integer> internalCooldowns = new HashMap<>();
    private final int[] slotProgress = new int[3];

    public ServerPlayer getPlayer() { return this.player; }

    public void setOwnerEntity(LivingEntity ownerEntity) {
        this.ownerEntity = ownerEntity;
        if (ownerEntity instanceof ServerPlayer serverPlayer) {
            this.player = serverPlayer;
        }
    }

    // Charge Up Data
    private int chargeTicks = 0;
    private bruhof.teenycraft.util.AbilityLoader.AbilityData pendingAbility = null;
    private int pendingSlot = -1;
    private boolean pendingIsGolden = false;
    private UUID pendingTargetUUID = null;
    
    // Blue Channel Data
    private int blueChannelTicks = 0;
    private int blueChannelTotalTicks = 0;
    private int blueChannelInterval = 0;
    private bruhof.teenycraft.util.AbilityLoader.AbilityData blueChannelAbility = null;
    private int blueChannelSlot = -1;
    private boolean blueChannelGolden = false;
    private float blueChannelTotalDamage = 0;
    private float blueChannelTotalHeal = 0;
    private float blueChannelTotalMana = 0;

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
        this.ownerEntity = player;
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
        
        this.batteryCharge = 0;
        this.batterySpawnPct = -1.0f;
        this.batterySpawnTimer = TeenyBalance.BATTERY_SPAWN_MIN_TICKS;
        this.accessoryActive = false;
        this.accessoryActiveTicks = 0;
        this.activeAccessoryId = null;
        
        // Reset Victory
        this.battleWon = false;
        this.victoryTimer = 0;
        this.winnerPlayer = null;

        if (!this.team.isEmpty()) {
            activateFigureInternal(0);
        }
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

        // Cancel flight on swap
        if (hasEffect("flight")) {
            removeEffect("flight");
        }

        activateFigureInternal(newIndex);
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
            if (player != null) player.sendSystemMessage(Component.literal("§c§lDISABLED! §7Current figure locked. Swapping..."));
            int nextIndex = -1;
            for (int i = 1; i < team.size(); i++) {
                int check = (activeFigureIndex + i) % team.size();
                if (team.get(check).getCurrentHp() > 0 && !hasEffect("disable_" + check)) {
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
                ItemStack icon = team.get(i).getOriginalStack().copy();
                // Preserve only identity for swapping logic
                String figureId = ItemFigure.getFigureID(icon);
                icon.setTag(new CompoundTag());
                icon.getOrCreateTag().putString("FigureID", figureId);
                
                player.getInventory().setItem(benchSlot, icon);
                benchSlot++;
            }
        }
        
        if (currentTofuMana > 0) {
            player.getInventory().setItem(4, new ItemStack(ModItems.TOFU.get()));
        }

        ItemStack accessoryStack = getEquippedAccessoryStack();
        if (!accessoryStack.isEmpty() && accessoryStack.getItem() instanceof ItemAccessory) {
            ItemStack battleAccessory = accessoryStack.copy();
            battleAccessory.getOrCreateTag().putBoolean(ItemAccessory.TAG_BATTLE_ACTIVE, accessoryActive);
            player.getInventory().setItem(BATTLE_ACCESSORY_SLOT, battleAccessory);
        }
        
        player.inventoryMenu.broadcastChanges();
    }
    
    @Override
    public void tick() {
        if (!isBattling) return;
        
        // End-of-battle exit timer
        if (winnerPlayer != null) {
            if (victoryTimer > 0) {
                victoryTimer--;
            } else {
                ServerPlayer playerToExit = this.winnerPlayer;
                if (playerToExit != null) {
                    ArenaBattleManager.finishBattleForPlayer(playerToExit);
                } else {
                    endBattle();
                }
                return;
            }
        }

        if (swapCooldown > 0) swapCooldown--;
        regenMana();

        // Battery Passive Charge
        if (batteryCharge < TeenyBalance.BATTERY_MAX_CHARGE) {
            batteryCharge += TeenyBalance.BATTERY_PASSIVE_CHARGE_RATE;
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
            accessoryActiveTicks++;
            AccessoryExecutor.onTick(this, player, equippedAccessory.get(), accessoryActiveTicks);
            addBatteryCharge(-TeenyBalance.ACCESSORY_DRAIN_PER_TICK);
            if (batteryCharge <= 0) {
                deactivateAccessory();
            }
        }

        // Tick internal cooldowns
        internalCooldowns.entrySet().forEach(entry -> {
            if (entry.getValue() > 0) entry.setValue(entry.getValue() - 1);
        });

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

        // Tick projectiles
        java.util.Iterator<PendingProjectile> it = pendingProjectiles.iterator();
        while (it.hasNext()) {
            PendingProjectile proj = it.next();
            proj.ticksRemaining--;
            if (proj.ticksRemaining <= 0) {
                if (player != null) {
                    bruhof.teenycraft.battle.AbilityExecutor.resolveProjectile(this, player, proj);
                }
                it.remove();
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
            // 1. Clear all effects (Fresh start)
            entity.removeEffect(net.minecraft.world.effect.MobEffects.LEVITATION);
            entity.removeEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
            this.activeEffects.clear();
            
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
    public float getBatteryCharge() { return batteryCharge; }
    
    @Override
    public void addBatteryCharge(float amount) {
        this.batteryCharge += amount;
        if (this.batteryCharge > TeenyBalance.BATTERY_MAX_CHARGE) {
            this.batteryCharge = TeenyBalance.BATTERY_MAX_CHARGE;
        } else if (this.batteryCharge < 0) {
            this.batteryCharge = 0;
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
    public boolean isBlueChanneling() { return blueChannelTicks > 0; }

    @Override
    public void startBlueChannel(int totalTicks, int interval, bruhof.teenycraft.util.AbilityLoader.AbilityData data, int slot, boolean isGolden, float totalDamage, float totalHeal, float totalMana) {
        this.blueChannelTicks = totalTicks;
        this.blueChannelTotalTicks = totalTicks;
        this.blueChannelInterval = interval;
        this.blueChannelAbility = data;
        this.blueChannelSlot = slot;
        this.lockedSlot = slot;
        this.blueChannelGolden = isGolden;
        this.blueChannelTotalDamage = totalDamage;
        this.blueChannelTotalHeal = totalHeal;
        this.blueChannelTotalMana = totalMana;
    }

    @Override
    public void cancelBlueChannel() {
        this.blueChannelTicks = 0;
        this.blueChannelTotalTicks = 0;
        this.blueChannelInterval = 0;
        this.blueChannelAbility = null;
        this.blueChannelSlot = -1;
        this.lockedSlot = -1;
        this.blueChannelGolden = false;
        this.blueChannelTotalDamage = 0;
        this.blueChannelTotalHeal = 0;
        this.blueChannelTotalMana = 0;
    }

    @Override
    public void decrementBlueChannelTicks() {
        if (this.blueChannelTicks > 0) {
            this.blueChannelTicks--;
        }
    }

    @Override
    public int getBlueChannelTicks() { return blueChannelTicks; }

    @Override
    public int getBlueChannelTotalTicks() { return blueChannelTotalTicks; }

    @Override
    public int getBlueChannelInterval() { return blueChannelInterval; }

    @Override
    public bruhof.teenycraft.util.AbilityLoader.AbilityData getBlueChannelAbility() { return blueChannelAbility; }

    @Override
    public int getBlueChannelSlot() { return blueChannelSlot; }

    @Override
    public boolean isBlueChannelGolden() { return blueChannelGolden; }

    @Override
    public float getBlueChannelTotalDamage() { return blueChannelTotalDamage; }

    @Override
    public float getBlueChannelTotalHeal() { return blueChannelTotalHeal; }

    @Override
    public float getBlueChannelTotalMana() { return blueChannelTotalMana; }

    @Override
    public void consumeMana(float amount) {
        this.currentMana -= amount;
        if (this.currentMana < 0) this.currentMana = 0;
    }

    @Override
    public void consumeMana(int amount) {
        this.currentMana -= amount;
        if (this.currentMana < 0) this.currentMana = 0;
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
        this.currentMana += amount;
        if (this.currentMana > TeenyBalance.BATTLE_MANA_MAX) {
            this.currentMana = TeenyBalance.BATTLE_MANA_MAX;
        } else if (this.currentMana < 0) {
            this.currentMana = 0;
        }
        checkBatteryCollection();
    }
    
    @Override
    public void regenMana() {
        if (hasEffect("stun") || isCharging() || isBlueChanneling()) return;
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
        
        if (this.player != null && "flight".equals(effectId)) {
            this.player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, duration, 7, false, false));
        }
        
        if (this.player != null) {
            updatePlayerSpeed(this.player);
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
            
            if (this.player != null && "flight".equals(effectId)) {
                this.player.removeEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
            }
            
            if (this.player != null) {
                updatePlayerSpeed(this.player);
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
        return ItemFigure.getAbilityOrder(active.getOriginalStack());
    }

    @Override
    public List<String> getAbilityTiers() {
        BattleFigure active = getActiveFigure();
        if (active == null) return new ArrayList<>();
        return ItemFigure.getAbilityTiers(active.getOriginalStack());
    }

    @Override
    public List<Boolean> getAbilityGoldenStatus() {
        BattleFigure active = getActiveFigure();
        if (active == null) return new ArrayList<>();
        ItemStack stack = active.getOriginalStack();
        List<String> ids = getAbilityIds();
        List<Boolean> golden = new ArrayList<>();
        for (String id : ids) {
            golden.add(ItemFigure.isAbilityGolden(stack, id));
        }
        return golden;
    }

    @Override
    public int getInternalCooldown(String key) {
        return internalCooldowns.getOrDefault(key, 0);
    }

    @Override
    public void setInternalCooldown(String key, int ticks) {
        internalCooldowns.put(key, ticks);
    }

    @Override
    public int getSlotProgress(int slot) {
        if (slot < 0 || slot >= 3) return 0;
        return slotProgress[slot];
    }

    @Override
    public void setSlotProgress(int slot, int val) {
        if (slot >= 0 && slot < 3) slotProgress[slot] = val;
    }

    @Override
    public boolean hasActiveMine(int slot, UUID targetUUID) {
        if (player == null || targetUUID == null) return false;
        Entity target = ((ServerLevel)player.level()).getEntity(targetUUID);
        if (!(target instanceof LivingEntity living)) return false;
        
        return living.getCapability(BattleStateProvider.BATTLE_STATE).map(s -> {
            String effectId = "remote_mine_" + slot;
            bruhof.teenycraft.battle.effect.EffectInstance inst = s.getEffectInstance(effectId);
            return inst != null && player.getUUID().equals(inst.casterUUID);
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
        this.isBattling = false;
        this.team.clear();
        this.activeFigureIndex = 0;
        this.ownerEntity = null;
        this.swapCooldown = 0;
        this.lockedSlot = -1;
        
        if (this.player != null) {
            updatePlayerSpeed(this.player);
        }

        this.currentMana = 0;
        this.currentTofuMana = 0;
        this.activeEffects.clear();
        this.internalCooldowns.clear();
        java.util.Arrays.fill(this.slotProgress, 0);
        
        this.batteryCharge = 0;
        this.batterySpawnPct = -1.0f;
        this.batterySpawnTimer = TeenyBalance.BATTERY_SPAWN_MIN_TICKS;
        this.pendingProjectiles.clear();
        cancelCharge();
        cancelBlueChannel();
        this.battleWon = false;
        this.victoryTimer = 0;
        this.winnerPlayer = null;
        deactivateAccessory();
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

    private void activateAccessory(AccessorySpec spec) {
        accessoryActive = true;
        accessoryActiveTicks = 0;
        activeAccessoryId = spec.getId();
        AccessoryExecutor.onActivated(this, player, spec, getEquippedAccessoryStack().getHoverName());
        if (player != null && isBattling && !team.isEmpty()) {
            refreshPlayerInventory(player);
        }
    }

    private void deactivateAccessory() {
        if (!accessoryActive) return;
        AccessorySpec currentSpec = AccessoryRegistry.get(activeAccessoryId);
        Component accessoryName = getEquippedAccessoryStack().isEmpty() ? null : getEquippedAccessoryStack().getHoverName();
        AccessoryExecutor.onDeactivated(this, player, currentSpec, accessoryName);
        accessoryActive = false;
        accessoryActiveTicks = 0;
        activeAccessoryId = null;
        if (player != null && isBattling && !team.isEmpty()) {
            refreshPlayerInventory(player);
        }
    }

    private void onActiveFigureChanged() {
        if (!accessoryActive) return;

        AccessorySpec currentSpec = AccessoryRegistry.get(activeAccessoryId);
        if (currentSpec != null) {
            AccessoryExecutor.onActiveFigureChanged(this, currentSpec);
        }
    }

    private void activateFigureInternal(int index) {
        if (index < 0 || index >= team.size()) {
            return;
        }

        this.activeFigureIndex = index;
        onActiveFigureChanged();

        BattleFigure active = getActiveFigure();
        if (active != null && active.markAppearedThisBattle()) {
            LivingEntity opponentEntity = findNearbyOpponentEntity(64.0);
            IBattleState opponentState = getBattleState(opponentEntity);
            ChipExecutor.onFirstAppearance(this, ownerEntity, active, opponentState, opponentEntity);
        }
    }

    private LivingEntity findNearbyOpponentEntity(double range) {
        if (ownerEntity == null) {
            return null;
        }

        return ownerEntity.level().getEntitiesOfClass(LivingEntity.class, ownerEntity.getBoundingBox().inflate(range),
                entity -> entity != ownerEntity && entity.getCapability(BattleStateProvider.BATTLE_STATE).isPresent())
                .stream()
                .findFirst()
                .orElse(null);
    }

    private IBattleState getBattleState(LivingEntity entity) {
        if (entity == null) {
            return null;
        }
        return entity.getCapability(BattleStateProvider.BATTLE_STATE).orElse(null);
    }
}
