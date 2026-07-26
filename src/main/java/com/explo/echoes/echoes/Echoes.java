package com.explo.echoes.echoes;

import java.util.Optional;

import com.explo.echoes.MemoryEchoes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.item.ItemStack;

/**
 * The spendable resource a tool's wear leaves behind.
 *
 * <p>Echoes are the economy layer, and the only layer a player ever consumes. They are not
 * identity: a tool that has spent every Echo it ever formed has lost nothing of who it is, because
 * who it is lives in Memory.
 *
 * <h2>Why wear, and not deeds</h2>
 *
 * <p>Memory counts every meaningful action. Echoes count only the ones that actually cost the tool
 * durability, and this is the single most important rule in the mod's economy.
 *
 * <p>Generating an Echo per action instead would be quietly catastrophic. Unbreaking III makes
 * roughly four actions cost one durability, so any exchange rate tuned against one-damage-per-
 * action flips net-<em>positive</em> under enchantment, and the mod accidentally ships infinite
 * durability. Counting wear instead means an Echo always costs {@link #WEAR_PER_ECHO} points of
 * real durability to form, whatever enchantments are involved and whatever a future mod does to
 * durability. The repair economy is then net-negative by construction rather than by tuning, and
 * no amount of rebalancing can silently break it.
 *
 * <h2>Schema</h2>
 *
 * <p>Unlike Memory, Echoes carry no schema version. Memory is permanent and irreplaceable, so it
 * is versioned from the first commit; Echoes are transient and constantly spent, both fields have
 * codec defaults, and the worst case for a future format change is a tool holding a few fewer
 * Echoes than it had. That is not worth a migration path.
 */
public record Echoes(int available, int progress) {

    /**
     * Points of real durability that must be worn away to form one Echo.
     *
     * <p>The repair sink's exchange rate must stay strictly below this, which is what keeps a
     * continuously-used tool trending toward breaking.
     */
    public static final int WEAR_PER_ECHO = 25;

    /** What a tool holds before it has ever been worn. */
    public static final Echoes NONE = new Echoes(0, 0);

    public static final Codec<Echoes> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("available", 0).forGetter(Echoes::available),
            Codec.INT.optionalFieldOf("progress", 0).forGetter(Echoes::progress))
            .apply(instance, Echoes::new));

    /** The Echoes carried by the given tool, if it has ever worn enough to form any. */
    public static Optional<Echoes> of(ItemStack stack) {
        return Optional.ofNullable(stack.get(MemoryEchoes.ECHOES));
    }

    /** Writes these Echoes back onto the tool that formed them. */
    public void saveTo(ItemStack stack) {
        stack.set(MemoryEchoes.ECHOES, this);
    }

    /**
     * These Echoes after the tool has worn by the given number of durability points.
     *
     * <p>Leftover wear is kept rather than discarded, so a tool loses nothing to rounding across a
     * long life — twenty-five single points of wear form exactly the same Echo as one block that
     * somehow cost twenty-five.
     */
    public Echoes afterWear(int wear) {
        int accumulated = progress + wear;
        return new Echoes(available + accumulated / WEAR_PER_ECHO, accumulated % WEAR_PER_ECHO);
    }

    /**
     * These Echoes after spending some of them.
     *
     * <p>Progress toward the next Echo is untouched: spending is not a reset, and a tool that has
     * almost formed its next Echo does not lose that ground by cashing in the ones it already has.
     *
     * @param count how many to spend, which callers are expected to have capped at
     *              {@link #available()} — see {@link Repair#of(int, int)}
     */
    public Echoes afterSpending(int count) {
        return new Echoes(available - count, progress);
    }
}
