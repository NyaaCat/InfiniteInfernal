package cat.nyaa.infiniteinfernal.abilitiy.impl;

import cat.nyaa.infiniteinfernal.abilitiy.AbilityTick;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.Utils;
import cat.nyaa.nyaacore.utils.NmsUtils;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

public class AbilitySummon extends BaseAbility implements AbilityTick {
    @Serializable
    public double chance = 0.1;
    @Serializable
    public String nbt = "";
    @Serializable
    public int amount = 2;
    @Serializable
    public double radius;
    @Serializable
    public EntityType type = EntityType.ZOMBIE;

    @Override
    public void tick(IMob iMob) {
        if (!Utils.possibility(chance))return;
        for (int i = 0; i < amount; i++) {
            Location location = Utils.randomSpawnLocation(iMob.getEntity().getLocation(), 0, radius);
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
