package cat.nyaa.infiniteinfernal.ability.impl.passive;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.ability.AbilityActive;
import cat.nyaa.infiniteinfernal.ability.AbilityAttack;
import cat.nyaa.infiniteinfernal.ability.AbilityPassive;
import cat.nyaa.infiniteinfernal.ability.AbilitySpawn;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class AbilityTag extends AbilityPassive implements AbilitySpawn, AbilityActive, AbilityAttack {

    @Serializable
    public String tag = "example_tag";

    @Serializable
    public String action = "ADD";

    @Serializable
    public String target = "self";

    @Serializable
    public String triggerMode = "SPAWN";

    @Serializable
    public int ttl = -1;

    @Serializable
    public double radius = 25;

    @Override
    public String getName() {
        return "Tag";
    }

    @Override
    public void onSpawn(IMob iMob) {
        if (!"SPAWN".equalsIgnoreCase(triggerMode)) {
            return;
        }
        applyTag(iMob, null);
    }

    @Override
    public void active(IMob iMob) {
        if (!"ACTIVE".equalsIgnoreCase(triggerMode)) {
            return;
        }
        applyTag(iMob, null);
    }

    @Override
    public void onAttack(IMob mob, LivingEntity attackedTarget) {
        if (!"ATTACK".equalsIgnoreCase(triggerMode)) {
            return;
        }
        applyTag(mob, attackedTarget);
    }

    private void applyTag(IMob iMob, LivingEntity attackedTarget) {
        if ("self".equalsIgnoreCase(target)) {
            applyToEntity(iMob.getEntity());
        } else if ("target".equalsIgnoreCase(target)) {
            if (attackedTarget != null) {
                applyToEntity(attackedTarget);
            } else {
                List<LivingEntity> targets = Utils.getValidTargets(
                        iMob,
                        iMob.getEntity().getNearbyEntities(radius, radius, radius)
                ).collect(Collectors.toList());
                for (LivingEntity targetEntity : targets) {
                    applyToEntity(targetEntity);
                }
            }
        }
    }

    private void applyToEntity(Entity entity) {
        boolean isAdd = "ADD".equalsIgnoreCase(action);

        if (isAdd) {
            entity.addScoreboardTag(tag);
        } else {
            entity.removeScoreboardTag(tag);
        }

        int effectiveTtl = ttl;
        if ("target".equalsIgnoreCase(target) && ttl < 0) {
            Bukkit.getLogger().warning("[InfiniteInfernal] AbilityTag: target='target' requires ttl>=0. Defaulting to ttl=1.");
            effectiveTtl = 1;
        }

        if (effectiveTtl >= 0) {
            UUID entityId = entity.getUniqueId();
            new BukkitRunnable() {
                @Override
                public void run() {
                    Entity e = Bukkit.getEntity(entityId);
                    if (e != null && !e.isDead()) {
                        if (isAdd) {
                            e.removeScoreboardTag(tag);
                        } else {
                            e.addScoreboardTag(tag);
                        }
                    }
                }
            }.runTaskLater(InfPlugin.plugin, Math.max(1, effectiveTtl));
        }
    }
}
