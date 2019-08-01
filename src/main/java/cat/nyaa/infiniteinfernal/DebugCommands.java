package cat.nyaa.infiniteinfernal;

import cat.nyaa.nyaacore.CommandReceiver;
import cat.nyaa.nyaacore.ILocalizer;
import cat.nyaa.nyaacore.utils.NmsUtils;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.plugin.java.JavaPlugin;

public class DebugCommands extends CommandReceiver {

    public DebugCommands(JavaPlugin plugin, ILocalizer _i18n) {
        super(plugin, _i18n);
    }

    @Override
    public String getHelpPrefix() {
        return null;
    }

    @SubCommand("effectcloud")
    public void onEffectCloud(CommandSender sender, Arguments arguments){
        String tag = arguments.nextString();
        if (sender instanceof Player) {
            Player player = (Player) sender;
            Block targetBlock = player.getTargetBlock(null, 20);
            AreaEffectCloud spawn = targetBlock.getWorld().spawn(targetBlock.getLocation(), AreaEffectCloud.class);
            NmsUtils.setEntityTag(spawn, tag);
        }
    }

    @SubCommand("zombie")
    public void onZombie(CommandSender sender, Arguments arguments){
        String tag = arguments.nextString();
        if (sender instanceof Player) {
            Player player = (Player) sender;
            Block targetBlock = player.getTargetBlock(null, 20);
            Zombie spawn = targetBlock.getWorld().spawn(targetBlock.getLocation(), Zombie.class);
            NmsUtils.setEntityTag(spawn, tag);
        }
    }

    @SubCommand("phantom")
    public void onPhantom(CommandSender sender, Arguments arguments){
        String tag = arguments.nextString();
        if (sender instanceof Player) {
            Player player = (Player) sender;
            Block targetBlock = player.getTargetBlock(null, 20);
            Phantom spawn = targetBlock.getWorld().spawn(targetBlock.getLocation(), Phantom.class);
            NmsUtils.setEntityTag(spawn, tag);
        }
    }

    @SubCommand("creeper")
    public void onCreeper(CommandSender sender, Arguments arguments){
        String tag = arguments.nextString();
        if (sender instanceof Player) {
            Player player = (Player) sender;
            Block targetBlock = player.getTargetBlock(null, 20);
            Creeper spawn = targetBlock.getWorld().spawn(targetBlock.getLocation(), Creeper.class);
            NmsUtils.setEntityTag(spawn, tag);
        }
    }
}
