package cat.nyaa.infiniteinfernal.mob.controller;

import cat.nyaa.infiniteinfernal.Config;
import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.CorrectionParser;
import cat.nyaa.infiniteinfernal.utils.ICorrector;
import cat.nyaa.infiniteinfernal.utils.Utils;
import org.bukkit.World;
import org.bukkit.entity.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InfAggroController implements IAggroControler {
    @Override
    public LivingEntity findAggroTarget(IMob iMob) {
        World world = iMob.getEntity().getWorld();
        Config config = InfPlugin.plugin.config();
        Map<String, String> addEffects = config.addEffects;
        String targetLost = addEffects.get("target_lost");
        String disorder = addEffects.get("disorder");
        ICorrector dec = config.getAggroDec();
        ICorrector inc = config.getAggroInc();
        ICorrector targetLostCorrection = CorrectionParser.parseStr(targetLost);
        ICorrector disorderCorrection = CorrectionParser.parseStr(disorder);
        double aggroBase = InfPlugin.plugin.config().aggroBase;
        if (targetLostCorrection!=null){
            aggroBase *= (1-(Utils.getCorrection(targetLostCorrection,iMob)/100d));
        }
        if (iMob.getEntityType().equals(EntityType.GUARDIAN) || iMob.getEntityType().equals(EntityType.ELDER_GUARDIAN)){
            aggroBase *= 0.6667;
        }
        List<Player> players = world.getPlayers();
        if (!players.isEmpty()) {
            double finalAggroBase = aggroBase;
            Comparator<LivingEntity> comparator = Comparator.comparing(player -> {
                double correctFactor = (100 + inc.getCorrection(player, null) - dec.getCorrection(player, null))/100d;
                return finalAggroBase * correctFactor;
            });
            Map<UUID, Double> aggroMap = new HashMap<>();
            Stream<LivingEntity> livingEntityStream = players.stream()
                    .filter(player -> {
                        if(!Utils.validGamemode(player))return false;
                        double correctFactor = (100 + inc.getCorrection(player, null) - dec.getCorrection(player, null))/100d;
                        double maxRange = config.aggroRangeMax * correctFactor;
                        double minRange = config.aggroRangeMin * correctFactor;
                        double distance = player.getLocation().distance(iMob.getEntity().getLocation());
//                           double baseAggro = InfPlugin.plugin.config().levelConfigs.get(iMob.getLevel()).attr.aggro;
                        double baseAggro = finalAggroBase;
                        double aggroDistance = baseAggro * correctFactor;
                        aggroMap.put(player.getUniqueId(), aggroDistance);
                        return distance < Math.min(Math.min(maxRange, aggroDistance), Math.max(minRange, aggroDistance));
                    }).sorted(Comparator.comparingDouble(player -> (aggroMap.getOrDefault(player.getUniqueId(), 0d) * 100) + player.getLocation().distance(iMob.getEntity().getLocation()))).map(player -> player);
            if (disorderCorrection != null){
                double correction = Utils.getCorrection(disorderCorrection, iMob);
                if (correction>0){
                    List<Entity> nearbyEntities = iMob.getEntity().getNearbyEntities(aggroBase, aggroBase, aggroBase);
                    Stream<LivingEntity> nearbyStream = nearbyEntities.stream()
                            .filter(entity1 -> entity1 instanceof LivingEntity && !(entity1 instanceof ArmorStand) && !(entity1 instanceof Player))
                            .map(entity1 -> ((LivingEntity) entity1));
                    livingEntityStream = nearbyStream;
                    return Utils.randomPick(livingEntityStream.collect(Collectors.toList()));
                }
            }
            return livingEntityStream.max(comparator).orElse(null);
        }
        return null;
    }
}
