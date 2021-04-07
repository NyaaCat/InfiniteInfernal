package cat.nyaa.infiniteinfernal.controler;

import cat.nyaa.infiniteinfernal.I18n;
import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.configs.WorldConfig;
import cat.nyaa.infiniteinfernal.utils.Utils;
import cat.nyaa.nyaacore.Message;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

public class FirenlyFireControler {
    private Map<Player, Map<Player, FriendlyFireTask>> taskMap = new LinkedHashMap<>();

    private static FirenlyFireControler instance;

    public static FirenlyFireControler instance() {
        if (instance == null) {
            synchronized (FirenlyFireControler.class) {
                if (instance == null) {
                    instance = new FirenlyFireControler();
                }
            }
        }
        return instance;
    }

    public void onFriendlyFire(Player damager, Player victim, double damage) {
        Map<Player, FriendlyFireTask> taskMap = this.taskMap.computeIfAbsent(damager, player -> new LinkedHashMap<>());
        FriendlyFireTask friendlyFireTask = taskMap.computeIfAbsent(victim, player -> new FriendlyFireTask(damager, victim));
        friendlyFireTask.submit(damage);
    }

    private class FriendlyFireTask {
        private final Player damager;
        private final Player victim;

        private double dps = 0;
        private CancelTask cancelTask;

        public FriendlyFireTask(Player damager, Player victim) {
            this.damager = damager;
            this.victim = victim;
        }


        public void submit(double damage) {
            World world = damager.getWorld();
            WorldConfig worldConfig = InfPlugin.plugin.config().worlds.get(world.getName());
            WorldConfig.FriendlyFireConfig friendlyFireConfig = worldConfig.friendlyFireConfig;
            if (!friendlyFireConfig.enable) {
                return;
            }
            if (cancelTask != null) {
                cancelTask.cancel();
            }
            cancelTask = new CancelTask();
            cancelTask.runTaskLater(InfPlugin.plugin, 100);
            dps += damage;
            if (dpsBetween(0, 10)) {
                hint();
            } else if (dpsBetween(10, 20)) {
                effect(1);
            } else if (dpsBetween(20, 40)) {
                effect(2);
            } else if (dps > 40) {
                effect(3);
            }
        }

        private void effect(int multiplier) {

            World world = damager.getWorld();
            WorldConfig worldConfig = InfPlugin.plugin.config().worlds.get(world.getName());
            if (worldConfig != null) {
                WorldConfig.FriendlyFireConfig friendlyFireConfig = worldConfig.friendlyFireConfig;
                if (!friendlyFireConfig.enable) {
                    return;
                }
                String effect = friendlyFireConfig.effect;
                String[] split = effect.split(":");
                try {
                    String effectName = split[0].toUpperCase();
                    int amplifier = Integer.parseInt(split[1]) * multiplier;
                    int duration = Integer.parseInt(split[2]) * multiplier;
                    Utils.doEffect(effectName, damager, duration, amplifier, "friendly fire");
                    String s = "friendly_fire.lv" + multiplier;
                    new Message(I18n.format(s))
                            .send(damager);
                } catch (Exception e) {
                    Bukkit.getLogger().log(Level.WARNING, "invalid friendly fire config: \"" + effect + "\"");
                }
            }
        }

        private void hint() {
            new Message(I18n.format("friendly_fire.hint"))
                    .send(damager);
        }

        private boolean dpsBetween(int lower, int upper) {
            return dps > lower && dps <= upper;
        }

        private class CancelTask extends BukkitRunnable {
            @Override
            public void run() {
                Map<Player, FriendlyFireTask> playerFriendlyFireTaskMap = instance.taskMap.computeIfAbsent(damager, player -> new LinkedHashMap<>());
                playerFriendlyFireTaskMap.remove(victim);
            }
        }
    }
}
