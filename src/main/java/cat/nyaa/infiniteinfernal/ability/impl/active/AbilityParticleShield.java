package cat.nyaa.infiniteinfernal.ability.impl.active;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.ability.AbilityHurt;
import cat.nyaa.infiniteinfernal.ability.ActiveAbility;
import cat.nyaa.infiniteinfernal.ability.IAbilitySet;
import cat.nyaa.infiniteinfernal.configs.ParticleConfig;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.mob.MobManager;
import cat.nyaa.infiniteinfernal.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AbilityParticleShield extends ActiveAbility implements AbilityHurt {

    @Serializable
    public ParticleConfig particleConfig = new ParticleConfig();
    @Serializable
    public int duration = 60;
    @Serializable
    public double resistance = 20;
    @Serializable
    public double thornsPercentage = 10;

    private static List<UUID> activited = new ArrayList<>();
    private static Listener listener;
    private static boolean registered = false;

    BukkitRunnable cancelTask;

    static {
        listener = new Listener() {
            @EventHandler
            public void onHurt(EntityDamageByEntityEvent ev) {
                if (activited.contains(ev.getEntity().getUniqueId())) {
                    if (!(ev.getDamager() instanceof Player))return;
                    IMob mob = MobManager.instance().toIMob(ev.getEntity());
                    if (mob != null) {
                        List<IAbilitySet> abilities = mob.getAbilities();
                        if (!abilities.isEmpty()) {
                            abilities.forEach(iAbilitySet -> {
                                        List<AbilityParticleShield> abilitiesInSet = iAbilitySet.getAbilitiesInSet(AbilityParticleShield.class);
                                        if (!abilitiesInSet.isEmpty()) {
                                            abilitiesInSet.forEach(abilityParticleShield -> abilityParticleShield.onHurtByPlayer(mob, ev));
                                        }
                                    });
                        }
                    }
                }
            }
        };
    }

    public AbilityParticleShield() {
        if (!registered) {
            Bukkit.getPluginManager().registerEvents(listener, InfPlugin.plugin);
            registered = true;
        }
    }

    @Override
    public void active(IMob iMob) {
        activited.add(iMob.getEntity().getUniqueId());
        LivingEntity mobEntity = iMob.getEntity();
        BukkitRunnable particleEffect = new BukkitRunnable() {
            @Override
            public void run() {
                World world = mobEntity.getWorld();
                Location location = mobEntity.getLocation();
                if (activited.contains(mobEntity.getUniqueId()) && !mobEntity.isDead()) {
                    Utils.spawnParticle(particleConfig, world, location);
                } else cancel();
            }
        };
        particleEffect.runTaskTimer(InfPlugin.plugin, 0, 1);
        cancelTask = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    activited.remove(mobEntity.getUniqueId());
                } catch (Exception e) {
                    particleEffect.cancel();
                }
            }
        };
        cancelTask.runTaskLater(InfPlugin.plugin, duration);
    }

    @Override
    public void onHurtByPlayer(IMob mob, EntityDamageByEntityEvent event) {
        if (!activited.contains(mob.getEntity().getUniqueId())) return;
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
