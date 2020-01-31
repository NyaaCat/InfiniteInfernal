package cat.nyaa.infiniteinfernal.ui;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.utils.ticker.TickEvent;
import cat.nyaa.infiniteinfernal.utils.ticker.TickTask;
import cat.nyaa.infiniteinfernal.utils.ticker.Ticker;
//import cat.nyaa.nyaacore.utils.ClassPathUtils;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class UiManager {
    private static UiManager INSTANCE;

    MaxValTicker maxValTicker;
    RegenerationTask tickTask;

    private UiManager() {
        tickTask = new RegenerationTask(tickEvent -> false);
        maxValTicker = new MaxValTicker();
        maxValTicker.start();
        Ticker.getInstance().register(tickTask);
    }

    public static UiManager getInstance() {
        if (INSTANCE == null) {
            synchronized (UiManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new UiManager();
                }
            }
        }
        return INSTANCE;
    }

    Map<UUID, BaseUi> uiMap = new LinkedHashMap<>();

    public BaseUi getUi(Player player) {
        return uiMap.computeIfAbsent(player.getUniqueId(), uuid -> new BaseUi(uuid));
    }

    public int getTick() {
        return tickTask.getTicked();
    }

    public void setPlayerStatus(Player player, PlayerStatus status, int duration) {
        BaseUi ui = getUi(player);
        ui.setStatus(status);
        new PlayerStatusUpdateTask(player, status).runTaskLater(InfPlugin.plugin, duration);
        ui.refreshIfOn(player);
    }

    public PlayerStatus getPlayerStatus(Player player) {
        return getUi(player).status;
    }

    public void refreshUi(Player player) {
        getUi(player).refreshUi(player);
    }

    public class RegenerationTask extends TickTask {
        Queue<Player> playerQueue = new LinkedList<>();

        public RegenerationTask(Predicate<TickEvent> shouldRemove) {
            super(shouldRemove);
        }

        @Override
        public void run(int ticked) {
            if (!InfPlugin.plugin.config().enableActionbarInfo) return;
            while (!playerQueue.isEmpty()) {
                Player poll = playerQueue.poll();
                BaseUi baseUi = uiMap.computeIfAbsent(poll.getUniqueId(), BaseUi::new);
                baseUi.regeneration(poll, ticked);
                baseUi.refreshIfOn(poll);
            }
            playerQueue.addAll(Bukkit.getOnlinePlayers());
        }
    }

    public static Cache<UUID, UiReceiveMode> cache = CacheBuilder.newBuilder()
            .expireAfterAccess(100, TimeUnit.SECONDS)
            .initialCapacity(Bukkit.getMaxPlayers())
            .build();

    public void invalidPlayerCache(Player player) {
        cache.invalidate(player.getUniqueId());
    }


    private class PlayerStatusUpdateTask extends BukkitRunnable {
        private Player player;
        private PlayerStatus status;

        public PlayerStatusUpdateTask(Player player, PlayerStatus status) {
            this.player = player;
            this.status = status;
        }

        @Override
        public void run() {
            BaseUi ui = getUi(player);
            if (!ui.getStatus().equals(status)) {
                return;
            }
            PlayerStatus playerStatus = ui.checkPlayer(player);
            ui.setStatus(playerStatus);
            ui.refreshIfOn(player);
        }
    }
}
