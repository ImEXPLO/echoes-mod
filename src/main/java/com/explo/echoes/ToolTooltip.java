package com.explo.echoes;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import com.explo.echoes.echoes.Echoes;
import com.explo.echoes.memory.Affinity;
import com.explo.echoes.memory.Memory;
import com.explo.echoes.traits.Trait;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TooltipDisplay;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Everything a storied tool has to say about itself.
 *
 * <p>This is the one place that composes the tool's whole readout, and it sits above both layers
 * because a tooltip is inherently about their arrangement: ordering two independent listeners
 * appending to the same list is undefined, and the design requires Memory and Echoes to appear in
 * a specific relationship to each other. Presentation depends on the domain; the domain never
 * depends back.
 *
 * <h2>Two voices</h2>
 *
 * <p>Memory speaks in sentences and Echoes reads as a counter, and the contrast is the point.
 * Rendering both as integers would leave the player unable to tell which number is a permanent
 * story and which one they are allowed to spend. So the permanent half is phrased the way a story
 * is phrased, and the spendable half is allowed to look like a resource — no legend required.
 *
 * <p>Restraint throughout: short lines, vanilla's own greys, one accent colour per layer. A
 * tooltip that shouts reads as a mod; a tooltip that murmurs reads as part of the game.
 */
@EventBusSubscriber(Dist.CLIENT)
public final class ToolTooltip {

    private static final long TICKS_PER_DAY = 24_000L;

    private ToolTooltip() {}

    @SubscribeEvent
    static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack tool = event.getItemStack();
        TooltipDisplay display = event.getDisplay();

        Optional<Memory> memory = display.shows(MemoryEchoes.MEMORY.get())
                ? Memory.of(tool)
                : Optional.empty();
        Optional<Echoes> echoes = display.shows(MemoryEchoes.ECHOES.get())
                ? Echoes.of(tool)
                : Optional.empty();

        if (memory.isEmpty() && echoes.isEmpty()) {
            return;
        }

        boolean advanced = event.getFlags().isAdvanced();
        List<Component> lines = event.getToolTip();
        lines.add(Component.empty());
        memory.ifPresent(m -> appendHistory(m, event.getEntity(), advanced, lines));
        memory.map(Trait::derivedFrom).ifPresent(traits -> appendTraits(traits, lines));
        echoes.ifPresent(e -> appendEchoes(e, advanced, lines));
    }

    private static void appendHistory(Memory memory, Player reader, boolean advanced, List<Component> lines) {
        lines.add(remembers(memory));
        memory.dominantAffinity().map(ToolTooltip::drawnTo).ifPresent(lines::add);
        if (advanced) {
            affinityCounts(memory).ifPresent(lines::add);
        }
        awakened(memory, reader).ifPresent(lines::add);
        memory.inheritedFrom().map(ToolTooltip::inheritedFrom).ifPresent(lines::add);
    }

    /**
     * The tally behind the affinity line.
     *
     * <p>"Drawn to stone" names a category and shows no number, which makes a tool that is quietly
     * accumulating look identical to one that has stalled — mine a hundred andesite and the line
     * never moves, because andesite <em>is</em> stone. The categories are the right granularity for
     * the mechanics, so the fix is to make the accumulation visible rather than to make the
     * categories narrower. It rides under F3+H because this is reassurance while testing and
     * tuning, not something the tooltip should say every time.
     */
    private static Optional<Component> affinityCounts(Memory memory) {
        MutableComponent tally = Component.empty();
        boolean first = true;

        for (Affinity affinity : Affinity.values()) {
            int count = memory.count(affinity);
            if (count == 0) {
                continue;
            }
            if (!first) {
                tally.append(" · ");
            }
            tally.append(Component.translatable("tooltip.memoryechoes.affinity_count",
                    Component.translatable("affinity.memoryechoes." + affinity.getSerializedName()),
                    count));
            first = false;
        }

        return first ? Optional.empty() : Optional.of(tally.withStyle(ChatFormatting.DARK_GRAY));
    }

    /**
     * What the tool has learned, sitting between what it has done and what it has to spend — the
     * layers in the order they depend on each other.
     *
     * <p>Always shown, never hidden behind F3+H: a Trait is the payoff for a long history, and a
     * payoff nobody notices is not a payoff.
     */
    private static void appendTraits(Set<Trait> traits, List<Component> lines) {
        for (Trait trait : traits) {
            lines.add(Component.translatable("trait.memoryechoes." + trait.id())
                    .withStyle(ChatFormatting.GREEN));
        }
    }

    private static void appendEchoes(Echoes echoes, boolean advanced, List<Component> lines) {
        lines.add(Component.translatable("tooltip.memoryechoes.echoes", echoes.available())
                .withStyle(ChatFormatting.LIGHT_PURPLE));

        // How close the next Echo is matters while tuning and almost never while playing, so it
        // rides along with the rest of the debug readout under F3+H.
        if (advanced) {
            lines.add(Component.translatable("tooltip.memoryechoes.echoes_progress",
                            echoes.progress(), Echoes.WEAR_PER_ECHO)
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
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
