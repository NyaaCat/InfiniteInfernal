package cat.nyaa.infiniteinfernal.mob.ability.impl.passive;

import cat.nyaa.infiniteinfernal.event.MobCastEvent;
import cat.nyaa.infiniteinfernal.mob.ability.AbilityPassive;
import cat.nyaa.infiniteinfernal.mob.ability.api.AbilitySpawn;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.nyaacore.utils.NmsUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import java.util.List;
import java.util.logging.Level;

public class AbilityPassenger extends AbilityPassive implements AbilitySpawn {
    @Serializable
    public String passengerType = "ZOMBIE";
    @Serializable
    public String passengerNbt = "";

    @Override
    public void onSpawn(IMob iMob) {
        try {
            EntityType entityType = EntityType.valueOf(passengerType.toUpperCase());
            LivingEntity mobEntity = iMob.getEntity();
            List<Entity> passengers = mobEntity.getPassengers();
            if (!passengers.isEmpty()){
                mobEntity.eject();
            }
            Entity spawn = mobEntity.getWorld().spawn(mobEntity.getLocation(), entityType.getEntityClass());
            if (passengerNbt!=null && !passengerNbt.equals("")){
                NmsUtils.setEntityTag(spawn, passengerNbt);
            }
            mobEntity.addPassenger(spawn);
        }catch (Exception e){
            Bukkit.getLogger().log(Level.WARNING, "wrong config for ability passage, type "+passengerType+", nbt "+passengerNbt);
        }
    }

    @Override
    public String getName() {
        return "Passenger";
    }

    @Override
    public void fire(IMob mob, MobCastEvent event) {
        onSpawn(mob);
    }
}
