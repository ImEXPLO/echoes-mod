package com.explo.echoes.echoes;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.RandomSource;

/**
 * The words a tool finds when its past is called on.
 *
 * <p>Spending Echoes is mechanically a subtraction, and left unadorned it reads like one. A line of
 * text is the cheapest thing in the mod and does more than any number could to make the act feel
 * like recalling something rather than paying for something — which is the difference between a
 * durability mod and a mod about tools that remember.
 *
 * <p>The pool is large on purpose. A message the player has seen forty times stops being
 * atmosphere and starts being a notification, so there are enough lines that the tool rarely
 * repeats itself within a session. They are deliberately short, never explanatory, and never
 * mention numbers — the tooltip already handles the arithmetic.
 */
public final class Recollection {

    /** Number of lines in the pool; keys run from {@code recall.memoryechoes.1} upward. */
    private static final int COUNT = 48;

    private Recollection() {}

    /**
     * One line, drawn at random, for the moment a tool's past answers.
     *
     * <p>Returns a {@link MutableComponent} rather than a bare {@link Component} so callers can
     * style it — only the mutable form carries {@code withStyle}.
     */
    public static MutableComponent random(RandomSource random) {
        return Component.translatable("recall.memoryechoes." + (1 + random.nextInt(COUNT)));
    }
}
