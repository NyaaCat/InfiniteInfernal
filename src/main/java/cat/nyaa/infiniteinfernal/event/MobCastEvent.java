package cat.nyaa.infiniteinfernal.event;

import cat.nyaa.infiniteinfernal.mob.IMob;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.util.Vector;

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
}
