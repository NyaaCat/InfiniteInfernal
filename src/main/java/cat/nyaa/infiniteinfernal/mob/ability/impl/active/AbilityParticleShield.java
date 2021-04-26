package cat.nyaa.infiniteinfernal.mob.ability.impl.active;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.mob.ability.AbilityHurt;
import cat.nyaa.infiniteinfernal.mob.ability.ActiveAbility;
import cat.nyaa.infiniteinfernal.mob.ability.IAbilitySet;
import cat.nyaa.infiniteinfernal.configs.ParticleConfig;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.mob.MobManager;
import cat.nyaa.infiniteinfernal.utils.BeamUtil;
import cat.nyaa.infiniteinfernal.utils.context.Context;
import cat.nyaa.infiniteinfernal.utils.context.ContextKeys;
import cat.nyaa.infiniteinfernal.utils.Utils;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

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
    @Serializable
    public int delay = 20;

    private static List<UUID> activited = new ArrayList<>();
    private static List<UUID> thorns = new ArrayList<>();
    private static Listener listener;
    private static boolean registered = false;
    BeamUtil.BeamConfig beamConfig;

    {
        ParticleConfig particle = new ParticleConfig();
        particle.type = Particle.END_ROD;
        particle.forced = true;
        particle.amount = 1;
        particle.speed = 0;
        beamConfig = new BeamUtil.BeamBuilder().particle(particle)
                .burst(1)
                .cone(0)
                .damage(0)
                .ignoreWall(true)
                .build();
    }

    BukkitRunnable cancelTask;

    static {
        listener = new Listener() {
            @EventHandler
            public void onHurt(EntityDamageByEntityEvent ev) {
                if (activited.contains(ev.getEntity().getUniqueId())) {
                    if (!(ev.getDamager() instanceof Player) && !(ev.getDamager() instanceof Projectile))
                        return;
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
        mobEntity.getWorld().playSound(mobEntity.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1, 0.5f);
        BukkitRunnable particleEffect = new BukkitRunnable() {
            @Override
            public void run() {
                World world = mobEntity.getWorld();
                Location location = mobEntity.getLocation();
                if (mobEntity.isDead()) {
                    cancel();
                    return;
                }
                Utils.spawnParticle(particleConfig, world, location);
            }
        };
        particleEffect.runTaskTimer(InfPlugin.plugin, 0, 1);
        new BukkitRunnable() {
            @Override
            public void run() {
                thorns.add(mobEntity.getUniqueId());
                mobEntity.getWorld().playSound(mobEntity.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1, 1f);
            }
        }.runTaskLater(InfPlugin.plugin, delay);
        cancelTask = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    activited.remove(mobEntity.getUniqueId());
                    thorns.remove(mobEntity.getUniqueId());
                    particleEffect.cancel();
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
        LivingEntity target = Utils.getValidTargets(mob, mob.getEntity().getNearbyEntities(24, 24, 24))
                .filter(livingEntity -> livingEntity.getLocation().distance(mob.getEntity().getLocation()) > 3)
                .findAny().orElse(null);
        if (target == null) return;
        double orig = event.getDamage();
        double damage = Math.max(mob.getDamage(), orig * thornsPercentage / 100d);
        if (thorns.contains(mob.getEntity().getUniqueId())) {
            Context.instance().putTemp(mob.getEntity().getUniqueId(), ContextKeys.DAMAGE_ATTACK_ABILITY, damage);
            beamConfig.length = mob.getEntity().getEyeLocation().distance(target.getLocation());
            beamConfig.damage = damage;
            BeamUtil.beam(beamConfig, mob.getEntity(), target.getLocation().subtract(mob.getEntity().getEyeLocation()).toVector(), aDouble -> new Vector(0, 1, 0).multiply(distanceShift(aDouble) * 20));
            Context.instance().removeTemp(mob.getEntity().getUniqueId(), ContextKeys.DAMAGE_ATTACK_ABILITY);
        }
        event.setDamage(orig * (1 - resistance / 100d));
    }

    private double distanceShift(double x) {
        //0.57x^2-3,89x^3+10.88x^4-7.56x^5
        return 0.57 * Math.pow(x, 2) - 3.89 * Math.pow(x, 3) + 10.88 * Math.pow(x, 4) - 7.56 * Math.pow(x, 5);
    }

    @Override
    public String getName() {
        return "ParticleShield";
    }
}
