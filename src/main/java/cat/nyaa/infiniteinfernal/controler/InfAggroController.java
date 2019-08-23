package cat.nyaa.infiniteinfernal.controler;

import cat.nyaa.infiniteinfernal.Config;
import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.configs.WorldConfig;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.CorrectionParser;
import cat.nyaa.infiniteinfernal.utils.ICorrector;
import cat.nyaa.infiniteinfernal.utils.Utils;
import com.google.common.collect.Streams;
import jdk.nashorn.internal.ir.CallNode;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InfAggroController implements IAggroControler {
    @Override
    public LivingEntity findAggroTarget(IMob iMob) {
        World world = iMob.getEntity().getWorld();
        Config config = InfPlugin.plugin.config();
        WorldConfig worldConfig = config.worlds.get(world.getName());
        Map<String, String> addEffects = config.addEffects;
        String targetLost = addEffects.get("target_lost");
        String disorder = addEffects.get("disorder");
        if (worldConfig != null) {
            WorldConfig.AggroConfig aggro = worldConfig.aggro;
            WorldConfig.AggroConfig.RangeConfig range = aggro.range;
            ICorrector dec = aggro.getDec();
            ICorrector inc = aggro.getInc();
            ICorrector targetLostCorrection = CorrectionParser.parseStr(targetLost);
            ICorrector disorderCorrection = CorrectionParser.parseStr(disorder);
            double aggroBase = InfPlugin.plugin.config().levelConfigs.get(iMob.getLevel()).attr.aggro;

            if (targetLostCorrection!=null){
                aggroBase *= (1-(Utils.getCorrection(targetLostCorrection,iMob)/100d));
            }
            List<Player> players = world.getPlayers();
            if (!players.isEmpty()) {
                double finalAggroBase = aggroBase;
                Comparator<LivingEntity> comparator = Comparator.comparing(player -> {
                    double correctFactor = (100 + inc.getCorrection(player, null) - dec.getCorrection(player, null))/100d;
                    return finalAggroBase * correctFactor;
                });

                Stream<LivingEntity> livingEntityStream = players.stream()
                        .filter(player -> {
                            if(!Utils.validGamemode(player))return false;
                            double correctFactor = (100 + inc.getCorrection(player, null) - dec.getCorrection(player, null))/100d;
                            double maxRange = range.max * correctFactor;
                            double minRange = range.min * correctFactor;
                            double distance = player.getLocation().distance(iMob.getEntity().getLocation());
                            double baseAggro = InfPlugin.plugin.config().levelConfigs.get(iMob.getLevel()).attr.aggro;
                            double aggroDistance = baseAggro * correctFactor;
                            return distance < Math.min(Math.min(maxRange, aggroDistance), Math.max(minRange, aggroDistance));
                        }).map(player -> player);
                if (disorderCorrection != null){
                    double correction = Utils.getCorrection(disorderCorrection, iMob);
                    if (correction>0){
                        List<Entity> nearbyEntities = iMob.getEntity().getNearbyEntities(aggroBase, aggroBase, aggroBase);
                        Stream<LivingEntity> nearbyStream = nearbyEntities.stream()
                                .filter(entity1 -> entity1 instanceof LivingEntity && !(entity1 instanceof ArmorStand) && !(entity1 instanceof Player))
                                .map(entity1 -> ((LivingEntity) entity1));
                        livingEntityStream = Stream.concat(livingEntityStream,nearbyStream);
                        return Utils.randomPick(livingEntityStream.collect(Collectors.toList()));
                    }
                }
                return livingEntityStream.max(comparator).orElse(null);
            }
        }
        return null;
    }
}
