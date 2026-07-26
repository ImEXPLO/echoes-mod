package com.explo.echoes.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Guards the promise that Memory survives the code that wrote it.
 *
 * <p>Memory is permanent and irreplaceable — a tool's entire identity — so the one thing loading it
 * may never do is fail. These tests pin the two directions that matter: data written by an older
 * build must still load, and data written by a newer build must come back untouched rather than
 * being rejected or silently rewritten.
 *
 * <p>JSON rather than NBT on purpose: it needs no registry access, so the schema can be tested as
 * plain data.
 */
class MemorySchemaTest {

    @Test
    @DisplayName("a Memory missing every optional field still loads")
    void olderDataLoadsWithDefaults() {
        Memory memory = parse(new JsonObject());

        assertEquals(Memory.SCHEMA_VERSION, memory.schemaVersion());
        assertEquals(0, memory.blocksMined());
        assertEquals(1, memory.generation(), "a tool with no recorded lineage is the first of its line");
        assertTrue(memory.affinities().isEmpty());
        assertTrue(memory.inheritedFrom().isEmpty());
    }

    @Test
    @DisplayName("a Memory from a newer schema is preserved, not rejected")
    void newerDataSurvivesAnOlderBuild() {
        JsonObject fromTheFuture = new JsonObject();
        fromTheFuture.addProperty("schema_version", Memory.SCHEMA_VERSION + 1);
        fromTheFuture.addProperty("blocks_mined", 12_043);

        Memory memory = parse(fromTheFuture);

        assertEquals(Memory.SCHEMA_VERSION + 1, memory.schemaVersion(),
                "an unrecognised version must be carried as-is; an old build has no business "
                        + "stamping its own version onto a lineage it cannot fully read");
        assertEquals(12_043, memory.blocksMined());
    }

    @Test
    @DisplayName("affinities survive a round trip")
    void affinitiesRoundTrip() {
        Memory original = Memory.awakening(0L).afterMining(Optional.of(Affinity.ORE));

        Memory restored = parse(Memory.CODEC
                .encodeStart(JsonOps.INSTANCE, original)
                .result()
                .orElseThrow(() -> new AssertionError("a Memory must always be writable"))
                .getAsJsonObject());

        assertEquals(1, restored.count(Affinity.ORE));
        assertEquals(1, restored.blocksMined());
    }

    private static Memory parse(JsonObject json) {
        return Memory.CODEC.parse(JsonOps.INSTANCE, json)
                .result()
                .orElseThrow(() -> new AssertionError("Memory must never fail to load: " + json));
    }
}
