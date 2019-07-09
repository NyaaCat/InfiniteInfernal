package cat.nyaa.infiniteinfernal.configs;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.nyaacore.configuration.ISerializable;

import java.util.ArrayList;
import java.util.List;

public class WorldConfig implements ISerializable {
    private InfPlugin plugin;

    public WorldConfig(InfPlugin plugin){
        this.plugin = plugin;
    }

    @Serializable(name = "disable-natural-spawning")
    public boolean disablenaturalSpawning = true;

    @Serializable(name = "max-mob-per-player")
    public int maxMobPerPlayer = 10;

    @Serializable(name = "max-mob-in-world")
    public int maxMobInWorld = 240;

    @Serializable(name = "spawn-range-min")
    public int spawnRangeMin = 60;

    @Serializable(name = "spawn-range-max")
    public int spawnRangeMax = 120;

    @Serializable(name = "mob-tick-interval")
    public int mobTickInterval = 60;

    @Serializable
    public AggroConfig aggro = new AggroConfig();

    @Serializable
    public LootingConfig looting = new LootingConfig();

    @Serializable(name = "friendlyfire")
    public FriendlyFireConfig friendlyFireConfig = new FriendlyFireConfig();

    @Serializable(name = "broadcast")
    public BroadcastConfig broadcastConfig = new BroadcastConfig();

    public static class AggroConfig implements ISerializable{
        @Serializable
        public int min =20;
        @Serializable
        public int max = 128;
        @Serializable
        public String dec = "INVISIBILITY:2";
        @Serializable
        public String inc = "UNLUCK:2";
    }

    public static class LootingConfig implements ISerializable{
        @Serializable
        public int global = 70;
        @Serializable
        public LootingModifiers overall = new LootingModifiers();
        @Serializable
        public LootingModifiers dynamic = new LootingModifiers();

        public static class LootingModifiers implements ISerializable{
            @Serializable
            public LootingModifier inc = new LootingModifier();
            @Serializable
            public LootingModifier dec = new LootingModifier();

            public static class LootingModifier{
                @Serializable
                public List<String> enchant = new ArrayList<>();
                @Serializable
                public List<String> effect = new ArrayList<>();
            }
        }
    }

    public static class FriendlyFireConfig implements ISerializable{
        @Serializable
        public boolean enable = true;
        @Serializable
        public String effect = "UNLUCK:4:600";
    }

    public static class BroadcastConfig implements ISerializable{
        @Serializable(name = "default")
        public BroadcastMode defaultMode = BroadcastMode.NEAR;
        @Serializable
        public int range = 160;
    }

}
