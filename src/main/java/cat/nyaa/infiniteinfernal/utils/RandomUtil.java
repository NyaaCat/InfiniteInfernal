package cat.nyaa.infiniteinfernal.utils;

import cat.nyaa.infiniteinfernal.mob.IMob;
import org.bukkit.entity.LivingEntity;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

public class RandomUtil {
    private static Random random = new Random();
    public static double random() {
        return random.nextDouble();
    }

    public static Double random(double lower, double upper) {
        return random.nextDouble() * (upper - lower) + lower;
    }


    public static <T> T randomPick(List<T> list) {
        return list.isEmpty() ? null : list.get(random.nextInt(list.size()));
    }

    public static <T extends Weightable> T weightedRandomPick(List<T> list) {
        int sum = list.stream().mapToInt(Weightable::getWeight)
                .sum();
        if (sum == 0) {
            if (list.size() > 0) return list.get(0);
            else return null;
        }
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
        return list.get(list.size() - 1);
    }

    public static <T> T weightedRandomPick(Map<T, Integer> weightMap) {
        int sum = weightMap.values().stream().mapToInt(Integer::intValue)
                .sum();
        if (sum == 0) {
            return weightMap.keySet().stream().findFirst().orElse(null);
        }
        int selected = random.nextInt(sum);
        Iterator<Map.Entry<T, Integer>> iterator = weightMap.entrySet().stream().iterator();
        int count = 0;
        Map.Entry<T, Integer> next = null;
        while (iterator.hasNext()) {
            next = iterator.next();
            int nextCount = count + next.getValue();
            if (count <= selected && nextCount > selected) {
                break;
            }
            count = nextCount;
        }
        return next == null ? null : next.getKey();
    }

    public static boolean possibility(double x) {
        if (x <= 0) return false;
        if (x >= 1) return true;
        return random.nextDouble() < x;
    }

    public static LivingEntity randomSelectTarget(IMob iMob, double range) {
        return randomPick(Utils.getValidTargets(iMob, iMob.getEntity().getNearbyEntities(range, range, range)).collect(Collectors.toList()));
    }
}
