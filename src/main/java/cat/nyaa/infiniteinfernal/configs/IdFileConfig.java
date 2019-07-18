package cat.nyaa.infiniteinfernal.configs;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.nyaacore.configuration.FileConfigure;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class IdFileConfig extends FileConfigure {
    private int id;

    public IdFileConfig(int id){
        this.id =id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId(){
        return id;
    }

    public abstract String getPrefix();
    public abstract String getDir();

    @Override
    protected String getFileName() {
        return getDir()+"/"+getPrefix()+"-"+id+".yml";
    }

    @Override
    protected JavaPlugin getPlugin() {
        return InfPlugin.plugin;
    }
}
