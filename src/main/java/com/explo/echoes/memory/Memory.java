package com.explo.echoes.memory;

import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

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

    /** How much of this tool's history was spent on the given kind of work. */
    public int count(Affinity affinity) {
        return affinities.getOrDefault(affinity, 0);
    }
}
