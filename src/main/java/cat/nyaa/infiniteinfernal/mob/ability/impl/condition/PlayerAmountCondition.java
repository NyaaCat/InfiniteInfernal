package cat.nyaa.infiniteinfernal.mob.ability.impl.condition;

import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.mob.ability.condition.PlayerCondition;

public class PlayerAmountCondition extends PlayerCondition {
    @Serializable
    int amount = 1;
    @Serializable
    boolean moreThan = true;

    @Override
    public boolean check(IMob mob) {
        long count = getValidTargets(mob).count();
        boolean enoughPlayers = count > amount;
        if (!moreThan){
            enoughPlayers = !enoughPlayers;
        }
        return enoughPlayers;
    }
}
