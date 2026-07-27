package com.explo.echoes.echoes;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

        Echoes before = Echoes.of(tool).orElse(Echoes.NONE);
        Echoes after = before.afterWear(wear);
        after.saveTo(tool);

        if (after.available() > before.available()) {
            markNewEcho(player);
        }
    }

    /**
     * The instant an Echo finishes forming.
     *
     * <p>Barely there on purpose. This happens every twenty-five points of wear for the rest of a
     * tool's life, so anything a player could describe as a jingle would become noise inside an
     * hour. The intent is that they register it without ever being interrupted by it — a small high
     * note and three motes, gone before they finish the swing.
     */
    private static void markNewEcho(ServerPlayer player) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.22F, 1.9F);

        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.ENCHANT,
                    player.getX(), player.getY() + 1.1, player.getZ(),
                    3, 0.2, 0.2, 0.2, 0.0);
        }
    }
}
