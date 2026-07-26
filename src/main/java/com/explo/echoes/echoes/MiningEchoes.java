package com.explo.echoes.echoes;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/**
 * Forms Echoes out of the durability a tool spends mining.
 *
 * <p>This deliberately duplicates the qualifying checks in {@code MiningMemory} rather than
 * sharing them. The two layers answer different questions about the same moment — Memory asks
 * "was this worth remembering?", Echoes asks "did it cost the tool anything?" — and keeping them
 * as independent listeners means neither package imports the other. Four similar lines are a
 * cheaper price than a coupling between the layers the whole design rests on separating.
 */
@EventBusSubscriber
public final class MiningEchoes {

    private MiningEchoes() {}

    @SubscribeEvent
    static void onBlockHarvested(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack tool = player.getMainHandItem();
        if (tool.isEmpty() || !tool.isDamageableItem() || !tool.isCorrectToolForDrops(event.getState())) {
            return;
        }

        // The event's copy of the tool was taken before it was damaged, so the difference between
        // it and the stack still in hand is exactly what this block cost. Anything else reaching
        // us with a mismatched tool -- another mod dropping resources on the player's behalf, say
        // -- would make that subtraction meaningless and hand out a whole tool's worth of Echoes
        // at once, so the identity check is what protects the economy, not just tidiness.
        ItemStack beforeMining = event.getTool();
        if (!ItemStack.isSameItem(beforeMining, tool)) {
            return;
        }

        int wear = tool.getDamageValue() - beforeMining.getDamageValue();
        if (wear <= 0) {
            return;
        }

        Echoes.of(tool).orElse(Echoes.NONE).afterWear(wear).saveTo(tool);
    }
}
