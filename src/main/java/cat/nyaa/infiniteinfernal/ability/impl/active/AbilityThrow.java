package cat.nyaa.infiniteinfernal.ability.impl.active;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.ability.ActiveAbility;
import cat.nyaa.infiniteinfernal.mob.IMob;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;

import java.util.logging.Level;

public class AbilityThrow extends ActiveAbility {

    @Serializable
    public String entityData = "";
    @Serializable
    public String entityName = "";
    @Serializable
    public double speed = 3;

    @Override
    public void active(IMob iMob) {
        LivingEntity mobEntity = iMob.getEntity();
        if (mobEntity instanceof Mob){
            LivingEntity target = ((Mob) mobEntity).getTarget();
            if (target != null){
                summonEntity(target);
            }
        }
    }

    private void summonEntity(LivingEntity target) {
        // NMS-based entity spawning with custom NBT is not supported in Paper 1.21+
        // This ability requires reimplementation using the modern Paper API
        InfPlugin.plugin.getLogger().log(Level.WARNING, "AbilityThrow is not supported in this Minecraft version");
    }

    @Override
    public String getName() {
        return "Throw";
    }
}
