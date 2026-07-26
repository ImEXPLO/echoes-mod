package com.explo.echoes.memory;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

/**
 * The kinds of work a tool is remembered for.
 *
 * <p>Affinities are deliberately coarse. Memory is a story the player reads at a glance, and
 * four broad categories carry more personality than forty precise ones — "this pickaxe knows
 * ore" is a character trait, "this pickaxe has broken 4,120 deepslate and 3,880 andesite" is a
 * spreadsheet.
 *
 * <p>Blocks belonging to none of these still count toward a tool's total history; they simply do
 * not shape its character.
 */
public enum Affinity implements StringRepresentable {
    STONE("stone"),
    ORE("ore"),
    WOOD("wood"),
    EARTH("earth");

    public static final Codec<Affinity> CODEC = StringRepresentable.fromEnum(Affinity::values);

    private final String serializedName;

    Affinity(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
