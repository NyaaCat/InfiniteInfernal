package cat.nyaa.infiniteinfernal.configs;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.nyaacore.configuration.FileConfigure;
import org.bukkit.plugin.java.JavaPlugin;

public class LevelConfig extends FileConfigure {
    private final InfPlugin plugin;
    private int level;

    public LevelConfig(InfPlugin plugin, int level){
        this.plugin = plugin;
        this.level = level;
    }

    @Override
    protected String getFileName() {
        return "level-"+level+".yml";
    }

    @Override
    protected JavaPlugin getPlugin() {
        return plugin;
    }
}
