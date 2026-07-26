package com.explo.echoes.echoes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Guards the one rule the Echo economy cannot be allowed to break: a tool mended only by its own
 * Echoes must still wear out.
 *
 * <p>If using a tool ever nets positive durability, the mod has shipped infinite durability and
 * quietly invalidated anvils, repair materials and Mending. That failure would be invisible in
 * play — the tool would simply never break — so it is encoded here instead, where a rebalance that
 * inverts it fails loudly.
 *
 * <p>These assertions are about the <em>relationship</em> between the tuning numbers, not the
 * numbers themselves. Both are meant to be retuned freely; neither may cross the other.
 */
class EchoEconomyTest {

    @Test
    @DisplayName("an Echo gives back less durability than it cost to form")
    void repairRateStaysBelowTheWearRate() {
        assertTrue(Repair.DURABILITY_PER_ECHO < Echoes.WEAR_PER_ECHO,
                "Repair must never return more durability than the wear that produced the Echo. "
                        + "DURABILITY_PER_ECHO=" + Repair.DURABILITY_PER_ECHO
                        + " must stay strictly below WEAR_PER_ECHO=" + Echoes.WEAR_PER_ECHO);
    }

    @Test
    @DisplayName("a tool repaired only by its own Echoes still breaks")
    void continuousUseTrendsTowardBreaking() {
        final int maxDurability = 250;
        final int giveUp = 1_000_000;

        int damage = 0;
        long blocksMined = 0;
        Echoes echoes = Echoes.NONE;

        while (damage < maxDurability) {
            damage++;
            blocksMined++;
            echoes = echoes.afterWear(1);

            Repair repair = Repair.of(echoes.available(), damage);
            damage -= repair.durabilityRestored();
            echoes = echoes.afterSpending(repair.echoesSpent());

            assertTrue(blocksMined < giveUp,
                    "the tool never broke after " + giveUp + " blocks, so repair is net-positive "
                            + "and the mod has created infinite durability");
        }

        assertTrue(blocksMined > maxDurability,
                "Echoes should meaningfully extend a tool's life, but it broke after "
                        + blocksMined + " blocks with only " + maxDurability + " durability");
    }

    @Test
    @DisplayName("wear below the threshold accumulates instead of being lost to rounding")
    void leftoverWearIsCarried() {
        Echoes echoes = Echoes.NONE;
        for (int i = 0; i < Echoes.WEAR_PER_ECHO; i++) {
            echoes = echoes.afterWear(1);
        }

        assertEquals(1, echoes.available(), "single points of wear should still add up to an Echo");
        assertEquals(0, echoes.progress());
    }

    @Test
    @DisplayName("spending Echoes does not reset progress toward the next one")
    void spendingKeepsProgress() {
        Echoes echoes = Echoes.NONE.afterWear(Echoes.WEAR_PER_ECHO + 7);
        assertEquals(1, echoes.available());
        assertEquals(7, echoes.progress());

        assertEquals(7, echoes.afterSpending(1).progress());
    }

    @Test
    @DisplayName("a barely damaged tool does not squander its whole history")
    void repairSpendsOnlyWhatTheDamageCanAbsorb() {
        Repair repair = Repair.of(40, 1);

        assertEquals(1, repair.echoesSpent(), "one point of damage should cost exactly one Echo");
        assertEquals(1, repair.durabilityRestored(), "and restore only the damage that existed");
    }

    @Test
    @DisplayName("nothing happens without Echoes or without damage")
    void repairIsANoOpWhenThereIsNothingToDo() {
        assertEquals(Repair.NOTHING, Repair.of(0, 100));
        assertEquals(Repair.NOTHING, Repair.of(10, 0));
    }
}
