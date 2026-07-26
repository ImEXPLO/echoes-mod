package com.explo.echoes.memory;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/**
 * Turns mining into Memory.
 *
 * <p>This is the only thing that writes mining history, and it deliberately writes it for one
 * narrow case: a player harvested a block with the right tool for the job. That gate is what
 * makes affinities honest — an axe is remembered for wood because an axe is what fells wood, and
 * a pickaxe cannot claim a relationship with soil by scraping at it.
 */
@EventBusSubscriber
public final class MiningMemory {

    private MiningMemory() {}

    /**
     * Fires once per harvested block, on the server, after the tool has already taken its damage.
     *
     * <p>Nothing fires in creative mode, where blocks are destroyed without ever being harvested.
     * A tool therefore cannot accrue Memory in creative — storied tools for the demo world have
     * to be handed out with the component already on them.
     */
    @SubscribeEvent
    static void onBlockHarvested(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer player)) {
            return;
        }

        // The event carries a copy of the tool taken *before* it was damaged, so writing to
        // event.getTool() would be silently discarded. The stack still in the player's hand is
        // the one that persists — and it is empty precisely when this block was the one that
        // finally broke the tool, which is when there is no longer anything to remember.
        ItemStack tool = player.getMainHandItem();
        if (tool.isEmpty() || !tool.isDamageableItem() || !tool.isCorrectToolForDrops(event.getState())) {
            return;
        }

        long gameTime = event.getLevel().getGameTime();
        Memory.of(tool)
                .orElseGet(() -> Memory.awakening(gameTime))
                .afterMining(Affinity.of(event.getState()))
                .saveTo(tool);
    }
}
