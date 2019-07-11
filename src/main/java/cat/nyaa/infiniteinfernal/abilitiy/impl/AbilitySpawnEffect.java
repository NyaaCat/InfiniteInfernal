package cat.nyaa.infiniteinfernal.abilitiy.impl;

import cat.nyaa.infiniteinfernal.abilitiy.AbilitySpawn;
import cat.nyaa.infiniteinfernal.mob.IMob;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;

public class AbilitySpawnEffect extends BaseAbility implements AbilitySpawn {
    @Serializable
    public Particle particle = Particle.LAVA;
    @Serializable
    public double offsetX = 0;
    @Serializable
    public double offsetY = 0;
    @Serializable
    public double offsetZ = 0;
    @Serializable
    public double speed = 1;
    @Serializable
    public int amount;
    @Serializable
    public boolean force = false;
    @Serializable
    public String extraData = "";
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
    public void onSpawn(IMob iMob) {
        LivingEntity entity = iMob.getEntity();
        Location location = entity.getLocation();
        World world = entity.getWorld();
        world.spawnParticle(particle, location, amount, offsetX, offsetY, offsetZ, speed, parseExtraData(), force);
        world.playSound(location, sound, (float) volume, (float) pitch);
    }

    private Object parseExtraData() {
        //todo
        return null;
    }

    @Override
    public String getName() {
        return "SpawnEffect";
    }
}
