package cat.nyaa.infiniteinfernal.event;

import cat.nyaa.infiniteinfernal.mob.IMob;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class InfernalTickEvent extends Event{
    private static final HandlerList handler = new HandlerList();
    private boolean canceled = false;
    private IMob iMob;

    public InfernalTickEvent(IMob iMob){
        this.iMob = iMob;
    }

    public void setCanceled(boolean canceled){
        this.canceled = canceled;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public IMob getMob() {
        return iMob;
    }

    @Override
    public HandlerList getHandlers() {
        return handler;
    }

    public static HandlerList getHandlerList(){
        return handler;
    }
}
