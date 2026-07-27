package com.explo.echoes.traits;

import java.util.Optional;

import com.explo.echoes.memory.Affinity;
import com.explo.echoes.memory.Memory;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/**
 * Marks the moment a tool earns a Trait.
 *
 * <p>Earning one is the payoff for hours of the same work, and until now it happened in total
 * silence — the tool simply started behaving differently and the player was left to notice on their
 * own. A permanent change to a tool's character deserves to be witnessed.
 *
 * <h2>Finding the moment without storing anything</h2>
 *
 * <p>Traits are derived, so there is no unlock event to hook and nothing on the stack that says
 * "this is new". The crossing is found instead by equality: counts rise one block at a time, so the
 * tick where a tool's tally for an affinity lands exactly on a threshold is the tick it crossed —
 * and it can only ever land there once.
 *
 * <p>That also keeps this listener independent of the one that records Memory. It does not care
 * whether the count it reads already includes the current block; either way the equality holds on
 * exactly one block, and the announcement fires exactly once.
 */
@EventBusSubscriber
public final class TraitAwakening {

    private TraitAwakening() {}

    @SubscribeEvent
    static void onBlockHarvested(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer player)) {
            return;
        }

        // Must match the gate that records Memory. Without it this fires on blocks that never
        // advanced the count -- dig soil with a pickaxe while the tool happens to sit exactly on the
        // earth threshold and the announcement repeats on every single block, forever.
        ItemStack tool = player.getMainHandItem();
        if (tool.isEmpty() || !tool.isDamageableItem() || !tool.isCorrectToolForDrops(event.getState())) {
            return;
        }

        Optional<Affinity> affinity = Affinity.of(event.getState());
        Optional<Memory> memory = Memory.of(tool);
        if (affinity.isEmpty() || memory.isEmpty()) {
            return;
        }

        int worked = memory.get().count(affinity.get());
        for (Trait trait : Trait.values()) {
            if (trait.affinity() == affinity.get() && trait.threshold() == worked) {
                announce(player, trait);
                return;
            }
        }
    }

    /**
     * Quiet, but unmistakably a milestone.
     *
     * <p>Pitched above the recall chime so it reads as something arriving rather than something
     * being spent, and paired with the affinity's name so the player learns <em>what</em> the tool
     * became attached to, not merely that something happened. No screen, no popup — the tooltip
     * holds the detail for whoever wants it.
     */
    private static void announce(ServerPlayer player, Trait trait) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.7F, 1.5F);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.45F, 1.2F);

        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.ENCHANT,
                    player.getX(), player.getY() + 1.2, player.getZ(),
                    16, 0.3, 0.35, 0.3, 0.0);
        }

        Component affinity = Component
                .translatable("affinity.memoryechoes." + trait.affinity().getSerializedName())
                .withStyle(ChatFormatting.AQUA);

        player.sendOverlayMessage(Component.translatable("trait.memoryechoes.learned", affinity)
                .withStyle(ChatFormatting.GRAY));
    }
}
