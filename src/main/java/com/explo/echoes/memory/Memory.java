package com.explo.echoes.memory;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import com.explo.echoes.MemoryEchoes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.item.ItemStack;

/**
 * A tool's permanent history — what it has done, how long it has been doing it, and whose legacy
 * it carries.
 *
 * <p>Memory is the identity layer. It is never spent, never consumed, and never reset; Echoes are
 * the spendable resource and Traits are a derived view over this record. Nothing here is a Trait
 * flag: what a tool has *learned* is always recomputed from what it has *done*, so a Memory that
 * survives an upgrade automatically brings its Traits with it.
 *
 * <p>The record is immutable. Accrual returns a new Memory rather than mutating one, which keeps
 * the arithmetic testable without a running game.
 *
 * <h2>Memory counts deeds, not wear</h2>
 *
 * <p>Every meaningful action is recorded here, whether or not it cost the tool any durability.
 * That is what keeps "12,043 blocks mined" literally true on a tool enchanted with Unbreaking.
 * Echoes are the layer that only accrues from real wear, which is what makes the repair economy
 * net-negative by construction rather than by tuning.
 *
 * <h2>Schema</h2>
 *
 * <p>Memory outlives the code that wrote it, so every field carries a default and the record
 * carries a {@link #schemaVersion()}. Every field the design already knows it needs is present
 * from version 1 — including {@link #generation()} and {@link #inheritedFrom()}, which no code
 * reads until Merge exists — specifically so that adding Merge does not require a version bump.
 * The version field earns its keep the first time a field's *meaning* changes rather than its
 * presence.
 */
public record Memory(
        int schemaVersion,
        long awakenedAt,
        int generation,
        Optional<Component> inheritedFrom,
        int blocksMined,
        Map<Affinity, Integer> affinities) {

    public static final int SCHEMA_VERSION = 1;

    public static final Codec<Memory> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("schema_version", SCHEMA_VERSION).forGetter(Memory::schemaVersion),
            Codec.LONG.optionalFieldOf("awakened_at", 0L).forGetter(Memory::awakenedAt),
            Codec.INT.optionalFieldOf("generation", 1).forGetter(Memory::generation),
            ComponentSerialization.CODEC.optionalFieldOf("inherited_from").forGetter(Memory::inheritedFrom),
            Codec.INT.optionalFieldOf("blocks_mined", 0).forGetter(Memory::blocksMined),
            Codec.unboundedMap(Affinity.CODEC, Codec.INT).optionalFieldOf("affinities", Map.of())
                    .forGetter(Memory::affinities))
            .apply(instance, Memory::new));

    public Memory {
        affinities = Map.copyOf(affinities);
    }

    /**
     * The Memory a tool receives the moment it first does something worth remembering.
     *
     * @param gameTime the level time of that first deed, which is what makes the tool's age
     *                 readable later
     */
    public static Memory awakening(long gameTime) {
        return new Memory(SCHEMA_VERSION, gameTime, 1, Optional.empty(), 0, Map.of());
    }

    /**
     * The history carried by the given tool, if it has begun one.
     *
     * <p>Absent means the tool has genuinely never done anything worth remembering — an untouched
     * pickaxe is byte-for-byte a vanilla pickaxe, not one holding an empty Memory.
     */
    public static Optional<Memory> of(ItemStack stack) {
        return Optional.ofNullable(stack.get(MemoryEchoes.MEMORY)).map(Memory::migrated);
    }

    /** Writes this history back onto the tool it belongs to. */
    public void saveTo(ItemStack stack) {
        stack.set(MemoryEchoes.MEMORY, this);
    }

    /**
     * This history one block later.
     *
     * @param affinity the character of the block, absent if it was something the tool has no
     *                 particular relationship with — it still counts toward the total history,
     *                 it just does not shape the tool's character
     */
    public Memory afterMining(Optional<Affinity> affinity) {
        Map<Affinity, Integer> updated = affinity.map(this::incremented).orElse(affinities);
        return new Memory(schemaVersion, awakenedAt, generation, inheritedFrom, blocksMined + 1, updated);
    }

    /** How much of this tool's history was spent on the given kind of work. */
    public int count(Affinity affinity) {
        return affinities.getOrDefault(affinity, 0);
    }

    /**
     * The kind of work this tool has done most of, if it has done any that counts.
     *
     * <p>Ties break toward the affinity declared first, so a tool's stated character never
     * flickers between two equal halves of its history.
     */
    public Optional<Affinity> dominantAffinity() {
        Affinity dominant = null;
        for (Affinity affinity : Affinity.values()) {
            if (count(affinity) > 0 && (dominant == null || count(affinity) > count(dominant))) {
                dominant = affinity;
            }
        }
        return Optional.ofNullable(dominant);
    }

    /**
     * Brings a history loaded from an older schema up to the current one.
     *
     * <p>Every read goes through here, so there is exactly one place a future version bump adds
     * its migration. Version 1 is currently the only version and every field in {@link #CODEC}
     * has a default, so there is nothing to do yet.
     *
     * <p>A history from a *newer* schema than this build understands is returned untouched rather
     * than rejected or rewritten. Memory is permanent and irreplaceable: an older client loading
     * a tool it does not fully understand must hand it back intact, not corrupt a lineage it
     * cannot read.
     */
    private Memory migrated() {
        return this;
    }

    private Map<Affinity, Integer> incremented(Affinity affinity) {
        EnumMap<Affinity, Integer> updated = new EnumMap<>(Affinity.class);
        updated.putAll(affinities);
        updated.merge(affinity, 1, Integer::sum);
        return updated;
    }
}
