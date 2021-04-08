package cat.nyaa.infiniteinfernal.configs;

import cat.nyaa.infiniteinfernal.utils.Weightable;
import cat.nyaa.nyaacore.configuration.ISerializable;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

public class MobConfig extends NamedFileConfig implements Weightable {
    @Serializable
    public String name = "A custom mob";
    @Serializable
    public EntityType type = EntityType.ZOMBIE;
    @Serializable
    public List<String> abilities = new LinkedList<>();
    @Serializable(name = "nbttags")
    public String nbtTags = "";
    @Serializable
    public double healthOverride = -1;
    @Serializable
    public boolean enableDynamicHealth = false;
    @Serializable
    public String dynamicHealthExpression = "";

    @Serializable(name = "attr.health")
    private double health = 20;

    @Serializable(name = "attr.attackDamage")
    private double attackDamage = 20;

    @Serializable(name = "attr.damageResist")
    private double damageResist = 20;

    @Serializable(name = "attr.movementSpeed")
    private double movementSpeed = 20;

    @Serializable(name = "spawn.levels")
    private List<String> spawnLevels = new ArrayList<>();

    public double getHealth() {
        return health;
    }

    public void setHealth(double health) {
        this.health = health;
    }

    public double getAttackDamage() {
        return attackDamage;
    }

    public void setAttackDamage(double attackDamage) {
        this.attackDamage = attackDamage;
    }

    public double getDamageResist() {
        return damageResist;
    }

    public void setDamageResist(double damageResist) {
        this.damageResist = damageResist;
    }

    public double getMovementSpeed() {
        return movementSpeed;
    }

    public void setMovementSpeed(double movementSpeed) {
        this.movementSpeed = movementSpeed;
    }

    public List<String> getSpawnLevels() {
        return spawnLevels;
    }

    public void setSpawnLevels(List<String> spawnLevels) {
        this.spawnLevels = spawnLevels;
    }

    @Serializable
    public MobSpawnConfig spawn = new MobSpawnConfig();
    @Serializable
    public MobLootConfig loot = new MobLootConfig();

    public MobConfig(String id) {
        super(id);
    }

    public static List<Integer> parseLevels(List<String> levels) {
        List<Integer> result = new ArrayList<>();
        if (!levels.isEmpty()) {
            levels.forEach(s -> {
                try {
                    String replace = s.replaceAll(" ", "");
                    if (replace.contains("-")) {
                        String[] split = replace.split("-");
                        if (split.length != 2) {
                            throw new IllegalArgumentException();
                        }
                        int to = Integer.parseInt(split[1]);
                        int from = Integer.parseInt(split[0]);
                        for (int i = from; i <= to; i++) {
                            result.add(i);
                        }
                    } else {
                        result.add(Integer.parseInt(s));
                    }
                }catch (IllegalArgumentException ex){
                    Bukkit.getLogger().log(Level.WARNING, "invalid level config "+s);
                }
            });
        }
        return result;
    }

    @Override
    public int getWeight() {
        return spawn.getWeight();
    }

    public static class MobSpawnConfig implements ISerializable, Weightable {
        @Serializable
        public boolean autoSpawn = true;
        @Serializable
        public int weight = 100;
        @Serializable
        public List<String> levels = new ArrayList<>();
        @Serializable
        public List<String> worlds = new ArrayList<>();
        @Serializable
        public List<String> biomes = new ArrayList<>();

        @Override
        public int getWeight() {
            return weight;
        }
    }

    public static class MobLootConfig implements ISerializable {
        @Serializable
        public boolean vanilla = false;
        @Serializable
        public boolean imLoot = true;
        @Serializable
        public SpecialConfig special = new SpecialConfig();
        @Serializable
        public int expOverride = -1;

        public static class SpecialConfig implements ISerializable {
            @Serializable
            public double chance = 20d;
            @Serializable
            public List<String> list = new ArrayList<>();
        }
    }

    @Override
    public String getPrefix() {
        return "";
    }

    @Override
    protected String getFileDirName() {
        return "mobs";
    }

}
