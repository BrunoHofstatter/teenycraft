package bruhof.teenycraft.capability;

import net.minecraft.nbt.CompoundTag;

public class TeenyCoins implements ITeenyCoins {
    private static final String TAG_COINS = "Coins";

    private int coins;

    @Override
    public int getCoins() {
        return coins;
    }

    @Override
    public void setCoins(int amount) {
        this.coins = Math.max(0, amount);
    }

    @Override
    public void addCoins(int delta) {
        long next = (long) this.coins + delta;
        if (next < 0) {
            this.coins = 0;
        } else if (next > Integer.MAX_VALUE) {
            this.coins = Integer.MAX_VALUE;
        } else {
            this.coins = (int) next;
        }
    }

    @Override
    public boolean trySpendCoins(int amount) {
        if (amount <= 0) {
            return true;
        }
        if (this.coins < amount) {
            return false;
        }
        this.coins -= amount;
        return true;
    }

    @Override
    public void copyFrom(ITeenyCoins oldStore) {
        CompoundTag nbt = new CompoundTag();
        oldStore.saveNBTData(nbt);
        this.loadNBTData(nbt);
    }

    @Override
    public void saveNBTData(CompoundTag tag) {
        tag.putInt(TAG_COINS, this.coins);
    }

    @Override
    public void loadNBTData(CompoundTag tag) {
        this.coins = Math.max(0, tag.getInt(TAG_COINS));
    }
}
