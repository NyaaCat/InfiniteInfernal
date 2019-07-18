package cat.nyaa.infiniteinfernal.ability;

import cat.nyaa.infiniteinfernal.mob.IMob;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.function.Function;

public interface AbilityAOE {
    void fire(IMob mob, Location targetLocation, Vector range, Function<Location, Double> ... damageFactor);
}
