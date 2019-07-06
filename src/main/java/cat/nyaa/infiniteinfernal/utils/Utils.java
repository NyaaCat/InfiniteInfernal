package cat.nyaa.infiniteinfernal.utils;

import java.util.Iterator;
import java.util.List;
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
}
