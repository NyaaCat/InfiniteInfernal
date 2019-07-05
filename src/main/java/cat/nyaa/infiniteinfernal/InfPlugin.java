package cat.nyaa.infiniteinfernal;

import org.bukkit.plugin.java.JavaPlugin;

public class InfPlugin extends JavaPlugin {
    public static InfPlugin plugin;

    Events events;
    Config config;
    I18n i18n;
    AdminCommands commands;

    @Override
    public void onEnable() {
        super.onEnable();
        plugin = this;
        config = new Config(this);
        config.load();
        events = new Events(this);
        i18n = new I18n(this, config.language);
        i18n.load();
        commands = new AdminCommands(this, i18n);
    }

    public void onReload() {
        config.load();
        i18n = new I18n(this,config.language);
        i18n.load();
    }
}
