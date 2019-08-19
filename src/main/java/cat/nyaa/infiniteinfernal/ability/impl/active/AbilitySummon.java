package cat.nyaa.infiniteinfernal.ability.impl.active;

import cat.nyaa.infiniteinfernal.ability.ActiveAbility;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.Utils;
import cat.nyaa.nyaacore.utils.NmsUtils;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

public class AbilitySummon extends ActiveAbility {
    @Serializable
    public String nbt = "";
    @Serializable
    public int amount = 2;
    @Serializable
    public double radius;
    @Serializable
    public EntityType type = EntityType.ZOMBIE;

    @Override
    public void active(IMob iMob) {
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
        }
    }

    @Override
    public String getName() {
        return "Summon";
    }
}
