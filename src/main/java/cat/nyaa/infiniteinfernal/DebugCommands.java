package cat.nyaa.infiniteinfernal;

import cat.nyaa.infiniteinfernal.ability.impl.active.AbilityShingeki;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.nyaacore.CommandReceiver;
import cat.nyaa.nyaacore.ILocalizer;
import cat.nyaa.nyaacore.utils.NmsUtils;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

public class DebugCommands extends CommandReceiver {

    public DebugCommands(JavaPlugin plugin, ILocalizer _i18n) {
        super(plugin, _i18n);
    }

    @Override
    public String getHelpPrefix() {
        return null;
    }

    @SubCommand("damageNum")
    public void onDamageNum(CommandSender sender, Arguments arguments){
        double v = arguments.nextDouble();
        if (sender instanceof Player) {
            LivingEntity entity = ((Player) sender).getNearbyEntities(5, 5, 5).stream()
                    .filter(entity1 -> entity1 instanceof LivingEntity)
                    .map(entity1 -> ((LivingEntity) entity1)).findAny().orElse(((Player) sender));
            Location eyeLocation = entity.getEyeLocation();
            World world = entity.getWorld();
            ArmorStand spawn = world.spawn(eyeLocation, ArmorStand.class, item -> {
                item.setVelocity(new Vector(0,0.2,0.1));
                item.setPersistent(false);
                item.setInvulnerable(true);
                item.setSilent(true);
                item.setMarker(true);
                item.setVisible(false);
                item.setSmall(true);
                item.setCollidable(false);
                item.setCustomName(ChatColor.translateAlternateColorCodes('&',String.format("&c&l%.2f", v)));
                item.setCustomNameVisible(true);
                item.addScoreboardTag("inf_damage_indicator");
            });
            new BukkitRunnable(){
                @Override
                public void run() {
                    spawn.remove();
                }
            }.runTaskLater(InfPlugin.plugin, 20);

        }
    }

    @SubCommand("effectcloud")
    public void onEffectCloud(CommandSender sender, Arguments arguments) {
        String tag = arguments.nextString();
        if (sender instanceof Player) {
            Player player = (Player) sender;
            Block targetBlock = player.getTargetBlock(null, 20);
            AreaEffectCloud spawn = targetBlock.getWorld().spawn(targetBlock.getLocation(), AreaEffectCloud.class);
            NmsUtils.setEntityTag(spawn, tag);
        }
    }

    @SubCommand("zombie")
    public void onZombie(CommandSender sender, Arguments arguments) {
        String tag = arguments.nextString();
        if (sender instanceof Player) {
            Player player = (Player) sender;
            Block targetBlock = player.getTargetBlock(null, 20);
            Zombie spawn = targetBlock.getWorld().spawn(targetBlock.getLocation(), Zombie.class);
            NmsUtils.setEntityTag(spawn, tag);
        }
    }

    @SubCommand("phantom")
    public void onPhantom(CommandSender sender, Arguments arguments) {
        String tag = arguments.nextString();
        if (sender instanceof Player) {
            Player player = (Player) sender;
            Block targetBlock = player.getTargetBlock(null, 20);
            Phantom spawn = targetBlock.getWorld().spawn(targetBlock.getLocation(), Phantom.class);
            NmsUtils.setEntityTag(spawn, tag);
        }
    }

    @SubCommand("creeper")
    public void onCreeper(CommandSender sender, Arguments arguments) {
        String tag = arguments.nextString();
        if (sender instanceof Player) {
            Player player = (Player) sender;
            Block targetBlock = player.getTargetBlock(null, 20);
            Creeper spawn = targetBlock.getWorld().spawn(targetBlock.getLocation(), Creeper.class);
            NmsUtils.setEntityTag(spawn, tag);
        }
    }

    Vector yAxies = new Vector(0, 1, 0);
    Vector xAxies = new Vector(1, 0, 0);


    @SubCommand("star")
    public void onDrawStar(CommandSender sender, Arguments arguments) {
        if (!(sender instanceof Player)) {
            return;
        }
        String s = arguments.nextString();
        LivingEntity endEntity = (LivingEntity) ((Player) sender).getNearbyEntities(10, 10, 10).stream()
                .filter(entity -> entity instanceof LivingEntity).findAny().orElse(null);
        LivingEntity fromEntity = (LivingEntity) sender;
        switch (s) {
            case "whole":
                new LifestealEffect(fromEntity, endEntity).runTaskTimer(InfPlugin.plugin, 0, 1);
                break;
            case "x":
                double x = arguments.nextDouble();
                Location direction = endEntity.getEyeLocation().subtract(fromEntity.getEyeLocation()).multiply(x);
                Location loc = fromEntity.getEyeLocation().add(direction);
                spawnStarParticle(loc, direction.toVector(), x, endEntity.getEyeLocation().distance(fromEntity.getEyeLocation()));
        }
    }

    private void spawnStarParticle(Location location, Vector towards, double x, double totalLength) {
        Vector nonLinerVec;
        if (towards.getX() != 0 || towards.getZ() != 0) {
            nonLinerVec = yAxies;
        } else if (towards.getY() != 0) {
            nonLinerVec = xAxies;
        } else throw new IllegalArgumentException("towards 0");
        Vector crossProduct = towards.getCrossProduct(nonLinerVec);
        Vector v1 = towards.getCrossProduct(crossProduct).normalize().multiply(1000 * (distanceShift(x, totalLength)));
        Vector v2 = v1.clone().rotateAroundNonUnitAxis(towards, Math.toRadians(72));
        Vector v3 = v2.clone().rotateAroundNonUnitAxis(towards, Math.toRadians(72));
        Vector v4 = v3.clone().rotateAroundNonUnitAxis(towards, Math.toRadians(72));
        Vector v5 = v4.clone().rotateAroundNonUnitAxis(towards, Math.toRadians(72));
        spawnParticle(location.clone().add(v1), Particle.HEART);
        spawnParticle(location.clone().add(v2), Particle.HEART);
        spawnParticle(location.clone().add(v3), Particle.HEART);
        spawnParticle(location.clone().add(v4), Particle.HEART);
        spawnParticle(location.clone().add(v5), Particle.HEART);
    }

    private void spawnParticle(Location location, Particle particle) {
        World world = location.getWorld();
        if (world != null) {
            world.spawnParticle(particle, location, 1, 0.05d, 0.05d, 0.05d, 0, null, true);
        }
    }

    private double distanceShift(double distance, double totalLength) {
        //0.57x^2-3,89x^3+10.88x^4-7.56x^5
        double x = (distance / totalLength);
        return 0.57 * Math.pow(x, 2) - 3.89 * Math.pow(x, 3) + 10.88 * Math.pow(x, 4) - 7.56 * Math.pow(x, 5);
    }

    private class LifestealEffect extends BukkitRunnable {
        double totalLength;
        Location end;
        Location current;
        Particle particle = Particle.HEART;
        World world;
        LivingEntity endEntity;
        LivingEntity fromEntity;

        public LifestealEffect(LivingEntity target, LivingEntity mob) {
            totalLength = target.getLocation().distance(mob.getLocation());
            current = target.getEyeLocation();
            end = mob.getEyeLocation();
            world = target.getWorld();
            endEntity = mob;
            fromEntity = target;
        }

        @Override
        public void run() {
            if (totalLength < 2) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        Location towards = end.clone().subtract(current).multiply(0.1);
                        for (int i = 0; i < 10; i++) {
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    Location clone = current.clone();
                                    clone.add(towards);
                                    spawnParticle(towards, particle);
                                    current = clone;
                                }
                            }.runTaskLater(InfPlugin.plugin, i);
                        }
                    }
                }.runTaskAsynchronously(InfPlugin.plugin);
            } else {
                new BukkitRunnable() {
                    AtomicDouble remains = new AtomicDouble(0);

                    @Override
                    public void run() {
                        Location eyeLocation = endEntity.getEyeLocation();
                        Location fromLocation = fromEntity.getEyeLocation();
                        if (Objects.equals(eyeLocation.getWorld(), fromLocation.getWorld())) {
                            totalLength = eyeLocation.distance(fromLocation);
                        }
                        end = eyeLocation;
                        Vector direction = end.clone().subtract(current).toVector().normalize().multiply(0.25);
                        double distance = end.distance(current);
                        if (distance < 0.5) {
                            this.cancel();
                            return;
                        }
                        double x = distance / totalLength;
                        double lengthInTick =( speedShift(x) / 20) + remains.getAndSet(0);
                        while ((lengthInTick -= 0.25) >= 0) {
                            distance = end.distance(current);
                            if (distance < 0.5) {
                                this.cancel();
                                return;
                            }
                            x = distance / totalLength;
                            spawnStarParticle(current, direction, x);
                            current.add(direction);
                        }
                        remains.set(lengthInTick + 0.25);
                    }
                }.runTaskTimer(InfPlugin.plugin, 0, 1);
            }
        }

        private void spawnParticle(Location location, Particle particle) {
            World world = location.getWorld();
            if (world != null) {
                world.spawnParticle(particle, location, 1, 0, 0, 0, 0, null, true);
            }
        }

        Vector yAxies = new Vector(0, 1, 0);
        Vector xAxies = new Vector(1, 0, 0);

        private void spawnStarParticle(Location location, Vector towards, double x) {
            Vector nonLinerVec;
            if (towards.getX() != 0 || towards.getZ() != 0) {
                nonLinerVec = yAxies;
            } else if (towards.getY() != 0) {
                nonLinerVec = xAxies;
            } else throw new IllegalArgumentException("towards 0");
            Vector crossProduct = towards.getCrossProduct(nonLinerVec);
            Vector v1 = crossProduct.getCrossProduct(towards).normalize().multiply(3*(distanceShift(x)));
            Vector v2 = v1.clone().rotateAroundAxis(towards, Math.toRadians(72));
            Vector v3 = v2.clone().rotateAroundAxis(towards, Math.toRadians(72));
            Vector v4 = v3.clone().rotateAroundAxis(towards, Math.toRadians(72));
            Vector v5 = v4.clone().rotateAroundAxis(towards, Math.toRadians(72));
            spawnParticle(location.clone().add(v1), particle);
            spawnParticle(location.clone().add(v2), particle);
            spawnParticle(location.clone().add(v3), particle);
            spawnParticle(location.clone().add(v4), particle);
            spawnParticle(location.clone().add(v5), particle);

        }

        private double distanceShift(double x) {
            //0.57x^2-3,89x^3+10.88x^4-7.56x^5
            return 0.57 * Math.pow(x, 2) - 3.89 * Math.pow(x, 3) + 10.88 * Math.pow(x, 4) - 7.56 * Math.pow(x, 5);
        }

        private double speedShift(double x) {
            return Math.pow(x,6) * (4.5) + 0.5;
        }
    }

    @SubCommand("shingeki")
    public void onShingeki(CommandSender sender, Arguments arguments){
        if (sender instanceof Player) {
            Block targetBlock = ((Player) sender).getTargetBlock(null, 50);
            if(targetBlock.getType().isSolid()){
                Block relative = targetBlock.getRelative(BlockFace.UP);
                if (!relative.getType().isSolid()){
                    strikeShingeki(relative.getLocation());
                    return;
                }else {
                    for (BlockFace value : BlockFace.values()) {
                        Block relative1 = targetBlock.getRelative(value);
                        if (!relative1.getType().isSolid()) {
                            strikeShingeki(relative1.getLocation());
                            return;
                        }
                    }
                }
            }
        }
    }

    private void strikeShingeki(Location location) {
        AbilityShingeki abilityShingeki = new AbilityShingeki();
        abilityShingeki.delay = 60;
        try {
            Method showEffect = abilityShingeki.getClass().getDeclaredMethod("showEffect", Location.class);
            showEffect.setAccessible(true);
            showEffect.invoke(abilityShingeki, location);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
