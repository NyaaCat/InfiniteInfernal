package cat.nyaa.infiniteinfernal;

import cat.nyaa.infiniteinfernal.ability.*;
import cat.nyaa.infiniteinfernal.ability.impl.active.AbilityProjectile;
import cat.nyaa.infiniteinfernal.event.IMobNearDeathEvent;
import cat.nyaa.infiniteinfernal.event.InfernalSpawnEvent;
import cat.nyaa.infiniteinfernal.event.LootDropEvent;
import cat.nyaa.infiniteinfernal.loot.ILootItem;
import cat.nyaa.infiniteinfernal.loot.IMessager;
import cat.nyaa.infiniteinfernal.loot.LootManager;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.mob.MobManager;
import cat.nyaa.infiniteinfernal.utils.Utils;
import cat.nyaa.nyaacore.Message;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.projectiles.ProjectileSource;

import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class Events implements Listener {
    private InfPlugin plugin;

    public Events(InfPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onMobDeath(EntityDeathEvent ev) {
        Entity entity = ev.getEntity();
        if (MobManager.instance().isIMob(entity)) {
            IMob iMob = MobManager.instance().toIMob(entity);
            if (iMob == null) return;
            List<IAbilitySet> abilities = iMob.getAbilities();
            abilities.stream()
                    .forEach(iAbilitySet -> {
                        List<AbilityDeath> abilitiesInSet = iAbilitySet.getAbilitiesInSet(AbilityDeath.class);
                        if (!abilitiesInSet.isEmpty()) {
                            abilitiesInSet.forEach(abilityDeath -> abilityDeath.onMobDeath(iMob, ev));
                        }
                    });
            InfPlugin.plugin.getSpawnControler().handleMobDeath(ev);
            Player killer = ev.getEntity().getKiller();
            if (killer == null) return;
            ILootItem loot = LootManager.makeDrop(killer, iMob);
            ILootItem specialLoot = LootManager.makeSpecialDrop(killer, iMob);
            ev.setDroppedExp(iMob.getExp());
            LootDropEvent lootDropEvent = new LootDropEvent(killer, iMob, loot, specialLoot, ev);
            Bukkit.getPluginManager().callEvent(lootDropEvent);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMobHurt(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (event instanceof EntityDamageByEntityEvent) return;
        if (MobManager.instance().isIMob(entity)) {
            IMob iMob = MobManager.instance().toIMob(entity);
            List<IAbilitySet> abilities = iMob.getAbilities();

            IAbilitySet triggeredAbilitySet = Utils.weightedRandomPick(abilities.stream()
                    .filter(IAbilitySet::containsPassive)
                    .collect(Collectors.toList()));

            if (triggeredAbilitySet != null) {
                if (triggeredAbilitySet.containsDummy()) return;
                List<AbilityHurt> abilitiesInSet = triggeredAbilitySet.getAbilitiesInSet(AbilityHurt.class);
                if (!abilitiesInSet.isEmpty()) {
                    abilitiesInSet.forEach(abilityHurt -> abilityHurt.onHurt(iMob, event));
                }
            }
            if (event.getFinalDamage() > iMob.getEntity().getHealth()) {
                callNearDeathEvent(event, iMob);
            }
            InfPlugin.plugin.bossbarManager.update(iMob);
        }
    }

    private void callNearDeathEvent(EntityDamageEvent event, IMob iMob) {
        IMobNearDeathEvent iMobNearDeathEvent = new IMobNearDeathEvent(iMob, iMob.getEntity());
        Bukkit.getPluginManager().callEvent(iMobNearDeathEvent);
        if (iMobNearDeathEvent.isCanceled()) {
            event.setDamage(0);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMobHurtByEntity(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();
        if (MobManager.instance().isIMob(entity)) {
            IMob iMob = MobManager.instance().toIMob(entity);
            List<IAbilitySet> abilities = iMob.getAbilities();

            IAbilitySet triggeredAbilitySet = Utils.weightedRandomPick(abilities.stream()
                    .filter(IAbilitySet::containsPassive)
                    .collect(Collectors.toList()));

            boolean hurtByPlayer = event.getDamager() instanceof Player;

            if (triggeredAbilitySet != null) {
                if (triggeredAbilitySet.containsDummy()) return;
                List<AbilityHurt> abilitiesInSet = triggeredAbilitySet.getAbilitiesInSet(AbilityHurt.class);
                if (!abilitiesInSet.isEmpty()) {
                    if (!hurtByPlayer) {
                        abilitiesInSet.forEach(abilityHurt -> abilityHurt.onHurtByNonPlayer(iMob, event));
                    } else {
                        abilitiesInSet.forEach(abilityHurt -> abilityHurt.onHurtByPlayer(iMob, event));
                    }
                }
            }
            if (event.getFinalDamage() > iMob.getEntity().getHealth()) {
                callNearDeathEvent(event, iMob);
            }
        } else if (entity instanceof Player) {
            Entity damager = event.getDamager();
            if (damager instanceof Player) {
                if (!event.isCancelled()) {
                    if (event.getFinalDamage() > 0) {
                        String effect = InfPlugin.plugin.config().worlds.get(damager.getWorld().getName())
                                .friendlyFireConfig.effect;
                        String[] split = effect.split(":");
                        try {
                            String effectName = split[0].toUpperCase();
                            int amplifier = Integer.parseInt(split[1]);
                            int duration = Integer.parseInt(split[2]);
                            Utils.doEffect(effectName, ((Player) damager), duration, amplifier, "friendly fire");
                            new Message(I18n.format("friendly_fire"))
                                    .send(damager);
                        } catch (Exception e) {
                            Bukkit.getLogger().log(Level.WARNING, "invalid friendly fire config: \"" + effect + "\"");
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onImobAttackLivingEntity(EntityDamageByEntityEvent ev) {
        if (!(ev.getEntity() instanceof LivingEntity)) return;
        IMob iMob;
        if (!MobManager.instance().isIMob(ev.getDamager())) {
            Entity damager = ev.getDamager();
            if (!(damager instanceof Projectile)) return;
            List<MetadataValue> metadata = damager.getMetadata(AbilityProjectile.INF_PROJECTILE_KEY);
            if (metadata.size() < 1) return;
            double damage = metadata.get(0).asDouble();
            ev.setDamage(damage);
            ProjectileSource shooter = ((Projectile) damager).getShooter();
            if (shooter == null)return;
            iMob = MobManager.instance().toIMob((Entity) shooter);
        } else {
            iMob = MobManager.instance().toIMob(ev.getDamager());
        }

        if (iMob == null) return;
        IAbilitySet iAbilitySet = Utils.weightedRandomPick(iMob.getAbilities().stream()
                .filter(IAbilitySet::containsPassive)
                .collect(Collectors.toList()));

        if (iAbilitySet == null) return;
        if (iAbilitySet.containsDummy()) return;
        List<AbilityAttack> attackAbilities = iAbilitySet.getAbilitiesInSet(AbilityAttack.class);
        if (attackAbilities.isEmpty()) return;
        attackAbilities.forEach(abilityAttack -> abilityAttack.onAttack(iMob, ((LivingEntity) ev.getEntity())));
    }

    @EventHandler
    public void onNatualMobSpawn(CreatureSpawnEvent event) {
        World world = event.getEntity().getWorld();
        if (event.getSpawnReason().equals(CreatureSpawnEvent.SpawnReason.NATURAL)) {
            if (!InfPlugin.plugin.getSpawnControler().canVanillaAutoSpawn(world)) {
                event.setCancelled(true);
                return;
            }
        }
        if (event.getSpawnReason().equals(CreatureSpawnEvent.SpawnReason.REINFORCEMENTS)) {
            event.setCancelled(true);
            return;
        }
        InfPlugin.plugin.getSpawnControler().handleSpawnEvent(event);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onIMobNearDeath(IMobNearDeathEvent ev) {
        List<IAbilitySet> abilities = ev.getMob().getAbilities();
        if (abilities.isEmpty()) return;
        abilities.forEach(iAbilitySet -> {
            List<AbilityNearDeath> abilitiesInSet = iAbilitySet.getAbilitiesInSet(AbilityNearDeath.class);
            if (abilitiesInSet.isEmpty()) return;
            abilitiesInSet.forEach(abilityNearDeath -> abilityNearDeath.onDeath(ev.getMob(), ev));
        });
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInfMobSpawn(InfernalSpawnEvent ev) {
        IMob iMob = ev.getIMob();
        List<IAbilitySet> abilities = iMob.getAbilities();
        if (!abilities.isEmpty()) {
            abilities.forEach(iAbilitySet -> {
                List<AbilitySpawn> spawnAbilities = iAbilitySet.getAbilitiesInSet(AbilitySpawn.class);
                if (!spawnAbilities.isEmpty()) {
                    spawnAbilities.forEach(abilitySpawn -> abilitySpawn.onSpawn(iMob));
                }
            });
        }
    }

    @EventHandler
    public void onLootEvent(LootDropEvent ev) {
        List<ItemStack> drops = ev.getEntityDeathEvent().getDrops();
        drops.clear();
        IMessager iMessager = InfPlugin.plugin.getMessager();
        ILootItem normalLoot = ev.getLoot();
        ILootItem specialLoot = ev.getSpecialLoot();
        if (normalLoot != null) {
            iMessager.broadcastToWorld(ev.getiMob(), ev.getKiller(), normalLoot);
            drops.add(normalLoot.getItemStack());
        } else {
            iMessager.broadcastToWorld(ev.getiMob(), ev.getKiller(), null);
        }

        if (specialLoot != null) {
            iMessager.broadcastExtraToWorld(ev.getiMob(), ev.getKiller(), specialLoot);
            drops.add(specialLoot.getItemStack());
        }
        if (drops.isEmpty()) {
            drops.add(new ItemStack(Material.AIR));
        }
    }
}
