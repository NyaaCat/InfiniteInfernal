package cat.nyaa.infiniteinfernal.ability.impl.active;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.ability.AbilityHurt;
import cat.nyaa.infiniteinfernal.ability.AbilitySpawn;
import cat.nyaa.infiniteinfernal.ability.ActiveAbility;
import cat.nyaa.infiniteinfernal.configs.MobConfig;
import cat.nyaa.infiniteinfernal.configs.ParticleConfig;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.mob.MobManager;
import cat.nyaa.infiniteinfernal.utils.Utils;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AbilityPhaseShift - Phase Shift / Invincibility Phases
 *
 * Boss periodically enters a "void phase" where it becomes invincible but cannot attack.
 * During void phase, shadow clones are spawned. Players can prevent the phase shift
 * by dealing enough damage during the vulnerable phase (DPS race).
 *
 * Solo mode: Lower DPS threshold, fewer clones.
 * Multi-player: Higher threshold, more clones.
 */
public class AbilityPhaseShift extends ActiveAbility implements AbilityHurt, AbilitySpawn {

    // === Phase Timing ===
    @Serializable
    public int vulnerablePhase = 200;

    @Serializable
    public int shiftedPhase = 100;

    @Serializable
    public int preShiftWarning = 40;

    @Serializable
    public int postShiftGrace = 20;

    // === DPS Threshold ===
    @Serializable
    public double damageThreshold = 100.0;

    @Serializable
    public double thresholdScalingPerPlayer = 30.0;

    // === Solo Mode Configuration ===
    @Serializable
    public double soloThresholdMultiplier = 0.5;

    @Serializable
    public int soloCloneCount = 1;

    // === Shadow Clone Configuration ===
    @Serializable
    public int shadowCloneCount = 2;

    @Serializable
    public double shadowCloneDamageMultiplier = 0.5;

    @Serializable
    public double shadowCloneHealthMultiplier = 0.2;

    @Serializable
    public boolean clonesPersistAfterShift = false;

    // === Message Configuration ===
    @Serializable
    public String messageDisplayType = "BOSSBAR";

    @Serializable
    public String bossbarColor = "PURPLE";

    @Serializable
    public String bossbarStyle = "SEGMENTED_10";

    @Serializable
    public String msgStability = "&e相位稳定性: {progress}%";

    @Serializable
    public String msgWarning = "&c⚠ 相位不稳！即将转移...";

    @Serializable
    public String msgPrevented = "&a相位稳定！转移已阻止！";

    @Serializable
    public String msgShiftOut = "&5Boss 进入虚空相位...";

    @Serializable
    public String msgShiftIn = "&dBoss 从虚空归来！";

    @Serializable
    public String msgCloneSpawn = "&8暗影分身已召唤";

    // === Particle Configuration ===
    @Serializable
    public ParticleConfig warningParticle = new ParticleConfig();

    @Serializable
    public ParticleConfig shiftOutParticle = new ParticleConfig();

    @Serializable
    public ParticleConfig shiftInParticle = new ParticleConfig();

    @Serializable
    public ParticleConfig cloneParticle = new ParticleConfig();

    @Serializable
    public ParticleConfig thresholdReachedParticle = new ParticleConfig();

    // === Sound Configuration ===
    @Serializable
    public String warningSound = "ENTITY_ENDERMAN_STARE";

    @Serializable
    public double warningPitch = 1.2;

    @Serializable
    public double warningVolume = 1.5;

    @Serializable
    public String shiftOutSound = "ENTITY_ENDERMAN_TELEPORT";

    @Serializable
    public String shiftInSound = "ENTITY_ILLUSIONER_PREPARE_MIRROR";

    @Serializable
    public String thresholdSound = "ENTITY_PLAYER_LEVELUP";

    // === State Tracking ===
    private static final Map<UUID, PhaseState> activePhases = new ConcurrentHashMap<>();

    {
        // Initialize default particles
        warningParticle.type = Particle.ENCHANT;
        warningParticle.amount = 50;
        warningParticle.deltaX = 1.0;
        warningParticle.deltaY = 2.0;
        warningParticle.deltaZ = 1.0;
        warningParticle.speed = 0.5;
        warningParticle.forced = true;

        shiftOutParticle.type = Particle.DRAGON_BREATH;
        shiftOutParticle.amount = 100;
        shiftOutParticle.speed = 0.2;
        shiftOutParticle.forced = true;

        shiftInParticle.type = Particle.REVERSE_PORTAL;
        shiftInParticle.amount = 80;
        shiftInParticle.speed = 0.3;
        shiftInParticle.forced = true;

        cloneParticle.type = Particle.SMOKE;
        cloneParticle.amount = 10;
        cloneParticle.speed = 0.01;
        cloneParticle.forced = true;

        thresholdReachedParticle.type = Particle.TOTEM_OF_UNDYING;
        thresholdReachedParticle.amount = 50;
        thresholdReachedParticle.speed = 1.0;
        thresholdReachedParticle.forced = true;
    }

    @Override
    public void onSpawn(IMob iMob) {
        // Start the phase cycle when mob spawns
        startPhaseCycle(iMob);
    }

    @Override
    public void active(IMob iMob) {
        // Active trigger - reset or boost the phase cycle
        UUID mobId = iMob.getEntity().getUniqueId();
        PhaseState state = activePhases.get(mobId);

        if (state == null) {
            startPhaseCycle(iMob);
        } else if (!state.isShifted) {
            // Reset damage accumulator to give players a chance
            state.accumulatedDamage = 0;
        }
    }

    private void startPhaseCycle(IMob iMob) {
        LivingEntity boss = iMob.getEntity();
        UUID mobId = boss.getUniqueId();
        World world = boss.getWorld();

        if (activePhases.containsKey(mobId)) return;

        List<Player> nearbyPlayers = getNearbyPlayers(iMob, 50);
        boolean isSoloMode = nearbyPlayers.size() <= 1;

        double effectiveThreshold = calculateThreshold(nearbyPlayers.size(), isSoloMode);
        int cloneCount = isSoloMode ? soloCloneCount : shadowCloneCount;

        PhaseState state = new PhaseState(isSoloMode, effectiveThreshold);
        activePhases.put(mobId, state);

        // Vulnerable phase loop
        runVulnerablePhase(iMob, state, cloneCount);
    }

    private void runVulnerablePhase(IMob iMob, PhaseState state, int cloneCount) {
        LivingEntity boss = iMob.getEntity();
        UUID mobId = boss.getUniqueId();
        World world = boss.getWorld();

        state.isShifted = false;
        state.accumulatedDamage = 0;

        BukkitRunnable vulnerableTask = new BukkitRunnable() {
            int ticks = 0;
            boolean thresholdReached = false;
            boolean warningShown = false;

            @Override
            public void run() {
                if (boss.isDead() || !activePhases.containsKey(mobId)) {
                    cancel();
                    endPhase(mobId);
                    return;
                }

                ticks++;

                List<Player> nearbyPlayers = getNearbyPlayers(iMob, 50);
                double progress = state.accumulatedDamage / state.effectiveThreshold;

                // Check if threshold reached
                if (!thresholdReached && progress >= 1.0) {
                    thresholdReached = true;
                    state.shiftPrevented = true;

                    sendMessage(nearbyPlayers, msgPrevented);
                    playSound(world, boss.getLocation(), thresholdSound, 1.5f, 1.0f);
                    Utils.spawnParticle(thresholdReachedParticle, world, boss.getLocation());

                    // Bonus for preventing shift - players get brief strength buff
                    for (Player player : nearbyPlayers) {
                        Utils.doEffect("STRENGTH", player, 60, 0, getName());
                    }
                }

                // Warning before shift
                if (!warningShown && !thresholdReached && ticks >= vulnerablePhase - preShiftWarning) {
                    warningShown = true;
                    sendMessage(nearbyPlayers, msgWarning);
                    playSound(world, boss.getLocation(), warningSound, warningVolume, warningPitch);
                }

                // Show warning particles as phase approaches
                if (ticks >= vulnerablePhase - preShiftWarning && !thresholdReached) {
                    Utils.spawnParticle(warningParticle, world, boss.getLocation());
                }

                // Update boss bar
                String msg = msgStability.replace("{progress}", String.format("%.0f", Math.min(progress * 100, 100)));
                updateBossBar(nearbyPlayers, state, (float) Math.min(progress, 1.0), msg);

                // End of vulnerable phase
                if (ticks >= vulnerablePhase) {
                    cancel();

                    if (thresholdReached || state.shiftPrevented) {
                        // Shift was prevented - restart vulnerable phase
                        state.shiftPrevented = false;
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                runVulnerablePhase(iMob, state, cloneCount);
                            }
                        }.runTaskLater(InfPlugin.plugin, postShiftGrace);
                    } else {
                        // Trigger shift
                        runShiftedPhase(iMob, state, cloneCount);
                    }
                }
            }
        };
        vulnerableTask.runTaskTimer(InfPlugin.plugin, 0, 1);
    }

    private void runShiftedPhase(IMob iMob, PhaseState state, int cloneCount) {
        LivingEntity boss = iMob.getEntity();
        UUID mobId = boss.getUniqueId();
        World world = boss.getWorld();
        Location bossLoc = boss.getLocation();

        state.isShifted = true;
        List<Player> nearbyPlayers = getNearbyPlayers(iMob, 50);

        // Notify players
        sendMessage(nearbyPlayers, msgShiftOut);
        playSound(world, bossLoc, shiftOutSound, 2.0f, 0.5f);
        Utils.spawnParticle(shiftOutParticle, world, bossLoc);

        // Make boss invincible (visual only - handled in onHurt)
        boss.setInvisible(true);
        Utils.doEffect("GLOWING", boss, shiftedPhase + 20, 0, getName());
        Utils.doEffect("RESISTANCE", boss, shiftedPhase + 20, 4, getName()); // Near invincibility

        // Spawn shadow clones
        List<IMob> clones = new ArrayList<>();
        sendMessage(nearbyPlayers, msgCloneSpawn);

        for (int i = 0; i < cloneCount; i++) {
            Location cloneLoc = Utils.randomFloorSpawnLocation(bossLoc, 3, 8);
            if (cloneLoc == null) cloneLoc = bossLoc.clone().add(Utils.random(-5, 5), 0, Utils.random(-5, 5));

            IMob clone = spawnShadowClone(iMob, cloneLoc);
            if (clone != null) {
                clones.add(clone);
                Utils.spawnParticle(cloneParticle, world, cloneLoc);
            }
        }
        state.clones = clones;

        // Shifted phase timer
        BukkitRunnable shiftedTask = new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (boss.isDead() || !activePhases.containsKey(mobId)) {
                    cancel();
                    endPhase(mobId);
                    return;
                }

                ticks++;

                // Particle trail on boss while shifted
                if (ticks % 5 == 0) {
                    Utils.spawnParticle(shiftOutParticle, world, boss.getLocation());
                }

                // Update boss bar with countdown
                float progress = 1.0f - (float) ticks / shiftedPhase;
                updateBossBar(getNearbyPlayers(iMob, 50), state, progress,
                    "&5虚空相位: " + (shiftedPhase - ticks) / 20 + "秒");

                // End of shifted phase
                if (ticks >= shiftedPhase) {
                    cancel();

                    // Return from void
                    boss.setInvisible(false);
                    List<Player> players = getNearbyPlayers(iMob, 50);
                    sendMessage(players, msgShiftIn);
                    playSound(world, boss.getLocation(), shiftInSound, 2.0f, 1.0f);
                    Utils.spawnParticle(shiftInParticle, world, boss.getLocation());

                    // Remove clones if configured
                    if (!clonesPersistAfterShift) {
                        for (IMob clone : state.clones) {
                            if (clone.getEntity() != null && !clone.getEntity().isDead()) {
                                Utils.spawnParticle(cloneParticle, world, clone.getEntity().getLocation());
                                clone.getEntity().remove();
                            }
                        }
                    }
                    state.clones.clear();

                    // Restart cycle
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            runVulnerablePhase(iMob, state, cloneCount);
                        }
                    }.runTaskLater(InfPlugin.plugin, postShiftGrace);
                }
            }
        };
        shiftedTask.runTaskTimer(InfPlugin.plugin, 0, 1);
    }

    private IMob spawnShadowClone(IMob original, Location location) {
        MobConfig config = original.getConfig();
        if (config == null) return null;

        try {
            IMob clone = MobManager.instance().spawnMobByConfig(config, location, original.getLevel());
            if (clone == null) return null;

            LivingEntity cloneEntity = clone.getEntity();

            // Reduce stats
            AttributeInstance damageAttr = cloneEntity.getAttribute(Attribute.ATTACK_DAMAGE);
            if (damageAttr != null) {
                damageAttr.setBaseValue(original.getDamage() * shadowCloneDamageMultiplier);
            }

            AttributeInstance healthAttr = cloneEntity.getAttribute(Attribute.MAX_HEALTH);
            if (healthAttr != null) {
                double newHealth = original.getMaxHealth() * shadowCloneHealthMultiplier;
                healthAttr.setBaseValue(newHealth);
                cloneEntity.setHealth(newHealth);
            }

            // Copy target
            if (cloneEntity instanceof Mob && original.getEntity() instanceof Mob) {
                ((Mob) cloneEntity).setTarget(((Mob) original.getEntity()).getTarget());
            }

            // Visual distinction
            cloneEntity.setCustomName("§8[分身] " + original.getName());
            Utils.doEffect("INVISIBILITY", cloneEntity, Integer.MAX_VALUE, 0, getName());
            Utils.doEffect("GLOWING", cloneEntity, Integer.MAX_VALUE, 0, getName());

            return clone;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void onHurtByPlayer(IMob mob, EntityDamageByEntityEvent event) {
        UUID mobId = mob.getEntity().getUniqueId();
        PhaseState state = activePhases.get(mobId);

        if (state != null) {
            if (state.isShifted) {
                // Reduce damage while shifted (in addition to resistance effect)
                event.setDamage(event.getDamage() * 0.1);
            } else {
                // Accumulate damage during vulnerable phase
                state.accumulatedDamage += event.getFinalDamage();
            }
        }
    }

    private double calculateThreshold(int playerCount, boolean isSoloMode) {
        double base = damageThreshold;
        if (isSoloMode) {
            return base * soloThresholdMultiplier;
        }
        return base + (playerCount - 1) * thresholdScalingPerPlayer;
    }

    private void endPhase(UUID mobId) {
        PhaseState state = activePhases.remove(mobId);
        if (state != null && state.bossBar != null) {
            // Clean up boss bar for all online players
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.hideBossBar(state.bossBar);
            }
        }
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
                default:
                    break;
            }
        }
    }

    private void updateBossBar(List<Player> players, PhaseState state, float progress, String message) {
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
        return "PhaseShift";
    }

    private static class PhaseState {
        final boolean isSoloMode;
        final double effectiveThreshold;
        boolean isShifted = false;
        boolean shiftPrevented = false;
        double accumulatedDamage = 0;
        List<IMob> clones = new ArrayList<>();
        BossBar bossBar = null;

        PhaseState(boolean isSoloMode, double effectiveThreshold) {
            this.isSoloMode = isSoloMode;
            this.effectiveThreshold = effectiveThreshold;
        }
    }
}
