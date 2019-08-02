package cat.nyaa.infiniteinfernal.ability.impl.active;

import cat.nyaa.infiniteinfernal.ability.AbilityAttack;
import cat.nyaa.infiniteinfernal.ability.ActiveAbility;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.Context;
import cat.nyaa.infiniteinfernal.utils.ContextKeys;
import cat.nyaa.infiniteinfernal.utils.Utils;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.List;

public class AbilityLifesteal extends ActiveAbility implements AbilityAttack {
    @Serializable
    public double suck = 10;
    @Serializable
    public double gain = 10;

    @Override
    public void onAttack(IMob mob, LivingEntity target) {
        fire(mob, target);
    }

    @Override
    public void active(IMob iMob) {
        List<Entity> nearbyEntities = iMob.getEntity().getNearbyEntities(10, 10, 10);
        Utils.getValidTargets(iMob, nearbyEntities)
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
        Context.instance().putTemp(mob.getEntity().getUniqueId(), ContextKeys.DAMAGE_ATTACK_ABILITY, suck);
        target.damage(suck, mob.getEntity());
        Context.instance().removeTemp(mob.getEntity().getUniqueId(), ContextKeys.DAMAGE_ATTACK_ABILITY);

    }

    @Override
    public String getName() {
        return "LifeSteal";
    }
}
