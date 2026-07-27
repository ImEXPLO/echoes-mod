package com.explo.echoes;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * A journal left behind by someone who studied Echoes before the player did, handed over once, the
 * first time they join a world.
 *
 * <p>Everything else in this mod is discoverable by looking at a tool. The parts that are not — that
 * Echoes can be recalled at all, and that a history can be handed to a successor — are verbs, and
 * verbs cannot be discovered by reading a tooltip. Something has to say them out loud.
 *
 * <p>It is written as a found object rather than a manual. A player who is told "sneak and
 * right-click to consume Echoes and restore durability" has read documentation; a player who reads
 * that someone before them learned to ask a tool for what it remembers has found a forgotten craft.
 * Both end up knowing the same thing. Only one of them enjoyed it.
 *
 * <p>No new item is registered — it is a vanilla written book, so it stacks, sits in a lectern, and
 * can be thrown away like any other. Deliberately: an onboarding aid should not become a fixture.
 */
@EventBusSubscriber
public final class WornJournal {

    /** Marks a player as having received the journal, under the tag that survives death. */
    private static final String RECEIVED = "memoryechoes:journal_received";

    private static final int PAGES = 6;

    private WornJournal() {}

    @SubscribeEvent
    static void onFirstJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        CompoundTag persisted = persistentData(player);
        if (persisted.getBooleanOr(RECEIVED, false)) {
            return;
        }
        persisted.putBoolean(RECEIVED, true);

        ItemStack journal = create();
        if (!player.getInventory().add(journal)) {
            player.drop(journal, false);
        }
    }

    private static ItemStack create() {
        ItemStack journal = new ItemStack(Items.WRITTEN_BOOK);

        List<Filterable<Component>> pages = new ArrayList<>(PAGES);
        for (int page = 1; page <= PAGES; page++) {
            pages.add(Filterable.passThrough(Component.translatable("journal.memoryechoes.page." + page)));
        }

        // Title and author are plain strings by vanilla's format, so unlike the pages they cannot
        // be translated per-client and resolve against the server's language.
        journal.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
                Filterable.passThrough(Component.translatable("journal.memoryechoes.title").getString()),
                Component.translatable("journal.memoryechoes.author").getString(),
                0,
                pages,
                true));

        return journal;
    }

    /**
     * The corner of a player's data that survives dying.
     *
     * <p>Without this the journal would be handed out again after every death, which would turn a
     * quiet gift into a nuisance.
     */
    private static CompoundTag persistentData(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        CompoundTag persisted = data.getCompoundOrEmpty(Player.PERSISTED_NBT_TAG);
        data.put(Player.PERSISTED_NBT_TAG, persisted);
        return persisted;
    }
}
