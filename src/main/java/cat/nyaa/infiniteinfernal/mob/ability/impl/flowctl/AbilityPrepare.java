package cat.nyaa.infiniteinfernal.mob.ability.impl.flowctl;

import cat.nyaa.infiniteinfernal.event.MobCastEvent;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.mob.ability.FlowCtlAbility;

public class AbilityPrepare extends FlowCtlAbility {
    @Serializable(name = "delay")
    private int delay = 0;

    @Override
    public int getFlowCtlDelay() {
        return delay;
    }

    @Override
    public String getName() {
        return "prepare";
    }

    @Override
    public void active(IMob iMob) {

    }

    @Override
    public void fire(IMob mob, MobCastEvent event) {

    }
}
