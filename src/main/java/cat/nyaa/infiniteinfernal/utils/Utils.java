package cat.nyaa.infiniteinfernal.utils;

import cat.nyaa.infiniteinfernal.BroadcastManager;
import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.configs.BroadcastMode;
import cat.nyaa.infiniteinfernal.configs.IllegalConfigException;
import cat.nyaa.infiniteinfernal.configs.ParticleConfig;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.correction.ICorrector;
import cat.nyaa.nyaacore.cmdreceiver.Arguments;
import cat.nyaa.nyaacore.utils.HexColorUtils;
import cat.nyaa.nyaacore.utils.InventoryUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Utils {
   
    public static String getTaggedName(String nameTag, EntityType type, String name, String level) {
        String levelPrefix = InfPlugin.plugin.config().levelConfigs.get(level).prefix;
        return nameTag.replaceAll("\\{level\\.prefix}", levelPrefix)
                .replaceAll("\\{mob\\.name}", name)
                .replaceAll("\\{mob\\.type}", type.name())
                .replaceAll("\\{level\\.level}", String.valueOf(level));
    }
    
    public static Vector unitDirectionVector(Vector from, Vector to) {
        Vector vec = to.clone().subtract(from);
        if (!Double.isFinite(vec.getX())) vec.setX(0D);
        if (!Double.isFinite(vec.getY())) vec.setY(0D);
        if (!Double.isFinite(vec.getZ())) vec.setZ(0D);
        if (vec.lengthSquared() == 0) return new Vector(0, 0, 0);
        return vec.normalize();
    }

    public static boolean validGamemode(Player entity) {
        GameMode gameMode = entity.getGameMode();
        return gameMode.equals(GameMode.SURVIVAL) || gameMode.equals(GameMode.ADVENTURE);
    }

    public static void removeEntityLater(Entity ent, int i) {
        new BukkitRunnable() {
            @Override
            public void run() {
                ent.remove();
            }
        }.runTaskLater(InfPlugin.plugin, i);
    }

    //todo: implement another to Aggro system
    public static Stream<LivingEntity> getValidTargets(IMob iMob, Collection<Entity> nearbyEntities) {
        return nearbyEntities.stream()
                .filter(entity -> (entity instanceof Player && validGamemode((Player) entity)) || (iMob != null && (entity instanceof LivingEntity && iMob.isTarget((LivingEntity) entity))))
                .map(entity -> ((LivingEntity) entity));
    }

    public static void doEffect(String effect, LivingEntity target, int duration, int amplifier, String ability) {
        PotionEffectType eff = PotionEffectType.getByName(effect);
        if (eff != null) {
            PotionEffect potionEffect = target.getPotionEffect(eff);
            if (potionEffect != null && potionEffect.getAmplifier() > amplifier) {
                return;
            }
            target.removePotionEffect(eff);
            target.addPotionEffect(eff.createEffect(duration, amplifier), true);
        } else {
            throw new IllegalConfigException("effect " + effect + " in ability " + ability + " don't exists");
        }
    }

    public static PotionEffectType parseEffect(String effect, String ability) {
        PotionEffectType eff = PotionEffectType.getByName(effect);
        if (eff != null) {
            return eff;
        } else {
            throw new IllegalConfigException("effect " + effect + " in ability " + ability + " don't exists");
        }
    }
    
    public static final Vector X_AXIS = new Vector(1, 0, 0);
    public static final Vector Y_AXIS = new Vector(0, 1, 0);
    public static final Vector Z_AXIS = new Vector(0, 0, 1);

    public static Vector cone(Vector direction, double cone) {
        double phi = RandomUtil.random() * 360;
        double theta = RandomUtil.random() * cone;
        Vector clone = direction.clone();
        Vector crossP;

        if (clone.length() == 0) return direction;

        if (clone.getX() != 0 && clone.getZ() != 0) {
            crossP = clone.getCrossProduct(Y_AXIS);
        } else if (clone.getX() != 0 && clone.getY() != 0) {
            crossP = clone.getCrossProduct(Z_AXIS);
        } else {
            crossP = clone.getCrossProduct(X_AXIS);
        }
        crossP.normalize();

        clone.add(crossP.multiply(Math.tan(Math.toRadians(theta))));
        clone.rotateAroundAxis(direction, Math.toRadians(phi));
        return clone;
    }

    public static Object parseExtraData(String extraData) {
        try {
            String[] split = extraData.split(",", 4);
            int r = Integer.parseInt(split[0]);
            int g = Integer.parseInt(split[1]);
            int b = Integer.parseInt(split[2]);
            float size = Float.parseFloat(split[3]);
            return new Particle.DustOptions(Color.fromRGB(r, g, b), size);
        } catch (Exception e) {
            return null;
        }
    }

    public static List<Location> getRoundLocations(Location location, double radius) {
        int spawns = Math.max(1, (int) Math.round(Math.PI * radius * 8));
        List<Location> locations = new ArrayList<>(spawns);
        double angle = Math.toRadians(360f / spawns);
        Vector vector = new Vector(radius, 0, 0);
        if (radius == 0) {

        }
        for (int i = 0; i < spawns; i++) {
            Location center = location.clone();
            Vector rotated = vector.rotateAroundY(angle * i);
            Location loc = center.add(rotated);
            locations.add(loc);
        }
        return locations;
    }

    public static List<Location> drawHexStar(List<Location> anchors) {
        if (anchors.size() != 6) {
            throw new IllegalArgumentException("not a valid anchors for a hex star");
        }
        List<Location> result = new ArrayList<>();
        List<Location> locations;
        List<Location> locations1;
        double length;
        int spawns;

        Location a0 = anchors.get(0);
        Location a2 = anchors.get(2);
        Location a3 = anchors.get(3);
        Location a5 = anchors.get(5);

        length = a0.distance(a2);
        spawns = (int) Math.max(3, Math.floor(length * 4));
        locations = drawLine(a0, a2, spawns);
        locations1 = drawLine(a3, a5, spawns);
        for (int i = 0; i < spawns; i++) {
            result.add(locations.get(i));
            result.add(locations1.get(i));
        }

        Location a4 = anchors.get(4);
        Location a1 = anchors.get(1);
        locations = drawLine(a2, a4, spawns);
        locations1 = drawLine(a5, a1, spawns);
        for (int i = 0; i < spawns; i++) {
            result.add(locations.get(i));
            result.add(locations1.get(i));
        }

        locations = drawLine(a4, a0, spawns);
        locations1 = drawLine(a1, a3, spawns);
        for (int i = 0; i < spawns; i++) {
            result.add(locations.get(i));
            result.add(locations1.get(i));
        }

        return result;
    }

    public static List<Location> drawLine(Location l1, Location l2, int spawns) {
        ArrayList<Location> locations = new ArrayList<>();
        Location clone = l1.clone();
        Vector direction = l2.clone().subtract(l1).toVector().multiply(1d / ((double) spawns));
        for (int i = 0; i < spawns; i++) {
            Location nextLoc = clone.clone();
            locations.add(nextLoc.add(direction));
            clone = nextLoc;
        }
        return locations;
    }

    public static void spawnParticle(ParticleConfig particleConfig, World world, Location location) {
        world.spawnParticle(
                particleConfig.type,
                location,
                particleConfig.amount,
                particleConfig.getOffsetX(),
                particleConfig.getOffsetY(),
                particleConfig.getOffsetZ(),
                particleConfig.speed,
                parseExtraData(particleConfig.extraData),
                particleConfig.forced
        );
    }

    public static void spawnDamageIndicator(LivingEntity entity, double damage, String format) {
        Location eyeLocation = entity.getEyeLocation();
        World world = entity.getWorld();
        Double x = RandomUtil.random(-1, 1);
        Double y = RandomUtil.random(-1, 1);
        Double z = RandomUtil.random(-1, 1);
        Vector vector = new Vector(x, y, z);
//        Vector vector = new Vector(0, 0.5, 0.2).rotateAroundAxis(new Vector(0, 1, 0), Math.toRadians(RandomUtil.random(0, 360)));
        ArmorStand spawn = world.spawn(eyeLocation.add(vector), ArmorStand.class, item -> {
            item.addScoreboardTag("inf_damage_indicator");
//            item.setVelocity(vector);
            item.setInvulnerable(true);
            item.setSilent(true);
            item.setMarker(true);
            item.setVisible(false);
            item.setSmall(true);
            item.setCollidable(false);
            item.setCustomName(HexColorUtils.hexColored( String.format(format, damage)));
            new BukkitRunnable() {
                @Override
                public void run() {
                    item.setCustomNameVisible(true);
                }
            }.runTaskLater(InfPlugin.plugin, 2);
        });
        new BukkitRunnable() {
            @Override
            public void run() {
                spawn.remove();
            }
        }.runTaskLater(InfPlugin.plugin, 30);
    }

    public static double getCorrection(ICorrector targetLost, IMob iMob) {
        LivingEntity entity = iMob.getEntity();
        EntityEquipment equipment = entity.getEquipment();
        ItemStack itemInMainHand = null;
        if (equipment != null) {
            itemInMainHand = equipment.getItemInMainHand();
        }
        return targetLost.getCorrection(entity, itemInMainHand);
    }

    public static void addToPlayer(Player player, ItemStack itemStack) {
        if (!InventoryUtils.addItem(player, itemStack)) {
            Location location = player.getLocation();
            World world = player.getWorld();
            world.dropItem(location, itemStack);
        }
    }

    public static boolean shouldReceiveMessage(Entity killer, Player player) {
        BroadcastManager broadcastManager = InfPlugin.plugin.getBroadcastManager();
        BroadcastMode receiveType = broadcastManager.getReceiveType(player.getWorld(), player.getUniqueId().toString());
        switch (receiveType) {
            case ALL:
                return true;
            case NEARBY:
                return player.getWorld().equals(killer.getWorld()) && player.getLocation().distance(killer.getLocation()) < broadcastManager.getNearbyRange(player.getWorld());
            case SELF_ONLY:
                return killer.equals(player) || (!(killer instanceof Player) && player.getWorld().equals(killer.getWorld()) && player.getLocation().distance(killer.getLocation()) < broadcastManager.getNearbyRange(player.getWorld()));
            case OFF:
                return false;
        }
        return true;
    }

    public static String colored(String str) {
        return HexColorUtils.hexColored(str);
    }

    public static List<String> filtered(Arguments arguments, List<String> completeStr) {
        String next = arguments.at(arguments.length() - 1);
        return completeStr.stream().filter(s -> s.startsWith(next)).collect(Collectors.toList());
    }
}
