package cat.nyaa.infiniteinfernal;

import cat.nyaa.infiniteinfernal.mob.MobManager;
import cat.nyaa.nyaacore.CommandReceiver;
import cat.nyaa.nyaacore.ILocalizer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

public class AdminCommands extends CommandReceiver {
    private InfPlugin plugin;

    public AdminCommands(InfPlugin plugin, ILocalizer _i18n) {
        super(plugin, _i18n);
        this.plugin = plugin;
    }

    @Override
    public String getHelpPrefix() {
        return null;
    }

    @SubCommand(value = "reload", permission = "im.admin")
    public void onReload(CommandSender sender, Arguments arguments){
        plugin.onReload();
    }

    @SubCommand(value = "spawn", permission = "im.spawnmob")
    public void onSpawn(CommandSender sender, Arguments arguments){
        String mobId = arguments.nextString();
        String worldName = arguments.nextString();
        double x = arguments.nextDouble();
        double y = arguments.nextDouble();
        double z = arguments.nextDouble();
        String top = arguments.top();
        Integer level = top ==null ? null : Integer.valueOf(top);
        World world = Bukkit.getWorld(worldName);
        MobManager.instance().spawnMobById(mobId, new Location(world, x, y, z), level);
    }

    @SubCommand(value = "addloot", permission = "im.addloot")
    public void onAddLoot(CommandSender sender, Arguments arguments){

    }
}
