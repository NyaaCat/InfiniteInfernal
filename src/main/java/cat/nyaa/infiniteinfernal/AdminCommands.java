package cat.nyaa.infiniteinfernal;

import cat.nyaa.nyaacore.CommandReceiver;
import cat.nyaa.nyaacore.ILocalizer;
import org.bukkit.plugin.java.JavaPlugin;

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

    @SubCommand(value = "reload", permission = "infiniteinfernal.admin")
    public void onReload(){
        plugin.onReload();
    }
}
