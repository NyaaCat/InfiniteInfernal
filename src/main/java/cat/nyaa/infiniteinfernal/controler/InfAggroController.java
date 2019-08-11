package cat.nyaa.infiniteinfernal.controler;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.configs.WorldConfig;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.ICorrector;
import cat.nyaa.infiniteinfernal.utils.Utils;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

public class InfAggroController implements IAggroControler {
    @Override
    public LivingEntity findAggroTarget(IMob iMob) {
        World world = iMob.getEntity().getWorld();
        WorldConfig worldConfig = InfPlugin.plugin.config().worlds.get(world.getName());
        if (worldConfig != null) {
            WorldConfig.AggroConfig aggro = worldConfig.aggro;
            WorldConfig.AggroConfig.RangeConfig range = aggro.range;
            ICorrector dec = aggro.getDec();
            ICorrector inc = aggro.getInc();
            List<Player> players = world.getPlayers();
            if (!players.isEmpty()) {
                Comparator<LivingEntity> comparator = Comparator.comparing(player -> {
                    double correctFactor = (1 + inc.getCorrection(player, null) - dec.getCorrection(player, null));
                    double aggroBase = InfPlugin.plugin.config().levelConfigs.get(iMob.getLevel()).attr.aggro;
                    return aggroBase * correctFactor;
                });
                Stream<LivingEntity> livingEntityStream = players.stream()
                        .filter(player -> {
                            if(!Utils.validGamemode(player))return false;
                            double correctFactor = (1 + inc.getCorrection(player, null) - dec.getCorrection(player, null));
                            double maxRange = range.max * correctFactor;
                            double minRange = range.min * correctFactor;
                            double distance = player.getLocation().distance(iMob.getEntity().getLocation());
                            double aggroBase = InfPlugin.plugin.config().levelConfigs.get(iMob.getLevel()).attr.aggro;
                            double aggroDistance = aggroBase * correctFactor;
                            return distance < Math.min(Math.min(maxRange, aggroDistance), Math.max(minRange, aggroDistance));
                        }).sorted(comparator.reversed())
                        .map(player -> player);
                return livingEntityStream.max(comparator).orElse(null);
            }
        }
        return null;
    }
}
