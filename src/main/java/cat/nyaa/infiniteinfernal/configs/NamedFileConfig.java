package cat.nyaa.infiniteinfernal.configs;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.nyaacore.configuration.FileConfigure;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public abstract class NamedFileConfig extends FileConfigure {
    public String fileName = "";
    protected String prefix = "";
    private File file;

    public NamedFileConfig(String name){
        this.fileName = name;
    }

    public String getName() {
        return fileName;
    }

    public String getPrefix() {
        return prefix;
    }

    @Override
    protected String getFileName() {
        return getFileDirName().concat("/")
                .concat(getName())
                .concat(".yml");
    }

    public File getFile(){
        if (file == null){
            synchronized (this){
                if (file == null){
                    file = new File(getPlugin().getDataFolder(), getFileName());
                }
            }
        }
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    protected abstract String getFileDirName();

    @Override
    protected JavaPlugin getPlugin() {
        return InfPlugin.plugin;
    }
}
