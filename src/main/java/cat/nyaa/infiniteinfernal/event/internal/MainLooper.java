package cat.nyaa.infiniteinfernal.event.internal;

import cat.nyaa.infiniteinfernal.Config;
import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.event.internal.tasks.MobTasks;
import cat.nyaa.infiniteinfernal.event.internal.tasks.SpawnTask;
import cat.nyaa.infiniteinfernal.mob.MobManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class MainLooper {
    private static List<BukkitRunnable> runnables = new ArrayList<>();

    public static void start() {
        stop();
        final Config config = InfPlugin.plugin.config();
        final List<World> enabledWorlds = InfPlugin.plugin.config().getEnabledWorlds();
        enabledWorlds.forEach(world -> {
            if (world == null) {
                Bukkit.getLogger().log(Level.WARNING, "world don't exists, skipping");
                return;
            }

            int mobSpawnInteval = config.mobSpawnInteval;
            MobTasks runnable = new MobTasks(world);

            SpawnTask spawnTask = new SpawnTask(world, mobSpawnInteval);
            runnables.add(spawnTask);
            spawnTask.runTaskTimer(InfPlugin.plugin, 0, mobSpawnInteval);
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
}
