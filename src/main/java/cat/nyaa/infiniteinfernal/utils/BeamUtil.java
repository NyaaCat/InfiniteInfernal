package cat.nyaa.infiniteinfernal.utils;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.configs.ParticleConfig;
import cat.nyaa.nyaacore.configuration.ISerializable;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BeamUtil {
    private final static Set<Material> transp = Stream.of(Material.values())
            .filter(material -> material.isBlock())
            .filter(material -> !material.isSolid() || !material.isOccluding())
            .collect(Collectors.toSet());

    public static void beam(BeamConfig beamConfig, LivingEntity from, Vector direction) {
        new MovingTask(from, direction, beamConfig).runTask(InfPlugin.plugin);
    }

    public static void beam(BeamConfig beamConfig, LivingEntity from, Vector direction, Function<Double, Vector> function) {
        new MovingTask(from, direction, beamConfig, function).runTask(InfPlugin.plugin);
    }

    private static class MovingTask extends BukkitRunnable {
        private LivingEntity from;
        private Vector direction;
        BeamConfig beamConfig;
        Function<Double, Vector> function;

        MovingTask(LivingEntity from, Vector direction, BeamConfig beamConfig) {
            this.from = from;
            this.direction = direction;
            this.beamConfig = beamConfig;
        }

        public MovingTask(LivingEntity from, Vector direction, BeamConfig beamConfig, Function<Double, Vector> function) {
            this.from = from;
            this.direction = direction;
            this.beamConfig = beamConfig;
            this.function = function;
        }

        private void spawnParticle(LivingEntity from, World world, Location lastLocation, int i) {
            if ((lastLocation.distance(from.getEyeLocation()) < 1)) {
                return;
            }
            world.spawnParticle(beamConfig.particle.type, lastLocation, i, beamConfig.particle.getOffsetX(), beamConfig.particle.getOffsetY(), beamConfig.particle.getOffsetZ(), beamConfig.particle.speed, Utils.parseExtraData(beamConfig.particle.extraData), beamConfig.particle.forced);
        }

        private boolean canHit(Location loc, Entity entity) {
            BoundingBox boundingBox = entity.getBoundingBox();
            BoundingBox particleBox;
            double x = Math.max(beamConfig.particle.getOffsetX(), 0.05);
            double y = Math.max(beamConfig.particle.getOffsetY(), 0.05);
            double z = Math.max(beamConfig.particle.getOffsetZ(), 0.05);
            particleBox = BoundingBox.of(loc, x, y, z);
            return boundingBox.overlaps(particleBox) || particleBox.overlaps(boundingBox);
        }

        private boolean tryHit(LivingEntity from, Location loc, boolean canHitSelf, double damage) {
            double offsetLength = new Vector(beamConfig.particle.getOffsetX(), beamConfig.particle.getOffsetY(), beamConfig.particle.getOffsetZ()).length();
            double length = Double.isNaN(offsetLength) ? 0 : Math.max(offsetLength, 10);
            Collection<Entity> candidates = from.getWorld().getNearbyEntities(loc, length, length, length);
            boolean result = false;
            if (!beamConfig.pierce) {
                List<Entity> collect = candidates.stream()
                        .filter(entity -> (entity instanceof LivingEntity) && (canHitSelf || !entity.equals(from)) && !entity.isDead())
                        .filter(entity -> canHit(loc, entity))
                        .limit(1)
                        .collect(Collectors.toList());
                if (!collect.isEmpty()) {
                    Entity entity = collect.get(0);
                    if (entity instanceof LivingEntity) {
                        ((LivingEntity) entity).damage(damage, from);
                    }
                    return true;
                }
            } else {
                List<Entity> collect = candidates.stream()
                        .filter(entity -> (entity instanceof LivingEntity) && (canHitSelf || !entity.equals(from)))
                        .filter(entity -> canHit(loc, entity))
                        .collect(Collectors.toList());

                if (!collect.isEmpty()) {
                    collect.stream()
                            .map(entity -> ((LivingEntity) entity))
                            .forEach(livingEntity -> {
                                livingEntity.damage(damage, from);
                            });
                    result = true;
                }
            }
            return result;
        }


        @Override
        public void run() {
            try {
                World world = from.getWorld();
                if (Double.isInfinite(beamConfig.lengthPerSpawn)) {
                    return;
                }
                Location lastLocation = from.getEyeLocation();
                int totalSteps = (int) Math.round(beamConfig.length / beamConfig.lengthPerSpawn);
                final double lengthPerTick = Math.max(beamConfig.speed / 20, beamConfig.length);
                AtomicDouble lengthRemains = new AtomicDouble(0);
                AtomicInteger currentStep = new AtomicInteger(0);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        double lengthInThisTick = lengthPerTick + lengthRemains.get();

                        while ((lengthInThisTick -= beamConfig.lengthPerSpawn) > 0) {
                            if (currentStep.getAndAdd(1) >= totalSteps) {
                                this.cancel();
                                return;
                            }
                            Location shiftedLoc = function == null? lastLocation.clone() : lastLocation.clone().add(function.apply(1 - lastLocation.distance(from.getLocation()) / beamConfig.length));
                            boolean isHit = beamConfig.damage > 0 && tryHit(from, shiftedLoc, false, beamConfig.damage);
                            if (!beamConfig.ignoreWall && !transp.contains(lastLocation.getBlock().getType())) {
                                this.cancel();
                                return;
                            }

                            spawnParticle(from, world, shiftedLoc, 1);
                            Vector step = direction.normalize().multiply(beamConfig.lengthPerSpawn);
                            lastLocation.add(step);
                            if (isHit && !beamConfig.pierce) {
                                this.cancel();
                                return;
                            }
                        }
                        lengthRemains.set(lengthInThisTick + beamConfig.lengthPerSpawn);
                    }
                }.runTaskTimer(InfPlugin.plugin, 0, 1);
            } catch (Exception e) {
                this.cancel();
            }
        }
    }

    public static class BeamConfig implements ISerializable {
        @Serializable
        public double length = 10;
        @Serializable
        public ParticleConfig particle = new ParticleConfig();
        @Serializable
        public double speed = 1;
        @Serializable
        public double spawnsPerBlock = 4;
        double lengthPerSpawn = 0.25;
        @Serializable
        public double cone = 0;
        @Serializable
        public boolean pierce = false;
        @Serializable
        public boolean ignoreWall = false;
        @Serializable
        public double range = 40;
        @Serializable
        public double damage = 1;
        @Serializable
        public int burst = 1;
        @Serializable
        public int burstInterval = 10;
    }

    public static class BeamBuilder {
        private BeamConfig config = new BeamConfig();

        public BeamConfig build() {
            return config;
        }

        public BeamBuilder length(int length) {
            config.length = length;
            return this;
        }

        public BeamBuilder particle(ParticleConfig particle) {
            config.particle = particle;
            return this;
        }

        public BeamBuilder speed(double speed) {
            config.speed = speed;
            return this;
        }

        public BeamBuilder spawnsPerBlock(double spawnsPerBlock) {
            config.spawnsPerBlock = spawnsPerBlock;
            config.lengthPerSpawn = 1 / spawnsPerBlock;
            return this;
        }

        public BeamBuilder cone(double cone) {
            config.cone = cone;
            return this;
        }

        public BeamBuilder pierce(boolean pierce) {
            config.pierce = pierce;
            return this;
        }

        public BeamBuilder ignoreWall(boolean ignoreWall) {
            config.ignoreWall = ignoreWall;
            return this;
        }

        public BeamBuilder range(double range) {
            config.range = range;
            return this;
        }

        public BeamBuilder damage(double damage) {
            config.damage = damage;
            return this;
        }

        public BeamBuilder burst(int burst) {
            config.burst = burst;
            return this;
        }

        public BeamBuilder burstInterval(int burstInterval) {
            config.burstInterval = burstInterval;
            return this;
        }
    }
}
