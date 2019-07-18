package cat.nyaa.infiniteinfernal.configs;

import cat.nyaa.nyaacore.configuration.ISerializable;

public class LevelConfig extends IdFileConfig {
    @Serializable
    public int level = 1;

    @Serializable
    public String prefix = "";

    @Serializable
    public LevelAttr attr = new LevelAttr();

    @Serializable
    public LevelSpawnConfig spawnConfig = new LevelSpawnConfig();

    public LevelConfig(int id) {
        super(id);
    }

    public static class LevelAttr implements ISerializable {
        @Serializable
        public double health = 50d;
        @Serializable
        public double damage = 2d;
        @Serializable
        public double damageResist = 0;
        @Serializable
        public int exp = 5;
        @Serializable
        public double aggro = 48;
    }

    public static class LevelSpawnConfig implements ISerializable{
        @Serializable
        public int from = 100;
        @Serializable
        public int to = 2000;
        @Serializable
        public int weight = 1;
    }

    @Override
    public String getPrefix() {
        return "level";
    }

    @Override
    public String getDir() {
        return "levels";
    }

}
