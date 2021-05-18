package cat.nyaa.infiniteinfernal.event;

import cat.nyaa.infiniteinfernal.mob.IMob;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.util.Vector;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * include information for an ability cast
 */
public class MobCastEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList(){
        return handlers;
    }

    private IMob iMob;
    private Location selectedLocation;
    private Vector selectedVector;

    public MobCastEvent(IMob iMob){
        this.iMob = iMob;
        this.selectedLocation = iMob.getEntity().getLocation();
    }

    public MobCastEvent(IMob iMob, Location location, Vector vector){
        this.iMob = iMob;
        this.selectedLocation = location;
        this.selectedVector = vector;
    }

    public IMob getMob() {
        return iMob;
    }

    public Location getSelectedLocation() {
        return selectedLocation;
    }

    public Vector getSelectedVector() {
        if (selectedVector == null){
            Vector v = selectedLocation.subtract(iMob.getEntity().getEyeLocation()).toVector();
            if(v.length() != 0){
                v.normalize();
            }
            return v;
        }
        return selectedVector;
    }

    public List<LivingEntity> getSelectedEntities(){
        return getSelectedEntities(entity -> true);
    }


    public List<LivingEntity> getSelectedEntities(Predicate<Entity> filter){
        List<LivingEntity> result = Collections.emptyList();
        Location selectedLocation = getSelectedLocation();
        World world = selectedLocation.getWorld();
        if (world == null){
            return result;
        }
        double length = getSelectedVector().length();
        result = world.getNearbyEntities(selectedLocation, length, length, length).stream()
                .filter(filter.and(entity -> entity instanceof LivingEntity && !entity.equals(getMob().getEntity())))
                .map(entity -> ((LivingEntity) entity)).collect(Collectors.toList());
        return result;
    }
}
