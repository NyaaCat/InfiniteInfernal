package cat.nyaa.infiniteinfernal.mob.ability.impl.passive;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.mob.ability.AbilityNearDeath;
import cat.nyaa.infiniteinfernal.mob.ability.AbilityPassive;
import cat.nyaa.infiniteinfernal.event.IMobNearDeathEvent;
import cat.nyaa.infiniteinfernal.mob.IMob;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.atomic.AtomicInteger;

public class AbilityNirvana extends AbilityPassive implements AbilityNearDeath {
    @Serializable
    public int lives = 1;

    private int tempLives;

    public AbilityNirvana(){
    }

    @Override
    public void deserialize(ConfigurationSection config) {
        super.deserialize(config);
        tempLives = lives;
    }

    @Override
    public void onDeath(IMob iMob, IMobNearDeathEvent mobNearDeathEvent) {
        if (tempLives > 0) {
            mobNearDeathEvent.setCanceled(true);
            respawn(iMob);
        }
    }

    public void respawn(IMob iMob) {
        LivingEntity mobEntity = iMob.getEntity();
        AttributeInstance attribute = mobEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attribute != null) {
            double maxHealth = attribute.getValue();
            mobEntity.setHealth(maxHealth);
            playEffect(mobEntity);
            tempLives--;
        }
    }

    private void playEffect(LivingEntity mobEntity) {
        Location location = mobEntity.getLocation();
        World world = location.getWorld();
        if (world != null) {
            int duration = 20;
            AtomicInteger runTimes = new AtomicInteger(0);
            new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        world.spawnParticle(Particle.FLAME, location, 20, 0.5, 0.5, 0.5, 0.4, null, true);
                        if (runTimes.getAndAdd(1) == duration - 1) {
                            this.cancel();
                        }
                    } catch (Exception e) {
                        this.cancel();
                    }
                }
            }.runTaskTimer(InfPlugin.plugin, 0, 1);
            world.playSound(location, Sound.ITEM_TOTEM_USE, 1, 2);
        }
    }

    @Override
    public String getName() {
        return "Nirvana";
    }
}
