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
import org.bukkit.entity.*;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AbilityEchoStrike - Echo Strike / Delayed Damage Reflection
 *
 * Boss marks a player, and all damage dealt to the boss during the mark period
 * is recorded. After a delay, the accumulated damage is reflected back to the
 * marked player (and optionally distributed to nearby players).
 *
 * Solo mode: Reduced reflection multiplier and maximum damage cap.
 * Multi-player: Damage can be distributed among players.
 */
public class AbilityEchoStrike extends ActiveAbility implements AbilityHurt {

    // === Target Selection ===
    @Serializable
    public double range = 30.0;

    // === Time Parameters ===
    @Serializable
    public int markDuration = 100;

    @Serializable
    public int echoDelay = 60;

    @Serializable
    public int warningTicks = 20;

    // === Damage Calculation ===
    @Serializable
    public double echoMultiplier = 0.5;

    @Serializable
    public double maxEchoDamage = 50.0;

    @Serializable
    public double echoDamageDistribution = 0.7;

    // === Solo Mode Configuration ===
    @Serializable
    public double soloEchoMultiplier = 0.3;

    @Serializable
    public double soloMaxEchoDamage = 30.0;

    // === Visual Feedback ===
    @Serializable
    public boolean showDamageAccumulator = true;

    @Serializable
    public String markEffectName = "GLOWING";

    @Serializable
    public int markEffectAmplifier = 0;

    // === Message Configuration ===
    @Serializable
    public String messageDisplayType = "BOSSBAR";

    @Serializable
    public String bossbarColor = "RED";

    @Serializable
    public String bossbarStyle = "SEGMENTED_20";

    @Serializable
    public String msgMarked = "&c你被回声标记！";

    @Serializable
    public String msgAccumulating = "&6累积伤害: {damage}";

    @Serializable
    public String msgWarning = "&c⚠ 回声即将释放！";

    @Serializable
    public String msgRelease = "&4回声释放！受到 {damage} 伤害！";

    @Serializable
    public String msgTip = "&7提示: 停止攻击可减少回声伤害";

    // === Particle Configuration ===
    @Serializable
    public ParticleConfig markParticle = new ParticleConfig();

    @Serializable
    public ParticleConfig chargeParticle = new ParticleConfig();

    @Serializable
    public ParticleConfig releaseParticle = new ParticleConfig();

    // === Sound Configuration ===
    @Serializable
    public String markSound = "ENTITY_ELDER_GUARDIAN_CURSE";

    @Serializable
    public double markPitch = 1.5;

    @Serializable
    public double markVolume = 1.0;

    @Serializable
    public String releaseSound = "ENTITY_WITHER_SHOOT";

    @Serializable
    public double releasePitch = 0.5;

    @Serializable
    public double releaseVolume = 2.0;

    // === State Tracking ===
    private static final Map<UUID, EchoState> activeEchoes = new ConcurrentHashMap<>();

    {
        // Initialize default particles
        markParticle.type = Particle.SOUL_FIRE_FLAME;
        markParticle.amount = 15;
        markParticle.deltaX = 0.5;
        markParticle.deltaY = 1.0;
        markParticle.deltaZ = 0.5;
        markParticle.speed = 0.02;
        markParticle.forced = true;

        chargeParticle.type = Particle.DAMAGE_INDICATOR;
        chargeParticle.amount = 5;
        chargeParticle.speed = 0.1;
        chargeParticle.forced = true;

        releaseParticle.type = Particle.FLASH;
        releaseParticle.amount = 1;
        releaseParticle.forced = true;
    }

    @Override
    public void active(IMob iMob) {
        LivingEntity boss = iMob.getEntity();
        UUID mobId = boss.getUniqueId();
        World world = boss.getWorld();

        // Select target
        List<Player> nearbyPlayers = getNearbyPlayers(iMob, range);
        if (nearbyPlayers.isEmpty()) return;

        Player target = Utils.randomPick(nearbyPlayers);
        if (target == null) return;

        boolean isSoloMode = nearbyPlayers.size() <= 1;

        // Create state
        EchoState state = new EchoState(target, isSoloMode);
        activeEchoes.put(mobId, state);

        // Apply mark effect
        Utils.doEffect(markEffectName, target, markDuration + echoDelay + 20, markEffectAmplifier, getName());

        // Notify target
        sendMessage(target, msgMarked);
        sendMessage(target, msgTip);
        playSound(world, target.getLocation(), markSound, markVolume, markPitch);

        // Accumulation phase - visual and tracking
        BukkitRunnable accumulateTask = new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks++ >= markDuration || !activeEchoes.containsKey(mobId)) {
                    cancel();
                    return;
                }

                // Update boss bar with accumulated damage
                double currentDamage = state.accumulatedDamage;
                double maxDamage = isSoloMode ? soloMaxEchoDamage : maxEchoDamage;
                float progress = (float) Math.min(currentDamage / maxDamage, 1.0);

                updateBossBar(target, state, progress,
                    msgAccumulating.replace("{damage}", String.format("%.1f", currentDamage)));

                // Particle effect on target
                if (target.isOnline() && !target.isDead()) {
                    Utils.spawnParticle(markParticle, world, target.getLocation().add(0, 1, 0));
                }

                // Charge particles when accumulating
                if (showDamageAccumulator && currentDamage > 0) {
                    Utils.spawnParticle(chargeParticle, world, boss.getLocation().add(0, 1.5, 0));
                }
            }
        };
        accumulateTask.runTaskTimer(InfPlugin.plugin, 0, 5);

        // Warning before release
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!activeEchoes.containsKey(mobId)) return;
                sendMessage(target, msgWarning);

                // Flash warning
                BukkitRunnable warningFlash = new BukkitRunnable() {
                    int flashes = 0;
                    @Override
                    public void run() {
                        if (flashes++ >= 4 || !activeEchoes.containsKey(mobId)) {
                            cancel();
                            return;
                        }
                        if (target.isOnline()) {
                            world.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
                        }
                    }
                };
                warningFlash.runTaskTimer(InfPlugin.plugin, 0, 5);
            }
        }.runTaskLater(InfPlugin.plugin, markDuration);

        // Echo release
        new BukkitRunnable() {
            @Override
            public void run() {
                accumulateTask.cancel();
                EchoState finalState = activeEchoes.remove(mobId);
                if (finalState == null) return;

                releaseEcho(iMob, finalState, nearbyPlayers);
            }
        }.runTaskLater(InfPlugin.plugin, markDuration + echoDelay);
    }

    @Override
    public void onHurtByPlayer(IMob mob, EntityDamageByEntityEvent event) {
        UUID mobId = mob.getEntity().getUniqueId();
        EchoState state = activeEchoes.get(mobId);

        if (state != null) {
            // Get the real player damager from various damage sources
            Player damager = getPlayerSource(event.getDamager());

            // Accumulate damage from the marked player
            if (damager != null && damager.getUniqueId().equals(state.target.getUniqueId())) {
                state.accumulatedDamage += event.getFinalDamage();
            }
        }
    }

    /**
     * Extract the Player source from various damage sources
     */
    private Player getPlayerSource(Entity damager) {
        if (damager instanceof Player) {
            return (Player) damager;
        } else if (damager instanceof Projectile) {
            ProjectileSource shooter = ((Projectile) damager).getShooter();
            if (shooter instanceof Player) {
                return (Player) shooter;
            }
        } else if (damager instanceof AreaEffectCloud) {
            ProjectileSource source = ((AreaEffectCloud) damager).getSource();
            if (source instanceof Player) {
                return (Player) source;
            }
        } else if (damager instanceof TNTPrimed) {
            Entity source = ((TNTPrimed) damager).getSource();
            if (source instanceof Player) {
                return (Player) source;
            }
        } else if (damager instanceof EvokerFangs) {
            LivingEntity owner = ((EvokerFangs) damager).getOwner();
            if (owner instanceof Player) {
                return (Player) owner;
            }
        }
        return null;
    }

    private void releaseEcho(IMob iMob, EchoState state, List<Player> nearbyPlayers) {
        Player target = state.target;
        if (target == null || !target.isOnline() || target.isDead()) {
            state.hideBossBar(target);
            return;
        }

        World world = target.getWorld();
        double multiplier = state.isSoloMode ? soloEchoMultiplier : echoMultiplier;
        double maxDmg = state.isSoloMode ? soloMaxEchoDamage : maxEchoDamage;

        double echoDamage = Math.min(state.accumulatedDamage * multiplier, maxDmg);

        if (echoDamage > 0) {
            // Visual effect
            Utils.spawnParticle(releaseParticle, world, target.getLocation());
            playSound(world, target.getLocation(), releaseSound, releaseVolume, releasePitch);

            // Apply damage to marked target
            double targetDamage = echoDamage;

            // In multi-player, distribute some damage
            if (!state.isSoloMode && nearbyPlayers.size() > 1) {
                targetDamage = echoDamage * echoDamageDistribution;
                double sharedDamage = (echoDamage - targetDamage) / (nearbyPlayers.size() - 1);

                for (Player player : nearbyPlayers) {
                    if (!player.getUniqueId().equals(target.getUniqueId())) {
                        player.damage(sharedDamage, iMob.getEntity());
                    }
                }
            }

            target.damage(targetDamage, iMob.getEntity());
            sendMessage(target, msgRelease.replace("{damage}", String.format("%.1f", targetDamage)));
        }

        state.hideBossBar(target);
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
            case "BOSSBAR":
            case "NONE":
            default:
                // BossBar handled separately
                break;
        }
    }

    private void updateBossBar(Player player, EchoState state, float progress, String message) {
        if (!"BOSSBAR".equalsIgnoreCase(messageDisplayType)) return;
        if (player == null || !player.isOnline()) return;

        Component name = LegacyComponentSerializer.legacyAmpersand().deserialize(message);

        if (state.bossBar == null) {
            BossBar.Color color;
            try {
                color = BossBar.Color.valueOf(bossbarColor.toUpperCase());
            } catch (IllegalArgumentException e) {
                color = BossBar.Color.RED;
            }

            BossBar.Overlay overlay;
            try {
                overlay = BossBar.Overlay.valueOf(bossbarStyle.toUpperCase());
            } catch (IllegalArgumentException e) {
                overlay = BossBar.Overlay.PROGRESS;
            }

            state.bossBar = BossBar.bossBar(name, progress, color, overlay);
            player.showBossBar(state.bossBar);
        } else {
            state.bossBar.name(name);
            state.bossBar.progress(Math.max(0, Math.min(1, progress)));
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
        return "EchoStrike";
    }

    private static class EchoState {
        final Player target;
        final boolean isSoloMode;
        double accumulatedDamage = 0;
        BossBar bossBar = null;

        EchoState(Player target, boolean isSoloMode) {
            this.target = target;
            this.isSoloMode = isSoloMode;
        }

        void hideBossBar(Player player) {
            if (bossBar != null && player != null && player.isOnline()) {
                player.hideBossBar(bossBar);
            }
        }
    }
}
