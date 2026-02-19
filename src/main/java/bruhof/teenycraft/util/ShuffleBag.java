package bruhof.teenycraft.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ShuffleBag {
    private final List<Boolean> bag = new ArrayList<>();
    private final Random random = new Random();
    private int currentSize;
    private int successCount;
    private int totalSize;

    public ShuffleBag(int totalSize, int successCount) {
        this.totalSize = Math.max(1, totalSize);
        this.successCount = Math.max(1, successCount); // At least 1 success card? Or can be 0?
        // Let's assume stats can result in 0 success if low enough, but user said "1 is Dodge, 9 are Hit".
        // Actually, user said "Higher stats improve the odds (e.g. deck size gets smaller)".
        // So success count is fixed (1), total size varies.
        this.successCount = 1; 
        reshuffle();
    }

    public void resize(int newTotalSize) {
        this.totalSize = Math.max(1, newTotalSize);
        reshuffle();
    }

    public boolean next() {
        if (bag.isEmpty()) {
            reshuffle();
        }
        return bag.remove(0);
    }

    private void reshuffle() {
        bag.clear();
        for (int i = 0; i < successCount; i++) {
            bag.add(true); // Success (Dodge/Crit)
        }
        for (int i = 0; i < totalSize - successCount; i++) {
            bag.add(false); // Fail (Hit/Normal)
        }
        Collections.shuffle(bag, random);
    }
}
