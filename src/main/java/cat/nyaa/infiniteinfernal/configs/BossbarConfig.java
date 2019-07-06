package cat.nyaa.infiniteinfernal.configs;

import cat.nyaa.nyaacore.configuration.ISerializable;

public class BossbarConfig implements ISerializable {

    @Serializable
    public boolean enabled = true;

    @Serializable(name = "killsuffix")
    public String killSuffix = "[KILLED]";
}
