package cat.nyaa.infiniteinfernal.utils;

import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.logging.Level;

public class WorldGuardUtils {
    public static boolean enabled = false;
    private static RegionContainer regionContainer;
    private static WorldGuardPlugin wgInst;

    public static void init(){
        try{
            wgInst = WorldGuardPlugin.inst();
            regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
        }catch(Exception e){
            Bukkit.getLogger().log(Level.WARNING, "WorldGuard didn't detected, support will be disabled");
        }
    }

    public static boolean isProtectedRegion(Location location, Player player){
        if (!enabled){
            return false;
        }
        World world = location.getWorld();
        if (world == null){
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

    public static boolean isPlayerInProtectedRegion(Player player){
        if (!enabled){
            return false;
        }
        LocalPlayer localPlayer = wgInst.wrapPlayer(player);
        return regionContainer.createQuery().getApplicableRegions(localPlayer.getLocation()).testState(localPlayer, Flags.MOB_SPAWNING);
    }
}
