package cat.nyaa.infiniteinfernal.ability.impl.active;

import cat.nyaa.infiniteinfernal.ability.ActiveAbility;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.Utils;

public class AbilityPotionSelf extends ActiveAbility {
    @Serializable
    public String effect = "SPEED";
    @Serializable
    public int duration = 1;
    @Serializable
    public int amplifier = 1;

    @Override
    public void active(IMob iMob) {
        Utils.doEffect(effect, iMob.getEntity(), duration, amplifier, getName());
    }

    @Override
    public String getName() {
        return "PotionSelf";
    }
}
