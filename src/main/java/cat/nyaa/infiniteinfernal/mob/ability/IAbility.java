package cat.nyaa.infiniteinfernal.mob.ability;

import cat.nyaa.infiniteinfernal.mob.ability.api.AbilityLocation;
import cat.nyaa.nyaacore.configuration.ISerializable;

public interface IAbility extends ISerializable, AbilityLocation {
    String getName();
}
