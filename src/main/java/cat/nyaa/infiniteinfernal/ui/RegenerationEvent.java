package cat.nyaa.infiniteinfernal.ui;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class RegenerationEvent extends Event {
    public static HandlerList handlerList = new HandlerList();

    private final Player player;
    private final IVar<Double> iVar;
    private int tick;
    private double regeneration;
    private double factor = 1;

    public RegenerationEvent(Player player, IVar<Double> iVar, int tick){
        this.player = player;
        this.iVar = iVar;
        this.tick = tick;
        regeneration = getDefaultRegeneration();
    }

    public double getDefaultRegeneration(){
        return iVar.defaultRegeneration(tick);
    }

    public double getRegeneration(){
        return regeneration;
    }

    public void setRegeneration(double regeneration){
        this.regeneration = regeneration;
    }

    public double getFactor(){
        return factor;
    }

    public void setFactor(double factor){
        this.factor = factor;
    }

    public IVar<Double> getIVar(){
        return iVar;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }

}
