package com.explo.echoes.echoes;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
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
        recall(player, repair.echoesSpent());

        event.setCancellationResult(InteractionResult.SUCCESS_SERVER);
        event.setCanceled(true);
    }

    /**
     * The moment the tool's past answers.
     *
     * <p>Everything here is trying to make a subtraction feel like a recollection. The particles
     * gather at the player's hands rather than scattering outward, because something is being drawn
     * back in rather than given off. The pitch falls as more Echoes are spent, so a deep recall
     * sounds older than a shallow one — the tool has reached further back. And a line of text says
     * what no counter can.
     *
     * <p>Understated throughout. This happens every time a player mends a tool, and anything
     * louder would wear out long before the mod does.
     */
    private static void recall(ServerPlayer player, int echoesSpent) {
        float depth = Math.min(echoesSpent, 8) / 8.0F;

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6F, 1.3F - depth * 0.4F);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.25F + depth * 0.2F, 1.1F);

        if (player.level() instanceof ServerLevel level) {
            // Zero speed: the motes hang where they appear instead of flying off, which reads as
            // something surfacing rather than something escaping.
            level.sendParticles(ParticleTypes.ENCHANT,
                    player.getX(), player.getY() + 1.1, player.getZ(),
                    8 + Math.min(echoesSpent, 6) * 2, 0.35, 0.3, 0.35, 0.0);
            level.sendParticles(ParticleTypes.END_ROD,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    3, 0.25, 0.2, 0.25, 0.005);
        }

        player.sendOverlayMessage(Recollection.random(player.getRandom())
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
