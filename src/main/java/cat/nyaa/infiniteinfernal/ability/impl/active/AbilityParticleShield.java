package cat.nyaa.infiniteinfernal.ability.impl.active;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.ability.AbilityHurt;
import cat.nyaa.infiniteinfernal.ability.ActiveAbility;
import cat.nyaa.infiniteinfernal.configs.ParticleConfig;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.Utils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class AbilityParticleShield extends ActiveAbility implements AbilityHurt {

    @Serializable
    public ParticleConfig particleConfig = new ParticleConfig();
    @Serializable
    public int duration = 60;
    @Serializable
    public double resistance = 20;
    @Serializable
    public double thornsPercentage = 10;

    private boolean activited = false;

    @Override
    public void active(IMob iMob) {
        activited = true;
        LivingEntity mobEntity = iMob.getEntity();
        BukkitRunnable particleEffect = new BukkitRunnable() {
            @Override
            public void run() {
                World world = mobEntity.getWorld();
                Location location = mobEntity.getLocation();
                if (activited) {
                    Utils.spawnParticle(particleConfig, world, location);
                }else cancel();
            }
        };
        particleEffect.runTaskTimer(InfPlugin.plugin, 0, 1);
        new BukkitRunnable(){
            @Override
            public void run() {
                try {
                    activited = false;
                }catch (Exception e){
                    particleEffect.cancel();
                }
            }
        }.runTaskLater(InfPlugin.plugin, duration);
    }

    @Override
    public void onHurtByPlayer(IMob mob, EntityDamageByEntityEvent event) {
        if (!activited) return;
        Entity damager = event.getDamager();
        if (damager instanceof LivingEntity) {
            double orig = event.getDamage();
            ((LivingEntity) damager).damage(orig * thornsPercentage / 100d, mob.getEntity());
            event.setDamage(orig * (1 - resistance / 100d));
        }
    }

    @Override
    public String getName() {
        return "ParticleShield";
    }
}
