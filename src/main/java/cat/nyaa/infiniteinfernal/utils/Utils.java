package cat.nyaa.infiniteinfernal.utils;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.mob.IMob;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;

public class Utils {
    private static Random random = new Random();

    public static <T> T randomPick(List<T> list){
        return list.get(random.nextInt(list.size()));
    }

    public static <T extends Weightable> T weightedRandomPick(List<T> list){
        int sum = list.stream().parallel()
                .mapToInt(Weightable::getWeight)
                .sum();
        int selected = random.nextInt(sum);
        Iterator<Integer> iterator = list.stream().mapToInt(Weightable::getWeight).iterator();
        int count = 0;
        int selectedItem = 0;
        while (iterator.hasNext()) {
            Integer next = iterator.next();
            int nextCount = count + next;
            if (count <= selected && nextCount > selected) {
                return list.get(selectedItem);
            }
            count = nextCount;
            selectedItem++;
        }
        return list.get(list.size()-1);
    }

    public static <T> T weightedRandomPick(Map<T, Integer> weightMap){
        int sum = weightMap.values().stream().parallel()
                .mapToInt(Integer::intValue)
                .sum();
        int selected = random.nextInt(sum);
        Iterator<Map.Entry<T, Integer>> iterator = weightMap.entrySet().stream().iterator();
        int count = 0;
        Map.Entry<T, Integer> next = null;
        while (iterator.hasNext()) {
            next = iterator.next();
            int nextCount = count + next.getValue();
            if (count <= selected && nextCount > selected) {
            }
            count = nextCount;
        }
        return next == null ? null : next.getKey();
    }

    public static String getTaggedName(String nameTag, String name, int level) {
        String levelPrefix = InfPlugin.plugin.config().levelConfigs.get(level).prefix;
        return nameTag.replaceAll("\\{level\\.prefix}", levelPrefix)
                .replaceAll("\\{mob\\.name}", name)
                .replaceAll("\\{level\\.level}", String.valueOf(level));
    }

    public static boolean possibility(double x) {
        if (x <= 0) return false;
        if (x >= 1) return true;
        return random.nextDouble() < x;
    }

    public static Vector unitDirectionVector(Vector from, Vector to) {
        Vector vec = to.clone().subtract(from);
        if (!Double.isFinite(vec.getX())) vec.setX(0D);
        if (!Double.isFinite(vec.getY())) vec.setY(0D);
        if (!Double.isFinite(vec.getZ())) vec.setZ(0D);
        if (vec.lengthSquared() == 0) return new Vector(0,0,0);
        return vec.normalize();
    }

    public static boolean validGamemode(Player entity) {
        GameMode gameMode = entity.getGameMode();
        return gameMode.equals(GameMode.SURVIVAL) || gameMode.equals(GameMode.ADVENTURE);
    }

    public static void removeEntityLater(Entity ent, int i) {
        new BukkitRunnable(){
            @Override
            public void run() {
                ent.remove();
            }
        }.runTaskLater(InfPlugin.plugin, i);
    }

    public static double random() {
        return random.nextDouble();
    }

    public static Stream<LivingEntity> getValidTarget(IMob iMob, List<Entity> nearbyEntities) {
        return nearbyEntities.stream()
                .filter(entity -> (entity instanceof Player && validGamemode((Player) entity)) || (entity instanceof LivingEntity && iMob.isTarget((LivingEntity) entity)))
                .map(entity -> ((LivingEntity) entity));
    }

    public static Location randomSpawnLocation(Location center, double innerRange, double outerRange){
        for (int i = 0; i < 20; i++) {
            Location targetLocation = randomLocation(center, innerRange, outerRange);
            Location validSpawnLocationInY = findValidSpawnLocationInY(targetLocation);
            if (!validSpawnLocationInY.equals(center))return validSpawnLocationInY;
        }
        return center;
    }

    private static Location findValidSpawnLocationInY(Location targetLocation) {
        for (int j = -10; j < 10; j++) {
            Location clone = targetLocation.clone().add(0, j, 0);
            if (isValidLocation(clone)){
                return clone;
            }
        }
        return targetLocation;
    }

    public static Location randomLocation(Location center, double innerRange, double outerRange){
        double r = innerRange + random() * outerRange;
        double theta = Math.toRadians(random.nextInt(360));
        Location targetLocation = center.clone();
        targetLocation.add(new Vector(r * Math.cos(theta) , 0 , r * Math.sin(theta)));
        return targetLocation;
    }

    private static boolean isValidLocation(Location targetLocation) {
        Block block = targetLocation.getBlock();
        Block lowerBlock = block.getRelative(BlockFace.DOWN);
        Block upperBlock = block.getRelative(BlockFace.UP);
        return !block.getType().isSolid() && !upperBlock.getType().isSolid() && lowerBlock.getType().isSolid() && block.getType().isBlock();
    }
}
