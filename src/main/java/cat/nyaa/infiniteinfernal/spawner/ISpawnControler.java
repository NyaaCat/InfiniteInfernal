package cat.nyaa.infiniteinfernal.spawner;

import cat.nyaa.infiniteinfernal.mob.IMob;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;

public interface ISpawnControler {
    boolean canSpawn(World world, Location location);
    boolean canSpawnNearPlayer(Player player);
    boolean canIMobAutoSpawn(World world);
    boolean canVanillaAutoSpawn(World world);

    int getMaxSpawnAmount(Player player);
    int getMaxSpawnAmount(World world);
    int getMaxSpawnDistance(World world);
    int getMinSpawnDistance(World world);

    void setIMobAutoSpawn(World world);
    void setVanillaAutoSpawn(World world);

    IMob spawnIMob(Player player);
    IMob spawnVanilla(Player player);

    void HandleSpawnEvent(CreatureSpawnEvent event);
}
