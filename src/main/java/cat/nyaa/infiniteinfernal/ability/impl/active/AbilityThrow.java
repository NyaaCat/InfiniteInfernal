package cat.nyaa.infiniteinfernal.ability.impl.active;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.ability.ActiveAbility;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.nyaacore.utils.ReflectionUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Projectile;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;
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

    @SuppressWarnings("deprecation")
    private void summonEntity(LivingEntity target) {
        try {
            Location loc = target.getEyeLocation().clone();
            Class craftWorld = ReflectionUtils.getOBCClass("CraftWorld");
            Method getHandleMethod = ReflectionUtils.getMethod(craftWorld, "getHandle");
            Object worldServer = getHandleMethod.invoke(loc.getWorld());
            Class<?> chunkRegionLoader = ReflectionUtils.getNMSClass("ChunkRegionLoader");
            Class<?> mojangsonParser = ReflectionUtils.getNMSClass("MojangsonParser");
            Method getTagFromJson = mojangsonParser.getMethod("parse", String.class);
            Class<?> nbtTagCompound = ReflectionUtils.getNMSClass("NBTTagCompound");
            Method setString = nbtTagCompound.getMethod("setString", String.class, String.class);
            Class<?> nmsEntity = ReflectionUtils.getNMSClass("Entity");
            Method getUUID = nmsEntity.getMethod("getUniqueID");
            Method setPositionRotation = nmsEntity.getMethod("setPositionRotation", double.class, double.class, double.class, float.class, float.class);
            Method spawnEntity = chunkRegionLoader.getMethod("a", nbtTagCompound, ReflectionUtils.getNMSClass("World"), double.class, double.class, double.class, boolean.class);
            Object nbt;
            try {
                nbt = getTagFromJson.invoke(null, entityData.replaceAll("\\{player}", target.getName()).replaceAll("\\{playerUUID}", target.getUniqueId().toString()));
            } catch (Exception e) {
                InfPlugin.plugin.getLogger().log(Level.WARNING, "wrong config", e);
                return;
            }
            setString.invoke(nbt, "id", entityName);
            Object entity = spawnEntity.invoke(null, nbt, worldServer, loc.getX(), loc.getY(), loc.getZ(), true);
            if (entity != null) {
                setPositionRotation.invoke(entity, loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
                UUID uuid = (UUID) getUUID.invoke(entity);
                Entity e = Bukkit.getEntity(uuid);
                if (e != null) {
                    if (e instanceof Projectile) {
                        ((Projectile) e).setShooter(target);
                    }
                    e.setVelocity(loc.getDirection().multiply(speed));
                }
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            InfPlugin.plugin.getLogger().log(Level.WARNING, "Execption spawning entity", e);
        }
    }

    @Override
    public String getName() {
        return "Throw";
    }
}
