package bruhof.teenycraft.battle;

import bruhof.teenycraft.TeenyBalance;
import bruhof.teenycraft.item.custom.ItemFigure;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * The "Hot" Data Wrapper.
 * Created when a battle starts, destroyed when it ends.
 * Tracks volatile state like HP, Mana, Cooldowns, and Effects.
 */
public class BattleFigure {

    // Identity
    private final ItemStack originalStack; // Keep reference to write back XP later
    private final String figureId;
    private final String nickname;

    // Stats (Snapshot from Item)
    private final int maxHp;
    private final int power;
    private final int dodge;
    private final int luck;

    // Volatile State
    private int currentHp;
    private final int[] abilityCooldowns = new int[3]; // Slots 0, 1, 2
    
    // Shuffle Bags
    private final bruhof.teenycraft.util.ShuffleBag dodgeBag;
    private final bruhof.teenycraft.util.ShuffleBag luckBag;

    public BattleFigure(ItemStack stack) {
        this.originalStack = stack;
        this.figureId = ItemFigure.getFigureID(stack);
        this.nickname = ItemFigure.getFigureName(stack); // Todo: Get actual nickname if implemented

        // Snapshot Stats
        this.maxHp = ItemFigure.getHealth(stack);
        this.power = ItemFigure.getPower(stack);
        this.dodge = ItemFigure.getDodge(stack);
        this.luck = ItemFigure.getLuck(stack);
        
        // Initialize Bags
        int dodgeDeck = TeenyBalance.getBagSize(this.dodge);
        this.dodgeBag = new bruhof.teenycraft.util.ShuffleBag(dodgeDeck, 1);
        
        int luckDeck = TeenyBalance.getBagSize(this.luck);
        this.luckBag = new bruhof.teenycraft.util.ShuffleBag(luckDeck, 1);

        // Initialize State
        this.currentHp = this.maxHp;
    }

    // ==========================================
    // LOGIC TICKS
    // ==========================================

    public void tick() {
        // 1. Cooldowns
        for (int i = 0; i < abilityCooldowns.length; i++) {
            if (abilityCooldowns[i] > 0) {
                abilityCooldowns[i]--;
            }
        }
    }

    // ==========================================
    // GETTERS & SETTERS
    // ==========================================

    public ItemStack getOriginalStack() { return originalStack; }
    public String getFigureId() { return figureId; }
    public String getNickname() { return nickname; }

    public int getCurrentHp() { return currentHp; }
    public int getMaxHp() { return maxHp; }
    
    public int getPowerStat() { return this.power; }
    public int getDodgeStat() { return this.dodge; }
    public int getLuckStat() { return this.luck; }

    public int getCooldown(int slotIndex) {
        if (slotIndex >= 0 && slotIndex < 3) return abilityCooldowns[slotIndex];
        return 0;
    }

    public void modifyHp(int amount) {
        this.currentHp += amount;
        if (this.currentHp > this.maxHp) this.currentHp = this.maxHp;
        if (this.currentHp < 0) this.currentHp = 0;
    }
    
    public void setCooldown(int slotIndex, int ticks) {
        if (slotIndex >= 0 && slotIndex < 3) {
            this.abilityCooldowns[slotIndex] = ticks;
        }
    }
    
    // ==========================================
    // BATTLE LOGIC HELPERS
    // ==========================================
    
    public boolean tryDodge() {
        return tryDodge(0);
    }

    public boolean tryDodge(int bagSizeModifier) {
        if (bagSizeModifier <= 0) {
            return dodgeBag.next();
        }
        
        // Logic: If we have a modifier, we roll against a virtual smaller bag
        // Probability = 1 / (OriginalSize - Modifier)
        int originalSize = TeenyBalance.getBagSize(getDodgeStat());
        int newSize = Math.max(1, originalSize - bagSizeModifier);
        
        // We use Math.random here because modifying the physical bag mid-battle is complex
        // This mimics the probability of a smaller shuffle bag
        if (Math.random() < (1.0 / newSize)) return true;
        
        // If the bonus roll fails, we still check the regular bag
        return dodgeBag.next();
    }
    
    public boolean tryCrit() {
        return luckBag.next();
    }
}
