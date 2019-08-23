package cat.nyaa.infiniteinfernal;

import cat.nyaa.infiniteinfernal.ability.*;
import cat.nyaa.infiniteinfernal.ability.impl.active.AbilityProjectile;
import cat.nyaa.infiniteinfernal.controler.FirenlyFireControler;
import cat.nyaa.infiniteinfernal.event.IMobNearDeathEvent;
import cat.nyaa.infiniteinfernal.event.InfernalSpawnEvent;
import cat.nyaa.infiniteinfernal.event.LootDropEvent;
import cat.nyaa.infiniteinfernal.loot.ILootItem;
import cat.nyaa.infiniteinfernal.loot.IMessager;
import cat.nyaa.infiniteinfernal.loot.LootManager;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.mob.MobManager;
import cat.nyaa.infiniteinfernal.utils.Context;
import cat.nyaa.infiniteinfernal.utils.ContextKeys;
import cat.nyaa.infiniteinfernal.utils.Utils;
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
import org.bukkit.event.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.projectiles.ProjectileSource;

import java.util.List;
import java.util.stream.Collectors;

public class Events implements Listener {
    private InfPlugin plugin;

    public Events(InfPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
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

    @EventHandler(priority = EventPriority.LOWEST)
    public void onArmorStandHurt(EntityDamageEvent event){
        Entity entity = event.getEntity();
            if (entity.getScoreboardTags().contains("inf_damage_indicator")){
                event.setCancelled(true);
            }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
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
            double finalDamage = event.getFinalDamage();
            Utils.spawnDamageIndicator(iMob.getEntity(), finalDamage, I18n.format("damage.mob_hurt"));
        }
    }

    private void callNearDeathEvent(EntityDamageEvent event, IMob iMob) {
        IMobNearDeathEvent iMobNearDeathEvent = new IMobNearDeathEvent(iMob, iMob.getEntity());
        Bukkit.getPluginManager().callEvent(iMobNearDeathEvent);
        if (iMobNearDeathEvent.isCanceled()) {
            event.setDamage(0);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST,ignoreCancelled = true)
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
            double finalDamage = event.getFinalDamage();
            Utils.spawnDamageIndicator(iMob.getEntity(), finalDamage, I18n.format("damage.mob_hurt"));
        } else if (entity instanceof Player) {
            Entity damager = event.getDamager();
            if ((damager instanceof Projectile && ((Projectile) damager).getShooter() instanceof Player)){
                damager = (Entity) ((Projectile) damager).getShooter();
            }
            if (damager instanceof Player) {
                if (!event.isCancelled()) {
                    if (event.getFinalDamage() > 0) {
                        FirenlyFireControler.instance().onFriendlyFire(((Player) damager), ((Player) entity), event.getFinalDamage());
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onImobAttackLivingEntity(EntityDamageByEntityEvent ev) {
        if (!(ev.getEntity() instanceof LivingEntity)) return;
        IMob iMob;
        if (!MobManager.instance().isIMob(ev.getDamager())) {
            Entity damager = ev.getDamager();
            if (!(damager instanceof Projectile)) return;
            List<MetadataValue> metadata = damager.getMetadata(AbilityProjectile.INF_PROJECTILE_KEY);
            if (metadata.size() >= 1) {
                double damage = metadata.get(0).asDouble();
                ev.setDamage(damage);
                ProjectileSource shooter = ((Projectile) damager).getShooter();
                if (shooter == null) return;
                iMob = MobManager.instance().toIMob((Entity) shooter);
            }else {
                ProjectileSource shooter = ((Projectile) damager).getShooter();
                if (!(shooter instanceof LivingEntity))return;
                iMob = MobManager.instance().toIMob((Entity) shooter);
                if (iMob!=null){
                    ev.setDamage(iMob.getDamage());
                }else{
                    return;
                }
            }
        } else {
            iMob = MobManager.instance().toIMob(ev.getDamager());
        }

        if (iMob == null) return;
        IAbilitySet iAbilitySet = Utils.weightedRandomPick(iMob.getAbilities().stream()
                .filter(IAbilitySet::containsPassive)
                .collect(Collectors.toList()));

        if (iAbilitySet == null) return;
        if (iAbilitySet.containsDummy()) return;
        if (Context.instance().getDouble(iMob.getEntity().getUniqueId(), ContextKeys.DAMAGE_ATTACK_ABILITY)!=null){
            return;
        }
        List<AbilityAttack> attackAbilities = iAbilitySet.getAbilitiesInSet(AbilityAttack.class);
        if (attackAbilities.isEmpty()) return;
        attackAbilities.forEach(abilityAttack -> abilityAttack.onAttack(iMob, ((LivingEntity) ev.getEntity())));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
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
        if (event.getSpawnReason().equals(CreatureSpawnEvent.SpawnReason.SLIME_SPLIT)) {
            event.setCancelled(true);
            return;
        }
        InfPlugin.plugin.getSpawnControler().handleSpawnEvent(event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onIMobNearDeath(IMobNearDeathEvent ev) {
        List<IAbilitySet> abilities = ev.getMob().getAbilities();
        if (abilities.isEmpty()) return;
        abilities.forEach(iAbilitySet -> {
            List<AbilityNearDeath> abilitiesInSet = iAbilitySet.getAbilitiesInSet(AbilityNearDeath.class);
            if (abilitiesInSet.isEmpty()) return;
            abilitiesInSet.forEach(abilityNearDeath -> abilityNearDeath.onDeath(ev.getMob(), ev));
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
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

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPotion(EntityPotionEffectEvent ev){
        Entity entity = ev.getEntity();
        IMob iMob = MobManager.instance().toIMob(entity);
        if (iMob == null) {
            return;
        }
        PotionEffect oldEffect = ev.getOldEffect();
        PotionEffect newEffect = ev.getNewEffect();
        if (newEffect == null || oldEffect == null)return;
        if (newEffect.getAmplifier() < oldEffect.getAmplifier()) return;
        iMob.autoRetarget();
    }
}
