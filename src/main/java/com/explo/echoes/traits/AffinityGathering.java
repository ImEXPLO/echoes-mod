package com.explo.echoes.traits;

import java.util.List;
import java.util.Optional;

import com.explo.echoes.memory.Affinity;
import com.explo.echoes.memory.Memory;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
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
     * Brings the drops to the player and lets vanilla do the picking up.
     *
     * <p>Moving the stacks straight into the inventory would be simpler and it is what this did
     * first, but it skipped everything that makes a pickup <em>feel</em> like a pickup: the item's
     * flight toward the player, the pickup sound, the statistic, and the events other mods listen
     * to. The Trait looked, from the player's chair, like nothing happening at all.
     *
     * <p>So the drops are repositioned onto the player and cleared to be taken immediately, and
     * vanilla collects them a tick later exactly as it collects anything else. Same convenience,
     * none of the feedback thrown away — and a full inventory still degrades to items on the floor
     * without any special handling.
     */
    private static void gather(ServerPlayer player, List<ItemEntity> drops) {
        for (ItemEntity drop : drops) {
            drop.setPos(player.getX(), player.getY() + 0.5, player.getZ());
            drop.setDeltaMovement(Vec3.ZERO);
            drop.setNoPickUpDelay();
            drop.setTarget(player.getUUID());
        }
    }
}
