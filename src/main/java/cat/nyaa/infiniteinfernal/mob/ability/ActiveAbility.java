package cat.nyaa.infiniteinfernal.mob.ability;

import cat.nyaa.infiniteinfernal.event.MobCastEvent;
import cat.nyaa.infiniteinfernal.event.MobNearDeathEvent;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.mob.ability.api.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;

public abstract class ActiveAbility extends BaseAbility implements AbilityActive, AbilityAttack, AbilityDeath, AbilityHurt, AbilityLocation, AbilityNearDeath, AbilitySpawn {
    @Override
    public void onAttack(IMob mob, LivingEntity target) {
        this.active(mob);
    }

    @Override
    public void onMobDeath(IMob iMob, EntityDeathEvent ev) {
        this.active(iMob);
    }

    @Override
    public void onHurt(IMob mob, EntityDamageEvent event) {
        this.active(mob);
    }

    @Override
    public void onDeath(IMob iMob, MobNearDeathEvent mobNearDeathEvent) {
        this.active(iMob);
    }

    @Override
    public void onSpawn(IMob iMob) {
        this.active(iMob);
    }
}
