package cat.nyaa.infiniteinfernal;

import cat.nyaa.infiniteinfernal.api.InfVarApi;
import cat.nyaa.infiniteinfernal.commands.AdminCommands;
import cat.nyaa.infiniteinfernal.commands.DebugCommands;
import cat.nyaa.infiniteinfernal.commands.ImbCommands;
import cat.nyaa.infiniteinfernal.commands.ImiCommands;
import cat.nyaa.infiniteinfernal.configs.MessageConfig;
import cat.nyaa.infiniteinfernal.event.internal.MainLooper;
import cat.nyaa.infiniteinfernal.mob.ability.AbilityManager;
import cat.nyaa.infiniteinfernal.mob.ability.Triggers;
import cat.nyaa.infiniteinfernal.mob.bossbar.BossbarManager;
import cat.nyaa.infiniteinfernal.mob.controller.ISpawnControler;
import cat.nyaa.infiniteinfernal.mob.controller.InfSpawnControler;
import cat.nyaa.infiniteinfernal.data.Database;
import cat.nyaa.infiniteinfernal.event.handler.MainEventHandler;
import cat.nyaa.infiniteinfernal.group.GroupCommands;
import cat.nyaa.infiniteinfernal.group.GroupListener;
import cat.nyaa.infiniteinfernal.loot.IMessager;
import cat.nyaa.infiniteinfernal.loot.InfMessager;
import cat.nyaa.infiniteinfernal.loot.LootManager;
import cat.nyaa.infiniteinfernal.mob.MobManager;
import cat.nyaa.infiniteinfernal.mob.TargetDummy;
import cat.nyaa.infiniteinfernal.event.handler.UiEvents;
import cat.nyaa.infiniteinfernal.ui.UiManager;
import cat.nyaa.infiniteinfernal.ui.impl.VarMana;
import cat.nyaa.infiniteinfernal.ui.impl.VarRage;
import cat.nyaa.infiniteinfernal.utils.support.WorldGuardUtils;
import cat.nyaa.infiniteinfernal.utils.ticker.Ticker;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.logging.Level;

public class InfPlugin extends JavaPlugin {
    public static InfPlugin plugin;
    public static boolean wgEnabled = false;

    MainEventHandler events;
    UiEvents uiEvents;
    Config config;
    MessageConfig messageConfig;
    I18n i18n;
    Database database;

    GroupListener groupListener;

    ImiCommands imiCommand;
    ImbCommands imbCommands;
    DebugCommands debugCommands;
    AdminCommands commands;
    GroupCommands groupCommands;

    LootManager lootManager;
    MobManager mobManager;
    BroadcastManager broadcastManager;
    BossbarManager bossbarManager;

    InfMessager infMessager;
    ISpawnControler spawnControler;



    @Override
    public void onEnable() {
        super.onEnable();
        plugin = this;
        config = new Config(this);
        config.load();
        events = new MainEventHandler(this);
        uiEvents = new UiEvents();
        i18n = new I18n(this, config.language);
        i18n.load();

        groupCommands = new GroupCommands(this, i18n);
        commands = new AdminCommands(this, i18n, groupCommands);
        imiCommand = new ImiCommands(this, i18n);
        debugCommands = new DebugCommands(this, i18n);
        imbCommands = new ImbCommands(this, i18n);

        groupListener = new GroupListener();

        lootManager = LootManager.instance();
        mobManager = MobManager.instance();
        broadcastManager = new BroadcastManager();
        broadcastManager.load();
        messageConfig = new MessageConfig();
        messageConfig.load();
        infMessager = new InfMessager(messageConfig);
        spawnControler = new InfSpawnControler(this);

        Bukkit.getPluginManager().registerEvents(events, this);
        Bukkit.getPluginManager().registerEvents(uiEvents, this);
        Bukkit.getPluginManager().registerEvents(groupListener, this);
        Bukkit.getPluginCommand("infiniteinfernal").setExecutor(commands);
        Bukkit.getPluginCommand("ig").setExecutor(groupCommands);
        Bukkit.getPluginCommand("imi").setExecutor(imiCommand);
        Bukkit.getPluginCommand("infdebug").setExecutor(debugCommands);
        Bukkit.getPluginCommand("imb").setExecutor(imbCommands);

        try {
            WorldGuardUtils.init();
            wgEnabled = true;
        } catch (NoClassDefFoundError e) {
            Bukkit.getLogger().log(Level.WARNING, "WorldGuard didn't detected, support will be disabled");
        }
        bossbarManager = new BossbarManager();
        if (config.bossbar.enabled) {
            bossbarManager.start(10);
        }
        MainLooper.start();
        Ticker.getInstance().init(this);
        UiManager.getInstance();
        MobManager.instance().initMobs();
        Database instance = Database.getInstance();
        AbilityManager.initPrototypeMap();
        Triggers.reload();
        instance.load();
        try {
            instance.init();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    public void onReload() {
        config.load();
        i18n = new I18n(this, config.language);
        i18n.load();
        LootManager.instance().load();
        MainLooper.start();
        AbilityManager.initPrototypeMap();
        Triggers.reload();

        mobManager.load();
        broadcastManager.load();
//        messageConfig = new MessageConfig();
        messageConfig.load();
        infMessager = new InfMessager(messageConfig);
        if (config.bossbar.enabled) {
            bossbarManager.start(10);
        }
        MobManager.instance().initMobs();
        TargetDummy.clearAll();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        LootManager.disable();
        MobManager.disable();
    }

    public Config config() {
        return config;
    }

    public ISpawnControler getSpawnControler() {
        return spawnControler;
    }

    public LootManager getLootManager() {
        return lootManager;
    }

    public BroadcastManager getBroadcastManager() {
        return broadcastManager;
    }

    public IMessager getMessager() {
        return infMessager;
    }

    InfVarApi infVarApi;

    public InfVarApi getVarApi(){
        if (infVarApi ==null) {
            infVarApi = new InfVarApi() {
                @Override
                public VarRage getRage(Player player) {
                    return UiManager.getInstance().getUi(player).getRage();
                }

                @Override
                public VarMana getMana(Player player) {
                    return UiManager.getInstance().getUi(player).getMana();
                }

                @Override
                public int getTick() {
                    return UiManager.getInstance().getTick();
                }
            };
        }
        return infVarApi;
    }
}
