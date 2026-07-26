package com.explo.echoes.memory;

import java.util.Optional;

import com.mojang.serialization.Codec;

import net.minecraft.tags.BlockTags;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;

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

    /**
     * The character of a block, if it has one this mod recognises.
     *
     * <p>Tag-driven rather than block-by-block, so modded stone and modded ore are understood
     * without knowing they exist. Ore is tested first: deepslate ores belong to both the ore and
     * the stone families, and a tool that has found ore should be remembered for the ore.
     */
    public static Optional<Affinity> of(BlockState state) {
        if (state.is(Tags.Blocks.ORES)) {
            return Optional.of(ORE);
        }
        if (state.is(BlockTags.LOGS) || state.is(BlockTags.PLANKS)) {
            return Optional.of(WOOD);
        }
        if (state.is(Tags.Blocks.STONES)
                || state.is(Tags.Blocks.COBBLESTONES)
                || state.is(Tags.Blocks.END_STONES)
                || state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.BASE_STONE_NETHER)) {
            return Optional.of(STONE);
        }
        if (state.is(BlockTags.DIRT) || state.is(Tags.Blocks.SANDS) || state.is(Tags.Blocks.GRAVELS)) {
            return Optional.of(EARTH);
        }
        return Optional.empty();
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
