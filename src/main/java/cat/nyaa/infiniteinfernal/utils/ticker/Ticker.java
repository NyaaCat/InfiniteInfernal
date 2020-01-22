package cat.nyaa.infiniteinfernal.utils.ticker;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class Ticker {
    private static Ticker INSTANCE = new Ticker();
    private JavaPlugin plugin;
    static BukkitRunnable tickerTask;
    static List<TickTask> tasks;

    private Ticker(){
        tasks = new ArrayList<>();
    }

    public void init(JavaPlugin plugin){
        this.plugin = plugin;
        if (tickerTask!=null){
            tickerTask.cancel();
        }
        List<TickTask> toRemove = new ArrayList<>();
        tickerTask = new BukkitRunnable() {
            @Override
            public void run() {
                toRemove.clear();
                tasks.forEach((task) ->{
                    TickEvent tickEvent = new TickEvent(task.getTickedAndIncrement());
                    if (task.getPredicate().test(tickEvent)){
                        toRemove.add(task);
                        return;
                    }
                    task.run();
                });
                tasks.removeAll(toRemove);
            }
        };
        tickerTask.runTaskTimer(plugin, 0, 0);
    }

    public static Ticker getInstance(){
        return INSTANCE;
    }

    public void stop() {
        if (tickerTask != null) {
            tickerTask.cancel();
            tickerTask = null;
        }
    }

    public void register(TickTask tickTask) {
        tasks.add(tickTask);
    }
}
