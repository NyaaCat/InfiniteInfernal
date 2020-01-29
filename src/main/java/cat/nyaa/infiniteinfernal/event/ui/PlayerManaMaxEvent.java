package cat.nyaa.infiniteinfernal.event.ui;

import cat.nyaa.infiniteinfernal.ui.impl.VarMana;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerManaMaxEvent extends Event {
    private final Player player;
    private final VarMana varMana;
    private double base;
    private double bonus = 0;

    public PlayerManaMaxEvent(Player player, VarMana varMana, double manaBase) {
        this.player = player;
        this.varMana = varMana;
        base = manaBase;
    }

    public Player getPlayer() {
        return player;
    }

    public VarMana getVarMana() {
        return varMana;
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
