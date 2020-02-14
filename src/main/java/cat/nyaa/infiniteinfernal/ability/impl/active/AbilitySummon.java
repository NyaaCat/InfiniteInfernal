package cat.nyaa.infiniteinfernal.ability.impl.active;

import cat.nyaa.infiniteinfernal.ability.ActiveAbility;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.Utils;
import cat.nyaa.nyaacore.utils.NmsUtils;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;

public class AbilitySummon extends ActiveAbility {
    @Serializable
    public String nbt = "";
    @Serializable
    public int amount = 2;
    @Serializable
    public double radius;
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
        for (int i = 0; i < amount; i++) {
            Location location = null;
            for (int j = 0; j < 20; j++) {
                location = Utils.randomSpawnLocation(iMob.getEntity().getLocation(), 0, radius);
                if (location!=null){
                    break;
                }
            }
            if (location == null) {
                return;
            }
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
        return "Summon";
    }
}
