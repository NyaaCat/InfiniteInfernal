package cat.nyaa.infiniteinfernal.mob.ability.impl.active;

import cat.nyaa.infiniteinfernal.mob.ability.ActiveAbility;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.Utils;
import cat.nyaa.nyaacore.utils.NmsUtils;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;

import java.util.List;
import java.util.stream.Collectors;

public class AbilitySummonOnPlayer extends ActiveAbility {
    @Serializable
    public String nbt = "";
    @Serializable
    public int amount = 2;
    @Serializable
    public double range = 24;
    @Serializable
    public double radius = 10;
    @Serializable
    public EntityType type = EntityType.ZOMBIE;
    @Serializable
    public int maxTimes = 0;

    private int counter = 0;

    @Override
    public void active(IMob iMob) {
        if (maxTimes != 0 && counter >= maxTimes){
            return;
        }
        List<LivingEntity> candidate = Utils.getValidTargets(iMob, iMob.getEntity().getNearbyEntities(range, range, range))
                .collect(Collectors.toList());
        LivingEntity victim = Utils.randomPick(candidate);
        if (victim == null)return;
        for (int i = 0; i < amount; i++) {
            Location location = Utils.randomNonNullLocation(victim.getLocation(), 0, radius);
            Entity entity = location.getWorld().spawnEntity(location, type);
            if (!nbt.equals("")){
                NmsUtils.setEntityTag(entity,nbt);
            }
            LivingEntity target = iMob.getTarget();
            if (target != null && entity instanceof Mob){
                ((Mob) entity).setTarget(target);
            }
        }
        counter++;
    }

    @Override
    public String getName() {
        return "SummonOnPlayer";
    }
}
