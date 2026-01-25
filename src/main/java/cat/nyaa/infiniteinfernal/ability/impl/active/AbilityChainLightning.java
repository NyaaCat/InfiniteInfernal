package cat.nyaa.infiniteinfernal.ability.impl.active;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.ability.AbilityAttack;
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
 * AbilityChainLightning - Chain Lightning
 *
 * Boss fires lightning that chains between players. Players hit become "charged"
 * and if multiple charged players get too close, they form a "circuit" that deals
 * massive damage.
 *
 * Solo mode: No chaining, no circuit danger. Only initial strike.
 * Multi-player: Chain jumps, circuit formation danger.
 */
public class AbilityChainLightning extends ActiveAbility implements AbilityAttack {

    // === Initial Strike ===
    @Serializable
    public double initialDamage = 15.0;

    @Serializable
    public double targetRange = 25.0;

    // === Chain Behavior (Multi-player) ===
    @Serializable
    public int maxChains = 5;

    @Serializable
    public double chainRange = 6.0;

    @Serializable
    public double chainDamageReduction = 0.7;

    @Serializable
    public int chainDelay = 3;

    @Serializable
    public boolean canChainToSameTarget = false;

    // === Solo Mode Configuration ===
    @Serializable
    public double soloInitialDamageMultiplier = 0.8;

    @Serializable
    public boolean soloChainEnabled = false;

    // === Charged State ===
    @Serializable
    public int chargedDuration = 100;

    @Serializable
    public String chargedEffectName = "GLOWING";

    // === Circuit Mechanism (Multi-player only) ===
    @Serializable
    public double circuitRange = 4.0;

    @Serializable
    public int circuitMinPlayers = 2;

    @Serializable
    public double circuitDamagePerPlayer = 10.0;

    @Serializable
    public double circuitDamageMultiplierPerPlayer = 1.5;

    @Serializable
    public int circuitCheckInterval = 10;

    // === Message Configuration ===
    @Serializable
    public String messageDisplayType = "SUBTITLE";

    @Serializable
    public String bossbarColor = "BLUE";

    @Serializable
    public String bossbarStyle = "SOLID";

    @Serializable
    public String msgStrike = "&b⚡ 闪电打击！";

    @Serializable
    public String msgCharged = "&e你已充能！";

    @Serializable
    public String msgChargedMulti = "&c远离其他充能玩家！";

    @Serializable
    public String msgChargedSolo = "&7充能将在 {time} 秒后消散";

    @Serializable
    public String msgCircuitWarning = "&c⚠ 电路形成！立即分散！";

    @Serializable
    public String msgCircuitDamage = "&4电路放电！受到 {damage} 伤害！";

    // === Particle Configuration ===
    @Serializable
    public ParticleConfig lightningParticle = new ParticleConfig();

    @Serializable
    public ParticleConfig chargedParticle = new ParticleConfig();

    @Serializable
    public ParticleConfig circuitWarningParticle = new ParticleConfig();

    @Serializable
    public ParticleConfig circuitDischargeParticle = new ParticleConfig();

    // === Sound Configuration ===
    @Serializable
    public String strikeSound = "ENTITY_LIGHTNING_BOLT_THUNDER";

    @Serializable
    public double strikePitch = 1.0;

    @Serializable
    public double strikeVolume = 1.0;

    @Serializable
    public String chainSound = "BLOCK_NOTE_BLOCK_PLING";

    @Serializable
    public double chainPitch = 1.5;

    @Serializable
    public double chainVolume = 0.8;

    @Serializable
    public String circuitSound = "ENTITY_GENERIC_EXPLODE";

    @Serializable
    public double circuitPitch = 0.5;

    @Serializable
    public double circuitVolume = 2.0;

    // === State Tracking ===
    private static final Map<UUID, ChargedState> chargedPlayers = new ConcurrentHashMap<>();
    private static final Map<UUID, BukkitRunnable> circuitCheckers = new ConcurrentHashMap<>();

    {
        // Initialize default particles
        lightningParticle.type = Particle.ELECTRIC_SPARK;
        lightningParticle.amount = 10;
        lightningParticle.deltaX = 0.1;
        lightningParticle.deltaY = 0.1;
        lightningParticle.deltaZ = 0.1;
        lightningParticle.forced = true;

        chargedParticle.type = Particle.ENCHANTED_HIT;
        chargedParticle.amount = 8;
        chargedParticle.deltaX = 0.4;
        chargedParticle.deltaY = 0.8;
        chargedParticle.deltaZ = 0.4;
        chargedParticle.speed = 0.02;
        chargedParticle.forced = true;

        circuitWarningParticle.type = Particle.DUST;
        circuitWarningParticle.amount = 5;
        circuitWarningParticle.extraData = "255,255,0,1.0";
        circuitWarningParticle.forced = true;

        circuitDischargeParticle.type = Particle.FLASH;
        circuitDischargeParticle.amount = 3;
        circuitDischargeParticle.forced = true;
    }

    @Override
    public void active(IMob iMob) {
        LivingEntity boss = iMob.getEntity();
        World world = boss.getWorld();

        List<Player> nearbyPlayers = getNearbyPlayers(iMob, targetRange);
        if (nearbyPlayers.isEmpty()) return;

        boolean isSoloMode = nearbyPlayers.size() <= 1;

        // Select initial target
        Player target = Utils.randomPick(nearbyPlayers);
        if (target == null) return;

        // Strike initial target
        double damage = isSoloMode ? initialDamage * soloInitialDamageMultiplier : initialDamage;
        strikeTarget(iMob, target, damage, world);

        // Chain to other targets (multi-player only)
        if (!isSoloMode && (soloChainEnabled || nearbyPlayers.size() > 1)) {
            chainLightning(iMob, target, nearbyPlayers, damage, 0, new HashSet<>());
        }

        // Start circuit checker if not already running for this mob
        UUID mobId = boss.getUniqueId();
        if (!isSoloMode && !circuitCheckers.containsKey(mobId)) {
            startCircuitChecker(iMob);
        }
    }

    @Override
    public void onAttack(IMob mob, LivingEntity target) {
        // Can also trigger on melee attack
        if (target instanceof Player && Utils.possibility(0.3)) {
            active(mob);
        }
    }

    private void strikeTarget(IMob iMob, Player target, double damage, World world) {
        Location loc = target.getLocation();

        // Visual effects
        world.strikeLightningEffect(loc);
        Utils.spawnParticle(lightningParticle, world, loc);
        playSound(world, loc, strikeSound, strikeVolume, strikePitch);

        // Apply damage
        target.damage(damage, iMob.getEntity());

        // Apply charged state
        applyChargedState(target, iMob);

        // Notify
        sendMessage(target, msgStrike);
    }

    private void chainLightning(IMob iMob, Player lastTarget, List<Player> allPlayers,
                                 double lastDamage, int chainCount, Set<UUID> alreadyHit) {
        if (chainCount >= maxChains) return;

        alreadyHit.add(lastTarget.getUniqueId());

        // Find next target
        Player nextTarget = null;
        double closestDist = Double.MAX_VALUE;

        for (Player player : allPlayers) {
            if (!canChainToSameTarget && alreadyHit.contains(player.getUniqueId())) continue;
            if (!Utils.validGamemode(player)) continue;

            double dist = player.getLocation().distance(lastTarget.getLocation());
            if (dist <= chainRange && dist < closestDist && dist > 0.5) {
                closestDist = dist;
                nextTarget = player;
            }
        }

        if (nextTarget == null) return;

        final Player finalTarget = nextTarget;
        final double chainDamage = lastDamage * chainDamageReduction;

        // Delay the chain
        new BukkitRunnable() {
            @Override
            public void run() {
                if (finalTarget.isDead() || !finalTarget.isOnline()) return;

                World world = finalTarget.getWorld();

                // Draw chain line
                drawChainLine(lastTarget.getLocation(), finalTarget.getLocation(), world);

                // Strike
                Utils.spawnParticle(lightningParticle, world, finalTarget.getLocation());
                playSound(world, finalTarget.getLocation(), chainSound, chainVolume, chainPitch);

                finalTarget.damage(chainDamage, iMob.getEntity());
                applyChargedState(finalTarget, iMob);

                // Continue chain
                chainLightning(iMob, finalTarget, allPlayers, chainDamage, chainCount + 1, alreadyHit);
            }
        }.runTaskLater(InfPlugin.plugin, chainDelay);
    }

    private void applyChargedState(Player player, IMob source) {
        UUID playerId = player.getUniqueId();

        // Apply visual effect
        Utils.doEffect(chargedEffectName, player, chargedDuration, 0, getName());

        // Track charged state
        ChargedState state = new ChargedState(source.getEntity().getUniqueId(), System.currentTimeMillis());
        chargedPlayers.put(playerId, state);

        // Determine message based on other charged players
        List<Player> otherCharged = getOtherChargedPlayers(player);
        if (otherCharged.isEmpty()) {
            sendMessage(player, msgCharged);
            sendMessage(player, msgChargedSolo.replace("{time}", String.valueOf(chargedDuration / 20)));
        } else {
            sendMessage(player, msgCharged);
            sendMessage(player, msgChargedMulti);
        }

        // Schedule removal
        new BukkitRunnable() {
            @Override
            public void run() {
                chargedPlayers.remove(playerId);
            }
        }.runTaskLater(InfPlugin.plugin, chargedDuration);

        // Start charged particle effect
        startChargedParticles(player, chargedDuration);
    }

    private void startChargedParticles(Player player, int duration) {
        UUID playerId = player.getUniqueId();

        BukkitRunnable particleTask = new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks++ >= duration || !chargedPlayers.containsKey(playerId) ||
                    player.isDead() || !player.isOnline()) {
                    cancel();
                    return;
                }

                Utils.spawnParticle(chargedParticle, player.getWorld(), player.getLocation().add(0, 1, 0));

                // Show remaining time via action bar
                if (ticks % 20 == 0) {
                    int remaining = (duration - ticks) / 20;
                    sendActionBar(player, "&e充能: " + remaining + "秒");
                }
            }
        };
        particleTask.runTaskTimer(InfPlugin.plugin, 0, 2);
    }

    private void startCircuitChecker(IMob iMob) {
        UUID mobId = iMob.getEntity().getUniqueId();

        BukkitRunnable checker = new BukkitRunnable() {
            @Override
            public void run() {
                if (iMob.getEntity().isDead()) {
                    circuitCheckers.remove(mobId);
                    cancel();
                    return;
                }

                checkForCircuits(iMob);
            }
        };
        checker.runTaskTimer(InfPlugin.plugin, circuitCheckInterval, circuitCheckInterval);
        circuitCheckers.put(mobId, checker);
    }

    private void checkForCircuits(IMob iMob) {
        // Group charged players by proximity
        List<Set<Player>> circuits = findCircuits();

        for (Set<Player> circuit : circuits) {
            if (circuit.size() >= circuitMinPlayers) {
                triggerCircuitDischarge(iMob, circuit);
            }
        }
    }

    private List<Set<Player>> findCircuits() {
        List<Set<Player>> circuits = new ArrayList<>();
        Set<UUID> processed = new HashSet<>();

        for (UUID playerId : chargedPlayers.keySet()) {
            if (processed.contains(playerId)) continue;

            Player player = Bukkit.getPlayer(playerId);
            if (player == null || player.isDead() || !player.isOnline()) continue;

            Set<Player> circuit = new HashSet<>();
            expandCircuit(player, circuit, processed);

            if (circuit.size() >= circuitMinPlayers) {
                circuits.add(circuit);
            }
        }

        return circuits;
    }

    private void expandCircuit(Player player, Set<Player> circuit, Set<UUID> processed) {
        UUID playerId = player.getUniqueId();
        if (processed.contains(playerId)) return;
        if (!chargedPlayers.containsKey(playerId)) return;

        processed.add(playerId);
        circuit.add(player);

        // Find nearby charged players
        for (UUID otherId : chargedPlayers.keySet()) {
            if (processed.contains(otherId)) continue;

            Player other = Bukkit.getPlayer(otherId);
            if (other == null || other.isDead() || !other.isOnline()) continue;

            if (player.getLocation().distance(other.getLocation()) <= circuitRange) {
                expandCircuit(other, circuit, processed);
            }
        }
    }

    private void triggerCircuitDischarge(IMob iMob, Set<Player> circuit) {
        World world = iMob.getEntity().getWorld();

        // Warning phase
        for (Player player : circuit) {
            sendMessage(player, msgCircuitWarning);
            Utils.spawnParticle(circuitWarningParticle, world, player.getLocation());
        }

        // Discharge after brief delay
        new BukkitRunnable() {
            @Override
            public void run() {
                // Calculate damage based on circuit size
                double baseDamage = circuitDamagePerPlayer * circuit.size();
                double multiplier = Math.pow(circuitDamageMultiplierPerPlayer, circuit.size() - 1);
                double totalDamage = baseDamage * multiplier;

                for (Player player : circuit) {
                    if (player.isDead() || !player.isOnline()) continue;

                    Utils.spawnParticle(circuitDischargeParticle, world, player.getLocation());
                    player.damage(totalDamage, iMob.getEntity());

                    sendMessage(player, msgCircuitDamage.replace("{damage}", String.format("%.1f", totalDamage)));

                    // Remove charged state
                    chargedPlayers.remove(player.getUniqueId());
                }

                playSound(world, circuit.iterator().next().getLocation(), circuitSound, circuitVolume, circuitPitch);
            }
        }.runTaskLater(InfPlugin.plugin, 10);
    }

    private List<Player> getOtherChargedPlayers(Player exclude) {
        List<Player> others = new ArrayList<>();
        for (UUID id : chargedPlayers.keySet()) {
            if (id.equals(exclude.getUniqueId())) continue;
            Player player = Bukkit.getPlayer(id);
            if (player != null && player.isOnline() && !player.isDead()) {
                others.add(player);
            }
        }
        return others;
    }

    private void drawChainLine(Location from, Location to, World world) {
        List<Location> line = Utils.drawLine(from.add(0, 1, 0), to.add(0, 1, 0), 10);
        for (Location loc : line) {
            Utils.spawnParticle(lightningParticle, world, loc);
        }
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
        return "ChainLightning";
    }

    private static class ChargedState {
        final UUID sourceMobId;
        final long startTime;

        ChargedState(UUID sourceMobId, long startTime) {
            this.sourceMobId = sourceMobId;
            this.startTime = startTime;
        }
    }
}
