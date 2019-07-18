package cat.nyaa.infiniteinfernal.ability.impl.active;

import cat.nyaa.infiniteinfernal.ability.ActiveAbility;
import cat.nyaa.infiniteinfernal.mob.IMob;
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
        LivingEntity livingEntity = Utils.randomSelectTarget(iMob, range);
        if (livingEntity == null)return;
        Location to = Utils.randomLocation(livingEntity.getLocation(), 0, radius);
        teleport(mobEntity, to);
    }

//    @Override
//    public void onHurt(IMob mob, EntityDamageEvent ev) {
//        LivingEntity mobEntity = mob.getEntity();
//        if (!Utils.possibility(hurtChance)) return;
//        if (!(ev instanceof EntityDamageByEntityEvent))return;
//        Entity damager = ((EntityDamageByEntityEvent) ev).getDamager();
//        Location to = Utils.randomLocation(damager.getLocation(), 0, radius);
//        teleport(mobEntity, to);
//    }
//
//    @Override
//    public void onAttack(IMob mob, LivingEntity target) {
//        if (!Utils.possibility(attackChance)) return;
//        LivingEntity mobEntity = mob.getEntity();
//        Location to = Utils.randomLocation(target.getLocation(), 0, radius);
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
}
