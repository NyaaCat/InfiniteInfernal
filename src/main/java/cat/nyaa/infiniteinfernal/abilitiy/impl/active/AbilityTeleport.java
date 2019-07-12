package cat.nyaa.infiniteinfernal.abilitiy.impl.active;

import cat.nyaa.infiniteinfernal.abilitiy.ActiveAbility;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityTeleportEvent;

public class AbilityTeleport extends ActiveAbility {
    @Serializable
    public double radius = 5;
    @Serializable
    public double range = 5;

    @Override
    public void active(IMob iMob) {
        LivingEntity mobEntity = iMob.getEntity();
        Location to = Utils.randomLocation(mobEntity.getLocation(), 0, radius);
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
}
