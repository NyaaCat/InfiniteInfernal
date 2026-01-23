package cat.nyaa.infiniteinfernal;

import cat.nyaa.infiniteinfernal.ability.AbilityActive;
import cat.nyaa.infiniteinfernal.ability.IAbilitySet;
import cat.nyaa.infiniteinfernal.configs.WorldConfig;
import cat.nyaa.infiniteinfernal.mob.CustomMob;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.mob.MobManager;
import cat.nyaa.infiniteinfernal.utils.CorrectionParser;
import cat.nyaa.infiniteinfernal.utils.ICorrector;
import cat.nyaa.infiniteinfernal.utils.Utils;
import cat.nyaa.infiniteinfernal.utils.support.WorldGuardUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Particle;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class MainLoopTask {
    private static List<BukkitRunnable> runnables = new ArrayList<>();

    public static void start() {
        stop();
        String dementia = InfPlugin.plugin.config.addEffects.get("dementia");
        if (dementia != null) {
            iCorrector = CorrectionParser.parseStr(dementia);
        }

        final Config config = InfPlugin.plugin.config();
        final List<World> enabledWorlds = InfPlugin.plugin.config().getEnabledWorlds();
        enabledWorlds.forEach(world -> {
            if (world == null) {
                Bukkit.getLogger().log(Level.WARNING, "world don't exists, skipping");
                return;
            }

            int interval = config.getMobTickInterval(world);
            int mobSpawnInteval = config.getMobSpawnInterval(world);
            MainLoopRunnable runnable = new MainLoopRunnable(world, interval);
            runnables.add(runnable);
            runnable.runTaskTimer(InfPlugin.plugin, 0, interval);

            // Use distributed spawn task instead of processing all players at once
            DistributedSpawnTask spawnTask = new DistributedSpawnTask(world, mobSpawnInteval);
            runnables.add(spawnTask);
            spawnTask.runTaskTimer(InfPlugin.plugin, 0, 1); // Run every tick for distributed processing

            // Add stuck mob teleportation task
            WorldConfig worldConfig = config.worlds.get(world.getName());
            if (worldConfig != null && worldConfig.stuckMobConfig.enabled) {
                StuckMobTask stuckMobTask = new StuckMobTask(world, worldConfig.stuckMobConfig.checkIntervalTicks);
                runnables.add(stuckMobTask);
                stuckMobTask.runTaskTimer(InfPlugin.plugin, 20, 1); // Run every tick for distributed processing
            }
        });

        BukkitRunnable nearbyRunnable = new BukkitRunnable() {
            @Override
            public void run() {
                MobManager instance = MobManager.instance();
                instance.updateNearbyList();
            }
        };
        nearbyRunnable.runTaskTimerAsynchronously(InfPlugin.plugin, 0, 10);
        runnables.add(nearbyRunnable);
    }

    public static void stop() {
        if (!runnables.isEmpty()) {
            runnables.forEach(BukkitRunnable::cancel);
            runnables.clear();
        }
    }

    private static ICorrector iCorrector = null;

    private static void mobEffect(IMob iMob) {
        MobManager mobManager = MobManager.instance();
        LivingEntity entity = iMob.getEntity();
        if (entity == null || entity.isDead()) {
            mobManager.removeMob(iMob, false);
        }
        iMob.showParticleEffect();
        iMob.autoRetarget();
        if (iMob.isDynamicHealth()) {
            iMob.tweakHealth();
        }
        List<Player> playersNearMob = mobManager.getPlayersNearMob(iMob);
        if (iCorrector != null) {
            EntityEquipment equipment = iMob.getEntity().getEquipment();
            ItemStack itemInMainHand = null;
            if (equipment != null) {
                itemInMainHand = equipment.getItemInMainHand();
            }
            double correction = iCorrector.getCorrection(iMob.getEntity(), itemInMainHand);
            if (Utils.possibility(correction / 100d)) {
                return;
            }
        }
        if (playersNearMob.size() == 0) {
            mobManager.removeMob(iMob, false);
            return;
        }
        LivingEntity target = iMob.getTarget();
        if (target == null || !target.getWorld().equals(iMob.getEntity().getWorld())) {
            // No valid target - increment counter and check for despawn
            iMob.incrementNoTargetTicks();
            return;
        }
        // Valid target found - reset counter
        iMob.resetNoTargetTicks();

        List<IAbilitySet> abilities = iMob.getAbilities().stream()
                .filter(IAbilitySet::containsActive)
                .collect(Collectors.toList());
        IAbilitySet iAbilitySet = Utils.weightedRandomPick(abilities);
        if (iAbilitySet == null) {
            return;
        }
        iAbilitySet.getAbilitiesInSet().stream()
                .filter(iAbility -> iAbility instanceof AbilityActive)
                .map(iAbility -> ((AbilityActive) iAbility))
                .forEach(abilityTick -> abilityTick.active(iMob));

    }

    static class MainLoopRunnable extends BukkitRunnable {

        private final World world;
        AsyncInfernalTicker infernalTicker;
        BukkitRunnable runnable;

        public MainLoopRunnable(World world, int interval) {
            this.world = world;
            this.infernalTicker = new AsyncInfernalTicker(interval);
            runnable = new BukkitRunnable() {
                @Override
                public void run() {
                    infernalTicker.tick();
                }
            };
            runnable.runTaskTimer(InfPlugin.plugin, 0, 1);
        }

        @Override
        public void run() {
            if (!InfPlugin.plugin.config().isEnabledInWorld(world))return;

            List<IMob> mobs = MobManager.instance().getMobsInWorld(world);
            if (!mobs.isEmpty()) {
                infernalTicker.submitInfernalTickMobs(mobs);
            }
        }

        @Override
        public synchronized void cancel() throws IllegalStateException {
            super.cancel();
            runnable.cancel();
        }
    }

    private static class AsyncInfernalTicker {
        private final BukkitScheduler scheduler;
        private final int interval;
        Queue<IMob> mobEffectQueue;
        private int lastTickMobCount = 0;
        private int nextTickTasks = 0;
        private boolean previousTaskFinished = true;
        private boolean overload = false;
        private int maxQueueSize = Integer.MAX_VALUE;

        AsyncInfernalTicker(int interval) {
            this.interval = interval;
            scheduler = Bukkit.getScheduler();
            mobEffectQueue = new LinkedList<>();
        }

        void tick() {
            if (mobEffectQueue.isEmpty()) return;
            previousTaskFinished = false;
            for (int i = 0; i < nextTickTasks; i++) {
                if (mobEffectQueue.isEmpty()) return;
                IMob iMob = mobEffectQueue.poll();
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        mobEffect(iMob);
                    }
                }.runTask(InfPlugin.plugin);
            }
            end();
        }

        private void end() {
            previousTaskFinished = true;
        }

        public void submitInfernalTickMobs(List<IMob> mobs) {
            if (mobs == null || mobs.isEmpty()) return;
            mobs.forEach(mob -> mobEffectQueue.offer(mob));
            nextTickTasks = (int) Math.ceil((mobs.size()) / (double) interval);
        }

        public int getLastTickMobCount() {
            return lastTickMobCount;
        }

        public int getMaxQueueSize() {
            return maxQueueSize;
        }
    }

    /**
     * Distributed spawn task that spreads player spawn checks across multiple ticks.
     * Ensures every player gets checked for spawning while minimizing per-tick workload.
     */
    private static class DistributedSpawnTask extends BukkitRunnable {

        private final World world;
        private final int spawnInterval;
        private final Queue<Player> playerQueue = new ConcurrentLinkedQueue<>();
        private int tickCounter = 0;
        private int playersPerTick = 1;

        public DistributedSpawnTask(World world, int spawnInterval) {
            this.world = world;
            this.spawnInterval = spawnInterval;
        }

        @Override
        public void run() {
            if (!InfPlugin.plugin.config.isEnabledInWorld(world)) return;

            tickCounter++;

            // Refill queue at the start of each spawn cycle
            if (tickCounter >= spawnInterval) {
                tickCounter = 0;
                refillPlayerQueue();
            }

            // Process players from queue if not empty
            if (!playerQueue.isEmpty()) {
                processPlayers();
            }
        }

        private void refillPlayerQueue() {
            playerQueue.clear();

            // Check world mob limit before processing any players
            int maxMobInWorld = InfPlugin.plugin.config().getMaxMobInWorld(world);
            int currentMobs = MobManager.instance().getMobsInWorld(world).size();
            if (currentMobs >= maxMobInWorld) {
                return; // World is at capacity, skip this spawn cycle
            }

            List<Player> players = world.getPlayers().stream()
                    .filter(player -> !player.getGameMode().equals(GameMode.SPECTATOR))
                    .collect(Collectors.toList());

            if (players.isEmpty()) {
                return;
            }

            // Shuffle players to ensure fair spawn distribution
            Collections.shuffle(players);
            playerQueue.addAll(players);

            // Calculate how many players to process per tick to spread across the interval
            // Leave some buffer ticks at the end (use 80% of interval)
            int ticksAvailable = Math.max(1, (int) (spawnInterval * 0.8));
            playersPerTick = Math.max(1, (int) Math.ceil((double) players.size() / ticksAvailable));
        }

        private void processPlayers() {
            int maxMobInWorld = InfPlugin.plugin.config().getMaxMobInWorld(world);

            for (int i = 0; i < playersPerTick && !playerQueue.isEmpty(); i++) {
                // Re-check world limit
                if (MobManager.instance().getMobsInWorld(world).size() >= maxMobInWorld) {
                    playerQueue.clear();
                    return;
                }

                Player player = playerQueue.poll();
                if (player == null || !player.isOnline()) continue;

                if (InfPlugin.wgEnabled) {
                    if (WorldGuardUtils.instance().isPlayerInProtectedRegion(player)) {
                        continue;
                    }
                }

                InfPlugin.plugin.spawnControler.spawnIMob(player, false);
            }
        }
    }

    /**
     * Distributed task for detecting and teleporting stuck mobs.
     * Spreads work across ticks to minimize impact on game thread.
     */
    private static class StuckMobTask extends BukkitRunnable {

        private final World world;
        private final int checkInterval;
        private final Queue<IMob> mobQueue = new ConcurrentLinkedQueue<>();
        private int tickCounter = 0;
        private int mobsPerTick = 1;

        public StuckMobTask(World world, int checkInterval) {
            this.world = world;
            this.checkInterval = checkInterval;
        }

        @Override
        public void run() {
            if (!InfPlugin.plugin.config.isEnabledInWorld(world)) return;

            tickCounter++;

            // Refill queue at the start of each check cycle
            if (tickCounter >= checkInterval) {
                tickCounter = 0;
                refillMobQueue();
            }

            // Process mobs from queue if not empty
            if (!mobQueue.isEmpty()) {
                processMobs();
            }
        }

        private void refillMobQueue() {
            mobQueue.clear();

            List<IMob> mobs = MobManager.instance().getMobsInWorld(world);
            if (mobs.isEmpty()) return;

            mobQueue.addAll(mobs);

            // Calculate how many mobs to process per tick
            int ticksAvailable = Math.max(1, (int) (checkInterval * 0.8));
            mobsPerTick = Math.max(1, (int) Math.ceil((double) mobs.size() / ticksAvailable));
        }

        private void processMobs() {
            WorldConfig worldConfig = InfPlugin.plugin.config().worlds.get(world.getName());
            if (worldConfig == null || !worldConfig.stuckMobConfig.enabled) {
                mobQueue.clear();
                return;
            }

            WorldConfig.StuckMobConfig stuckConfig = worldConfig.stuckMobConfig;

            for (int i = 0; i < mobsPerTick && !mobQueue.isEmpty(); i++) {
                IMob iMob = mobQueue.poll();
                if (iMob == null) continue;

                LivingEntity entity = iMob.getEntity();
                if (entity == null || entity.isDead()) continue;

                // Update position tracking and check if stuck
                iMob.updateLastPosition();
                if (iMob.isStuck(stuckConfig.movementThreshold)) {
                    // Increment stuck ticks
                    if (iMob instanceof CustomMob) {
                        ((CustomMob) iMob).incrementStuckTicks(checkInterval);
                    }

                    // Check if stuck for too long
                    if (iMob.getStuckTicks() >= stuckConfig.stuckThresholdTicks) {
                        // Try to teleport to target
                        teleportStuckMob(iMob, stuckConfig);
                    }
                }
            }
        }

        private void teleportStuckMob(IMob iMob, WorldConfig.StuckMobConfig stuckConfig) {
            LivingEntity entity = iMob.getEntity();
            LivingEntity target = iMob.getTarget();

            if (entity == null || entity.isDead()) return;
            if (target == null || target.isDead() || !(target instanceof Player)) return;

            Player targetPlayer = (Player) target;
            Location targetLoc = targetPlayer.getLocation();

            // Find a valid teleport location near the target
            Location teleportLoc = findValidTeleportLocation(
                    targetLoc,
                    stuckConfig.teleportDistanceMin,
                    stuckConfig.teleportDistanceMax
            );

            if (teleportLoc != null) {
                Location fromLoc = entity.getLocation().clone();

                // Force teleport - bypass other plugins that might cancel it
                // First try normal teleport
                boolean teleported = entity.teleport(teleportLoc);

                // If teleport was cancelled or failed, force reposition
                if (!teleported || entity.getLocation().distanceSquared(teleportLoc) > 4) {
                    forceReposition(entity, teleportLoc);
                }

                // Spawn particle trail from old position to new position
                if (stuckConfig.particleTrail.enabled) {
                    spawnTeleportParticleTrail(fromLoc, teleportLoc, stuckConfig.particleTrail);
                }

                iMob.resetStuckTracking();
            }
        }

        /**
         * Force repositions an entity, bypassing teleport event cancellation.
         * Uses velocity reset and direct location setting.
         */
        private void forceReposition(LivingEntity entity, Location targetLoc) {
            // Reset velocity to prevent any momentum issues
            entity.setVelocity(new Vector(0, 0, 0));

            // Remove from any vehicle first
            if (entity.isInsideVehicle()) {
                entity.leaveVehicle();
            }

            // Eject any passengers
            entity.eject();

            // Use teleport with a scheduled retry if initial fails
            new BukkitRunnable() {
                int attempts = 0;
                @Override
                public void run() {
                    if (entity.isDead() || attempts >= 3) {
                        this.cancel();
                        return;
                    }
                    attempts++;

                    // Set fall distance to 0 to prevent fall damage
                    entity.setFallDistance(0);
                    entity.setVelocity(new Vector(0, 0, 0));

                    // Try teleport again
                    entity.teleport(targetLoc);

                    // Check if successfully moved
                    if (entity.getLocation().distanceSquared(targetLoc) < 4) {
                        this.cancel();
                    }
                }
            }.runTaskTimer(InfPlugin.plugin, 1, 2);
        }

        /**
         * Spawns a directed particle trail between two locations.
         * Particles are spawned gradually along the path.
         */
        private void spawnTeleportParticleTrail(Location from, Location to, WorldConfig.ParticleTrailConfig config) {
            World world = from.getWorld();
            if (world == null || !world.equals(to.getWorld())) return;

            // Parse particle type
            Particle particleType;
            try {
                particleType = Particle.valueOf(config.particleType.toUpperCase());
            } catch (IllegalArgumentException e) {
                particleType = Particle.WITCH;
            }

            double dx = to.getX() - from.getX();
            double dy = to.getY() - from.getY() + 1; // Adjust to eye level
            double dz = to.getZ() - from.getZ();
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (distance < 1) return;

            // Normalize direction
            dx /= distance;
            dy /= distance;
            dz /= distance;

            double step = 1.0 / config.particlesPerBlock;
            final Particle finalParticleType = particleType;
            final double finalDx = dx;
            final double finalDy = dy;
            final double finalDz = dz;

            // Spawn particles over time for visual effect
            new BukkitRunnable() {
                double traveled = 0;
                int ticksElapsed = 0;

                @Override
                public void run() {
                    ticksElapsed++;

                    // Spawn several particles per tick for smooth trail
                    for (int i = 0; i < 3 && traveled < distance; i++) {
                        double x = from.getX() + finalDx * traveled;
                        double y = from.getY() + 1 + finalDy * traveled;
                        double z = from.getZ() + finalDz * traveled;

                        world.spawnParticle(
                                finalParticleType,
                                x, y, z,
                                config.particleCount,
                                config.offsetX,
                                config.offsetY,
                                config.offsetZ,
                                config.speed
                        );

                        traveled += step;
                    }

                    // Stop after reaching destination or timeout
                    if (traveled >= distance || ticksElapsed > 20) {
                        this.cancel();
                    }
                }
            }.runTaskTimer(InfPlugin.plugin, 0, 1);
        }

        /**
         * Finds a valid teleport location near the target with path accessibility.
         * Uses lightweight checks to minimize performance impact.
         */
        private Location findValidTeleportLocation(Location center, int minDistance, int maxDistance) {
            World world = center.getWorld();
            if (world == null) return null;

            // Try multiple random locations
            for (int attempt = 0; attempt < 10; attempt++) {
                // Random angle and distance
                double angle = Utils.random() * 2 * Math.PI;
                double distance = minDistance + Utils.random() * (maxDistance - minDistance);

                double x = center.getX() + Math.cos(angle) * distance;
                double z = center.getZ() + Math.sin(angle) * distance;

                int blockX = (int) Math.floor(x);
                int blockZ = (int) Math.floor(z);

                // Check if chunk is loaded
                if (!world.isChunkLoaded(blockX >> 4, blockZ >> 4)) {
                    continue;
                }

                // Find valid Y position
                Location candidate = findGroundLocation(world, blockX, blockZ, center.getBlockY());
                if (candidate == null) continue;

                // Check if there's a clear path (simple check - no solid blocks blocking)
                if (hasSimplePath(center, candidate)) {
                    // Face towards the target
                    candidate.setYaw((float) Math.toDegrees(Math.atan2(
                            center.getZ() - candidate.getZ(),
                            center.getX() - candidate.getX()
                    )) - 90);
                    return candidate;
                }
            }

            return null;
        }

        /**
         * Finds a valid ground location at the given X, Z coordinates.
         */
        private Location findGroundLocation(World world, int x, int z, int preferredY) {
            int minY = world.getMinHeight();
            int maxY = world.getMaxHeight();

            // Search around preferred Y first
            for (int offset = 0; offset <= 20; offset++) {
                for (int sign : new int[]{1, -1}) {
                    int y = preferredY + (offset * sign);
                    if (y < minY || y >= maxY - 2) continue;

                    Block block = world.getBlockAt(x, y, z);
                    Block below = block.getRelative(BlockFace.DOWN);
                    Block above = block.getRelative(BlockFace.UP);

                    // Check for valid standing position: solid below, air at feet and head
                    if (below.getType().isSolid() &&
                            !below.getType().equals(Material.LAVA) &&
                            block.getType().isAir() &&
                            above.getType().isAir()) {
                        return new Location(world, x + 0.5, y, z + 0.5);
                    }
                }
            }

            return null;
        }

        /**
         * Simple path check - verifies no solid blocks are directly between two points.
         * Uses a lightweight approach to minimize performance impact.
         */
        private boolean hasSimplePath(Location from, Location to) {
            World world = from.getWorld();
            if (world == null || !world.equals(to.getWorld())) return false;

            double dx = to.getX() - from.getX();
            double dy = to.getY() - from.getY();
            double dz = to.getZ() - from.getZ();
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (distance < 1) return true;

            // Normalize direction
            dx /= distance;
            dy /= distance;
            dz /= distance;

            // Check blocks along the path (sample every 2 blocks for performance)
            double step = 2.0;
            for (double d = step; d < distance - 1; d += step) {
                int x = (int) Math.floor(from.getX() + dx * d);
                int y = (int) Math.floor(from.getY() + dy * d + 1); // Check at eye level
                int z = (int) Math.floor(from.getZ() + dz * d);

                Block block = world.getBlockAt(x, y, z);
                if (block.getType().isSolid()) {
                    return false;
                }
            }

            return true;
        }
    }
}
