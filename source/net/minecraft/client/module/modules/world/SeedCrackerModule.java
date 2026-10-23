package net.minecraft.client.module.modules.world;

import net.minecraft.client.Minecraft;
import net.minecraft.client.module.Module;
import net.minecraft.client.module.ModuleCategory;
import net.minecraft.client.value.BooleanValue;
import net.minecraft.client.value.EnumValue;
import net.minecraft.client.value.FloatValue;
import net.minecraft.client.value.IntegerValue;
import net.minecraft.client.value.StringValue;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SeedCrackerModule extends Module {
    public enum CrackMode {
        Structure,
        Ore,
        Biome,
        Slime,
        Combined
    }

    private final EnumValue<CrackMode> mode = registerValue(new EnumValue<>("Mode", "Cracking method", CrackMode.Combined, List.of(CrackMode.values())));
    private final IntegerValue threads = registerValue(new IntegerValue("Threads", "Worker threads", 4, 1, 16));
    private final IntegerValue maxSeeds = registerValue(new IntegerValue("MaxSeeds", "Max seeds to test", 1000000, 1000, 100000000));
    private final FloatValue timeout = registerValue(new FloatValue("Timeout", "Timeout per attempt (seconds)", 30.0f, 5.0f, 300.0f, 5.0f));
    private final BooleanValue autoApply = registerValue(new BooleanValue("AutoApply", "Auto-apply found seed", true));
    private final BooleanValue notifyChat = registerValue(new BooleanValue("NotifyChat", "Chat notification on crack", true));
    private final StringValue knownSeed = registerValue(new StringValue("KnownSeed", "Partial seed hint (optional)", "", 20));

    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private boolean cracking = false;
    private long startTime = 0;
    private long seedsTested = 0;
    private long foundSeed = 0;

    public SeedCrackerModule() {
        super("SeedCracker", "Cracks world seed from structures/ores", ModuleCategory.WORLD);
    }

    @Override
    public void onEnable() {
        if (cracking) return;
        startCracking();
    }

    @Override
    public void onDisable() {
        cracking = false;
        executor.shutdownNow();
    }

    @Override
    public void onTick() {
        if (cracking && foundSeed != 0) {
            cracking = false;
            if (autoApply.getValue()) {
                applySeed(foundSeed);
            }
            if (notifyChat.getValue()) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    mc.player.sendSystemMessage(Component.literal("§aSeed cracked: " + foundSeed + " (§eseeds tested: " + seedsTested + "§a)"));
                }
            }
            foundSeed = 0;
            seedsTested = 0;
        }
    }

    private void startCracking() {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return;

        cracking = true;
        startTime = System.currentTimeMillis();
        seedsTested = 0;
        foundSeed = 0;

        List<Long> candidates = generateCandidates(level);

        CompletableFuture.runAsync(() -> {
            for (long seed : candidates) {
                if (!cracking) break;
                if (System.currentTimeMillis() - startTime > timeout.getValue() * 1000) break;

                if (testSeed(seed, level)) {
                    foundSeed = seed;
                    cracking = false;
                    break;
                }
                seedsTested++;
            }
            if (foundSeed == 0 && notifyChat.getValue()) {
                mc.execute(() -> {
                    if (mc.player != null) {
                        mc.player.sendSystemMessage(Component.literal("§cSeed crack failed. Tested: " + seedsTested + " seeds"));
                    }
                });
            }
        }, executor);
    }

    private List<Long> generateCandidates(Level level) {
        List<Long> candidates = new ArrayList<>();
        long hint = 0;

        if (!knownSeed.getValue().isEmpty()) {
            try {
                hint = Long.parseLong(knownSeed.getValue());
            } catch (NumberFormatException ignored) {}
        }

        if (hint != 0) {
            for (long i = -1000; i <= 1000; i++) {
                candidates.add(hint + i);
            }
        } else {
            switch (mode.getValue()) {
                case Structure -> candidates.addAll(crackFromStructures(level));
                case Ore -> candidates.addAll(crackFromOres(level));
                case Biome -> candidates.addAll(crackFromBiomes(level));
                case Slime -> candidates.addAll(crackFromSlimeChunks(level));
                case Combined -> {
                    candidates.addAll(crackFromStructures(level));
                    candidates.addAll(crackFromOres(level));
                    candidates.addAll(crackFromSlimeChunks(level));
                }
            }
        }

        if (candidates.size() > maxSeeds.getValue()) {
            candidates = candidates.subList(0, maxSeeds.getValue());
        }

        return candidates;
    }

    private List<Long> crackFromStructures(Level level) {
        List<Long> seeds = new ArrayList<>();
        Player player = Minecraft.getInstance().player;
        if (player == null) return seeds;

        ChunkPos playerChunk = new ChunkPos(player.blockPosition());

        for (int cx = playerChunk.x - 8; cx <= playerChunk.x + 8; cx++) {
            for (int cz = playerChunk.z - 8; cz <= playerChunk.z + 8; cz++) {
                ChunkPos chunk = new ChunkPos(cx, cz);

                if (level.getChunk(chunk.x, chunk.z).getStructureStarts().containsKey(
                        net.minecraft.core.registries.Registries.STRUCTURE.getResourceKey(
                            net.minecraft.world.level.levelgen.structure.Structure.VILLAGE).orElseThrow())) {
                    long structureSeed = getStructureSeed(chunk, net.minecraft.world.level.levelgen.structure.Structure.VILLAGE);
                    if (structureSeed != 0) seeds.add(structureSeed);
                }

                if (level.getChunk(chunk.x, chunk.z).getStructureStarts().containsKey(
                        net.minecraft.core.registries.Registries.STRUCTURE.getResourceKey(
                            net.minecraft.world.level.levelgen.structure.Structure.MINESHAFT).orElseThrow())) {
                    long structureSeed = getStructureSeed(chunk, net.minecraft.world.level.levelgen.structure.Structure.MINESHAFT);
                    if (structureSeed != 0) seeds.add(structureSeed);
                }

                if (level.getChunk(chunk.x, chunk.z).getStructureStarts().containsKey(
                        net.minecraft.core.registries.Registries.STRUCTURE.getResourceKey(
                            net.minecraft.world.level.levelgen.structure.Structure.STRONGHOLD).orElseThrow())) {
                    long structureSeed = getStructureSeed(chunk, net.minecraft.world.level.levelgen.structure.Structure.STRONGHOLD);
                    if (structureSeed != 0) seeds.add(structureSeed);
                }
            }
        }

        return seeds;
    }

    private long getStructureSeed(ChunkPos chunk, net.minecraft.world.level.levelgen.structure.Structure structure) {
        return chunk.x * 10387313L ^ chunk.z * 38472913L ^ structure.getSalt();
    }

    private List<Long> crackFromOres(Level level) {
        List<Long> seeds = new ArrayList<>();
        Player player = Minecraft.getInstance().player;
        if (player == null) return seeds;

        BlockPos pos = player.blockPosition();
        int radius = 32;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
                    BlockPos checkPos = pos.offset(x, y - pos.getY(), z);
                    net.minecraft.world.level.block.state.BlockState state = level.getBlockState(checkPos);

                    if (isDiamondOre(state) || isEmeraldOre(state) || isAncientDebris(state)) {
                        int chunkX = checkPos.getX() >> 4;
                        int chunkZ = checkPos.getZ() >> 4;
                        long oreSeed = reverseOreSeed(chunkX, chunkZ, checkPos.getY(), state);
                        if (oreSeed != 0) seeds.add(oreSeed);
                    }
                }
            }
        }

        return seeds;
    }

    private boolean isDiamondOre(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(net.minecraft.world.level.block.Blocks.DIAMOND_ORE) ||
               state.is(net.minecraft.world.level.block.Blocks.DEEPSLATE_DIAMOND_ORE);
    }

    private boolean isEmeraldOre(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(net.minecraft.world.level.block.Blocks.EMERALD_ORE) ||
               state.is(net.minecraft.world.level.block.Blocks.DEEPSLATE_EMERALD_ORE);
    }

    private boolean isAncientDebris(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(net.minecraft.world.level.block.Blocks.ANCIENT_DEBRIS);
    }

    private long reverseOreSeed(int chunkX, int chunkZ, int y, net.minecraft.world.level.block.state.BlockState state) {
        Random rand = new Random();
        long baseSeed = (long) chunkX * 10387313L ^ (long) chunkZ * 38472913L;

        for (long i = 0; i < 10000; i++) {
            long testSeed = baseSeed ^ i;
            rand.setSeed(testSeed);
            if (simulateOreGen(rand, chunkX, chunkZ, y, state)) {
                return testSeed;
            }
        }
        return 0;
    }

    private boolean simulateOreGen(Random rand, int chunkX, int chunkZ, int y, net.minecraft.world.level.block.state.BlockState target) {
        int veinCount = rand.nextInt(8) + 1;
        for (int i = 0; i < veinCount; i++) {
            int genX = rand.nextInt(16);
            int genY = rand.nextInt(level.getMaxBuildHeight() - level.getMinBuildHeight()) + level.getMinBuildHeight();
            int genZ = rand.nextInt(16);
            if (genX == (chunkX & 15) && genZ == (chunkZ & 15) && genY == y) {
                return true;
            }
        }
        return false;
    }

    private List<Long> crackFromBiomes(Level level) {
        List<Long> seeds = new ArrayList<>();
        Player player = Minecraft.getInstance().player;
        if (player == null) return seeds;

        int cx = player.getChunkX();
        int cz = player.getChunkZ();
        net.minecraft.world.level.biome.Biome biome = level.getBiome(player.blockPosition()).value();

        for (long i = 0; i < 10000; i++) {
            if (simulateBiome(i, cx, cz, biome)) {
                seeds.add(i);
            }
        }
        return seeds;
    }

    private boolean simulateBiome(long seed, int cx, int cz, net.minecraft.world.level.biome.Biome target) {
        return false;
    }

    private List<Long> crackFromSlimeChunks(Level level) {
        List<Long> seeds = new ArrayList<>();
        Player player = Minecraft.getInstance().player;
        if (player == null) return seeds;

        int cx = player.getChunkX();
        int cz = player.getChunkZ();

        for (int x = cx - 4; x <= cx + 4; x++) {
            for (int z = cz - 4; z <= cz + 4; z++) {
                if (isSlimeChunk(level, x, z)) {
                    for (long i = 0; i < 10000; i++) {
                        if (isSlimeChunkSeed(i, x, z)) {
                            seeds.add(i);
                        }
                    }
                }
            }
        }
        return seeds;
    }

    private boolean isSlimeChunk(Level level, int cx, int cz) {
        return level.getChunk(cx, cz).getBlockEntities().values().stream()
            .anyMatch(e -> e.getType() == net.minecraft.world.level.block.entity.BlockEntityType.MOB_SPAWNER);
    }

    private boolean isSlimeChunkSeed(long seed, int cx, int cz) {
        Random rand = new Random(seed + (long) cx * cx * 4987142 + (long) cx * 5947611 + (long) cz * cz * 4392871 + (long) cz * 389711 ^ 987234911L);
        return rand.nextInt(10) == 0;
    }

    private boolean testSeed(long seed, Level level) {
        XRayModule xray = ModuleManager.getInstance().getModule(XRayModule.class);
        if (xray != null) {
            xray.cachedSeed = seed;
            xray.seedCached = true;
        }
        return true;
    }

    private void applySeed(long seed) {
        XRayModule xray = ModuleManager.getInstance().getModule(XRayModule.class);
        if (xray != null) {
            xray.cachedSeed = seed;
            xray.seedCached = true;
            xray.useCustomSeed.setValue(true);
            xray.seedInput.setValue(String.valueOf(seed));
        }
    }
}