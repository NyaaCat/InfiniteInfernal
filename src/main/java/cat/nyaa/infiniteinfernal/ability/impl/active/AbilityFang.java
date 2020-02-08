package cat.nyaa.infiniteinfernal.ability.impl.active;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.ability.AbilityDeath;
import cat.nyaa.infiniteinfernal.ability.ActiveAbility;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class AbilityFang extends ActiveAbility implements AbilityDeath {
    @Serializable
    public double range = 48;
    @Serializable
    public double cone = 0;
    @Serializable
    public int spawnAmount = 5;
    @Serializable
    public double damageAmplifier = 1.0d;
    @Serializable
    public double castToAllNearbyChance = 0d;
    @Serializable
    public int spawnInterval = 5;
    @Serializable
    public int burst = 1;
    @Serializable
    public int burstInterval = 20;

    private static boolean inited = false;
    private static Map<UUID, IMob> summoned = new HashMap<>();

    private void init() {
        EventListener eventListener = new EventListener();
        Bukkit.getPluginManager().registerEvents(eventListener, InfPlugin.plugin);
        inited = true;
    }

    @Override
    public void onMobDeath(IMob iMob, EntityDeathEvent ev) {
        List<UUID> toRemove = new ArrayList<>();
        summoned.forEach((uuid, iMob1) -> {
            if (iMob1.equals(iMob)){
                toRemove.add(uuid);
            }
        });
        toRemove.forEach(uuid -> summoned.remove(uuid));
    }

    class EventListener implements Listener{
        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onDamage(EntityDamageByEntityEvent event){
            Entity damager = event.getDamager();
            Entity entity = event.getEntity();
            if(!isSummonedEntity(damager) || !(entity instanceof LivingEntity)){
                return;
            }
            IMob iMob = summoned.get(damager.getUniqueId());
            if (iMob.getEntity().equals(damager)){
                //prevent recursive call
                return;
            }
            event.setCancelled(true);
            double damage = iMob.getDamage();
            LivingEntity victim = (LivingEntity) event.getEntity();
            victim.damage(damage * damageAmplifier, iMob.getEntity());
        }

        private boolean isSummonedEntity(Entity damager) {
            return summoned.containsKey(damager.getUniqueId());
        }
    }


    @Override
    public void active(IMob iMob) {
        if (!inited){
            init();
        }
        if (iMob.getTarget() == null) return;
        for (int i = 0; i < burst; i++) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    LivingEntity target = iMob.getTarget();
                    if (Utils.possibility(castToAllNearbyChance)){
                        getNearbyEntities(iMob, range).forEach(livingEntity -> {
                            summon(iMob, livingEntity);
                        });
                    }else {
                        summon(iMob, target);
                    }
                }
            }.runTaskLater(InfPlugin.plugin, burstInterval * i);
        }
    }

    private void summon(IMob iMob, LivingEntity player) {
        Location mobLoc = iMob.getEntity().getLocation();
        Location playerLoc = player.getLocation();

        Vector towards = getTowards(mobLoc, playerLoc, spawnAmount);

        for (int i = 0; i < spawnAmount; i++) {
            int finalI = i;
            new BukkitRunnable() {
                @Override
                public void run() {
                    Location expectedLoc = mobLoc.clone().add(towards.clone().multiply(finalI + 1));
                    Location validSpawnLocationInY = Utils.findValidSpawnLocationInY(expectedLoc);
                    spawnFang(iMob, validSpawnLocationInY == null? expectedLoc : validSpawnLocationInY);
                }
            }.runTaskLater(InfPlugin.plugin, finalI * spawnInterval);
        }
    }

    private Vector getTowards(Location mobLoc, Location playerLoc, int spawnAmount) {
        Location towards = playerLoc.clone().subtract(mobLoc);
        if (towards.length() <= 0) {
            return towards.toVector();
        }
        double totalLength = towards.length();
        Vector step = towards.toVector().normalize().multiply(totalLength / spawnAmount);
        Vector cone = Utils.cone(step, this.cone);
        return cone;
    }

    private EvokerFangs spawnFang(IMob iMob, Location location) {
        World world = location.getWorld();
        if (world == null) {
            return null;
        }
        return world.spawn(location, EvokerFangs.class, evokerFangs -> {
            evokerFangs.setOwner(iMob.getEntity());
            evokerFangs.setCustomName("Evoker fang");
            evokerFangs.setCustomNameVisible(false);
            summoned.put(evokerFangs.getUniqueId(), iMob);
        });
    }

    @Override
    public String getName() {
        return "Fang";
    }
}
