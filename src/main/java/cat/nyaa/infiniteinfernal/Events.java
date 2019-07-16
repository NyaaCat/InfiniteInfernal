package cat.nyaa.infiniteinfernal;

import cat.nyaa.infiniteinfernal.abilitiy.*;
import cat.nyaa.infiniteinfernal.event.IMobNearDeathEvent;
import cat.nyaa.infiniteinfernal.event.InfernalSpawnEvent;
import cat.nyaa.infiniteinfernal.event.LootDropEvent;
import cat.nyaa.infiniteinfernal.loot.ILootItem;
import cat.nyaa.infiniteinfernal.loot.IMessager;
import cat.nyaa.infiniteinfernal.loot.LootManager;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.mob.MobManager;
import cat.nyaa.infiniteinfernal.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.stream.Collectors;

public class Events implements Listener {
    private InfPlugin plugin;

    public Events(InfPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
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
            ILootItem loot = LootManager.makeDrop(killer, iMob);
            ILootItem specialLoot = LootManager.makeSpecialDrop(killer, iMob);
            LootDropEvent lootDropEvent = new LootDropEvent(killer, iMob, loot, specialLoot, ev);
            Bukkit.getPluginManager().callEvent(lootDropEvent);
        }
    }

    @EventHandler
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
                IMobNearDeathEvent iMobNearDeathEvent = new IMobNearDeathEvent(iMob, (LivingEntity) entity);
                Bukkit.getPluginManager().callEvent(iMobNearDeathEvent);
            }
        }
    }

    @EventHandler
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
                IMobNearDeathEvent iMobNearDeathEvent = new IMobNearDeathEvent(iMob, (LivingEntity) entity);
                Bukkit.getPluginManager().callEvent(iMobNearDeathEvent);
            }
        }
    }

    @EventHandler
    public void onImobAttackLivingEntity(EntityDamageByEntityEvent ev) {
        if (!(ev.getEntity() instanceof LivingEntity)) return;
        if (!MobManager.instance().isIMob(ev.getDamager())) return;

        IMob iMob = MobManager.instance().toIMob(ev.getEntity());

        IAbilitySet iAbilitySet = Utils.weightedRandomPick(iMob.getAbilities().stream()
                .filter(IAbilitySet::containsPassive)
                .collect(Collectors.toList()));

        if (iAbilitySet == null) return;
        if (iAbilitySet.containsDummy()) return;
        List<AbilityAttack> attackAbilities = iAbilitySet.getAbilitiesInSet(AbilityAttack.class);
        if (attackAbilities.isEmpty()) return;
        attackAbilities.forEach(abilityAttack -> abilityAttack.onAttack(iMob, ((LivingEntity) ev.getEntity())));
        if (ev.getFinalDamage() > iMob.getEntity().getHealth()) {
            IMobNearDeathEvent iMobNearDeathEvent = new IMobNearDeathEvent(iMob, (LivingEntity) ev.getEntity());
            Bukkit.getPluginManager().callEvent(iMobNearDeathEvent);
        }
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

    @EventHandler
    public void onIMobNearDeath(IMobNearDeathEvent ev) {
        List<IAbilitySet> abilities = ev.getMob().getAbilities();
        if (abilities.isEmpty()) return;
        abilities.forEach(iAbilitySet -> {
            List<AbilityNearDeath> abilitiesInSet = iAbilitySet.getAbilitiesInSet(AbilityNearDeath.class);
            if (abilitiesInSet.isEmpty()) return;
            abilitiesInSet.forEach(abilityNearDeath -> abilityNearDeath.onDeath(ev.getMob(), ev));
        });
    }

    @EventHandler
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
        //todo
        IMessager iMessager = null;
        ILootItem normalLoot = ev.getLoot();
        ILootItem specialLoot = ev.getSpecialLoot();
        if (normalLoot != null) {
            iMessager.broadcastToWorld(ev.getiMob(), ev.getKiller(), normalLoot);
            drops.add(normalLoot.getItemStack());
        }else {
            iMessager.broadcastToWorld(ev.getiMob(), ev.getKiller(), null);
        }

        if (specialLoot != null){
            iMessager.broadcastExtraToWorld(ev.getiMob(), ev.getKiller(), specialLoot);
            drops.add(specialLoot.getItemStack());
        }else {
            iMessager.broadcastExtraToWorld(ev.getiMob(), ev.getKiller(), null);
        }
    }

}
