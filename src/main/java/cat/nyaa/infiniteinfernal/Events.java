package cat.nyaa.infiniteinfernal;

import cat.nyaa.infiniteinfernal.ability.*;
import cat.nyaa.infiniteinfernal.ability.impl.active.AbilityProjectile;
import cat.nyaa.infiniteinfernal.configs.LevelConfig;
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
import org.bukkit.*;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Events implements Listener {
    private InfPlugin plugin;

    public Events(InfPlugin plugin) {
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
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDot(EntityDamageEvent event) {
        World world = event.getEntity().getWorld();
        if (!enabledInWorld(world))return;

        if (!InfPlugin.plugin.config().enableTrueDamage(world))return;

        EntityDamageEvent.DamageCause cause = event.getCause();
        String type = cause.name().toLowerCase();
        double damage = InfPlugin.plugin.config().getTrueDamageFor(type, world);
        if (damage == -1.01d){
            damage = event.getDamage();
        }
        if(!(event.getEntity() instanceof LivingEntity)) return;
        LivingEntity entity = (LivingEntity) event.getEntity();
        Collection<PotionEffect> activePotionEffects = entity.getActivePotionEffects();
        if (activePotionEffects.stream().anyMatch(potionEffect -> potionEffect.getType().equals(PotionEffectType.DAMAGE_RESISTANCE) && potionEffect.getAmplifier()>=4)){
            return;
        }
        entity.setHealth(Math.max(entity.getHealth() - damage, 0.01));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMobHurt(EntityDamageEvent event) {
        World world = event.getEntity().getWorld();
        if (!enabledInWorld(world))return;

        Entity entity = event.getEntity();
        if (event instanceof EntityDamageByEntityEvent) return;
        if (MobManager.instance().isIMob(entity)) {
            IMob iMob = MobManager.instance().toIMob(entity);
            LevelConfig levelConfig = InfPlugin.plugin.config.levelConfigs.get(iMob.getLevel());
            double damageResist;
            damageResist = levelConfig == null? 0 : levelConfig.attr.damageResist;
            if(damageResist!=0){
                double origDamage = event.getDamage();
                double resist = origDamage * (damageResist / 100d);
                event.setDamage(Math.max(0, origDamage - resist));
            }
            iMob.setLastDamageCause(event);
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMobHurtByEntity(EntityDamageByEntityEvent event) {
        World world = event.getEntity().getWorld();
        if (!enabledInWorld(world))return;

        Entity entity = event.getEntity();
        if (MobManager.instance().isIMob(entity)) {
            IMob iMob = MobManager.instance().toIMob(entity);
            iMob.setLastDamageCause(event);
            List<IAbilitySet> abilities = iMob.getAbilities();

            LevelConfig levelConfig = InfPlugin.plugin.config.levelConfigs.get(iMob.getLevel());
            double damageResist;
            damageResist = levelConfig == null? 0 : levelConfig.attr.damageResist;
            if(damageResist!=0){
                double origDamage = event.getDamage();
                double resist = origDamage * (damageResist / 100d);
                event.setDamage(Math.max(0, origDamage - resist));
            }
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onImobAttackLivingEntity(EntityDamageByEntityEvent ev) {
        World world = ev.getEntity().getWorld();
        if (!enabledInWorld(world))return;

        Difficulty difficulty = world.getDifficulty();
        if (!(ev.getEntity() instanceof LivingEntity)) return;
        IMob iMob;
        if (!MobManager.instance().isIMob(ev.getDamager())) {
            Entity damager = ev.getDamager();
            if (!(damager instanceof Projectile)) return;
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
        IAbilitySet iAbilitySet = Utils.weightedRandomPick(iMob.getAbilities().stream()
                .filter(IAbilitySet::containsPassive)
                .collect(Collectors.toList()));

        if (iAbilitySet == null) return;
        if (iAbilitySet.containsDummy()) return;
        if (Context.instance().getDouble(iMob.getEntity().getUniqueId(), ContextKeys.DAMAGE_ATTACK_ABILITY) != null) {
            return;
        }
        List<AbilityAttack> attackAbilities = iAbilitySet.getAbilitiesInSet(AbilityAttack.class);
        if (attackAbilities.isEmpty()) return;
        attackAbilities.forEach(abilityAttack -> abilityAttack.onAttack(iMob, ((LivingEntity) ev.getEntity())));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
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

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onIMobNearDeath(IMobNearDeathEvent ev) {
        World world = ev.getMob().getEntity().getWorld();
        if (!enabledInWorld(world))return;

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
        World world = ev.getIMob().getEntity().getWorld();
        if (!enabledInWorld(world))return;

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
