package cat.nyaa.infiniteinfernal.mob.ability.condition;

import cat.nyaa.infiniteinfernal.mob.ability.api.ICondition;

public abstract class BaseCondition implements ICondition {
    @Serializable
    private String id;

    @Override
    public String getId() {
        return this.id;
    }
}
