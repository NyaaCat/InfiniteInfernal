package cat.nyaa.infiniteinfernal.ability.impl.active;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.ability.ActiveAbility;
import cat.nyaa.infiniteinfernal.configs.ParticleConfig;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.Utils;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AbilityGravityWell - Gravity Anomaly Fields
 *
 * Creates multiple gravity wells around the boss - some are black holes (pull)
 * and some are white holes (push). Where opposite polarity wells overlap,
 * a "null zone" safe area forms.
 *
 * Solo mode: Fewer wells and smaller radius, guaranteed safe zone nearby.
 * Multi-player: More wells, larger chaos.
 */
public class AbilityGravityWell extends ActiveAbility {

    // === Gravity Well Configuration ===
    @Serializable
    public int wellCount = 4;

    @Serializable
    public double wellRadius = 5.0;

    @Serializable
    public int duration = 160;

    @Serializable
    public int spawnDelay = 10;

    // === Solo Mode Configuration ===
    @Serializable
    public int soloWellCount = 2;

    @Serializable
    public double soloWellRadius = 4.0;

    // === Force Field Strength ===
    @Serializable
    public double pullStrength = 0.8;

    @Serializable
    public double pushStrength = 1.2;

    // === Core Damage ===
    @Serializable
    public double coreDamage = 4.0;

    @Serializable
    public double coreRadius = 1.5;

    // === Spawn Position ===
    @Serializable
    public double minDistanceFromBoss = 5.0;

    @Serializable
    public double maxDistanceFromBoss = 20.0;

    // === Null Zone (Safe Zone) ===
    @Serializable
    public double nullZoneRadius = 2.0;

    @Serializable
    public boolean guaranteeNullZone = true;

    // === Message Configuration ===
    @Serializable
    public String messageDisplayType = "ACTIONBAR";

    @Serializable
    public String bossbarColor = "YELLOW";

    @Serializable
    public String bossbarStyle = "SOLID";

    @Serializable
    public String msgActivate = "&e重力异常出现！";

    @Serializable
    public String msgSafeZone = "&a安全区方向: {direction} {distance}格";

    @Serializable
    public String msgWarning = "&c你正在被吸入/推离！";

    @Serializable
    public String msgCoreDamage = "&4核心伤害！立即离开！";

    // === Particle Configuration ===
    @Serializable
    public ParticleConfig blackHoleParticle = new ParticleConfig();

    @Serializable
    public ParticleConfig whiteHoleParticle = new ParticleConfig();

    @Serializable
    public ParticleConfig nullZoneParticle = new ParticleConfig();

    // === Sound Configuration ===
    @Serializable
    public String spawnSound = "ENTITY_ENDERMAN_TELEPORT";

    @Serializable
    public double spawnPitch = 0.5;

    @Serializable
    public double spawnVolume = 1.5;

    @Serializable
    public String nullZoneSound = "BLOCK_BEACON_ACTIVATE";

    // === State Tracking ===
    private static final Map<UUID, GravityState> activeGravity = new ConcurrentHashMap<>();

    {
        // Initialize default particles
        blackHoleParticle.type = Particle.SQUID_INK;
        blackHoleParticle.amount = 20;
        blackHoleParticle.speed = 0.05;
        blackHoleParticle.forced = true;

        whiteHoleParticle.type = Particle.END_ROD;
        whiteHoleParticle.amount = 20;
        whiteHoleParticle.speed = 0.1;
        whiteHoleParticle.forced = true;

        nullZoneParticle.type = Particle.DUST;
        nullZoneParticle.amount = 30;
        nullZoneParticle.deltaX = 0.3;
        nullZoneParticle.deltaY = 0.3;
        nullZoneParticle.deltaZ = 0.3;
        nullZoneParticle.speed = 1.5;
        nullZoneParticle.extraData = "255,215,0,1.5"; // Gold color
        nullZoneParticle.forced = true;
    }

    @Override
    public void active(IMob iMob) {
        LivingEntity boss = iMob.getEntity();
        UUID mobId = boss.getUniqueId();
        World world = boss.getWorld();
        Location bossLoc = boss.getLocation();

        List<Player> nearbyPlayers = getNearbyPlayers(iMob, maxDistanceFromBoss + 10);
        boolean isSoloMode = nearbyPlayers.size() <= 1;

        int numWells = isSoloMode ? soloWellCount : wellCount;
        double effectiveRadius = isSoloMode ? soloWellRadius : wellRadius;

        // Generate well positions
        List<GravityWell> wells = new ArrayList<>();
        Location guaranteedNullZoneCenter = null;

        for (int i = 0; i < numWells; i++) {
            Location wellLoc = Utils.randomFloorSpawnLocation(bossLoc, minDistanceFromBoss, maxDistanceFromBoss);
            if (wellLoc == null) wellLoc = bossLoc.clone().add(Utils.random(-10, 10), 0, Utils.random(-10, 10));

            boolean isBlackHole = i % 2 == 0; // Alternate between black and white holes
            wells.add(new GravityWell(wellLoc, effectiveRadius, isBlackHole));
        }

        // Calculate null zones (overlapping areas)
        List<Location> nullZones = calculateNullZones(wells);

        // Guarantee at least one safe zone near a player
        if (guaranteeNullZone && !nearbyPlayers.isEmpty()) {
            Player closestPlayer = nearbyPlayers.get(0);
            if (nullZones.isEmpty() || getClosestDistance(closestPlayer.getLocation(), nullZones) > 10) {
                // Create a guaranteed null zone
                Location safeZone = Utils.randomFloorSpawnLocation(closestPlayer.getLocation(), 3, 8);
                if (safeZone != null) {
                    nullZones.add(safeZone);
                    guaranteedNullZoneCenter = safeZone;
                }
            }
        }

        GravityState state = new GravityState(wells, nullZones);
        activeGravity.put(mobId, state);

        // Notify players
        for (Player player : nearbyPlayers) {
            sendMessage(player, msgActivate);
        }
        playSound(world, bossLoc, spawnSound, spawnVolume, spawnPitch);

        // Spawn wells with delay
        for (int i = 0; i < wells.size(); i++) {
            final int index = i;
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!activeGravity.containsKey(mobId)) return;
                    GravityWell well = wells.get(index);
                    playSound(world, well.center, spawnSound, spawnVolume * 0.7f, well.isBlackHole ? 0.3f : 1.5f);
                }
            }.runTaskLater(InfPlugin.plugin, i * spawnDelay);
        }

        // Main effect loop
        BukkitRunnable effectTask = new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks++ >= duration || !activeGravity.containsKey(mobId)) {
                    endGravity(mobId, iMob);
                    cancel();
                    return;
                }

                // Apply gravity effects
                for (Player player : getNearbyPlayers(iMob, maxDistanceFromBoss + 10)) {
                    if (!Utils.validGamemode(player)) continue;

                    Location playerLoc = player.getLocation();

                    // Check if in null zone (safe)
                    boolean inNullZone = isInNullZone(playerLoc, state.nullZones);
                    if (inNullZone) {
                        // Safe - show particles
                        if (ticks % 10 == 0) {
                            Utils.spawnParticle(nullZoneParticle, world, playerLoc);
                        }
                        continue;
                    }

                    // Calculate combined force from all wells
                    Vector totalForce = new Vector(0, 0, 0);
                    boolean nearCore = false;

                    for (GravityWell well : state.wells) {
                        double distance = playerLoc.distance(well.center);
                        if (distance > well.radius) continue;

                        // Core damage
                        if (distance < coreRadius) {
                            nearCore = true;
                            if (ticks % 20 == 0) {
                                player.damage(coreDamage, boss);
                                sendActionBar(player, msgCoreDamage);
                            }
                        }

                        // Calculate force direction and magnitude
                        Vector direction = well.center.toVector().subtract(playerLoc.toVector());
                        if (direction.lengthSquared() > 0) {
                            direction.normalize();
                        }

                        double strength = (1 - distance / well.radius) * (well.isBlackHole ? pullStrength : -pushStrength);
                        totalForce.add(direction.multiply(strength));
                    }

                    // Apply force
                    if (totalForce.lengthSquared() > 0.001) {
                        Vector velocity = player.getVelocity();
                        velocity.add(totalForce.multiply(0.1));
                        // Cap velocity
                        if (velocity.length() > 2.0) {
                            velocity.normalize().multiply(2.0);
                        }
                        player.setVelocity(velocity);

                        if (ticks % 40 == 0 && !nearCore) {
                            sendActionBar(player, msgWarning);
                        }
                    }

                    // Show safe zone direction
                    if (ticks % 20 == 0 && "ACTIONBAR".equalsIgnoreCase(messageDisplayType)) {
                        Location closestSafe = getClosestLocation(playerLoc, state.nullZones);
                        if (closestSafe != null) {
                            String direction = getDirectionArrow(playerLoc, closestSafe);
                            double dist = playerLoc.distance(closestSafe);
                            sendActionBar(player, msgSafeZone
                                .replace("{direction}", direction)
                                .replace("{distance}", String.format("%.1f", dist)));
                        }
                    }
                }

                // Visual effects
                if (ticks % 2 == 0) {
                    for (GravityWell well : state.wells) {
                        ParticleConfig particle = well.isBlackHole ? blackHoleParticle : whiteHoleParticle;
                        Utils.spawnParticle(particle, world, well.center);

                        // Draw well boundary
                        if (ticks % 10 == 0) {
                            List<Location> circle = Utils.getRoundLocations(well.center, well.radius);
                            for (int i = 0; i < circle.size(); i += 4) {
                                Utils.spawnParticle(particle, world, circle.get(i));
                            }
                        }
                    }

                    // Draw null zones
                    for (Location zone : state.nullZones) {
                        Utils.spawnParticle(nullZoneParticle, world, zone);
                    }
                }

                // Update boss bar
                float progress = 1.0f - (float) ticks / duration;
                updateBossBar(nearbyPlayers, state, progress, "&e重力异常: " + (duration - ticks) / 20 + "秒");
            }
        };
        effectTask.runTaskTimer(InfPlugin.plugin, 0, 1);
    }

    private void endGravity(UUID mobId, IMob iMob) {
        GravityState state = activeGravity.remove(mobId);
        if (state != null) {
            state.hideBossBar(getNearbyPlayers(iMob, maxDistanceFromBoss + 10));
        }
    }

    private List<Location> calculateNullZones(List<GravityWell> wells) {
        List<Location> nullZones = new ArrayList<>();

        // Find overlap points between opposite polarity wells
        for (int i = 0; i < wells.size(); i++) {
            for (int j = i + 1; j < wells.size(); j++) {
                GravityWell well1 = wells.get(i);
                GravityWell well2 = wells.get(j);

                if (well1.isBlackHole != well2.isBlackHole) {
                    double distance = well1.center.distance(well2.center);
                    double sumRadius = well1.radius + well2.radius;

                    if (distance < sumRadius && distance > 0) {
                        // Calculate midpoint weighted by radius
                        double ratio = well1.radius / sumRadius;
                        Location midpoint = well1.center.clone().add(
                            well2.center.clone().subtract(well1.center).multiply(ratio)
                        );
                        nullZones.add(midpoint);
                    }
                }
            }
        }

        return nullZones;
    }

    private boolean isInNullZone(Location loc, List<Location> nullZones) {
        for (Location zone : nullZones) {
            if (zone.getWorld().equals(loc.getWorld()) && zone.distance(loc) <= nullZoneRadius) {
                return true;
            }
        }
        return false;
    }

    private double getClosestDistance(Location loc, List<Location> locations) {
        double minDist = Double.MAX_VALUE;
        for (Location l : locations) {
            if (l.getWorld().equals(loc.getWorld())) {
                double dist = l.distance(loc);
                if (dist < minDist) minDist = dist;
            }
        }
        return minDist;
    }

    private Location getClosestLocation(Location loc, List<Location> locations) {
        Location closest = null;
        double minDist = Double.MAX_VALUE;
        for (Location l : locations) {
            if (l.getWorld().equals(loc.getWorld())) {
                double dist = l.distance(loc);
                if (dist < minDist) {
                    minDist = dist;
                    closest = l;
                }
            }
        }
        return closest;
    }

    private String getDirectionArrow(Location from, Location to) {
        Vector direction = to.toVector().subtract(from.toVector());
        direction.setY(0);
        if (direction.lengthSquared() < 0.01) return "●";

        double angle = Math.toDegrees(Math.atan2(direction.getZ(), direction.getX()));
        angle = (angle + 360) % 360;

        if (angle < 22.5 || angle >= 337.5) return "→";
        if (angle < 67.5) return "↘";
        if (angle < 112.5) return "↓";
        if (angle < 157.5) return "↙";
        if (angle < 202.5) return "←";
        if (angle < 247.5) return "↖";
        if (angle < 292.5) return "↑";
        return "↗";
    }

    private void sendMessage(Player player, String message) {
        if (player == null || !player.isOnline()) return;

        Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(message);

        switch (messageDisplayType.toUpperCase()) {
            case "SUBTITLE":
                player.showTitle(Title.title(
                    Component.empty(),
                    component,
                    Title.Times.times(Duration.ofMillis(100), Duration.ofSeconds(2), Duration.ofMillis(500))
                ));
                break;
            case "ACTIONBAR":
                player.sendActionBar(component);
                break;
            default:
                break;
        }
    }

    private void sendActionBar(Player player, String message) {
        if (player == null || !player.isOnline()) return;
        Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(message);
        player.sendActionBar(component);
    }

    private void updateBossBar(List<Player> players, GravityState state, float progress, String message) {
        if (!"BOSSBAR".equalsIgnoreCase(messageDisplayType)) return;

        Component name = LegacyComponentSerializer.legacyAmpersand().deserialize(message);

        if (state.bossBar == null) {
            BossBar.Color color;
            try {
                color = BossBar.Color.valueOf(bossbarColor.toUpperCase());
            } catch (IllegalArgumentException e) {
                color = BossBar.Color.YELLOW;
            }

            BossBar.Overlay overlay;
            try {
                overlay = BossBar.Overlay.valueOf(bossbarStyle.toUpperCase());
            } catch (IllegalArgumentException e) {
                overlay = BossBar.Overlay.PROGRESS;
            }

            state.bossBar = BossBar.bossBar(name, progress, color, overlay);
        } else {
            state.bossBar.name(name);
            state.bossBar.progress(Math.max(0, Math.min(1, progress)));
        }

        for (Player player : players) {
            if (player.isOnline()) {
                player.showBossBar(state.bossBar);
            }
        }
    }

    private void playSound(World world, Location location, String soundName, double volume, double pitch) {
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            world.playSound(location, sound, (float) volume, (float) pitch);
        } catch (IllegalArgumentException e) {
            // Invalid sound, ignore
        }
    }

    @Override
    public String getName() {
        return "GravityWell";
    }

    private static class GravityWell {
        final Location center;
        final double radius;
        final boolean isBlackHole;

        GravityWell(Location center, double radius, boolean isBlackHole) {
            this.center = center;
            this.radius = radius;
            this.isBlackHole = isBlackHole;
        }
    }

    private static class GravityState {
        final List<GravityWell> wells;
        final List<Location> nullZones;
        BossBar bossBar = null;

        GravityState(List<GravityWell> wells, List<Location> nullZones) {
            this.wells = wells;
            this.nullZones = nullZones;
        }

        void hideBossBar(List<Player> players) {
            if (bossBar != null) {
                for (Player player : players) {
                    if (player.isOnline()) {
                        player.hideBossBar(bossBar);
                    }
                }
            }
        }
    }
}
