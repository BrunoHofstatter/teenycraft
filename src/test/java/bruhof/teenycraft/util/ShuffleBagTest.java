package bruhof.teenycraft.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShuffleBagTest {
    @Test
    void honorsConfiguredSuccessCardCount() {
        ShuffleBag bag = new ShuffleBag(10, 3);
        int successes = 0;

        for (int i = 0; i < 10; i++) {
            if (bag.next()) {
                successes++;
            }
        }

        assertEquals(3, successes);
    }
}
