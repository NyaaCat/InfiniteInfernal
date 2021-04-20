package cat.nyaa.infiniteinfernal.mob.bossbar;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.mob.MobManager;
import cat.nyaa.nyaacore.Pair;
import org.bukkit.Bukkit;
import org.bukkit.boss.KeyedBossBar;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.stream.Collectors;

public class BossbarManager {
    SingleBarRefreshTask task;

    public void start(int interval) {
        if (task != null) {
            if (!task.isCancelled()) {
                task.cancel();
            }
        }
        task = new SingleBarRefreshTask();
        task.runTaskTimer(InfPlugin.plugin, 0, interval);
    }

    public static void update(IMob iMob) {
        KeyedBossBar bossBar = iMob.getBossBar();
        if (bossBar == null){
            return;
        }
        iMob.updateBossBar(bossBar, iMob.getEntity());
        if (iMob.getEntity().isDead()) {
            bossBar.removeAll();
        }
    }
}
