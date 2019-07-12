package cat.nyaa.infiniteinfernal.abilitiy.impl.active;

import cat.nyaa.infiniteinfernal.abilitiy.ActiveAbility;
import cat.nyaa.infiniteinfernal.configs.MobConfig;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.mob.MobManager;
import cat.nyaa.infiniteinfernal.utils.Utils;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;

public class AbilityClone extends ActiveAbility {
    @Serializable
    public int amount = 1;

    @Override
    public void active(IMob iMob) {
        cloneMob(iMob);
    }

    private void cloneMob(IMob iMob) {
        MobConfig config = iMob.getConfig();
        LivingEntity mobEntity = iMob.getEntity();
        for (int i = 0; i < amount; i++) {
            Location location = mobEntity.getLocation();
            Utils.randomSpawnLocation(location, 0, 5);
            IMob cloned = MobManager.instance().spawnMobByConfig(config, location, iMob.getLevel());
            LivingEntity clonedEntity = cloned.getEntity();
            clonedEntity.setHealth(mobEntity.getHealth());
            if (clonedEntity instanceof Mob && mobEntity instanceof Mob) {
                ((Mob) clonedEntity).setTarget(((Mob) mobEntity).getTarget());
            }
            clonedEntity.addPotionEffects(mobEntity.getActivePotionEffects());
        }

    }

    @Override
    public String getName() {
        return "Clone";
    }
}
