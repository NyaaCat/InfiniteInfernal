package cat.nyaa.infiniteinfernal.configs;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.utils.CorrectionParser;
import cat.nyaa.infiniteinfernal.utils.ICorrector;
import cat.nyaa.nyaacore.configuration.ISerializable;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WorldConfig implements ISerializable {

    public WorldConfig() {
        trueDamage = new LinkedHashMap<>();
        initDefault();
    }

    @Serializable
    public boolean enabled = true;

    @Serializable(name = "disable-natural-spawning")
    public boolean disableNaturalSpawning = true;

    @Serializable(name = "max-mob-per-player")
    public int maxMobPerPlayer = 10;

    @Serializable(name = "max-mob-in-world")
    public int maxMobInWorld = 240;

    @Serializable(name = "spawn-range-min")
    public int spawnRangeMin = 60;

    @Serializable(name = "spawn-range-max")
    public int spawnRangeMax = 120;

    @Serializable(name = "spawn-interval")
    public int mobSpawnInteval = 20;

    @Serializable(name = "mob-active-interval")
    public int mobTickInterval = 60;

    @Serializable
    public AggroConfig aggro = new AggroConfig();

    @Serializable
    public LootingConfig looting = new LootingConfig();

    @Serializable(name = "friendlyfire")
    public FriendlyFireConfig friendlyFireConfig = new FriendlyFireConfig();

    @Serializable(name = "broadcast")
    public BroadcastConfig broadcastConfig = new BroadcastConfig();

    @Serializable
    public boolean enableTrueDamage = true;

    @Serializable
    public Map<String, Double> trueDamage;

    @Serializable(name = "spawn.light.sky.min")
    public int minSkyLight = 0;

    @Serializable(name = "spawn.light.sky.max")
    public int maxSkyLight = 0xf;

    @Serializable(name = "spawn.light.block.min")
    public int minBlockLight = 0;

    @Serializable(name = "spawn.light.block.max")
    public int maxBlockLight = 0xf;

    @Serializable(name = "spawn.light.total.min")
    public int minLight = 0;

    @Serializable(name = "spawn.light.total.max")
    public int maxLight = 0xf;


    private void initDefault() {
        trueDamage.put(EntityDamageEvent.DamageCause.WITHER.name().toLowerCase(), 1d);
        trueDamage.put(EntityDamageEvent.DamageCause.POISON.name().toLowerCase(), 1d);
        trueDamage.put(EntityDamageEvent.DamageCause.FIRE_TICK.name().toLowerCase(), 1d);
        trueDamage.put(EntityDamageEvent.DamageCause.FIRE.name().toLowerCase(), 2d);
        trueDamage.put(EntityDamageEvent.DamageCause.LAVA.name().toLowerCase(), 4d);
        trueDamage.put(EntityDamageEvent.DamageCause.LIGHTNING.name().toLowerCase(), 1d);
        trueDamage.put(EntityDamageEvent.DamageCause.FIRE_TICK.name().toLowerCase(), 1d);
        trueDamage.put(EntityDamageEvent.DamageCause.MAGIC.name().toLowerCase(), -1.01d);
        trueDamage.put(EntityDamageEvent.DamageCause.SUFFOCATION.name().toLowerCase(), 1d);
        trueDamage.put(EntityDamageEvent.DamageCause.VOID.name().toLowerCase(), 4d);
        trueDamage.put(EntityDamageEvent.DamageCause.DROWNING.name().toLowerCase(), 2d);
        trueDamage.put(EntityDamageEvent.DamageCause.HOT_FLOOR.name().toLowerCase(), 1d);
        trueDamage.put(EntityDamageEvent.DamageCause.STARVATION.name().toLowerCase(), 1d);
        trueDamage.put(EntityDamageEvent.DamageCause.DRAGON_BREATH.name().toLowerCase(), 1d);
        trueDamage.put(EntityDamageEvent.DamageCause.CONTACT.name().toLowerCase(), 1d);
    }

    @Serializable
    public double despawnRange = 128;

    public double getTruedamage(String type) {
        return trueDamage.getOrDefault(type, 0d);
    }

    public boolean isTrueDamageEnabled() {
        return enableTrueDamage;
    }

    public static class AggroConfig implements ISerializable {
        @Serializable
        public RangeConfig range = new RangeConfig();
        @Serializable
        public int base = 10;
        @Serializable
        String dec = "effect:INVISIBILITY:2";
        @Serializable
        String inc = "attribute:GENERIC_LUCK:-2";

        private String decWatcher = dec;
        private String incWatcher = inc;
        ICorrector incCorrector;
        ICorrector decCorrector;

        public ICorrector getInc() {
            if (!incWatcher.equals(inc) || incCorrector == null) {
                incCorrector = CorrectionParser.parseStr(inc);
            }
            return incCorrector;
        }

        public ICorrector getDec() {
            if (!decWatcher.equals(dec) || decCorrector == null) {
                decCorrector = CorrectionParser.parseStr(dec);
            }
            return decCorrector;
        }

        public static class RangeConfig implements ISerializable {
            @Serializable
            public int min = 20;
            @Serializable
            public int max = 128;
        }
    }

    public static class LootingConfig implements ISerializable {
        @Serializable
        public int global = 70;
        @Serializable
        public LootingModifiers overall = new LootingModifiers();
        @Serializable
        public LootingModifiers dynamic = new LootingModifiers();

        public static class LootingModifiers implements ISerializable {
            @Serializable
            public List<String> inc = new ArrayList<>();
            @Serializable
            public List<String> dec = new ArrayList<>();
            @Serializable
            public double max = 100;
        }
    }

    public static class FriendlyFireConfig implements ISerializable {
        @Serializable
        public boolean enable = true;
        @Serializable
        public String effect = "UNLUCK:4:600";
    }

    public static class BroadcastConfig implements ISerializable {
        @Serializable(name = "default")
        public BroadcastMode defaultMode = BroadcastMode.NEARBY;
        @Serializable
        public int range = 160;
    }

}
