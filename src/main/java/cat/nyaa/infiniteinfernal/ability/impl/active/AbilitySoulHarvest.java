package cat.nyaa.infiniteinfernal.ability.impl.active;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.ability.AbilityHurt;
import cat.nyaa.infiniteinfernal.ability.AbilitySpawn;
import cat.nyaa.infiniteinfernal.ability.ActiveAbility;
import cat.nyaa.infiniteinfernal.configs.ParticleConfig;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.Utils;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AbilitySoulHarvest - Soul Harvest / Death Penalty and Recovery
 *
 * When players die near the boss, their souls are harvested, making the boss
 * stronger. Periodically, a "soul vulnerable window" appears where players
 * can deal enough damage to recover souls.
 *
 * Solo mode: Lower recovery damage threshold, longer window duration.
 * Multi-player: Higher threshold, shorter window.
 */
public class AbilitySoulHarvest extends ActiveAbility implements AbilityHurt, AbilitySpawn {

    // === Harvest Configuration ===
    @Serializable
    public double harvestRange = 50.0;

    @Serializable
    public int maxSouls = 10;

    // === Boss Scaling Per Soul ===
    @Serializable
    public double damagePerSoul = 2.0;

    @Serializable
    public double healthPerSoul = 20.0;

    @Serializable
    public double speedPerSoul = 0.02;

    // === Soul Recovery Window ===
    @Serializable
    public int soulWindowInterval = 400;

    @Serializable
    public int soulWindowDuration = 100;

    @Serializable
    public double soulRecoveryDamage = 30.0;

    @Serializable
    public double soulRecoveryDamagePerSoul = 10.0;

    // === Solo Mode Configuration ===
    @Serializable
    public double soloRecoveryDamageMultiplier = 0.6;

    @Serializable
    public int soloWindowDuration = 120;

    // === Recovery Reward ===
    @Serializable
    public int recoveryBuffDuration = 200;

    @Serializable
    public String recoveryBuffEffect = "STRENGTH";

    @Serializable
    public int recoveryBuffAmplifier = 1;

    // === Message Configuration ===
    @Serializable
    public String messageDisplayType = "BOSSBAR";

    @Serializable
    public String bossbarColor = "WHITE";

    @Serializable
    public String bossbarStyle = "SEGMENTED_10";

    @Serializable
    public String msgSoulCount = "&c灵魂: {current}/{max}";

    @Serializable
    public String msgHarvested = "&4你的灵魂被收割！Boss 变得更强了...";

    @Serializable
    public String msgWindowOpen = "&a灵魂易伤窗口开启！";

    @Serializable
    public String msgWindowClose = "&7灵魂易伤窗口关闭";

    @Serializable
    public String msgRecoveryProgress = "&6回收进度: {progress}%";

    @Serializable
    public String msgRecovered = "&a灵魂已回收！获得 {buff} 增益！";

    @Serializable
    public String msgBossStrength = "&c警告: Boss 已收割 {count} 个灵魂";

    // === Particle Configuration ===
    @Serializable
    public ParticleConfig harvestParticle = new ParticleConfig();

    @Serializable
    public ParticleConfig soulOrbParticle = new ParticleConfig();

    @Serializable
    public ParticleConfig windowParticle = new ParticleConfig();

    @Serializable
    public ParticleConfig recoveryParticle = new ParticleConfig();

    // === Sound Configuration ===
    @Serializable
    public String harvestSound = "ENTITY_WITHER_SPAWN";

    @Serializable
    public double harvestPitch = 0.5;

    @Serializable
    public double harvestVolume = 2.0;

    @Serializable
    public String windowOpenSound = "BLOCK_BEACON_ACTIVATE";

    @Serializable
    public String windowCloseSound = "BLOCK_BEACON_DEACTIVATE";

    @Serializable
    public String recoverySound = "ENTITY_EXPERIENCE_ORB_PICKUP";

    // === State Tracking ===
    private static final Map<UUID, HarvestState> activeHarvests = new ConcurrentHashMap<>();
    private static Listener deathListener;
    private static boolean listenerRegistered = false;

    {
        // Initialize default particles
        harvestParticle.type = Particle.SOUL;
        harvestParticle.amount = 30;
        harvestParticle.speed = 0.1;
        harvestParticle.forced = true;

        soulOrbParticle.type = Particle.SOUL_FIRE_FLAME;
        soulOrbParticle.amount = 3;
        soulOrbParticle.deltaX = 0.1;
        soulOrbParticle.deltaY = 0.1;
        soulOrbParticle.deltaZ = 0.1;
        soulOrbParticle.forced = true;

        windowParticle.type = Particle.DUST;
        windowParticle.amount = 20;
        windowParticle.deltaX = 1.0;
        windowParticle.deltaY = 1.5;
        windowParticle.deltaZ = 1.0;
        windowParticle.speed = 1.5;
        windowParticle.extraData = "0,255,255,1.5";
        windowParticle.forced = true;

        recoveryParticle.type = Particle.TOTEM_OF_UNDYING;
        recoveryParticle.amount = 30;
        recoveryParticle.speed = 0.5;
        recoveryParticle.forced = true;
    }

    public AbilitySoulHarvest() {
        registerDeathListener();
    }

    private void registerDeathListener() {
        if (listenerRegistered) return;

        deathListener = new Listener() {
            @EventHandler(priority = EventPriority.MONITOR)
            public void onPlayerDeath(PlayerDeathEvent event) {
                Player player = event.getEntity();
                Location deathLoc = player.getLocation();

                // Check if death is near any boss with this ability
                for (Map.Entry<UUID, HarvestState> entry : activeHarvests.entrySet()) {
                    HarvestState state = entry.getValue();
                    if (state.boss == null || state.boss.isDead()) continue;

                    double distance = deathLoc.distance(state.boss.getLocation());
                    if (distance <= harvestRange && state.soulCount < maxSouls) {
                        harvestSoul(state, player);
                        break; // Only one boss harvests
                    }
                }
            }
        };

        Bukkit.getPluginManager().registerEvents(deathListener, InfPlugin.plugin);
        listenerRegistered = true;
    }

    @Override
    public void onSpawn(IMob iMob) {
        initializeHarvest(iMob);
    }

    @Override
    public void active(IMob iMob) {
        UUID mobId = iMob.getEntity().getUniqueId();
        if (!activeHarvests.containsKey(mobId)) {
            initializeHarvest(iMob);
        }
    }

    private void initializeHarvest(IMob iMob) {
        LivingEntity boss = iMob.getEntity();
        UUID mobId = boss.getUniqueId();

        if (activeHarvests.containsKey(mobId)) return;

        List<Player> nearbyPlayers = getNearbyPlayers(iMob, harvestRange);
        boolean isSoloMode = nearbyPlayers.size() <= 1;

        HarvestState state = new HarvestState(boss, isSoloMode);
        activeHarvests.put(mobId, state);

        // Start soul window cycle
        startSoulWindowCycle(iMob, state);

        // Start soul orb visual effect
        startSoulOrbEffect(iMob, state);
    }

    private void harvestSoul(HarvestState state, Player victim) {
        if (state.boss == null || state.boss.isDead()) return;

        state.soulCount++;
        World world = state.boss.getWorld();

        // Visual effect: soul flying to boss
        animateSoulHarvest(victim.getLocation(), state.boss.getLocation(), world);

        // Apply boss buffs
        applyBossBuffs(state);

        // Notify
        sendMessage(victim, msgHarvested);
        playSound(world, state.boss.getLocation(), harvestSound, harvestVolume, harvestPitch);

        // Notify nearby players of boss strength
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distance(state.boss.getLocation()) <= harvestRange) {
                sendMessage(player, msgBossStrength.replace("{count}", String.valueOf(state.soulCount)));
            }
        }
    }

    private void animateSoulHarvest(Location from, Location to, World world) {
        new BukkitRunnable() {
            Location current = from.clone().add(0, 1, 0);
            int steps = 0;
            final int maxSteps = 20;

            @Override
            public void run() {
                if (steps++ >= maxSteps) {
                    Utils.spawnParticle(harvestParticle, world, to);
                    cancel();
                    return;
                }

                // Move towards boss
                org.bukkit.util.Vector direction = to.toVector().subtract(current.toVector()).normalize();
                current.add(direction.multiply(from.distance(to) / maxSteps));

                Utils.spawnParticle(soulOrbParticle, world, current);
            }
        }.runTaskTimer(InfPlugin.plugin, 0, 1);
    }

    private void applyBossBuffs(HarvestState state) {
        LivingEntity boss = state.boss;
        if (boss == null || boss.isDead()) return;

        int souls = state.soulCount;

        // Increase damage
        AttributeInstance damageAttr = boss.getAttribute(Attribute.ATTACK_DAMAGE);
        if (damageAttr != null) {
            double newDamage = state.baseDamage + (souls * damagePerSoul);
            damageAttr.setBaseValue(newDamage);
        }

        // Increase max health
        AttributeInstance healthAttr = boss.getAttribute(Attribute.MAX_HEALTH);
        if (healthAttr != null) {
            double newMaxHealth = state.baseHealth + (souls * healthPerSoul);
            healthAttr.setBaseValue(newMaxHealth);
            boss.setHealth(Math.min(boss.getHealth() + healthPerSoul, newMaxHealth));
        }

        // Increase speed
        AttributeInstance speedAttr = boss.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttr != null) {
            double newSpeed = state.baseSpeed + (souls * speedPerSoul);
            speedAttr.setBaseValue(newSpeed);
        }
    }

    private void removeBossBuffs(HarvestState state, int soulsRemoved) {
        LivingEntity boss = state.boss;
        if (boss == null || boss.isDead()) return;

        state.soulCount = Math.max(0, state.soulCount - soulsRemoved);
        int souls = state.soulCount;

        // Restore to base + remaining soul buffs
        AttributeInstance damageAttr = boss.getAttribute(Attribute.ATTACK_DAMAGE);
        if (damageAttr != null) {
            damageAttr.setBaseValue(state.baseDamage + (souls * damagePerSoul));
        }

        AttributeInstance healthAttr = boss.getAttribute(Attribute.MAX_HEALTH);
        if (healthAttr != null) {
            double newMaxHealth = state.baseHealth + (souls * healthPerSoul);
            healthAttr.setBaseValue(newMaxHealth);
            boss.setHealth(Math.min(boss.getHealth(), newMaxHealth));
        }

        AttributeInstance speedAttr = boss.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.setBaseValue(state.baseSpeed + (souls * speedPerSoul));
        }
    }

    private void startSoulWindowCycle(IMob iMob, HarvestState state) {
        BukkitRunnable cycleTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (state.boss == null || state.boss.isDead()) {
                    cancel();
                    activeHarvests.remove(iMob.getEntity().getUniqueId());
                    return;
                }

                if (state.soulCount > 0) {
                    openSoulWindow(iMob, state);
                }
            }
        };
        cycleTask.runTaskTimer(InfPlugin.plugin, soulWindowInterval, soulWindowInterval);
    }

    private void openSoulWindow(IMob iMob, HarvestState state) {
        state.windowOpen = true;
        state.windowDamage = 0;

        LivingEntity boss = state.boss;
        World world = boss.getWorld();
        List<Player> nearbyPlayers = getNearbyPlayers(iMob, harvestRange);

        int duration = state.isSoloMode ? soloWindowDuration : soulWindowDuration;
        double requiredDamage = calculateRecoveryDamage(state);

        // Notify players
        for (Player player : nearbyPlayers) {
            sendMessage(player, msgWindowOpen);
        }
        playSound(world, boss.getLocation(), windowOpenSound, 1.5f, 1.0f);

        // Window effect loop
        BukkitRunnable windowTask = new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (boss.isDead() || ticks++ >= duration || !state.windowOpen) {
                    state.windowOpen = false;

                    // Check if soul was recovered
                    if (state.windowDamage >= requiredDamage) {
                        recoverSoul(iMob, state, nearbyPlayers);
                    } else {
                        // Window closed without recovery
                        for (Player player : nearbyPlayers) {
                            sendMessage(player, msgWindowClose);
                        }
                        playSound(world, boss.getLocation(), windowCloseSound, 1.0f, 0.5f);
                    }

                    state.hideBossBar(nearbyPlayers);
                    cancel();
                    return;
                }

                // Visual effect
                Utils.spawnParticle(windowParticle, world, boss.getLocation());

                // Update progress bar
                float progress = (float) (state.windowDamage / requiredDamage);
                updateBossBar(nearbyPlayers, state, Math.min(progress, 1.0f),
                    msgRecoveryProgress.replace("{progress}", String.format("%.0f", progress * 100)));
            }
        };
        windowTask.runTaskTimer(InfPlugin.plugin, 0, 1);
    }

    private void recoverSoul(IMob iMob, HarvestState state, List<Player> players) {
        World world = state.boss.getWorld();

        // Remove one soul from boss
        removeBossBuffs(state, 1);

        // Visual and sound
        Utils.spawnParticle(recoveryParticle, world, state.boss.getLocation());
        playSound(world, state.boss.getLocation(), recoverySound, 2.0f, 1.0f);

        // Reward players
        for (Player player : players) {
            Utils.doEffect(recoveryBuffEffect, player, recoveryBuffDuration, recoveryBuffAmplifier, getName());
            sendMessage(player, msgRecovered.replace("{buff}", recoveryBuffEffect));
        }

        state.windowOpen = false;
    }

    private double calculateRecoveryDamage(HarvestState state) {
        double base = soulRecoveryDamage + (state.soulCount * soulRecoveryDamagePerSoul);
        if (state.isSoloMode) {
            base *= soloRecoveryDamageMultiplier;
        }
        return base;
    }

    @Override
    public void onHurtByPlayer(IMob mob, EntityDamageByEntityEvent event) {
        UUID mobId = mob.getEntity().getUniqueId();
        HarvestState state = activeHarvests.get(mobId);

        if (state != null && state.windowOpen) {
            state.windowDamage += event.getFinalDamage();

            // Check if threshold reached
            double required = calculateRecoveryDamage(state);
            if (state.windowDamage >= required) {
                state.windowOpen = false; // Will trigger recovery in next tick
            }
        }
    }

    private void startSoulOrbEffect(IMob iMob, HarvestState state) {
        BukkitRunnable orbTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (state.boss == null || state.boss.isDead()) {
                    cancel();
                    return;
                }

                if (state.soulCount > 0) {
                    // Show orbiting souls
                    double angle = (System.currentTimeMillis() / 50.0) % (2 * Math.PI);
                    for (int i = 0; i < Math.min(state.soulCount, 5); i++) {
                        double offsetAngle = angle + (i * 2 * Math.PI / Math.min(state.soulCount, 5));
                        double x = Math.cos(offsetAngle) * 1.5;
                        double z = Math.sin(offsetAngle) * 1.5;
                        Location orbLoc = state.boss.getLocation().add(x, 1.5, z);
                        Utils.spawnParticle(soulOrbParticle, state.boss.getWorld(), orbLoc);
                    }
                }

                // Update soul count display
                List<Player> nearbyPlayers = state.boss.getWorld().getPlayers().stream()
                    .filter(p -> p.getLocation().distance(state.boss.getLocation()) <= harvestRange)
                    .filter(Utils::validGamemode)
                    .toList();

                if (!state.windowOpen) {
                    float progress = (float) state.soulCount / maxSouls;
                    updateBossBar(nearbyPlayers, state, progress,
                        msgSoulCount.replace("{current}", String.valueOf(state.soulCount))
                                   .replace("{max}", String.valueOf(maxSouls)));
                }
            }
        };
        orbTask.runTaskTimer(InfPlugin.plugin, 0, 5);
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

    private void updateBossBar(List<Player> players, HarvestState state, float progress, String message) {
        if (!"BOSSBAR".equalsIgnoreCase(messageDisplayType)) return;

        Component name = LegacyComponentSerializer.legacyAmpersand().deserialize(message);

        if (state.bossBar == null) {
            BossBar.Color color;
            try {
                color = BossBar.Color.valueOf(bossbarColor.toUpperCase());
            } catch (IllegalArgumentException e) {
                color = BossBar.Color.WHITE;
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
        return "SoulHarvest";
    }

    private static class HarvestState {
        final LivingEntity boss;
        final boolean isSoloMode;
        final double baseDamage;
        final double baseHealth;
        final double baseSpeed;

        int soulCount = 0;
        boolean windowOpen = false;
        double windowDamage = 0;
        BossBar bossBar = null;

        HarvestState(LivingEntity boss, boolean isSoloMode) {
            this.boss = boss;
            this.isSoloMode = isSoloMode;

            // Store base stats
            AttributeInstance damageAttr = boss.getAttribute(Attribute.ATTACK_DAMAGE);
            this.baseDamage = damageAttr != null ? damageAttr.getBaseValue() : 5.0;

            AttributeInstance healthAttr = boss.getAttribute(Attribute.MAX_HEALTH);
            this.baseHealth = healthAttr != null ? healthAttr.getBaseValue() : 20.0;

            AttributeInstance speedAttr = boss.getAttribute(Attribute.MOVEMENT_SPEED);
            this.baseSpeed = speedAttr != null ? speedAttr.getBaseValue() : 0.25;
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
