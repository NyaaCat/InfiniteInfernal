package cat.nyaa.infiniteinfernal.abilitiy.impl;

import cat.nyaa.infiniteinfernal.abilitiy.AbilityAttack;
import cat.nyaa.infiniteinfernal.abilitiy.AbilityTick;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.Utils;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.List;

public class AbilityLifesteal extends BaseAbility implements AbilityAttack, AbilityTick {
    @Serializable
    public double chance = 0.5;
    @Serializable
    public double suck = 10;
    @Serializable
    public double gain = 10;

    @Override
    public void onAttack(IMob mob, LivingEntity target) {
        if (!Utils.possibility(chance)) return;
        fire(mob, target);
    }

    @Override
    public void tick(IMob iMob) {
        List<Entity> nearbyEntities = iMob.getEntity().getNearbyEntities(10, 10, 10);
        Utils.getValidTarget(iMob, nearbyEntities)
                .forEach(livingEntity -> fire(iMob, livingEntity));
    }

    private void fire(IMob mob, LivingEntity target) {
//        EntityDamageByEntityEvent ev = new EntityDamageByEntityEvent(mob.getEntity(), target, EntityDamageEvent.DamageCause.ENTITY_ATTACK, mob.getDamage());
//        Bukkit.getPluginManager().callEvent(ev);
//        if (ev.isCancelled()){
//            return;
//        }
//        double finalDamage = ev.getFinalDamage();
        double health = mob.getEntity().getHealth();
        double max = mob.getEntity().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        mob.getEntity().setHealth(Math.max(0, Math.min(health + gain, max)));
        target.damage(suck, mob.getEntity());
    }

    @Override
    public String getName() {
        return "LifeSteal";
    }
}
