package cat.nyaa.infiniteinfernal.ability.impl.active;

import cat.nyaa.infiniteinfernal.ability.ActiveAbility;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.Utils;
import cat.nyaa.nyaacore.utils.NmsUtils;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import java.util.List;
import java.util.stream.Collectors;

public class AbilitySummonOnPlayer extends ActiveAbility {
    @Serializable
    public String nbt = "";
    @Serializable
    public int amount = 2;
    @Serializable
    public double radius = 10;
    @Serializable
    public EntityType type = EntityType.ZOMBIE;

    @Override
    public void active(IMob iMob) {
        List<LivingEntity> candidate = Utils.getValidTargets(iMob, iMob.getEntity().getNearbyEntities(radius, radius, radius))
                .collect(Collectors.toList());
        LivingEntity victim = Utils.randomPick(candidate);
        if (victim == null)return;
        for (int i = 0; i < amount; i++) {
            Location location = Utils.randomSpawnLocation(victim.getLocation(), 0, radius);
            Entity entity = location.getWorld().spawnEntity(location, type);
            if (!nbt.equals("")){
                NmsUtils.setEntityTag(entity,nbt);
            }
        }
    }

    @Override
    public String getName() {
        return "SummonOnPlayer";
    }
}
