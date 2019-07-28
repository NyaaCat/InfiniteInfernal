package cat.nyaa.infiniteinfernal.configs;

import cat.nyaa.nyaacore.configuration.FileConfigure;

import java.io.File;
import java.util.Objects;

public abstract class NamedFileConfig extends FileConfigure {
    public String name = "";
    protected String prefix = "";

    public File rootDir;

    NamedFileConfig(String name){
        this.name = name;
    }

    public NamedFileConfig(File rootDir, String name) {
        Objects.requireNonNull(rootDir);
        this.rootDir = rootDir;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getPrefix() {
        return prefix;
    }

    @Override
    protected String getFileName() {
        return rootDir.getName().concat("/")
                .concat(getPrefix())
                .concat("-")
                .concat(getName())
                .concat(".yml");
    }
}
