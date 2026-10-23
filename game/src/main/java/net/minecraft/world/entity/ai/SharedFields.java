package net.minecraft.world.entity.ai;

import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemorySlot;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Shared fields for Lithium client-side optimizations.
 * Provides dummy brain and maximum biome particle chance tracking.
 */
public class SharedFields {
    // Using AtomicInteger as atomic float replacement for maximum biome particle chance
    public static final AtomicInteger MAXIMUM_BIOME_PARTICLE_CHANCE = new AtomicInteger(Float.floatToIntBits(0.0F));

    // Dummy brain for client-side entities to avoid unnecessary brain creation
    public static final AtomicReference<Brain<?>> DUMMY_BRAIN = new AtomicReference<>(null);
    public static final AtomicReference<MemorySlot<?>> DUMMY_SLOT = new AtomicReference<>(null);

    static {
        // Initialize dummy brain and slot
        Brain<?> dummyBrain = new Brain<>();
        DUMMY_BRAIN.set(dummyBrain);
        DUMMY_SLOT.set(MemorySlot.create());
    }

    public static Brain<?> getDummyBrain() {
        return DUMMY_BRAIN.get();
    }

    public static MemorySlot<?> getDummySlot() {
        return DUMMY_SLOT.get();
    }
}