package cat.nyaa.infiniteinfernal.mob.ability;

import cat.nyaa.infiniteinfernal.mob.IMob;
import org.bukkit.Location;
import org.bukkit.util.Vector;

public interface AbilityLocation {
    void fire(IMob mob, Location targetLocation, Vector range);
}
