package com.explo.echoes.traits;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import com.explo.echoes.memory.Affinity;
import com.explo.echoes.memory.Memory;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/**
 * A tool that has spent enough of its life on one kind of work starts collecting that work itself:
 * the drops go straight to the player instead of scattering on the ground.
 *
 * <p>This is the mod's first Trait, and it was chosen for what it deliberately is <em>not</em>. It
 * is not faster, not luckier, not more durable; it competes with no enchantment and sits on no
 * progression axis. It is a convenience a tool earns by doing one thing for a long time, and it is
 * legible the instant a player sees it — which matters more than magnitude, because the thing they
 * should remember is the behaviour, not a number.
 *
 * <p>Only the drops of the affinity the tool is drawn to are gathered. An ore-drawn pickaxe does
 * not tidy up the player's cobblestone.
 */
@EventBusSubscriber
public final class AffinityGathering {

    private AffinityGathering() {}

    /**
     * Runs on the same event that records Memory, so on the block that crosses a threshold the
     * Trait may or may not have applied yet depending on listener order. One block either way is
     * beneath notice against thresholds in the hundreds, and forcing an ordering between two
     * independent layers would cost more than it buys.
     */
    @SubscribeEvent
    static void onBlockHarvested(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer player)) {
            return;
        }

        Optional<Affinity> affinity = Affinity.of(event.getState());
        if (affinity.isEmpty() || !isDrawnTo(player.getMainHandItem(), affinity.get())) {
            return;
        }

        gather(player, event.getDrops());
    }

    private static boolean isDrawnTo(ItemStack tool, Affinity affinity) {
        Optional<Memory> memory = Memory.of(tool);
        if (memory.isEmpty()) {
            return false;
        }

        for (Trait trait : Trait.derivedFrom(memory.get())) {
            if (trait.affinity() == affinity) {
                return true;
            }
        }
        return false;
    }

    /**
     * Moves what fits into the player's inventory and leaves the rest to drop normally.
     *
     * <p>{@code Inventory.add} consumes the stack as it goes, so a partially-accepted drop keeps
     * its remainder and still falls to the ground — a full inventory quietly degrades to vanilla
     * behaviour rather than eating anything.
     */
    private static void gather(ServerPlayer player, List<ItemEntity> drops) {
        Iterator<ItemEntity> remaining = drops.iterator();
        while (remaining.hasNext()) {
            ItemStack stack = remaining.next().getItem();
            player.getInventory().add(stack);
            if (stack.isEmpty()) {
                remaining.remove();
            }
        }
    }
}
