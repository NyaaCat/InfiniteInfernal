package cat.nyaa.infiniteinfernal;

import cat.nyaa.infiniteinfernal.loot.LootManager;
import cat.nyaa.infiniteinfernal.mob.MobManager;
import org.bukkit.plugin.java.JavaPlugin;

public class InfPlugin extends JavaPlugin {
    public static InfPlugin plugin;

    Events events;
    Config config;
    I18n i18n;
    AdminCommands commands;
    LootManager lootManager;
    MobManager mobManager;

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
        lootManager = LootManager.instance();
        mobManager = MobManager.instance();
        MainLoopTask.start();
    }

    public void onReload() {
        config.load();
        i18n = new I18n(this,config.language);
        i18n.load();
        LootManager.disable();
        MobManager.disable();
        MainLoopTask.stop();
    }

    public Config config() {
        return config;
    }
}
