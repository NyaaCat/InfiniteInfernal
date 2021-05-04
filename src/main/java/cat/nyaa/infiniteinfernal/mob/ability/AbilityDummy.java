package cat.nyaa.infiniteinfernal.mob.ability;

import cat.nyaa.infiniteinfernal.mob.IMob;
import org.bukkit.Location;
import org.bukkit.util.Vector;

public class AbilityDummy implements IAbility {

    public AbilityDummy(){}

    @Override
    public String getName() {
        return "dummy";
    }

    @Override
    public void fire(IMob mob, Location targetLocation, Vector range) {

    }
}
