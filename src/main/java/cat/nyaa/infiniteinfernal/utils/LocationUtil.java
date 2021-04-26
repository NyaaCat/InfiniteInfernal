package cat.nyaa.infiniteinfernal.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

import java.util.function.Predicate;

import static cat.nyaa.infiniteinfernal.utils.RandomUtil.random;
import static cat.nyaa.infiniteinfernal.utils.Utils.cone;

public class LocationUtil {
    public static Location randomSpawnLocationInFront(Location location, int minSpawnDistance, int maxSpawnDistance, Predicate<Location> predicate) {
        Vector direction = location.getDirection().clone();
        direction.setY(0);
        if (direction.length() > 1e-4) {
            direction = cone(direction, 30);
            for (int i = 0; i < 20; i++) {
                Location targetLocation = location.clone().add(direction.normalize().multiply(random(minSpawnDistance, maxSpawnDistance)));
                if (predicate.test(targetLocation)){
                    return targetLocation;
                }
            }
        }
        return randomSpawnLocation(location, minSpawnDistance, maxSpawnDistance, predicate);
    }

    public static Location randomSpawnLocation(Location center, double innerRange, double outerRange, Predicate<Location> predicate) {
        Location targetLocation = center;
        for (int i = 0; i < 20; i++) {
            targetLocation = randomLocation(center, innerRange, outerRange);
            if (predicate.test(targetLocation)){
                return targetLocation;
            }
        }
        return null;
    }

    public static Location randomFloorSpawnLocationInFront(Location location, int minSpawnDistance, int maxSpawnDistance) {
        Vector direction = location.getDirection().clone();
        direction.setY(0);
        if (direction.length() > 1e-4) {
            direction = cone(direction, 30);
            for (int i = 0; i < 20; i++) {
                Location targetLocation = location.clone().add(direction.normalize().multiply(random(minSpawnDistance, maxSpawnDistance)));
                Location validSpawnLocationInY = findValidSpawnLocationInY(targetLocation);
                if (validSpawnLocationInY != null) return validSpawnLocationInY;
            }
        }
        return randomFloorSpawnLocation(location, minSpawnDistance, maxSpawnDistance);
    }

    public static Location randomFloorSpawnLocation(Location center, double innerRange, double outerRange) {
        Location targetLocation = center;
        for (int i = 0; i < 20; i++) {
            targetLocation = randomLocation(center, innerRange, outerRange);
            Location validSpawnLocationInY = findValidSpawnLocationInY(targetLocation);
            if (validSpawnLocationInY != null) return validSpawnLocationInY;
        }
        if (isSky(targetLocation)) {
            return targetLocation;
        }
        return null;
    }

    private static boolean isSky(Location center) {
        Block block = center.getBlock();
        Block up = block.getRelative(BlockFace.UP);
        Block upup = up.getRelative(BlockFace.UP);
        Block down = block.getRelative(BlockFace.DOWN);

        return block.getType().equals(Material.AIR) && up.getType().equals(Material.AIR) && upup.getType().equals(Material.AIR) && down.getType().equals(Material.AIR);
    }

    public static Location randomNonNullLocation(Location center, double innerRange, double outerRange) {
        for (int i = 0; i < 30; i++) {
            Location targetLocation = randomLocation(center, innerRange, outerRange);
            Location validSpawnLocationInY = findValidSpawnLocationInY(targetLocation);
            if (validSpawnLocationInY != null) return validSpawnLocationInY;
        }
        return center;
    }

    public static Location findValidSpawnLocationInY(Location targetLocation) {
        if (targetLocation.getBlock().getType().isAir()) {
            for (int j = 0; j > -15; j--) {
                Location clone = targetLocation.clone().add(0, j, 0);
                if (isValidLocation(clone)) {
                    return clone;
                }
            }
        }
        for (int j = 0; j < 10; j++) {
            Location clone = targetLocation.clone().add(0, j, 0);
            if (isValidLocation(clone)) {
                return clone;
            }
        }
        return null;
    }

    private static Location randomLocation(Location center, double innerRange, double outerRange) {
        double r = innerRange + random() * outerRange;
        double theta = Math.toRadians(RandomUtil.random(0, 360));
        Location targetLocation = center.clone();
        targetLocation.add(new Vector(r * Math.cos(theta), 0, r * Math.sin(theta)));
        return targetLocation;
    }

    private static boolean isValidLocation(Location targetLocation) {
        World world = targetLocation.getWorld();
        if (world == null || !world.isChunkLoaded(targetLocation.getBlockX() >> 4, targetLocation.getBlockZ() >> 4)) {
            return false;
        }
        Block block = targetLocation.getBlock();
        Block lowerBlock = block.getRelative(BlockFace.DOWN);
        Block upperBlock = block.getRelative(BlockFace.UP);
        return !block.getType().isSolid() && !upperBlock.getType().isSolid() && ((lowerBlock.getType().isSolid() || block.getType().equals(Material.WATER)));
    }
}
