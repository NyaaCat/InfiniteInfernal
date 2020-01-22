package cat.nyaa.infiniteinfernal.utils.ticker;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class TickEvent extends Event {
    private static final HandlerList handlerList = new HandlerList();
    private int ticksFromStarted;

    public TickEvent(int ticksFromStarted){
        this.ticksFromStarted = ticksFromStarted;
    }

    public int getTicksFromStarted() {
        return ticksFromStarted;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
}
