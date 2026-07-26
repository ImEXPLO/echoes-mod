package com.explo.echoes.merge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import com.explo.echoes.memory.Affinity;
import com.explo.echoes.memory.Memory;
import com.explo.echoes.traits.Trait;

import net.minecraft.network.chat.Component;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Guards the two promises Merge makes: an upgrade never costs a tool its personality, and no amount
 * of merging invents a history nobody lived.
 *
 * <p>The first is the one that would hurt most if broken, and it would break silently — a player
 * upgrades their pickaxe and the Trait they earned over hours is simply gone, with nothing in the
 * logs and no error to trace.
 */
class MergeTest {

    private static final Component OLD_RELIABLE = Component.literal("Old Reliable");

    @Test
    @DisplayName("an upgrade never revokes a Trait the predecessor had earned")
    void mergeNeverRevokesATrait() {
        Trait earned = Trait.GATHERS_ORE;
        Memory veteran = workedAt(earned.affinity(), earned.threshold());
        assertTrue(Trait.derivedFrom(veteran).contains(earned), "precondition: the predecessor has the Trait");

        Memory successor = Merge.inherit(veteran, Optional.empty(), OLD_RELIABLE);

        assertTrue(Trait.derivedFrom(successor).contains(earned),
                "the proportional transfer dropped the successor below the threshold and silently "
                        + "revoked an earned Trait -- an upgrade must never downgrade a tool's personality");
    }

    @Test
    @DisplayName("a Trait earned right at the threshold survives, which is the fragile case")
    void traitsAtTheExactThresholdSurvive() {
        for (Trait trait : Trait.values()) {
            Memory atThreshold = workedAt(trait.affinity(), trait.threshold());
            Memory successor = Merge.inherit(atThreshold, Optional.empty(), OLD_RELIABLE);

            assertTrue(Trait.derivedFrom(successor).contains(trait),
                    trait + " was lost when inherited from a predecessor sitting exactly on its threshold");
        }
    }

    @Test
    @DisplayName("histories are never summed, so tools cannot be funnelled into one")
    void mergeIsNotAdditive() {
        Memory donor = workedAt(Affinity.STONE, 400);

        Memory once = Merge.inherit(donor, Optional.empty(), OLD_RELIABLE);
        Memory twice = Merge.inherit(donor, Optional.of(once), OLD_RELIABLE);
        Memory thrice = Merge.inherit(donor, Optional.of(twice), OLD_RELIABLE);

        assertEquals(once.count(Affinity.STONE), thrice.count(Affinity.STONE),
                "feeding in more predecessors must not accumulate history -- each merge takes the "
                        + "greater of the two, never the sum");
        assertTrue(thrice.count(Affinity.STONE) < donor.count(Affinity.STONE),
                "a successor should carry less than the life its predecessor actually lived");
    }

    @Test
    @DisplayName("the successor keeps its own history when it already outstrips the predecessor")
    void aRicherSuccessorKeepsItsOwnPast() {
        Memory novice = workedAt(Affinity.STONE, 10);
        Memory veteran = workedAt(Affinity.STONE, 900);

        Memory merged = Merge.inherit(novice, Optional.of(veteran), OLD_RELIABLE);

        assertEquals(900, merged.count(Affinity.STONE), "inheriting from a lesser tool must not erase a greater past");
    }

    @Test
    @DisplayName("each merge deepens the line by exactly one")
    void generationsAccumulate() {
        Memory first = workedAt(Affinity.STONE, 100);

        Memory second = Merge.inherit(first, Optional.empty(), OLD_RELIABLE);
        Memory third = Merge.inherit(second, Optional.empty(), OLD_RELIABLE);

        assertEquals(2, second.generation());
        assertEquals(3, third.generation());
        assertTrue(second.inheritedFrom().isPresent(), "a successor must be able to name its predecessor");
    }

    @Test
    @DisplayName("the line is dated from its beginning, not from the newest tool")
    void lineageKeepsTheEarliestBeginning() {
        Memory ancient = Memory.awakening(1_000L).afterMining(Optional.of(Affinity.STONE));
        Memory recent = Memory.awakening(9_000L).afterMining(Optional.of(Affinity.STONE));

        assertEquals(1_000L, Merge.inherit(ancient, Optional.of(recent), OLD_RELIABLE).awakenedAt());
    }

    @Test
    @DisplayName("the total never contradicts the affinities it is made of")
    void totalStaysConsistentWithItsParts() {
        Trait trait = Trait.GATHERS_ORE;
        Memory veteran = workedAt(trait.affinity(), trait.threshold());

        Memory successor = Merge.inherit(veteran, Optional.empty(), OLD_RELIABLE);

        assertTrue(successor.blocksMined() >= successor.count(trait.affinity()),
                "a tool cannot remember fewer blocks in total than it remembers of one kind");
    }

    private static Memory workedAt(Affinity affinity, int blocks) {
        Memory memory = Memory.awakening(0L);
        for (int i = 0; i < blocks; i++) {
            memory = memory.afterMining(Optional.of(affinity));
        }
        return memory;
    }
}
