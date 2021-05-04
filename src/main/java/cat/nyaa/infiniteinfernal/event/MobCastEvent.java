package cat.nyaa.infiniteinfernal.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

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
}
