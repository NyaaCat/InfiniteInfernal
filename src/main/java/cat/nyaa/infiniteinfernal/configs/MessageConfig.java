package cat.nyaa.infiniteinfernal.configs;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.nyaacore.Pair;
import cat.nyaa.nyaacore.configuration.FileConfigure;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class MessageConfig extends FileConfigure {
    private InfPlugin plugin;

    public MessageConfig(){
        plugin = InfPlugin.plugin;
    }

    @Serializable
    List<String> playerKill = new ArrayList<>();
    @Serializable
    List<String> mobKill = new ArrayList<>();
    @Serializable
    List<String> drop = new ArrayList<>();
    @Serializable
    List<String> specialDrop = new ArrayList<>();
    @Serializable
    List<String> noDrop = new ArrayList<>();

    @Override
    protected String getFileName() {
        return "messages.yml";
    }

    @Override
    protected JavaPlugin getPlugin() {
        return plugin;
    }
}
