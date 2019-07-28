package cat.nyaa.infiniteinfernal.ability.impl.active;

import cat.nyaa.infiniteinfernal.ability.ActiveAbility;
import cat.nyaa.infiniteinfernal.configs.ParticleConfig;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.Utils;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;

public class AbilitySkillEffect extends ActiveAbility {
    @Serializable
    public ParticleConfig particle = new ParticleConfig();

    @Serializable
    public boolean particleEnabled = true;

    @Serializable
    public boolean soundEnabled = true;
    @Serializable
    public Sound sound = Sound.ENTITY_ELDER_GUARDIAN_CURSE;
    @Serializable
    public double pitch = 0.5;
    @Serializable
    public double volume = 1;

    @Override
    public String getName() {
        return "SkillEffect";
    }

    @Override
    public void active(IMob iMob) {
        LivingEntity entity = iMob.getEntity();
        Location location = entity.getLocation();
        World world = entity.getWorld();
        if (particleEnabled){
            world.spawnParticle(particle.type, location, particle.amount, particle.getOffsetX(), particle.getOffsetY(), particle.getOffsetZ(), particle.speed, Utils.parseExtraData(particle.extraData), particle.forced);
        }
        if (soundEnabled){
            world.playSound(location, sound, (float) volume, (float) pitch);
        }
    }
}
