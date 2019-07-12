package cat.nyaa.infiniteinfernal.utils;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.configs.IllegalConfigException;
import cat.nyaa.infiniteinfernal.mob.IMob;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.stream.Collectors;
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

    public static LivingEntity randomSelectTarget(IMob iMob, double range){
        return Utils.randomPick(Utils.getValidTargets(iMob, iMob.getEntity().getNearbyEntities(range, range, range)).collect(Collectors.toList()));
    }

    //todo: implement another to Aggro system
    public static Stream<LivingEntity> getValidTargets(IMob iMob, Collection<Entity> nearbyEntities) {
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

    public static void doEffect(String effect, LivingEntity target, int duration, int amplifier, String ability) {
        PotionEffectType eff = PotionEffectType.getByName(effect);
        if (eff!=null) {
            target.addPotionEffect(eff.createEffect(duration, amplifier));
        }else{
            throw new IllegalConfigException("effect " + effect+ " in ability "+ ability +" don't exists");
        }
    }

    public static PotionEffectType parseEffect(String effect, String ability) {
        PotionEffectType eff = PotionEffectType.getByName(effect);
        if (eff!=null) {
            return eff;
        }else{
            throw new IllegalConfigException("effect " + effect+ " in ability "+ ability +" don't exists");
        }
    }

    public static Double random(double lower, double upper) {
        return random.nextDouble() * (upper-lower) + lower;
    }


    private static final Vector x_axis = new Vector(1, 0, 0);
    private static final Vector y_axis = new Vector(0, 1, 0);
    private static final Vector z_axis = new Vector(0, 0, 1);

    public static Vector cone(Vector direction, double cone) {
        double phi = Utils.random() * 360;
        double theta = Utils.random() * cone;
        Vector clone = direction.clone();
        Vector crossP;

        if (clone.length() == 0) return direction;

        if (clone.getX() != 0 && clone.getZ() != 0) {
            crossP = clone.getCrossProduct(y_axis);
        } else if (clone.getX() != 0 && clone.getY() != 0) {
            crossP = clone.getCrossProduct(z_axis);
        } else {
            crossP = clone.getCrossProduct(x_axis);
        }
        crossP.normalize();

        clone.add(crossP.multiply(Math.tan(Math.toRadians(theta))));
        clone.rotateAroundNonUnitAxis(direction, Math.toRadians(phi));
        return clone;
    }

    public static Object parseExtraData(String extraData) {
        try {
            String[] split = extraData.split(",", 4);
            int r = Integer.parseInt(split[0]);
            int g = Integer.parseInt(split[1]);
            int b = Integer.parseInt(split[2]);
            float size = Float.parseFloat(split[3]);
            return new Particle.DustOptions(Color.fromRGB(r, g, b), size);
        }catch (Exception e){
            return null;
        }
    }
}
