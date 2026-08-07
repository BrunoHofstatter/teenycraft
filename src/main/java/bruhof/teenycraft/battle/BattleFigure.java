package bruhof.teenycraft.battle;

import bruhof.teenycraft.TeenyBalance;
import bruhof.teenycraft.battle.effect.EffectInstance;
import bruhof.teenycraft.chip.ChipExecutor;
import bruhof.teenycraft.item.custom.ItemFigure;
import bruhof.teenycraft.group.GroupComboStatBonus;
import bruhof.teenycraft.util.AbilityLoader;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The hot per-figure battle snapshot.
 * Created when a battle starts and discarded when it ends.
 */
public class BattleFigure {

    // Identity
    private final ItemStack originalStack;
    private final ItemStack equippedChip;
    private final String figureId;
    private final String nickname;
    private final FigureClassType figureClass;
    private final int teamSlotIndex;

    // Stats (snapshot from the source item)
    private final int maxHp;
    private final int power;
    private final int dodge;
    private final int luck;

    // Volatile state
    private int currentHp;
    private int accessoryMaxHpBonus = 0;
    private final int[] abilityCooldowns = new int[3];
    private boolean appearedThisBattle = false;

    // Figure-owned runtime combat state. activeEffects is reserved for explicitly figure-scoped effects such as Disable.
    private final Map<String, EffectInstance> activeEffects = new HashMap<>();
    private final Map<String, Integer> internalCooldowns = new HashMap<>();
    private final int[] slotProgress = new int[3];
    private boolean removingEffect = false;
    private int lockedSlot = -1;
    private boolean secondChanceUsed = false;

    private int chargeTicks = 0;
    private int chargeTotalTicks = 0;
    private AbilityLoader.AbilityData pendingAbility = null;
    private int pendingSlot = -1;
    private boolean pendingIsGolden = false;
    private UUID pendingTargetUUID = null;

    private int blueChannelTicks = 0;
    private int blueChannelTotalTicks = 0;
    private int blueChannelInterval = 0;
    private AbilityLoader.AbilityData blueChannelAbility = null;
    private int blueChannelSlot = -1;
    private boolean blueChannelGolden = false;
    private float blueChannelTotalDamage = 0;
    private float blueChannelTotalHeal = 0;
    private float blueChannelTotalMana = 0;

    // Shuffle bags
    private final bruhof.teenycraft.util.ShuffleBag dodgeBag;
    private final bruhof.teenycraft.util.ShuffleBag luckBag;
    private final Map<Integer, bruhof.teenycraft.util.ShuffleBag> bonusCritBags = new HashMap<>();

    public BattleFigure(ItemStack stack) {
        this(stack, GroupComboStatBonus.NONE, -1);
    }

    public BattleFigure(ItemStack stack, GroupComboStatBonus comboBonus, int teamSlotIndex) {
        this.originalStack = stack;
        this.equippedChip = ItemFigure.getEquippedChip(stack).copy();
        this.figureId = ItemFigure.getFigureID(stack);
        this.nickname = ItemFigure.getFigureName(stack);
        this.figureClass = FigureClassType.fromSerialized(ItemFigure.getFigureClass(stack));
        this.teamSlotIndex = teamSlotIndex;

        ChipExecutor.ResolvedBattleStats resolvedStats = ChipExecutor.resolveBattleStats(stack, comboBonus);
        this.maxHp = resolvedStats.maxHp;
        this.power = resolvedStats.power;
        this.dodge = resolvedStats.dodge;
        this.luck = resolvedStats.luck;

        int dodgeDeck = TeenyBalance.getBagSize(this.dodge);
        this.dodgeBag = new bruhof.teenycraft.util.ShuffleBag(dodgeDeck, 1);

        int luckDeck = TeenyBalance.getBagSize(this.luck);
        this.luckBag = new bruhof.teenycraft.util.ShuffleBag(luckDeck, 1);

        this.currentHp = this.maxHp;
    }

    public void tickPersistentState() {
        for (int i = 0; i < abilityCooldowns.length; i++) {
            if (abilityCooldowns[i] > 0) {
                abilityCooldowns[i]--;
            }
        }

        internalCooldowns.entrySet().forEach(entry -> {
            if (entry.getValue() > 0) {
                entry.setValue(entry.getValue() - 1);
            }
        });
    }

    public ItemStack getOriginalStack() { return originalStack; }
    public ItemStack getEquippedChip() { return equippedChip; }
    public String getFigureId() { return figureId; }
    public String getNickname() { return nickname; }
    public FigureClassType getFigureClass() { return figureClass; }
    public int getTeamSlotIndex() { return teamSlotIndex; }

    public int getCurrentHp() { return currentHp; }
    public int getMaxHp() { return maxHp + accessoryMaxHpBonus; }
    public int getBaseMaxHp() { return maxHp; }

    public int getPowerStat() { return power; }
    public int getDodgeStat() { return dodge; }
    public int getLuckStat() { return luck; }

    public int getEffectiveStat(StatType type, bruhof.teenycraft.capability.IBattleState state) {
        if (state == null) {
            return switch (type) {
                case POWER -> power;
                case DODGE -> dodge;
                case LUCK -> luck;
                default -> 0;
            };
        }

        return switch (type) {
            case POWER -> power;
            case DODGE -> dodge;
            case LUCK -> {
                int baseLuck = luck;
                int luckUp = state.getEffectMagnitude("luck_up");
                if (luckUp > 0) {
                    baseLuck = Math.round(baseLuck * (1.0f + (luckUp / 100.0f)));
                }
                yield baseLuck;
            }
            case FLAT_DAMAGE -> state.getEffectMagnitude("power_up") + state.getEffectMagnitude("flat_damage_up")
                    - state.getEffectMagnitude("power_down") - state.getEffectMagnitude("flat_damage_down")
                    + state.getEffectMagnitude("pets");
            case DEFENSE_PERCENT -> state.getEffectMagnitude("defense_up") - state.getEffectMagnitude("defense_down");
            case REFLECT_PERCENT -> state.getEffectMagnitude("cuteness") + state.getEffectMagnitude("reflect") + state.getEffectMagnitude("cuteness_reflect");
            case PETS_FLAT -> state.getEffectMagnitude("pets");
        };
    }

    public int getCooldown(int slotIndex) {
        if (slotIndex >= 0 && slotIndex < 3) {
            return abilityCooldowns[slotIndex];
        }
        return 0;
    }

    public void setCooldown(int slotIndex, int ticks) {
        if (slotIndex >= 0 && slotIndex < 3) {
            abilityCooldowns[slotIndex] = ticks;
        }
    }

    public void modifyHp(int amount) {
        if (amount > 0) {
            if (currentHp < getMaxHp()) {
                currentHp = Math.min(getMaxHp(), currentHp + amount);
            }
        } else {
            currentHp += amount;
        }
        if (currentHp < 0) {
            currentHp = 0;
        }
    }

    public void applyOverheal(int amount, float overhealPct) {
        if (amount <= 0) {
            modifyHp(amount);
            return;
        }
        int overhealCap = Math.round(getMaxHp() * (1.0f + Math.max(0.0f, overhealPct)));
        currentHp = Math.min(overhealCap, currentHp + amount);
    }

    public void setAccessoryMaxHpBonus(int bonus) {
        int sanitizedBonus = Math.max(0, bonus);
        if (accessoryMaxHpBonus == sanitizedBonus) {
            return;
        }

        int oldMaxHp = getMaxHp();
        accessoryMaxHpBonus = sanitizedBonus;
        int newMaxHp = getMaxHp();
        boolean wasAlive = currentHp > 0;
        currentHp += (newMaxHp - oldMaxHp);
        if (currentHp > newMaxHp) {
            currentHp = newMaxHp;
        }
        if (currentHp < 0) {
            currentHp = 0;
        }
        if (wasAlive && sanitizedBonus == 0 && currentHp == 0) {
            currentHp = 1;
        }
    }

    public boolean markAppearedThisBattle() {
        if (appearedThisBattle) {
            return false;
        }
        appearedThisBattle = true;
        return true;
    }

    public boolean hasAppearedThisBattle() {
        return appearedThisBattle;
    }

    public Map<String, EffectInstance> getActiveEffects() {
        return activeEffects;
    }

    public boolean isRemovingEffect() {
        return removingEffect;
    }

    public void setRemovingEffect(boolean removingEffect) {
        this.removingEffect = removingEffect;
    }

    public int getInternalCooldown(String key) {
        return internalCooldowns.getOrDefault(key, 0);
    }

    public void setInternalCooldown(String key, int ticks) {
        internalCooldowns.put(key, ticks);
    }

    public int getSlotProgress(int slot) {
        if (slot >= 0 && slot < 3) {
            return slotProgress[slot];
        }
        return 0;
    }

    public void setSlotProgress(int slot, int value) {
        if (slot >= 0 && slot < 3) {
            slotProgress[slot] = value;
        }
    }

    public int getLockedSlot() {
        return lockedSlot;
    }

    public void setLockedSlot(int lockedSlot) {
        this.lockedSlot = lockedSlot;
    }

    public boolean hasUsedSecondChance() {
        return secondChanceUsed;
    }

    public boolean consumeSecondChance() {
        if (secondChanceUsed) {
            return false;
        }
        secondChanceUsed = true;
        return true;
    }

    public boolean isCharging() {
        return chargeTicks > 0;
    }

    public int getChargeTicks() {
        return chargeTicks;
    }

    public int getChargeTotalTicks() {
        return chargeTotalTicks;
    }

    public void startCharge(int ticks, AbilityLoader.AbilityData data, int slot, boolean isGolden, UUID targetUUID) {
        chargeTicks = ticks;
        chargeTotalTicks = ticks;
        pendingAbility = data;
        pendingSlot = slot;
        lockedSlot = slot;
        pendingIsGolden = isGolden;
        pendingTargetUUID = targetUUID;
    }

    public void decrementChargeTicks() {
        if (chargeTicks > 0) {
            chargeTicks--;
        }
    }

    public void cancelCharge() {
        chargeTicks = 0;
        chargeTotalTicks = 0;
        pendingAbility = null;
        pendingSlot = -1;
        if (blueChannelTicks <= 0) {
            lockedSlot = -1;
        }
        pendingIsGolden = false;
        pendingTargetUUID = null;
    }

    public AbilityLoader.AbilityData getPendingAbility() {
        return pendingAbility;
    }

    public int getPendingSlot() {
        return pendingSlot;
    }

    public boolean isPendingGolden() {
        return pendingIsGolden;
    }

    public UUID getPendingTargetUUID() {
        return pendingTargetUUID;
    }

    public boolean isBlueChanneling() {
        return blueChannelTicks > 0;
    }

    public void startBlueChannel(int totalTicks, int interval, AbilityLoader.AbilityData data, int slot, boolean isGolden,
                                 float totalDamage, float totalHeal, float totalMana) {
        blueChannelTicks = totalTicks;
        blueChannelTotalTicks = totalTicks;
        blueChannelInterval = interval;
        blueChannelAbility = data;
        blueChannelSlot = slot;
        lockedSlot = slot;
        blueChannelGolden = isGolden;
        blueChannelTotalDamage = totalDamage;
        blueChannelTotalHeal = totalHeal;
        blueChannelTotalMana = totalMana;
    }

    public void cancelBlueChannel() {
        blueChannelTicks = 0;
        blueChannelTotalTicks = 0;
        blueChannelInterval = 0;
        blueChannelAbility = null;
        blueChannelSlot = -1;
        if (chargeTicks <= 0) {
            lockedSlot = -1;
        }
        blueChannelGolden = false;
        blueChannelTotalDamage = 0;
        blueChannelTotalHeal = 0;
        blueChannelTotalMana = 0;
    }

    public void decrementBlueChannelTicks() {
        if (blueChannelTicks > 0) {
            blueChannelTicks--;
        }
    }

    public int getBlueChannelTicks() {
        return blueChannelTicks;
    }

    public int getBlueChannelTotalTicks() {
        return blueChannelTotalTicks;
    }

    public int getBlueChannelInterval() {
        return blueChannelInterval;
    }

    public AbilityLoader.AbilityData getBlueChannelAbility() {
        return blueChannelAbility;
    }

    public int getBlueChannelSlot() {
        return blueChannelSlot;
    }

    public boolean isBlueChannelGolden() {
        return blueChannelGolden;
    }

    public float getBlueChannelTotalDamage() {
        return blueChannelTotalDamage;
    }

    public float getBlueChannelTotalHeal() {
        return blueChannelTotalHeal;
    }

    public float getBlueChannelTotalMana() {
        return blueChannelTotalMana;
    }

    public void cancelActiveOnlyRuntime() {
        cancelCharge();
        cancelBlueChannel();
    }

    public boolean tryDodge() {
        return tryDodge(0);
    }

    public boolean tryDodge(int bagSizeModifier) {
        if (bagSizeModifier <= 0) {
            return dodgeBag.next();
        }

        int originalSize = TeenyBalance.getBagSize(getDodgeStat());
        int newSize = Math.max(1, originalSize - bagSizeModifier);

        if (Math.random() < (1.0 / newSize)) {
            return true;
        }

        return dodgeBag.next();
    }

    public boolean tryCrit() {
        return tryCrit(null);
    }

    public boolean tryCrit(bruhof.teenycraft.capability.IBattleState state) {
        if (state == null) {
            return luckBag.next();
        }

        int effectiveLuck = getEffectiveStat(StatType.LUCK, state);
        int baseLuck = getLuckStat();

        if (effectiveLuck == baseLuck) {
            return luckBag.next();
        }

        int originalSize = TeenyBalance.getBagSize(baseLuck);
        int newSize = TeenyBalance.getBagSize(effectiveLuck);

        if (newSize < originalSize) {
            if (Math.random() < (1.0 / newSize)) {
                return true;
            }
        } else if (newSize > originalSize) {
            if (luckBag.next()) {
                return Math.random() < ((double) originalSize / newSize);
            }
            return false;
        }

        return luckBag.next();
    }

    public boolean tryCrit(bruhof.teenycraft.capability.IBattleState state, float flatBonusChance) {
        if (tryCrit(state)) {
            return true;
        }

        float bonus = Math.max(0.0f, flatBonusChance);
        if (bonus <= 0.0f) {
            return false;
        }

        int baseLuck = getLuckStat();
        int effectiveLuck = state == null ? baseLuck : getEffectiveStat(StatType.LUCK, state);
        int baseBagSize = TeenyBalance.getBagSize(baseLuck);
        int effectiveBagSize = TeenyBalance.getBagSize(effectiveLuck);

        // The bonus bag is rolled only after a normal failure, so calibrate its
        // conditional chance to produce a true flat increase overall.
        double baseCritChance = 1.0 / effectiveBagSize;
        if (effectiveBagSize < baseBagSize) {
            baseCritChance += (1.0 - baseCritChance) / baseBagSize;
        }

        double conditionalBonusChance = Math.min(1.0, bonus / Math.max(0.0001, 1.0 - baseCritChance));
        int bonusCards = Math.max(1, (int) Math.round(conditionalBonusChance * 1000.0));
        return bonusCritBags.computeIfAbsent(
                bonusCards,
                cards -> new bruhof.teenycraft.util.ShuffleBag(1000, cards)
        ).next();
    }
}
