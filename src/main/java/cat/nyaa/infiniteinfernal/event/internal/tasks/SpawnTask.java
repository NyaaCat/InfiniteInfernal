package cat.nyaa.infiniteinfernal.event.internal.tasks;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.utils.support.WorldGuardUtils;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.stream.Collectors;

public class SpawnTask extends BukkitRunnable {
    private final World world;
    private final int mobSpawnInteval;

    public SpawnTask(World world, int mobSpawnInteval) {
        super();
        this.world = world;
        this.mobSpawnInteval = mobSpawnInteval;
    }

    @Override
    public void run() {
        if (!InfPlugin.plugin.config().isEnabledInWorld(world)) return;
        List<Player> players = world.getPlayers().stream()
                .filter(player -> !player.getGameMode().equals(GameMode.SPECTATOR))
                .collect(Collectors.toList());
        players.stream().forEach(player -> {
            if (InfPlugin.wgEnabled) {
                if (WorldGuardUtils.instance().isPlayerInProtectedRegion(player)) {
                    return;
                }
            }
            InfPlugin.plugin.getSpawnControler().spawnIMob(player, false);
        });
    }
}