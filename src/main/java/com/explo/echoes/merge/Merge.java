package com.explo.echoes.merge;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import com.explo.echoes.memory.Affinity;
import com.explo.echoes.memory.Memory;
import com.explo.echoes.traits.Trait;

import net.minecraft.network.chat.Component;

/**
 * Inheritance: an old tool's history passed forward to its successor.
 *
 * <p>This exists to solve a problem the rest of the mod would otherwise create. Once a player is
 * attached to a pickaxe, vanilla progression eventually asks them to abandon it for a better one —
 * and a mod about attachment that punishes upgrading has failed. Merge makes the upgrade a
 * continuation instead of a replacement: the diamond pickaxe is not a new tool, it is the next one
 * in a line.
 *
 * <h2>Inheritance, not fusion</h2>
 *
 * <p>One predecessor, one successor, in one direction. Histories are never combined and the
 * transfer is never selectable — the player cannot pick which parts of a past to keep, which is
 * what stops affinities being laundered away.
 *
 * <p>Every dimension takes the <em>greater</em> of what the successor already had and what it
 * inherits, never the sum. Summing would turn Merge into a funnel: mine briefly with a stream of
 * throwaway tools, feed them all into one, and accumulate a history nothing actually lived. Taking
 * the maximum means a merged tool's past is the deeper of the two lives behind it, and no amount
 * of chaining invents more.
 *
 * <h2>Why Traits need no special handling</h2>
 *
 * <p>Traits are derived from Memory, so a naive fractional transfer could land the successor just
 * under a threshold and quietly revoke a Trait its predecessor had earned. The inherited amount is
 * therefore floored at {@link Trait#highestThresholdCrossed} — never below what was already
 * achieved. With that floor in place no Trait is ever lost to an upgrade, and none of them had to
 * be stored, migrated, or copied to make it true.
 */
public final class Merge {

    /**
     * How much of a predecessor's history carries forward before the Trait floor is applied.
     *
     * <p>Below one on purpose. A line of tools should feel like it is carrying something across,
     * not photocopying it — and a successor that inherited everything would make the predecessor's
     * own long life meaningless.
     */
    public static final float INHERITED_FRACTION = 0.6F;

    private Merge() {}

    /**
     * The history a successor carries after inheriting from its predecessor.
     *
     * @param predecessor     the history of the tool being given up
     * @param existing        the successor's own history, if it had already begun one
     * @param predecessorName what the predecessor was called, kept so the successor can say whose
     *                        legacy it carries
     */
    public static Memory inherit(Memory predecessor, Optional<Memory> existing, Component predecessorName) {
        Map<Affinity, Integer> affinities = inheritedAffinities(predecessor, existing);

        return new Memory(
                Memory.SCHEMA_VERSION,
                lineageBegan(predecessor, existing),
                Math.max(predecessor.generation() + 1, existing.map(Memory::generation).orElse(1)),
                Optional.of(predecessorName),
                inheritedTotal(predecessor, existing, affinities),
                affinities);
    }

    private static Map<Affinity, Integer> inheritedAffinities(Memory predecessor, Optional<Memory> existing) {
        EnumMap<Affinity, Integer> affinities = new EnumMap<>(Affinity.class);

        for (Affinity affinity : Affinity.values()) {
            int inherited = predecessor.count(affinity);
            int carried = Math.max(
                    fraction(inherited),
                    Trait.highestThresholdCrossed(affinity, inherited));

            int kept = Math.max(carried, existing.map(memory -> memory.count(affinity)).orElse(0));
            if (kept > 0) {
                affinities.put(affinity, kept);
            }
        }
        return affinities;
    }

    /**
     * The successor's total, kept consistent with the parts it is made of.
     *
     * <p>The Trait floor can lift an individual affinity above its proportional share, which could
     * otherwise leave a tool claiming fewer blocks in total than the affinities it lists. The total
     * is held at or above their sum so the readout never contradicts itself.
     */
    private static int inheritedTotal(Memory predecessor, Optional<Memory> existing, Map<Affinity, Integer> affinities) {
        int carried = Math.max(fraction(predecessor.blocksMined()), existing.map(Memory::blocksMined).orElse(0));
        int accountedFor = affinities.values().stream().mapToInt(Integer::intValue).sum();
        return Math.max(carried, accountedFor);
    }

    /**
     * When this line of tools began — the earlier of the two, because the lineage is older than any
     * single tool in it.
     */
    private static long lineageBegan(Memory predecessor, Optional<Memory> existing) {
        return Math.min(predecessor.awakenedAt(), existing.map(Memory::awakenedAt).orElse(predecessor.awakenedAt()));
    }

    private static int fraction(int value) {
        return Math.round(value * INHERITED_FRACTION);
    }
}
