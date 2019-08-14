package cat.nyaa.infiniteinfernal;

import cat.nyaa.infiniteinfernal.ability.AbilityActive;
import cat.nyaa.infiniteinfernal.ability.IAbilitySet;
import cat.nyaa.infiniteinfernal.configs.IllegalConfigException;
import cat.nyaa.infiniteinfernal.configs.WorldConfig;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.mob.MobManager;
import cat.nyaa.infiniteinfernal.utils.Utils;
import cat.nyaa.infiniteinfernal.utils.WorldGuardUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class MainLoopTask {
    private static List<BukkitRunnable> runnables = new ArrayList<>();

    public static void start() {
        stop();
        Map<String, WorldConfig> worlds = InfPlugin.plugin.config.worlds;
        worlds.forEach((wn, value) -> {
            World world = Bukkit.getWorld(wn);
            if (world == null) {
                Bukkit.getLogger().log(Level.WARNING, "world " + wn + " don't exists, skipping");
                return;
            }

            int interval = value.mobTickInterval;
            MainLoopRunnable runnable = new MainLoopRunnable(world, interval);
            runnables.add(runnable);
            runnable.runTaskTimer(InfPlugin.plugin, 0, interval);
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

    private static void mobEffect(IMob iMob) {
        MobManager mobManager = MobManager.instance();
        LivingEntity entity = iMob.getEntity();
        if (entity == null || entity.isDead()) {
            mobManager.removeMob(iMob, false);
            return;
        }
        iMob.showParticleEffect();
        iMob.autoRetarget();
        List<Player> playersNearMob = mobManager.getPlayersNearMob(iMob);
        if (playersNearMob.size() == 0 ) {
            mobManager.removeMob(iMob, false);
        }
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
            List<IMob> mobs = MobManager.instance().getMobsInWorld(world);
            if (!mobs.isEmpty()) {
                infernalTicker.submitInfernalTickMobs(mobs);
            }
            List<Player> players = world.getPlayers().stream().filter(player -> !player.getGameMode().equals(GameMode.SPECTATOR)).collect(Collectors.toList());
            players.stream().forEach(player -> {
                if (InfPlugin.wgEnabled){
                    if(WorldGuardUtils.instance().isPlayerInProtectedRegion(player)){
                        return;
                    }
                }
                AtomicInteger tried = new AtomicInteger(0);
                class SpawnTask extends BukkitRunnable{
                    @Override
                    public void run() {
                        IMob iMob = InfPlugin.plugin.spawnControler.spawnIMob(player, false);
                        if (iMob == null){
                            if ((tried.getAndAdd(1) >= 20)) {
                                return;
                            }
                            new SpawnTask().runTaskLater(InfPlugin.plugin, 1);
                        }
                    }
                }
                new SpawnTask().runTask(InfPlugin.plugin);
            });
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
//            if (!previousTaskFinished && !overload){
//                getLogger().log(Level.WARNING, "previous server active didn't finished, maybe there's too much task to do.");
//                overload = true;
//            }else if (overload){
//
//            }
            previousTaskFinished = false;
            for (int i = 0; i < nextTickTasks; i++) {
                if (mobEffectQueue.isEmpty()) return;
                IMob iMob = mobEffectQueue.poll();
                new BukkitRunnable(){
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
}
