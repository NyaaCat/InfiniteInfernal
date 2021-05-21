package cat.nyaa.infiniteinfernal.mob.ability.impl.active;

import cat.nyaa.infiniteinfernal.event.MobCastEvent;
import cat.nyaa.infiniteinfernal.mob.ability.ActiveAbility;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.LocationUtil;
import cat.nyaa.infiniteinfernal.utils.RandomUtil;
import cat.nyaa.infiniteinfernal.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityTeleportEvent;

public class AbilityTeleportToPlayer extends ActiveAbility {
    @Serializable
    public double radius = 5;
    @Serializable
    public double range = 5;

    @Override
    public void active(IMob iMob) {
        LivingEntity mobEntity = iMob.getEntity();
        LivingEntity livingEntity = RandomUtil.randomSelectTarget(iMob, range);
        if (livingEntity == null)return;
        Location to = LocationUtil.randomNonNullLocation(livingEntity.getLocation(), 0, radius);
        teleport(mobEntity, to);
    }

//    @Override
//    public void onHurt(IMob mob, EntityDamageEvent ev) {
//        LivingEntity mobEntity = mob.getEntity();
//        if (!RandomUtil.possibility(hurtChance)) return;
//        if (!(ev instanceof EntityDamageByEntityEvent))return;
//        Entity damager = ((EntityDamageByEntityEvent) ev).getDamager();
//        Location to = RandomUtil.randomLocation(damager.getLocation(), 0, radius);
//        teleport(mobEntity, to);
//    }
//
//    @Override
//    public void onAttack(IMob mob, LivingEntity target) {
//        if (!RandomUtil.possibility(attackChance)) return;
//        LivingEntity mobEntity = mob.getEntity();
//        Location to = RandomUtil.randomLocation(target.getLocation(), 0, radius);
//        teleport(mobEntity, to);
//    }

    private void teleport(LivingEntity mobEntity, Location to) {
        if (mobEntity.isInsideVehicle()) return;
        EntityTeleportEvent event = new EntityTeleportEvent(mobEntity, mobEntity.getLocation(), to);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }
        mobEntity.teleport(to);
    }

    @Override
    public String getName() {
        return "TeleportToPlayer";
    }

    @Override
    public void fire(IMob mob, MobCastEvent event) {
        Location selectedLocation = event.getSelectedLocation();
        teleport(mob.getEntity(), selectedLocation);
    }
}
