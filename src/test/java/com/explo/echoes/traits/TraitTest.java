package com.explo.echoes.traits;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import com.explo.echoes.memory.Affinity;
import com.explo.echoes.memory.Memory;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Guards the rule that Traits are a view over Memory and never a thing in their own right.
 *
 * <p>The moment a Trait is stored, the design starts to rot: stored unlocks can drift out of sync
 * with the history that justified them, they need migrating, and Merge has to remember to carry
 * them across. Deriving them makes all three problems impossible rather than merely handled.
 */
class TraitTest {

    @Test
    @DisplayName("a tool that has done nothing has learned nothing")
    void freshToolsHaveNoTraits() {
        assertTrue(Trait.derivedFrom(Memory.awakening(0L)).isEmpty());
    }

    @Test
    @DisplayName("a Trait appears exactly at its threshold, not before")
    void traitsAppearAtTheirThreshold() {
        Trait trait = Trait.GATHERS_ORE;

        assertFalse(Trait.derivedFrom(workedAt(trait.affinity(), trait.threshold() - 1)).contains(trait),
                "one block short of the threshold must not earn the Trait");
        assertTrue(Trait.derivedFrom(workedAt(trait.affinity(), trait.threshold())).contains(trait),
                "reaching the threshold must earn it");
    }

    @Test
    @DisplayName("work on one affinity does not teach another affinity's Trait")
    void traitsDoNotBleedAcrossAffinities() {
        Memory oreOnly = workedAt(Affinity.ORE, Trait.GATHERS_ORE.threshold());

        assertTrue(Trait.derivedFrom(oreOnly).contains(Trait.GATHERS_ORE));
        assertFalse(Trait.derivedFrom(oreOnly).contains(Trait.GATHERS_STONE));
    }

    @Test
    @DisplayName("no Trait is ever written to disk, yet survives a round trip")
    void traitsAreDerivedNotStored() {
        Memory earned = workedAt(Trait.GATHERS_ORE.affinity(), Trait.GATHERS_ORE.threshold());

        JsonElement written = Memory.CODEC.encodeStart(JsonOps.INSTANCE, earned).result().orElseThrow();
        assertFalse(written.toString().contains("trait"),
                "a Trait must never be persisted -- it is a view over Memory, not state: " + written);

        Memory reloaded = Memory.CODEC.parse(JsonOps.INSTANCE, written).result().orElseThrow();
        assertTrue(Trait.derivedFrom(reloaded).contains(Trait.GATHERS_ORE),
                "the Trait must reappear from the history alone, with nothing stored to remind it");
    }

    private static Memory workedAt(Affinity affinity, int blocks) {
        Memory memory = Memory.awakening(0L);
        for (int i = 0; i < blocks; i++) {
            memory = memory.afterMining(Optional.of(affinity));
        }
        return memory;
    }
}
