package cat.nyaa.infiniteinfernal.mob.ability.impl.active;

import cat.nyaa.infiniteinfernal.event.MobCastEvent;
import cat.nyaa.infiniteinfernal.mob.ability.ActiveAbility;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.LocationUtil;
import cat.nyaa.infiniteinfernal.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class AbilityTeleport extends ActiveAbility {
    @Serializable
    public double radius = 5;
    @Serializable
    public double range = 5;

    @Override
    public void active(IMob iMob) {
        LivingEntity mobEntity = iMob.getEntity();
        Location to = LocationUtil.randomNonNullLocation(mobEntity.getLocation(), 0, radius);
        teleport(mobEntity, to);
    }

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
        return "Teleport";
    }

    @Override
    public void fire(IMob mob, MobCastEvent event) {
        Location selectedLocation = event.getSelectedLocation();
        teleport(mob.getEntity(), selectedLocation);
    }
}
