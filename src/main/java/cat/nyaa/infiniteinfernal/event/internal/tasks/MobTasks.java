package cat.nyaa.infiniteinfernal.event.internal.tasks;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.event.MobTickEvent;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.mob.MobManager;
import cat.nyaa.infiniteinfernal.utils.hook.HookUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class MobTasks extends BukkitRunnable {
    private final World world;
    AsyncMobTicker infernalTicker;

    public MobTasks(World world) {
        this.world = world;
        int interval = InfPlugin.plugin.config().mobTickInterval;
        this.infernalTicker = new AsyncMobTicker(interval);

        infernalTicker.runTaskTimer(InfPlugin.plugin, 0, 1);
    }

    @Override
    public void run() {
        if (!InfPlugin.plugin.config().isEnabledInWorld(world)) return;

        List<IMob> mobs = MobManager.instance().getMobsInWorld(world);
        if (!mobs.isEmpty()) {
            infernalTicker.submitInfernalTickMobs(mobs);
        }
    }

    @Override
    public synchronized void cancel() throws IllegalStateException {
        super.cancel();
        infernalTicker.cancel();
    }


    public class AsyncMobTicker extends BukkitRunnable{
        private final int interval;
        Queue<IMob> mobEffectQueue;
        private int nextTickTasks = 0;

        AsyncMobTicker(int interval) {
            this.interval = interval;
            mobEffectQueue = new LinkedList<>();
        }

        public void submitInfernalTickMobs(List<IMob> mobs) {
            if (mobs == null || mobs.isEmpty()) return;
            mobs.forEach(mob -> mobEffectQueue.offer(mob));
            nextTickTasks = (int) Math.ceil((mobs.size()) / (double) interval);
        }

        @Override
        public void run() {
            if (mobEffectQueue.isEmpty()) return;
            for (int i = 0; i < nextTickTasks; i++) {
                if (mobEffectQueue.isEmpty()) return;
                IMob iMob = mobEffectQueue.poll();

                MobTickEvent event = new MobTickEvent(iMob);
                Bukkit.getPluginManager().callEvent(event);
                if (event.isCanceled()){
                    continue;
                }
                mobEffect(iMob);
            }
        }

        private void mobEffect(IMob iMob) {
            HookUtil.runHook("mobTick", iMob);
        }
    }
}