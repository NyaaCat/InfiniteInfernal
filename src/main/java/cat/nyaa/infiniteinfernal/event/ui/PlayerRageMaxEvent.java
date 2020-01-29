package cat.nyaa.infiniteinfernal.event.ui;

import cat.nyaa.infiniteinfernal.ui.impl.VarRage;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerRageMaxEvent extends Event {
    private final Player player;
    private final VarRage varRage;
    private double base;
    private double bonus = 0;

    public PlayerRageMaxEvent(Player player, VarRage varRage, double base){
        this.player = player;
        this.varRage = varRage;
        this.base = base;
    }

    public Player getPlayer() {
        return player;
    }

    public VarRage getVarRage() {
        return varRage;
    }

    public double getBase() {
        return base;
    }

    public double getBonus() {
        return bonus;
    }

    public void setBonus(double bonus) {
        this.bonus = bonus;
    }

    public static final HandlerList handlerList = new HandlerList();
    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }
}
