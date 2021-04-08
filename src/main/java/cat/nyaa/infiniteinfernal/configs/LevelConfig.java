package cat.nyaa.infiniteinfernal.configs;

public class LevelConfig extends IdFileConfig {
    @Serializable
    public int level = 1;

    @Serializable
    public String prefix = "";

    @Serializable(name = "attr.amplifier.health")
    public double healthAmplifier = 1.0;
    @Serializable(name = "attr.amplifier.damage")
    public double damageAmplifier = 1.0;
    @Serializable(name = "attr.amplifier.damageResist")
    public double damageResistAmplifier = 1.0;
    @Serializable(name = "attr.exp")
    public int exp = 5;
    @Serializable(name = "attr.aggro")
    public double aggro = 48;

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
