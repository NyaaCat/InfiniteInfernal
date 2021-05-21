package cat.nyaa.infiniteinfernal.mob.ability.impl.active;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.event.MobCastEvent;
import cat.nyaa.infiniteinfernal.mob.ability.ActiveAbility;
import cat.nyaa.infiniteinfernal.configs.MobConfig;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.mob.MobManager;
import cat.nyaa.infiniteinfernal.utils.LocationUtil;
import cat.nyaa.infiniteinfernal.utils.Utils;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;

import java.util.LinkedHashMap;
import java.util.Map;

public class AbilityClone extends ActiveAbility {
    @Serializable
    public int amount = 1;

    @Serializable
    public int cloneTimes = 2;

    private static Map<IMob, Integer> clonedTimesMap = new LinkedHashMap<>();

    private boolean canClone = true;

    @Override
    public void active(IMob iMob) {
        cloneMob(iMob);
    }

    private void cloneMob(IMob iMob) {
        MobConfig config = iMob.getConfig();
        LivingEntity mobEntity = iMob.getEntity();
        Integer timesRemains = clonedTimesMap.computeIfAbsent(iMob, iMob1 -> cloneTimes);
        if (--timesRemains < 0){
            clonedTimesMap.remove(iMob);
            canClone = false;
            return;
        }
        if (!canClone)return;
        clonedTimesMap.put(iMob, timesRemains);
        for (int i = 0; i < amount; i++) {
            Location location = mobEntity.getLocation();
            LocationUtil.randomNonNullLocation(location, 0, 5);
            IMob cloned = MobManager.instance().spawnMobByConfig(config, location, iMob.getLevel());
            Integer finalTimesRemains = timesRemains;
            clonedTimesMap.put(cloned, clonedTimesMap.computeIfAbsent(iMob, iMob1 -> finalTimesRemains));
            cloneAttributes(iMob,cloned);
        }

    }

    private void cloneAttributes(IMob from, IMob to){
        LivingEntity clonedEntity = to.getEntity();
//            this function will produce unexpected exception, skipping.
        AttributeInstance damageAttr = clonedEntity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        AttributeInstance maxHealthAttr = clonedEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        AttributeInstance followRangeAttr = clonedEntity.getAttribute(Attribute.GENERIC_FOLLOW_RANGE);
        if(damageAttr != null){
            damageAttr.setBaseValue(from.getDamage());
        }else {}
        if(maxHealthAttr != null){
            maxHealthAttr.setBaseValue(from.getMaxHealth());
        }else{}
        if(followRangeAttr != null){
            final double aggroRangeMax = InfPlugin.plugin.config().aggroRangeMax;
            followRangeAttr.setBaseValue(aggroRangeMax);
        }else{}
        if (clonedEntity instanceof Mob && from.getEntity() instanceof Mob) {
            ((Mob) clonedEntity).setTarget(((Mob) from.getEntity()).getTarget());
        }
        clonedEntity.addPotionEffects(from.getEntity().getActivePotionEffects());
    }

    @Override
    public String getName() {
        return "Clone";
    }

    @Override
    public void fire(IMob mob, MobCastEvent event) {
        cloneMob(mob);
    }
}
