package cat.nyaa.infiniteinfernal.configs;

public class LevelConfig extends IdFileConfig {
    @Serializable
    public int level = 1;

    @Serializable
    public String prefix = "";

    @Serializable(name = "attr.health")
    public double health = 50d;
    @Serializable(name = "attr.damage")
    public double damage = 2d;
    @Serializable(name = "attr.damageResist")
    public double damageResist = 0;
    @Serializable(name = "attr.exp")
    public int exp = 5;
    @Serializable(name = "attr.aggro")
    public double aggro = 48;

    @Serializable(name = "spawn.from")
    public int spawnFrom = 100;
    @Serializable(name = "spawn.to")
    public int spawnTo = 2000;
    @Serializable(name = "spawn.weight")
    public int spawnWeight = 1;

    public LevelConfig(int id) {
        super(id);
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
