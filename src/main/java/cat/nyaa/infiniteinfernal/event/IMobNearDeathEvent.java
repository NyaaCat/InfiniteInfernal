package cat.nyaa.infiniteinfernal.event;

import cat.nyaa.infiniteinfernal.mob.IMob;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class IMobNearDeathEvent extends Event {
    private final IMob iMob;
    private final LivingEntity damager;
    public static final HandlerList handlerList = new HandlerList();
    private boolean canceled = false;

    public IMobNearDeathEvent(IMob iMob, LivingEntity damager){
        this.iMob = iMob;
        this.damager = damager;
    }

    public IMob getMob(){
        return iMob;
    }

    public LivingEntity getDamager(){
        return damager;
    }

    public void setCanceled(boolean canceled){
        this.canceled = true;
    }

    public boolean getCanceled(){
        return canceled;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }

}
