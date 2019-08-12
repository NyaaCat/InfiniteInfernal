package cat.nyaa.infiniteinfernal.utils;

import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class WorldGuardUtils {
    public static boolean enabled = false;
    private RegionContainer regionContainer;
    private WorldGuardPlugin wgInst;
    private static WorldGuardUtils instance;

    public static void init() {
        instance = new WorldGuardUtils();
        instance.wgInst = WorldGuardPlugin.inst();
        instance.regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
        enabled = true;
    }

    public static WorldGuardUtils instance(){
        return instance;
    }

    public boolean isProtectedRegion(Location location, Player player) {
        if (!enabled) {
            return false;
        }
        World world = location.getWorld();
        if (world == null) {
            throw new IllegalArgumentException("location without world");
        }
        BukkitWorld bukkitWorld = new BukkitWorld(world);
        LocalPlayer localPlayer = wgInst.wrapPlayer(player);
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        float yaw = location.getYaw();
        float pitch = location.getPitch();
        com.sk89q.worldedit.util.Location spawnLoc = new com.sk89q.worldedit.util.Location(bukkitWorld, x, y, z, yaw, pitch);
        ApplicableRegionSet spawnLocRegion = regionContainer.createQuery().getApplicableRegions(spawnLoc);
        return !(spawnLocRegion.testState(localPlayer, Flags.MOB_SPAWNING));
    }

    public boolean isPlayerInProtectedRegion(Player player) {
        if (!enabled) {
            return false;
        }
        LocalPlayer localPlayer = wgInst.wrapPlayer(player);
        return !regionContainer.createQuery().getApplicableRegions(localPlayer.getLocation()).testState(localPlayer, Flags.MOB_SPAWNING);
    }
}
