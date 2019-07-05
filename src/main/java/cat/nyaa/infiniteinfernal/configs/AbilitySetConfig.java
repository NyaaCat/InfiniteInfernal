package cat.nyaa.infiniteinfernal.configs;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.nyaacore.configuration.FileConfigure;
import org.bukkit.plugin.java.JavaPlugin;

public class AbilitySetConfig extends FileConfigure {
    private InfPlugin plugin;
    private int id;

    public AbilitySetConfig(InfPlugin plugin, int id){
        this.plugin = plugin;
        this.id = id;
    }

    @Override
    protected String getFileName() {
        return "abilities/set-"+id+".yml";
    }

    @Override
    protected JavaPlugin getPlugin() {
        return plugin;
    }
}
