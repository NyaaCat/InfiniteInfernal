package cat.nyaa.infiniteinfernal.configs;

import cat.nyaa.nyaacore.configuration.ISerializable;

public class RandomLootChestConfig implements ISerializable {
    @Serializable
    public boolean enabled = true;

    @Serializable(name = "items.min")
    public int minItems = 1;

    @Serializable(name = "items.max")
    public int maxItems = 4;

    @Serializable(name = "reset.minHours")
    public int minResetHours = 1;

    @Serializable(name = "reset.maxHours")
    public int maxResetHours = 4;

    @Serializable(name = "scan.intervalTicks")
    public int scanIntervalTicks = 200;

    @Serializable(name = "scan.chunksPerRun")
    public int scanChunksPerRun = 2;

    @Serializable(name = "scan.maxContainersPerChunk")
    public int maxContainersPerChunk = 32;
}
