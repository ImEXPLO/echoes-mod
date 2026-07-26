package com.explo.echoes.echoes;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Lets a player call on a tool's Echoes to mend it: sneak and right-click, holding the tool.
 *
 * <p>Deliberately bound to right-clicking <em>air</em> rather than a block. Sneaking does not stop
 * items from acting on blocks — an axe still strips a log, a shovel still cuts a path — so
 * intercepting that would break vanilla behaviour players rely on. Aiming at nothing is
 * unambiguous, costs no keybind, and cannot collide with anything.
 *
 * <p>Nothing happens silently: with no Echoes, or on an undamaged tool, the event is left alone so
 * the tool behaves exactly as vanilla.
 */
@EventBusSubscriber
public final class RepairOnUse {

    private RepairOnUse() {}

    @SubscribeEvent
    static void onRightClick(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isShiftKeyDown()) {
            return;
        }

        ItemStack tool = event.getItemStack();
        if (!tool.isDamageableItem() || !tool.isDamaged()) {
            return;
        }

        Echoes echoes = Echoes.of(tool).orElse(Echoes.NONE);
        Repair repair = Repair.of(echoes.available(), tool.getDamageValue());
        if (repair.echoesSpent() == 0) {
            return;
        }

        tool.setDamageValue(tool.getDamageValue() - repair.durabilityRestored());
        echoes.afterSpending(repair.echoesSpent()).saveTo(tool);

        // Restrained on purpose: a chime, not a spectacle. The tooltip already shows what changed.
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.7F, 1.2F);

        event.setCancellationResult(InteractionResult.SUCCESS_SERVER);
        event.setCanceled(true);
    }
}
