package cat.nyaa.infiniteinfernal.utils;

import cat.nyaa.infiniteinfernal.InfPlugin;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

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
}
