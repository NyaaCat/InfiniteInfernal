package cat.nyaa.infiniteinfernal;

import cat.nyaa.infiniteinfernal.ability.AbilityActive;
import cat.nyaa.infiniteinfernal.ability.IAbilitySet;
import cat.nyaa.infiniteinfernal.configs.IllegalConfigException;
import cat.nyaa.infiniteinfernal.configs.WorldConfig;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.mob.MobManager;
import cat.nyaa.infiniteinfernal.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.*;

public class MainLoopTask {
    private static List<MainLoopRunnable> runnables = new ArrayList<>();

    public static void start(){
        stop();
        Map<String, WorldConfig> worlds = InfPlugin.plugin.config.worlds;
        worlds.forEach((wn, value) -> {
            World world = Bukkit.getWorld(wn);
            if (world == null) throw new IllegalConfigException("world " + wn + " don't exists");
            int interval = value.mobTickInterval;
            MainLoopRunnable runnable = new MainLoopRunnable(world, interval);
            runnables.add(runnable);
            runnable.runTaskTimer(InfPlugin.plugin, 0, interval);
        });
    }

    public static void stop(){
        if (!runnables.isEmpty()) {
            runnables.forEach(BukkitRunnable::cancel);
            runnables.clear();
        }
    }

    private static void mobEffect(IMob iMob) {
        iMob.showParticleEffect();
        iMob.autoRetarget();
        if (iMob.getEntity().isDead()){
            MobManager.instance().removeMob(iMob);
        }
        List<IAbilitySet> abilities = iMob.getAbilities();
        Utils.weightedRandomPick(abilities).getAbilitiesInSet().stream()
                .filter(iAbility -> iAbility instanceof AbilityActive)
                .map(iAbility -> ((AbilityActive) iAbility))
                .forEach(abilityTick -> abilityTick.active(iMob));
    }

    static class MainLoopRunnable extends BukkitRunnable {

        private final World world;
        private final int interval;
        AsyncInfernalTicker infernalTicker;

        public MainLoopRunnable(World world, int interval) {
            this.world = world;
            this.interval = interval;
            this.infernalTicker = new AsyncInfernalTicker(interval);
        }

        @Override
        public void run() {
            List<IMob> mobs = MobManager.instance().getMobsInWorld(world);
            if (!mobs.isEmpty()) {
                infernalTicker.submitInfernalTickMobs(mobs);
                infernalTicker.tick();
            }
            world.getPlayers().forEach(player -> {
                for (int i = 0; i < 5; i++) {
                    InfPlugin.plugin.spawnControler.spawnIMob(player, false);
                }
            });
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
                mobEffect(iMob);
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
