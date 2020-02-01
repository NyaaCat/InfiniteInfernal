package cat.nyaa.infiniteinfernal.controler;

import cat.nyaa.infiniteinfernal.mob.IMob;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;

public interface ISpawnControler {
    boolean canSpawn(World world, Location location);
    boolean canSpawnNearPlayer(Player player, Location location);
    boolean canIMobAutoSpawn(World world);
    boolean canVanillaAutoSpawn(World world);

    int getMaxSpawnAmount(Player player);
    int getMaxSpawnAmount(World world);
    int getMaxSpawnDistance(World world);
    int getMinSpawnDistance(World world);

    void setVanillaAutoSpawn(World world, boolean flag);

    IMob spawnIMob(Player player, boolean force);
    IMob spawnIMob(Location location, boolean force);
    LivingEntity spawnVanilla(Player player, boolean force);

    void handleSpawnEvent(CreatureSpawnEvent event);
    void handleMobDeath(EntityDeathEvent event);
}
