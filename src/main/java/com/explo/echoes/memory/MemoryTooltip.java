package com.explo.echoes.memory;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.explo.echoes.MemoryEchoes;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Renders a tool's history as prose.
 *
 * <p>Memory is written as sentences on purpose. Echoes will be a number the player spends, and the
 * two must never read as a pair of lookalike integers — so the permanent story is phrased the way
 * a story is phrased, and the spendable resource gets to look like a resource. That contrast is
 * the whole reason a player can tell at a glance which half of the tooltip they are allowed to
 * use up.
 *
 * <p>Restraint is deliberate. Three short lines, one accent colour, and vanilla's own greys: a
 * tooltip that shouts reads as a mod, and a tooltip that murmurs reads as part of the game.
 */
@EventBusSubscriber(Dist.CLIENT)
public final class MemoryTooltip {

    private static final long TICKS_PER_DAY = 24_000L;

    private MemoryTooltip() {}

    @SubscribeEvent
    static void onItemTooltip(ItemTooltipEvent event) {
        if (!event.getDisplay().shows(MemoryEchoes.MEMORY.get())) {
            return;
        }
        Memory.of(event.getItemStack())
                .ifPresent(memory -> append(memory, event.getEntity(), event.getToolTip()));
    }

    private static void append(Memory memory, Player reader, List<Component> tooltip) {
        tooltip.add(Component.empty());
        tooltip.add(remembers(memory));
        memory.dominantAffinity().map(MemoryTooltip::drawnTo).ifPresent(tooltip::add);
        awakened(memory, reader).ifPresent(tooltip::add);
        memory.inheritedFrom().map(MemoryTooltip::inheritedFrom).ifPresent(tooltip::add);
    }

    private static Component remembers(Memory memory) {
        String blocks = String.format(Locale.ROOT, "%,d", memory.blocksMined());
        return Component.translatable("tooltip.memoryechoes.remembers", blocks)
                .withStyle(ChatFormatting.GRAY);
    }

    private static Component drawnTo(Affinity affinity) {
        MutableComponent name = Component
                .translatable("affinity.memoryechoes." + affinity.getSerializedName())
                .withStyle(ChatFormatting.AQUA);
        return Component.translatable("tooltip.memoryechoes.drawn_to", name)
                .withStyle(ChatFormatting.GRAY);
    }

    /**
     * How long the tool has been at work, dated against the player holding it.
     *
     * <p>Absent when there is no reader to date it against: the client also builds tooltips with a
     * null player while populating its item search index at startup.
     *
     * @param reader the player reading the tooltip, or {@code null} during search indexing
     */
    private static Optional<Component> awakened(Memory memory, Player reader) {
        if (reader == null) {
            return Optional.empty();
        }

        long elapsed = Math.max(0L, reader.level().getGameTime() - memory.awakenedAt());
        long days = elapsed / TICKS_PER_DAY;

        MutableComponent line;
        if (days == 0L) {
            line = Component.translatable("tooltip.memoryechoes.awakened_today");
        } else if (days == 1L) {
            line = Component.translatable("tooltip.memoryechoes.awakened_yesterday");
        } else {
            line = Component.translatable("tooltip.memoryechoes.awakened", days);
        }
        return Optional.of(line.withStyle(ChatFormatting.DARK_GRAY));
    }

    /**
     * The lineage line, shown only once a tool has actually inherited something.
     *
     * <p>This is what turns a Memory count that jumps on the anvil from a glitch into a story, so
     * it has to be legible in the anvil's own output preview — before the player has committed to
     * anything, and without reading documentation.
     */
    private static Component inheritedFrom(Component predecessor) {
        return Component.translatable("tooltip.memoryechoes.inherited_from",
                        predecessor.copy().withStyle(ChatFormatting.ITALIC))
                .withStyle(ChatFormatting.GRAY);
    }
}
