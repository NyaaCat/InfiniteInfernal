package cat.nyaa.infiniteinfernal.mob;

import cat.nyaa.infiniteinfernal.ability.IAbilitySet;
import cat.nyaa.infiniteinfernal.configs.MobConfig;
import cat.nyaa.infiniteinfernal.controler.Aggro;
import cat.nyaa.infiniteinfernal.loot.ILootItem;
import org.bukkit.Location;
import org.bukkit.boss.KeyedBossBar;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.List;
import java.util.Map;

public interface IMob {
    Map<ILootItem, Integer> getLoots();
    Map<ILootItem, Integer> getSpecialLoots();
    List<IAbilitySet> getAbilities();
    LivingEntity getEntity();
    EntityType getEntityType();
    KeyedBossBar getBossBar();
    LivingEntity getTarget();

    int getLevel();
    double getDamage();
    double getMaxHealth();
    double getSpecialChance();
    int getExp();
    boolean isAutoSpawn();
    boolean dropVanilla();
    boolean isDynamicHealth();
    String getName();
    String getTaggedName();

    void showParticleEffect();
    void makeInfernal(LivingEntity entity);
    void autoRetarget();
    void retarget(LivingEntity entity);
    void tweakHealth();

    Map<LivingEntity, Aggro> getNonPlayerTargets();

    boolean isTarget(LivingEntity target);
    MobConfig getConfig();

    void onDeath();

    EntityDamageEvent getLastDamageCause();
    void setLastDamageCause(EntityDamageEvent event);

    void updateBossBar(KeyedBossBar bossBar, LivingEntity entity);

    /**
     * Increment the counter tracking ticks without a valid target.
     * If threshold is exceeded, the mob will despawn.
     */
    void incrementNoTargetTicks();

    /**
     * Reset the no-target counter when a valid target is found.
     */
    void resetNoTargetTicks();

    /**
     * Update the last known position for stuck detection.
     * Should be called periodically to track movement.
     */
    void updateLastPosition();

    /**
     * Check if the mob is stuck (hasn't moved beyond threshold since last update).
     * @param threshold minimum distance to be considered "moved"
     * @return true if mob has moved less than threshold since last position update
     */
    boolean isStuck(double threshold);

    /**
     * Get the number of ticks since last significant movement.
     * @return ticks since last movement
     */
    int getStuckTicks();

    /**
     * Reset stuck tracking after teleportation or other intervention.
     */
    void resetStuckTracking();
}
