package cat.nyaa.infiniteinfernal.event;

import cat.nyaa.infiniteinfernal.mob.IMob;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class MobSpawnEvent extends Event implements Cancellable {
    private final IMob iMob;
    private boolean canceled = false;
    public static final HandlerList handlerList = new HandlerList();

    public MobSpawnEvent(IMob iMob) {
        this.iMob = iMob;
    }

    public IMob getIMob() {
        return iMob;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }

    @Override
    public boolean isCancelled() {
        return canceled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.canceled = cancel;
    }
}
