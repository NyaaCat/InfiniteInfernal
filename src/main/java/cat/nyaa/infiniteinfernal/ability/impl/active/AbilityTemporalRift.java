package cat.nyaa.infiniteinfernal.ability.impl.active;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.ability.AbilityHurt;
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
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AbilityTemporalRift - Time Distortion Zone
 *
 * Creates a time distortion field around the boss that slows players inside
 * while boosting the boss's attack speed.
 *
 * Multi-player mode: 2+ players standing at opposite ends of the zone can
 * "anchor" and collapse the rift early.
 *
 * Solo mode: Player can deal enough damage to collapse the rift.
 */
public class AbilityTemporalRift extends ActiveAbility implements AbilityHurt {

    // === Zone Parameters ===
    @Serializable
    public double radius = 8.0;

    @Serializable
    public int duration = 200;

    @Serializable
    public int warningTicks = 40;

    // === Effect Parameters ===
    @Serializable
    public int slowAmplifier = 3;

    @Serializable
    public double bossSpeedMultiplier = 1.5;

    // === Multi-player Anchor Mechanism ===
    @Serializable
    public int anchorPlayersRequired = 2;

    @Serializable
    public double anchorDistance = 6.0;

    @Serializable
    public double anchorCollapseBonus = 0.3;

    // === Solo Mode Configuration ===
    @Serializable
    public double soloEscapeDamage = 50.0;

    @Serializable
    public double soloRadiusMultiplier = 0.7;

    // === Multi-player Scaling ===
    @Serializable
    public boolean scaleWithPlayers = true;

    @Serializable
    public double radiusPerPlayer = 1.5;

    // === Detection Range ===
    @Serializable
    public double detectionRange = 128.0;

    // === Message Configuration ===
    @Serializable
    public String messageDisplayType = "BOSSBAR";

    @Serializable
    public String bossbarColor = "PURPLE";

    @Serializable
    public String bossbarStyle = "SEGMENTED_10";

    @Serializable
    public String msgWarning = "&d⚠ 时间裂隙即将开启！";

    @Serializable
    public String msgActive = "&5时间裂隙已激活！";

    @Serializable
    public String msgAnchorHint = "&e提示: 与队友站在区域两端可瓦解裂隙";

    @Serializable
    public String msgSoloHint = "&e提示: 造成 {damage} 伤害可瓦解裂隙";

    @Serializable
    public String msgProgress = "&6瓦解进度: {progress}%";

    @Serializable
    public String msgCollapsed = "&a时间裂隙已瓦解！";

    @Serializable
    public String msgExpired = "&7时间裂隙已消散";

    // === Particle Configuration ===
    @Serializable
    public ParticleConfig warningParticle = new ParticleConfig();

    @Serializable
    public ParticleConfig activeParticle = new ParticleConfig();

    @Serializable
    public ParticleConfig collapseParticle = new ParticleConfig();

    // === Sound Configuration ===
    @Serializable
    public String warningSound = "BLOCK_BEACON_AMBIENT";

    @Serializable
    public double warningPitch = 0.5;

    @Serializable
    public double warningVolume = 1.5;

    @Serializable
    public String activeSound = "BLOCK_PORTAL_AMBIENT";

    @Serializable
    public double activePitch = 0.3;

    @Serializable
    public double activeVolume = 2.0;

    @Serializable
    public String collapseSound = "ENTITY_ILLUSIONER_MIRROR_MOVE";

    // === State Tracking ===
    private static final Map<UUID, RiftState> activeRifts = new ConcurrentHashMap<>();

    {
        // Initialize default particles
        warningParticle.type = Particle.PORTAL;
        warningParticle.amount = 50;
        warningParticle.deltaY = 2.0;
        warningParticle.speed = 0.5;
        warningParticle.forced = true;

        activeParticle.type = Particle.DUST_COLOR_TRANSITION;
        activeParticle.amount = 30;
        activeParticle.deltaX = 0.5;
        activeParticle.deltaY = 0.5;
        activeParticle.deltaZ = 0.5;
        activeParticle.speed = 2.0;
        activeParticle.extraData = "128,0,255,0,128,255,2.0";
        activeParticle.forced = true;

        collapseParticle.type = Particle.REVERSE_PORTAL;
        collapseParticle.amount = 200;
        collapseParticle.speed = 2.0;
        collapseParticle.forced = true;
    }

    @Override
    public void active(IMob iMob) {
        LivingEntity boss = iMob.getEntity();
        Location center = boss.getLocation().clone();
        World world = boss.getWorld();
        UUID mobId = boss.getUniqueId();

        // Calculate effective radius
        List<Player> nearbyPlayers = getNearbyPlayers(iMob, detectionRange);
        int playerCount = nearbyPlayers.size();
        boolean isSoloMode = playerCount <= 1;

        final double effectiveRadius;
        if (isSoloMode) {
            effectiveRadius = radius * soloRadiusMultiplier;
        } else if (scaleWithPlayers) {
            effectiveRadius = radius + (playerCount - 1) * radiusPerPlayer;
        } else {
            effectiveRadius = radius;
        }

        // Create state
        RiftState state = new RiftState(center, effectiveRadius, isSoloMode);
        activeRifts.put(mobId, state);

        // Warning phase
        sendMessage(nearbyPlayers, msgWarning);
        playSound(world, center, warningSound, warningVolume, warningPitch);

        // Send appropriate hint
        if (isSoloMode) {
            String hint = msgSoloHint.replace("{damage}", String.format("%.0f", soloEscapeDamage));
            sendMessage(nearbyPlayers, hint);
        } else if (playerCount >= anchorPlayersRequired) {
            sendMessage(nearbyPlayers, msgAnchorHint);
        }

        // Warning particle effect
        BukkitRunnable warningTask = new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks++ >= warningTicks || !activeRifts.containsKey(mobId)) {
                    cancel();
                    return;
                }
                spawnWarningParticles(world, center, effectiveRadius);
                updateBossBar(nearbyPlayers, state, (float) ticks / warningTicks, msgWarning);
            }
        };
        warningTask.runTaskTimer(InfPlugin.plugin, 0, 2);

        // Activate after warning
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!activeRifts.containsKey(mobId)) return;

                state.isActive = true;
                sendMessage(nearbyPlayers, msgActive);
                playSound(world, center, activeSound, activeVolume, activePitch);

                // Main effect loop
                final double fRadius = effectiveRadius;
                BukkitRunnable effectTask = new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        if (ticks++ >= duration || !activeRifts.containsKey(mobId) || state.collapsed) {
                            endRift(mobId, iMob, state, state.collapsed);
                            cancel();
                            return;
                        }

                        // Apply effects to players in zone
                        List<Player> playersInZone = getPlayersInRadius(center, fRadius);
                        for (Player player : playersInZone) {
                            if (Utils.validGamemode(player)) {
                                Utils.doEffect("SLOWNESS", player, 40, slowAmplifier, getName());
                            }
                        }

                        // Check anchor collapse (multi-player)
                        if (!isSoloMode && playersInZone.size() >= anchorPlayersRequired) {
                            if (checkAnchorCollapse(playersInZone, center, fRadius)) {
                                state.collapsed = true;
                                state.anchorCollapsed = true;
                            }
                        }

                        // Visual effects
                        spawnActiveParticles(world, center, fRadius);

                        // Update progress
                        float progress = isSoloMode ?
                            (float) (state.accumulatedDamage / soloEscapeDamage) :
                            (float) ticks / duration;
                        updateBossBar(playersInZone, state, Math.min(progress, 1.0f),
                            msgProgress.replace("{progress}", String.format("%.0f", progress * 100)));
                    }
                };
                effectTask.runTaskTimer(InfPlugin.plugin, 0, 1);
            }
        }.runTaskLater(InfPlugin.plugin, warningTicks);
    }

    @Override
    public void onHurtByPlayer(IMob mob, EntityDamageByEntityEvent event) {
        UUID mobId = mob.getEntity().getUniqueId();
        RiftState state = activeRifts.get(mobId);

        if (state != null && state.isActive && state.isSoloMode && !state.collapsed) {
            state.accumulatedDamage += event.getFinalDamage();
            if (state.accumulatedDamage >= soloEscapeDamage) {
                state.collapsed = true;
            }
        }
    }

    private void endRift(UUID mobId, IMob iMob, RiftState state, boolean wasCollapsed) {
        activeRifts.remove(mobId);

        List<Player> nearbyPlayers = getNearbyPlayers(iMob, detectionRange);
        World world = state.center.getWorld();

        if (wasCollapsed) {
            sendMessage(nearbyPlayers, msgCollapsed);
            playSound(world, state.center, collapseSound, 2.0f, 1.0f);
            spawnCollapseParticles(world, state.center);

            // Apply bonus damage buff to players if anchor-collapsed
            if (state.anchorCollapsed) {
                for (Player player : nearbyPlayers) {
                    Utils.doEffect("STRENGTH", player, 100, 0, getName());
                }
            }
        } else {
            sendMessage(nearbyPlayers, msgExpired);
        }

        // Clean up boss bar
        state.hideBossBar(nearbyPlayers);
    }

    private boolean checkAnchorCollapse(List<Player> players, Location center, double radius) {
        if (players.size() < anchorPlayersRequired) return false;

        // Check if players are spread out enough (at opposite ends)
        for (int i = 0; i < players.size(); i++) {
            for (int j = i + 1; j < players.size(); j++) {
                double distance = players.get(i).getLocation().distance(players.get(j).getLocation());
                if (distance >= anchorDistance) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<Player> getPlayersInRadius(Location center, double radius) {
        List<Player> result = new ArrayList<>();
        World world = center.getWorld();
        if (world == null) return result;

        for (Player player : world.getPlayers()) {
            if (player.getLocation().distance(center) <= radius && Utils.validGamemode(player)) {
                result.add(player);
            }
        }
        return result;
    }

    private void spawnWarningParticles(World world, Location center, double radius) {
        List<Location> circle = Utils.getRoundLocations(center, radius);
        for (Location loc : circle) {
            Utils.spawnParticle(warningParticle, world, loc);
        }
    }

    private void spawnActiveParticles(World world, Location center, double radius) {
        // Spiral effect
        List<Location> circle = Utils.getRoundLocations(center, radius);
        for (Location loc : circle) {
            Utils.spawnParticle(activeParticle, world, loc.add(0, Utils.random() * 3, 0));
        }
    }

    private void spawnCollapseParticles(World world, Location center) {
        Utils.spawnParticle(collapseParticle, world, center);
    }

    private void sendMessage(List<Player> players, String message) {
        Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(message);

        for (Player player : players) {
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
                case "BOSSBAR":
                case "NONE":
                default:
                    // BossBar handled separately
                    break;
            }
        }
    }

    private void updateBossBar(List<Player> players, RiftState state, float progress, String message) {
        if (!"BOSSBAR".equalsIgnoreCase(messageDisplayType)) return;

        Component name = LegacyComponentSerializer.legacyAmpersand().deserialize(message);

        if (state.bossBar == null) {
            BossBar.Color color;
            try {
                color = BossBar.Color.valueOf(bossbarColor.toUpperCase());
            } catch (IllegalArgumentException e) {
                color = BossBar.Color.PURPLE;
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
            player.showBossBar(state.bossBar);
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
        return "TemporalRift";
    }

    private static class RiftState {
        final Location center;
        final double radius;
        final boolean isSoloMode;
        boolean isActive = false;
        boolean collapsed = false;
        boolean anchorCollapsed = false;
        double accumulatedDamage = 0;
        BossBar bossBar = null;

        RiftState(Location center, double radius, boolean isSoloMode) {
            this.center = center;
            this.radius = radius;
            this.isSoloMode = isSoloMode;
        }

        void hideBossBar(List<Player> players) {
            if (bossBar != null) {
                for (Player player : players) {
                    player.hideBossBar(bossBar);
                }
            }
        }
    }
}
