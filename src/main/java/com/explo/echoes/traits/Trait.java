package com.explo.echoes.traits;

import java.util.EnumSet;
import java.util.Set;

import com.explo.echoes.memory.Affinity;
import com.explo.echoes.memory.Memory;

/**
 * What a tool has learned from what it has done.
 *
 * <p>Traits are never stored. There is no unlock flag anywhere on the stack: a Trait is always
 * recomputed from Memory, every time it is asked for. That is not an optimisation, it is the
 * mechanism — because Traits are a pure function of history, any Memory that survives an upgrade
 * brings its Traits with it automatically, with no migration, no flags to keep in sync, and no way
 * for the two to disagree.
 *
 * <h2>What a Trait may and may not be</h2>
 *
 * <p>Traits never touch durability. Not preserve it, not increase it, not reduce its loss.
 * Durability belongs exclusively to the Echo repair sink, and a Trait that saved durability would
 * reduce the need to repair — starving the only sink Echoes have and quietly recreating the
 * self-obsoleting mod this design exists to avoid.
 *
 * <p>Traits also never compete with vanilla enchantments. Nothing here makes a tool faster, luckier
 * or sharper; Efficiency, Fortune, Sharpness, Unbreaking and Mending keep their whole axis to
 * themselves. The question a Trait answers is "what has this tool learned from its past?", never
 * "how much stronger is it?" — the player should come away remembering the behaviour, not a
 * percentage.
 *
 * <h2>Thresholds</h2>
 *
 * <p>Deliberately uneven. Ore is scarce and stone is everywhere, so asking for the same count of
 * each would make the ore Trait a lifetime achievement and the stone one an afternoon. The numbers
 * are tuned to take roughly comparable effort, not to be tidy.
 */
public enum Trait {
    GATHERS_STONE("gathers_stone", Affinity.STONE, 1_000),
    GATHERS_ORE("gathers_ore", Affinity.ORE, 150),
    GATHERS_WOOD("gathers_wood", Affinity.WOOD, 300),
    GATHERS_EARTH("gathers_earth", Affinity.EARTH, 500);

    private final String id;
    private final Affinity affinity;
    private final int threshold;

    Trait(String id, Affinity affinity, int threshold) {
        this.id = id;
        this.affinity = affinity;
        this.threshold = threshold;
    }

    /**
     * Everything the given history has taught the tool.
     *
     * <p>Returned in declaration order, so a tool's Traits never reshuffle between tooltip renders.
     */
    public static Set<Trait> derivedFrom(Memory memory) {
        EnumSet<Trait> earned = EnumSet.noneOf(Trait.class);
        for (Trait trait : values()) {
            if (memory.count(trait.affinity) >= trait.threshold) {
                earned.add(trait);
            }
        }
        return earned;
    }

    /**
     * The highest Trait threshold a given amount of work has already earned, or zero.
     *
     * <p>Merge uses this as a floor. A proportional transfer alone could drop a successor just
     * below a threshold its predecessor had passed, silently revoking a Trait and turning an
     * upgrade into a downgrade of the tool's personality. Guaranteeing the inherited count never
     * falls below what was already achieved preserves every earned Trait without storing a single
     * unlock flag.
     */
    public static int highestThresholdCrossed(Affinity affinity, int count) {
        int crossed = 0;
        for (Trait trait : values()) {
            if (trait.affinity == affinity && count >= trait.threshold) {
                crossed = Math.max(crossed, trait.threshold);
            }
        }
        return crossed;
    }

    /** The lang key suffix identifying this Trait. */
    public String id() {
        return id;
    }

    /** The kind of work that teaches this Trait. */
    public Affinity affinity() {
        return affinity;
    }

    /** How much of that work it takes. */
    public int threshold() {
        return threshold;
    }
}
