package com.explo.echoes.merge;

import java.util.List;
import java.util.Optional;

import com.explo.echoes.memory.Memory;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.entity.player.AnvilCraftEvent;

/**
 * Merge, as the player meets it: put the new tool on the left, the old one on the right.
 *
 * <p>The anvil was chosen over a custom station because Merge adds to it rather than replacing
 * anything. Where vanilla already produces a result — two tools of the same material — that result
 * is kept exactly as it was and the inherited history is stamped onto it, so repairing and
 * combining enchantments behave as they always have. Where vanilla refuses, which is precisely the
 * iron-to-diamond upgrade this feature exists for, Merge supplies the successor itself.
 *
 * <p>That is also what makes Merge enchantment-agnostic for free: enchantments remain entirely the
 * anvil's business. Merge moves history and nothing else.
 *
 * <p>Same tool class only. A sword that remembers mining twelve thousand blocks is nonsense, and
 * the check is tag-based so a modded pickaxe counts as a pickaxe without this mod knowing it
 * exists.
 */
@EventBusSubscriber
public final class MergeOnAnvil {

    /**
     * Levels charged for the inheritance itself, on top of whatever the anvil already wanted.
     *
     * <p>A merge should be a decision, not a reflex.
     */
    private static final int LEVEL_COST = 5;

    private static final List<TagKey<Item>> TOOL_CLASSES =
            List.of(ItemTags.PICKAXES, ItemTags.AXES, ItemTags.SHOVELS, ItemTags.HOES, ItemTags.SWORDS);

    private MergeOnAnvil() {}

    /**
     * Builds the preview the player sees before committing.
     *
     * <p>The preview matters as much as the result here: the output slot renders an ordinary
     * tooltip, so the successor arrives already saying whose legacy it carries and how deep the
     * line runs. The player reads the story before paying for it.
     */
    @SubscribeEvent
    static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack successor = event.getLeft();
        ItemStack predecessor = event.getRight();

        Optional<Memory> ancestry = Memory.of(predecessor);
        if (ancestry.isEmpty() || !isSameToolClass(successor, predecessor)) {
            return;
        }

        ItemStack inheritor = event.getOutput();
        if (inheritor.isEmpty()) {
            // Vanilla has no answer for a cross-material pair, which is exactly the upgrade this
            // feature exists to rescue. The successor passes through unchanged apart from its
            // new history -- and its name, since carrying "Old Reliable" forward is the player's
            // to decide and the anvil's rename field is already right there.
            inheritor = successor.copy();
            applyName(inheritor, event.getName());
            event.setXpCost(LEVEL_COST);
        } else {
            event.setXpCost(event.getXpCost() + LEVEL_COST);
        }

        Merge.inherit(ancestry.get(), Memory.of(inheritor), predecessor.getHoverName()).saveTo(inheritor);

        event.setMaterialCost(1);
        event.setOutput(inheritor);
    }

    /**
     * The moment itself, once the successor is actually in the player's hands.
     *
     * <p>Gated on the same conditions as the preview rather than on the output simply carrying a
     * lineage — otherwise every later repair of an inherited tool would replay a ceremony that
     * belongs to one moment only.
     */
    @SubscribeEvent
    static void onAnvilCraft(AnvilCraftEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (Memory.of(event.getRight()).isEmpty() || !isSameToolClass(event.getLeft(), event.getRight())) {
            return;
        }

        Memory.of(event.getOutput())
                .flatMap(Memory::inheritedFrom)
                .ifPresent(predecessor -> commemorate(player, predecessor));
    }

    private static void commemorate(ServerPlayer player, Component predecessor) {
        player.sendOverlayMessage(Component.translatable("merge.memoryechoes.lives_on", predecessor)
                .withStyle(ChatFormatting.GRAY));

        // Restrained on purpose: this should read as something quietly passing between two tools,
        // not as a spell being cast.
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8F, 1.0F);

        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.ENCHANT,
                    player.getX(), player.getY() + 1.2, player.getZ(),
                    24, 0.4, 0.5, 0.4, 0.05);
        }
    }

    private static void applyName(ItemStack inheritor, String name) {
        if (name == null) {
            return;
        }
        if (name.isBlank()) {
            inheritor.remove(DataComponents.CUSTOM_NAME);
        } else {
            inheritor.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        }
    }

    private static boolean isSameToolClass(ItemStack successor, ItemStack predecessor) {
        for (TagKey<Item> toolClass : TOOL_CLASSES) {
            if (successor.is(toolClass) && predecessor.is(toolClass)) {
                return true;
            }
        }
        return false;
    }
}
