package cat.nyaa.infiniteinfernal.event.internal.tasks;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.mob.MobManager;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class MobTasks extends BukkitRunnable {
    private final World world;
    AsyncInfernalTicker infernalTicker;
    BukkitRunnable runnable;

    public MobTasks(World world) {
        this.world = world;
        int interval = InfPlugin.plugin.config().mobTickInterval;
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
        if (!InfPlugin.plugin.config().isEnabledInWorld(world)) return;

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