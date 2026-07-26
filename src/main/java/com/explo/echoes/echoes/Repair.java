package com.explo.echoes.echoes;

/**
 * What spending Echoes on a worn tool would actually accomplish.
 *
 * <p>Repair is the only sink Echoes have, and the only thing in the entire mod permitted to touch
 * durability. Traits never will — a Trait that preserved durability would reduce the need to
 * repair, starving the sole sink and quietly recreating the self-obsoleting mod this design exists
 * to avoid.
 *
 * <p>It is also, deliberately, not the point of the mod. Repair is the smallest useful thing to do
 * with Echoes, shipped so the economy has an outlet at all; the identity lives in Memory.
 *
 * <h2>The exchange rate is the invariant</h2>
 *
 * <p>{@link #DURABILITY_PER_ECHO} must stay strictly below {@link Echoes#WEAR_PER_ECHO}. That
 * single inequality is what guarantees a tool mended only by its own Echoes still trends toward
 * breaking: every Echo costs more durability to form than it gives back. Both numbers are free to
 * be retuned, but not past each other — and a test asserts the relationship rather than the
 * values, so tuning cannot silently invert it.
 *
 * <p>This is a value describing a repair, not a repair being performed. Keeping the arithmetic
 * separate from the act is what lets the economy be simulated to exhaustion in a unit test without
 * a running game.
 */
public record Repair(int echoesSpent, int durabilityRestored) {

    /** Durability restored per Echo spent. Must remain strictly below {@link Echoes#WEAR_PER_ECHO}. */
    public static final int DURABILITY_PER_ECHO = 10;

    /** No Echoes to spend, or nothing worth spending them on. */
    public static final Repair NOTHING = new Repair(0, 0);

    /**
     * What a tool holding {@code available} Echoes and carrying {@code damage} would get out of a
     * repair right now.
     *
     * <p>Spends only as many Echoes as the damage can absorb, so mending a barely-scratched tool
     * never wastes the rest of its history on a single point of durability.
     */
    public static Repair of(int available, int damage) {
        if (available <= 0 || damage <= 0) {
            return NOTHING;
        }

        int spent = Math.min(available, Math.ceilDiv(damage, DURABILITY_PER_ECHO));
        return new Repair(spent, Math.min(damage, spent * DURABILITY_PER_ECHO));
    }
}
