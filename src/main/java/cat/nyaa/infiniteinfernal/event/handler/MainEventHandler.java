package cat.nyaa.infiniteinfernal.event.handler;

import cat.nyaa.infiniteinfernal.I18n;
import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.configs.LevelConfig;
import cat.nyaa.infiniteinfernal.event.LootDropEvent;
import cat.nyaa.infiniteinfernal.event.MobNearDeathEvent;
import cat.nyaa.infiniteinfernal.event.MobSpawnEvent;
import cat.nyaa.infiniteinfernal.loot.ILootItem;
import cat.nyaa.infiniteinfernal.loot.IMessager;
import cat.nyaa.infiniteinfernal.loot.LootManager;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.mob.MobManager;
import cat.nyaa.infiniteinfernal.mob.ability.AbilityDeath;
import cat.nyaa.infiniteinfernal.mob.ability.IAbilitySet;
import cat.nyaa.infiniteinfernal.mob.ability.Triggers;
import cat.nyaa.infiniteinfernal.mob.ability.impl.active.AbilityProjectile;
import cat.nyaa.infiniteinfernal.mob.bossbar.BossbarManager;
import cat.nyaa.infiniteinfernal.mob.controller.FirenlyFireControler;
import cat.nyaa.infiniteinfernal.utils.Utils;
import cat.nyaa.infiniteinfernal.utils.context.Context;
import cat.nyaa.infiniteinfernal.utils.context.ContextKeys;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

public class MainEventHandler implements Listener {
    private InfPlugin plugin;

    public MainEventHandler(InfPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMobDeath(EntityDeathEvent ev) {
        World world = ev.getEntity().getWorld();
        if (!enabledInWorld(world))return;

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
//            if (killer == null) return;
            ILootItem loot = LootManager.makeDrop(killer, iMob);
            ILootItem specialLoot = LootManager.makeSpecialDrop(killer, iMob);
            ev.setDroppedExp(iMob.getExp());
            EntityDamageEvent lastDamageSource = iMob.getLastDamageCause();
            LootDropEvent lootDropEvent = new LootDropEvent(lastDamageSource, iMob, loot, specialLoot, ev);
            Bukkit.getPluginManager().callEvent(lootDropEvent);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onArmorStandHurt(EntityDamageEvent event) {
        World world = event.getEntity().getWorld();
        if (!enabledInWorld(world))return;

        Entity entity = event.getEntity();
        if (entity.getScoreboardTags().contains("inf_damage_indicator")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onDot(EntityDamageEvent event) {
        World world = event.getEntity().getWorld();
        if (!enabledInWorld(world))return;

        if (!InfPlugin.plugin.config().isTrueDamageEnabled())return;

        EntityDamageEvent.DamageCause cause = event.getCause();
        String type = cause.name().toLowerCase();
        double damage = InfPlugin.plugin.config().getTruedamage(type);
        if (damage == -1.01d){
            damage = event.getDamage();
        }
        if(!(event.getEntity() instanceof LivingEntity)) return;
        LivingEntity entity = (LivingEntity) event.getEntity();
        Collection<PotionEffect> activePotionEffects = entity.getActivePotionEffects();
        if (activePotionEffects.stream().anyMatch(potionEffect -> potionEffect.getType().equals(PotionEffectType.DAMAGE_RESISTANCE) && potionEffect.getAmplifier()>=4)){
            return;
        }
        entity.setHealth(Math.max(Math.min(entity.getHealth() - damage, entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()), 0.01));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMobHurt(EntityDamageEvent event) {
        World world = event.getEntity().getWorld();
        if (!enabledInWorld(world))return;

        Entity entity = event.getEntity();
        if (event instanceof EntityDamageByEntityEvent) return;
        if (MobManager.instance().isIMob(entity)) {
            IMob iMob = MobManager.instance().toIMob(entity);
            LevelConfig levelConfig = InfPlugin.plugin.config().levelConfigs.get(iMob.getLevel());
            double damageResistAmplifier;
            damageResistAmplifier = levelConfig == null? 1 : levelConfig.damageResistAmplifier;
            double origDamage = event.getDamage();

            double damageResist = iMob.getDamageResist() * damageResistAmplifier;
            double resist = origDamage * (damageResist / 100d);
            event.setDamage(Math.max(0, origDamage - resist));
            iMob.setLastDamageCause(event);

            List<IAbilitySet> abilities = iMob.getAbilities();

            iMob.triggerAbility(Triggers.HURT, event);

            if (event.getFinalDamage() > iMob.getEntity().getHealth()) {
                callNearDeathEvent(event, iMob);
            }
            BossbarManager.update(iMob);
            double finalDamage = event.getFinalDamage();
            Utils.spawnDamageIndicator(iMob.getEntity(), finalDamage, I18n.format("damage.mob_hurt"));
        }
    }

    private void callNearDeathEvent(EntityDamageEvent event, IMob iMob) {
        MobNearDeathEvent iMobNearDeathEvent = new MobNearDeathEvent(iMob, iMob.getEntity());
        Bukkit.getPluginManager().callEvent(iMobNearDeathEvent);
        if (iMobNearDeathEvent.isCanceled()) {
            event.setDamage(0);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMobHurtByEntity(EntityDamageByEntityEvent event) {
        World world = event.getEntity().getWorld();
        if (!enabledInWorld(world))return;

        Entity entity = event.getEntity();
        if (MobManager.instance().isIMob(entity)) {
            IMob iMob = MobManager.instance().toIMob(entity);
            iMob.setLastDamageCause(event);
            List<IAbilitySet> abilities = iMob.getAbilities();

            LevelConfig levelConfig = InfPlugin.plugin.config().levelConfigs.get(iMob.getLevel());
            double damageResistAmplifier;
            damageResistAmplifier = levelConfig == null? 1 : levelConfig.damageResistAmplifier;
            double origDamage = event.getDamage();

            double damageResist = iMob.getDamageResist() * damageResistAmplifier;
            double resist = origDamage * (damageResist / 100d);
            event.setDamage(Math.max(0, origDamage - resist));
            iMob.setLastDamageCause(event);

            iMob.triggerAbility(Triggers.HURT, event);

            if (event.getFinalDamage() > iMob.getEntity().getHealth()) {
                callNearDeathEvent(event, iMob);
            }
            double finalDamage = event.getFinalDamage();
            Utils.spawnDamageIndicator(iMob.getEntity(), finalDamage, I18n.format("damage.mob_hurt"));
        } else if (entity instanceof Player) {
            Entity damager = event.getDamager();
            if ((damager instanceof Projectile && ((Projectile) damager).getShooter() instanceof Player)) {
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

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onImobAttackLivingEntity(EntityDamageByEntityEvent ev) {
        World world = ev.getEntity().getWorld();
        if (!enabledInWorld(world))return;

        Difficulty difficulty = world.getDifficulty();
        if (!(ev.getEntity() instanceof LivingEntity)) return;
        IMob iMob;
        if (!MobManager.instance().isIMob(ev.getDamager())) {
            Entity damager = ev.getDamager();
            if (!(damager instanceof Projectile) || ev.getCause().equals(EntityDamageEvent.DamageCause.MAGIC)) return;
            List<MetadataValue> metadata = damager.getMetadata(AbilityProjectile.INF_PROJECTILE_KEY);
            if (metadata.size() >= 1) {
                double damage = metadata.get(0).asDouble();
                if (difficulty.equals(Difficulty.HARD)) {
                    damage *= 1.5;
                }else if (difficulty.equals(Difficulty.EASY)){
                    damage *= 0.6;
                }
                ev.setDamage(damage);
                ProjectileSource shooter = ((Projectile) damager).getShooter();
                if (shooter == null) return;
                iMob = MobManager.instance().toIMob((Entity) shooter);
            } else {
                ProjectileSource shooter = ((Projectile) damager).getShooter();
                if (!(shooter instanceof LivingEntity)) return;
                iMob = MobManager.instance().toIMob((Entity) shooter);
                if (iMob != null) {
                    double damage = iMob.getDamage();
                    if (difficulty.equals(Difficulty.HARD)) {
                        damage *= 1.5;
                    }else if (difficulty.equals(Difficulty.EASY)){
                        damage *= 0.6;
                    }
                    ev.setDamage(damage);
                } else {
                    return;
                }
            }
        } else {
            iMob = MobManager.instance().toIMob(ev.getDamager());
        }

        if (iMob == null) return;

        if (Context.instance().getDouble(iMob.getEntity().getUniqueId(), ContextKeys.DAMAGE_ATTACK_ABILITY) != null) {
            return;
        }
        iMob.triggerAbility(Triggers.ATTACK, ev);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onNatualMobSpawn(CreatureSpawnEvent event) {
        World world = event.getEntity().getWorld();
        if (!enabledInWorld(world))return;

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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onIMobNearDeath(MobNearDeathEvent ev) {
        World world = ev.getMob().getEntity().getWorld();
        if (!enabledInWorld(world))return;

        ev.getMob().triggerAllAbility(Triggers.NEARDEATH, ev);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInfMobSpawn(MobSpawnEvent ev) {
        World world = ev.getIMob().getEntity().getWorld();
        if (!enabledInWorld(world))return;

        IMob iMob = ev.getIMob();
        iMob.triggerAllAbility(Triggers.SPAWN, ev);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLootEvent(LootDropEvent ev) {
        World world = ev.getiMob().getEntity().getWorld();
        if (!enabledInWorld(world))return;

        List<ItemStack> drops = ev.getEntityDeathEvent().getDrops();
        drops.clear();
        IMessager iMessager = InfPlugin.plugin.getMessager();
        ILootItem normalLoot = ev.getLoot();
        ILootItem specialLoot = ev.getSpecialLoot();
        Entity killer = ev.getKiller();
        if (normalLoot != null) {
            iMessager.broadcastToWorld(ev.getiMob(), killer, normalLoot);
            drops.add(normalLoot.getItemStack());
        } else {
            iMessager.broadcastToWorld(ev.getiMob(), killer, null);
        }

        if (specialLoot != null) {
            iMessager.broadcastExtraToWorld(ev.getiMob(), killer, specialLoot);
            drops.add(specialLoot.getItemStack());
        }
        if (drops.isEmpty()) {
            drops.add(new ItemStack(Material.AIR));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPotion(EntityPotionEffectEvent ev) {
        World world = ev.getEntity().getWorld();
        if (!enabledInWorld(world))return;

        Entity entity = ev.getEntity();
        IMob iMob = MobManager.instance().toIMob(entity);
        if (iMob == null) {
            return;
        }
        PotionEffect oldEffect = ev.getOldEffect();
        PotionEffect newEffect = ev.getNewEffect();
        if (newEffect == null) return;
        if (oldEffect != null && newEffect.getAmplifier() < oldEffect.getAmplifier()) return;
        new BukkitRunnable() {
            @Override
            public void run() {
                iMob.autoRetarget();
            }
        }.runTaskLater(InfPlugin.plugin, 1);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
    public void onChunkUnload(ChunkUnloadEvent event){
        clearChunkEntities(event.getChunk());
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
    public void onChunkLoad(ChunkLoadEvent event){
        clearChunkEntities(event.getChunk());
    }

    private void clearChunkEntities(Chunk chunk) {
        Entity[] entities = chunk.getEntities();
        Stream.of(entities).filter(entity -> entity instanceof LivingEntity)
                .filter(entity -> {
                    Set<String> scoreboardTags = entity.getScoreboardTags();
                    List<String> tags = InfPlugin.plugin.config().tags;
                    return scoreboardTags.contains("inf_damage_indicator") ||
                            scoreboardTags.stream().anyMatch(tags::contains);
                })
                .forEach(entity -> entity.remove());
    }


    @EventHandler
    private void onTarget(EntityTargetEvent ev) {
        Entity entity = ev.getEntity();
        IMob iMob = MobManager.instance().toIMob(entity);
        if (iMob == null) return;
        Entity target = ev.getTarget();
        if (iMob.getEntityType().equals(EntityType.GUARDIAN) || iMob.getEntityType().equals(EntityType.ELDER_GUARDIAN)) {
            return;
        }
        LivingEntity currentTarget = iMob.getTarget();
        if (!Objects.equals(target, currentTarget)) {
            ev.setCancelled(true);
        }
    }

    private boolean enabledInWorld(World world) {
        return InfPlugin.plugin.config().isEnabledInWorld(world);
    }
}
