package cat.nyaa.infiniteinfernal.configs;

import cat.nyaa.nyaacore.configuration.ISerializable;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class MobConfig extends IdFileConfig {
    @Serializable
    public String name = "A custom mob";
    @Serializable
    public EntityType type = EntityType.ZOMBIE;
    @Serializable
    public List<String> abilities = new LinkedList<>();
    @Serializable(name = "nbttags")
    public String nbtTags = "";
    @Serializable
    public MobSpawnConfig spawn = new MobSpawnConfig();
    @Serializable
    public MobLootConfig loot = new MobLootConfig();

    public MobConfig(int id) {
        super(id);
    }

    public static class MobSpawnConfig implements ISerializable {
        @Serializable
        public boolean autoSpawn = true;
        @Serializable
        public int weight = 100;
        @Serializable
        List<String> levels = new ArrayList<>();
        @Serializable
        List<String> worlds = new ArrayList<>();
        @Serializable
        List<String> biomes = new ArrayList<>();
    }

    public static class MobLootConfig implements ISerializable{
        @Serializable
        public boolean vanilla = false;
        @Serializable
        public boolean imLoot = true;
        @Serializable
        public SpecialConfig special = new SpecialConfig();

        public static class SpecialConfig implements ISerializable{
            @Serializable
            public double chance = 20d;
            @Serializable
            public List<String> list = new ArrayList<>();
        }
    }

    @Override
    public String getPrefix() {
        return "mob";
    }
}
