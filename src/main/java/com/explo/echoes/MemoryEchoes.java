package com.explo.echoes;

import org.slf4j.Logger;

import com.explo.echoes.memory.Memory;
import com.mojang.logging.LogUtils;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(MemoryEchoes.MODID)
public class MemoryEchoes {
    public static final String MODID = "memoryechoes";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID);

    /**
     * A tool's permanent history.
     *
     * <p>Absent until the tool first does something worth remembering, so an untouched pickaxe
     * is byte-for-byte a vanilla pickaxe. The component is persistent, which also means a
     * storied tool can be handed out with {@code /give ...[memoryechoes:memory={...}]} — the
     * only practical way to seed the demo world, since Memory does not accrue in creative mode.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Memory>> MEMORY =
            COMPONENTS.registerComponentType("memory", builder -> builder.persistent(Memory.CODEC));

    public MemoryEchoes(IEventBus modEventBus) {
        COMPONENTS.register(modEventBus);
    }
}
