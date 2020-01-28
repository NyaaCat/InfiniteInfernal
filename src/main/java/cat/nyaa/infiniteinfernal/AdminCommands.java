package cat.nyaa.infiniteinfernal;

import cat.nyaa.infiniteinfernal.ability.AbilityCollection;
import cat.nyaa.infiniteinfernal.ability.IAbility;
import cat.nyaa.infiniteinfernal.configs.*;
import cat.nyaa.infiniteinfernal.group.GroupCommands;
import cat.nyaa.infiniteinfernal.loot.ILootItem;
import cat.nyaa.infiniteinfernal.loot.LootManager;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.mob.MobManager;
import cat.nyaa.infiniteinfernal.utils.Utils;
import cat.nyaa.infiniteinfernal.utils.WeightedPair;
import cat.nyaa.nyaacore.ILocalizer;
import cat.nyaa.nyaacore.Message;
import cat.nyaa.nyaacore.cmdreceiver.Arguments;
import cat.nyaa.nyaacore.cmdreceiver.BadCommandException;
import cat.nyaa.nyaacore.cmdreceiver.CommandReceiver;
import cat.nyaa.nyaacore.cmdreceiver.SubCommand;
import cat.nyaa.nyaacore.configuration.ISerializable;
import cat.nyaa.nyaacore.utils.InventoryUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class AdminCommands extends CommandReceiver {
    private static final List<String> MOB_TYPES = Arrays.stream(EntityType.values())
            .filter(entityType -> {
                Class<? extends Entity> entityClass = entityType.getEntityClass();
                return entityClass != null && Mob.class.isAssignableFrom(entityClass);
            }).map(Enum::name)
            .collect(Collectors.toList());

    private InfPlugin plugin;
    private ILocalizer i18n;

    public AdminCommands(InfPlugin plugin, ILocalizer _i18n, GroupCommands groupCommands) {
        super(plugin, _i18n);
        this.plugin = plugin;
        this.i18n = _i18n;
        inspectCommand = new InspectCommand(plugin, i18n);
        createCommand = new CreateCommand(plugin, i18n);
        modifyCommand = new ModifyCommand(plugin, i18n);
        deleteCommand = new DeleteCommand(plugin, i18n);
        this.groupCommands = groupCommands;
    }

    @Override
    public String getHelpPrefix() {
        return null;
    }

    @SubCommand(value = "reload", permission = "im.reload")
    public void onReload(CommandSender sender, Arguments arguments) {
        plugin.onReload();
    }

    @SubCommand(value = "spawn", permission = "im.spawn", tabCompleter = "spawnCompleter")
    public void onSpawn(CommandSender sender, Arguments arguments) {
        String mobName = arguments.nextString();
        if (arguments.top() == null) {
            if (sender instanceof Player) {
                Block targetBlock = ((Player) sender).getTargetBlock(null, 50);
                Location location = Utils.randomSpawnLocation(targetBlock.getLocation(), 0, 1);
                if (location != null) {
                    MobManager.instance().spawnMobByName(mobName, location, null);
                }
                return;
            }
        }
        String worldName = arguments.nextString();
        double x = arguments.nextDouble();
        double y = arguments.nextDouble();
        double z = arguments.nextDouble();
        String top = arguments.top();
        Integer level = top == null ? null : Integer.valueOf(top);
        World world = Bukkit.getWorld(worldName);
        MobManager.instance().spawnMobByName(mobName, new Location(world, x, y, z), level);
    }

    @SubCommand(value = "addloot", permission = "im.addloot", tabCompleter = "addLootCompleter")
    public void onAddLoot(CommandSender sender, Arguments arguments) {
        LootManager lootManager = plugin.getLootManager();
        String itemName = arguments.nextString();
        boolean isDynamic = arguments.top() != null && arguments.nextBoolean();
        if (sender instanceof Player) {
            ItemStack itemInMainHand = ((Player) sender).getInventory().getItemInMainHand();
            if (itemInMainHand.getType().equals(Material.AIR)) {
                new Message("").append(I18n.format("loot.add.error.no_item"))
                        .send(sender);
                return;
            }
            ILootItem loot = lootManager.getLoot(itemName);
            if (loot != null) {
                Message message = new Message("");
                message.append(I18n.format("loot.add.error.exists", itemName), loot.getItemStack())
                        .send(sender);
                return;
            }
            lootManager.addLoot(itemName, isDynamic, itemInMainHand);
            Message append = new Message("");
            append.append(I18n.format("loot.add.success", itemName, isDynamic), itemInMainHand)
                    .send(sender);

        } else {
            new Message(I18n.format("error.not_player")).send(sender);
        }
    }

    @SubCommand(value = "getloot", permission = "im.getloot", tabCompleter = "getLootCompleter")
    public void onGetLoot(CommandSender sender, Arguments arguments) {
        String lootName = arguments.nextString();
        ILootItem loot = plugin.getLootManager().getLoot(lootName);
        if (loot != null) {
            if (InfPlugin.plugin.config().isGetDropMessageEnabled) {
                new Message("").append(I18n.format("loot.get.success"), loot.getItemStack())
                        .send(sender);
            }
            if (sender instanceof Player) {
                if (!InventoryUtils.addItem((Player) sender, loot.getItemStack())) {
                    ((Player) sender).getWorld().dropItem(((Player) sender).getLocation(), loot.getItemStack());
                }
            }
        } else {
            new Message("").append(I18n.format("loot.get.no_item", lootName))
                    .send(sender);
        }
    }

    public void onModify(CommandSender sender, Arguments arguments) {
        String target = arguments.nextString();
        switch (target) {
            case "loot":
                String name = arguments.nextString();
                ILootItem iLootItem = LootManager.instance().getLoot(name);
                if (iLootItem == null) {
                    new Message(I18n.format("loot.get.no_item", name))
                            .send(sender);
                    return;
                }
                if (sender instanceof Player) {
                    ItemStack itemInMainHand = ((Player) sender).getInventory().getItemInMainHand();
                    if (itemInMainHand.getType().equals(Material.AIR)) {
                        new Message(I18n.format("loot.add.error.no_item"))
                                .send(sender);
                        return;
                    }
                    LootManager.instance().addLoot(name, iLootItem.isDynamic(), itemInMainHand);
                    new Message("").append(I18n.format("loot.add.success", name, iLootItem.isDynamic()), itemInMainHand)
                            .send(sender);
                } else {
                    new Message(I18n.format("error.not_player"))
                            .send(sender);
                }
                break;
        }
    }

    @SubCommand(value = "inspect", permission = "im.inspect")
    public InspectCommand inspectCommand;

    @SubCommand(value = "create", permission = "im.create")
    public CreateCommand createCommand;

    @SubCommand(value = "modify", permission = "im.modify")
    public ModifyCommand modifyCommand;

    @SubCommand(value = "delete", permission = "im.delete")
    public DeleteCommand deleteCommand;

    @SubCommand(value = "setdrop", permission = "im.setdrop", tabCompleter = "setDropCompleter")
    public void onSetDrop(CommandSender sender, Arguments arguments) {
        String itemName = arguments.nextString();
        int level = arguments.nextInt();
        int weight = arguments.nextInt();
        ILootItem lootItem = LootManager.instance().getLoot(itemName);
        if (lootItem == null) {
            new Message(I18n.format("loot.set.no_item", itemName))
                    .send(sender);
            return;
        }
        LootManager.instance().addCommonLoot(lootItem, level, weight);
        new Message("").append(I18n.format("loot.set.success", level, weight), lootItem.getItemStack())
                .send(sender);
    }

    @SubCommand(value = "killall", permission = "im.kill.all")
    public void onKillAll(CommandSender sender, Arguments arguments) {
        String top = arguments.top();
        Collection<IMob> toKill;
        if (top != null) {
            World world = Bukkit.getWorld(top);
            if (world != null) {
                toKill = new LinkedList<>(MobManager.instance().getMobsInWorld(world));
            } else {
                new Message(I18n.format("killall.error.unknown_world", top))
                        .send(sender);
                return;
            }
        } else {
            toKill = new LinkedList<>(MobManager.instance().getMobs());
        }
        toKill.stream().forEach(iMob -> MobManager.instance().removeMob(iMob, false));
        new Message(I18n.format("killall.success", toKill.size()))
                .send(sender);
    }

    @SubCommand(value = "killDamages", permission = "im.kill.damages")
    public void onKillDamages(CommandSender sender, Arguments arguments) {
        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.getWorlds().stream().
                        forEach(world -> world.getLivingEntities().stream()
                                .forEach(entity -> {
                                    if (entity.getScoreboardTags().contains("inf_damage_indicator")) {
                                        new BukkitRunnable() {
                                            @Override
                                            public void run() {
                                                entity.remove();
                                            }
                                        }.runTask(InfPlugin.plugin);
                                    }
                                }));
            }
        }.runTaskAsynchronously(InfPlugin.plugin);
    }

    @SubCommand(value = "toggleActionbarUi", permission = "im.command")
    public void onToggleUi(CommandSender sender, Arguments arguments) {
        boolean enableActionbarInfo = InfPlugin.plugin.config.enableActionbarInfo;
        InfPlugin.plugin.config.enableActionbarInfo = !enableActionbarInfo;
        InfPlugin.plugin.config.save();
    }

    @SubCommand(value = "group", permission = "im.group")
    GroupCommands groupCommands;

    @SubCommand(value = "enable", permission = "im.admin", tabCompleter = "enableCompleter")
    public void onEnable(CommandSender sender, Arguments arguments) {
        boolean enabled = InfPlugin.plugin.config.enabled;
        if (enabled) {
            MobManager instance = MobManager.instance();
            Collection<IMob> mobs = instance.getMobs();
            mobs.forEach(iMob -> instance.removeMob(iMob, false));
        }
        InfPlugin.plugin.config.enabled = !enabled;
        if (!enabled) {
            new Message("").append(I18n.format("enabled")).send(sender);
        } else {
            new Message("").append(I18n.format("disabled")).send(sender);
        }
        InfPlugin.plugin.config().save();
    }

    public List<String> enableCompleter(CommandSender sender, Arguments arguments) {
        List<String> completeStr = new ArrayList<>();
        switch (arguments.remains()) {
            case 1:
                break;
        }
        return filtered(arguments, completeStr);
    }

    public List<String> addLootCompleter(CommandSender sender, Arguments arguments) {
        List<String> completeStr = new ArrayList<>();
        switch (arguments.remains()) {
            case 1:
                completeStr.add("loot name");
                break;
            case 2:
                completeStr.add("true");
                completeStr.add("false");
                break;
        }
        return filtered(arguments, completeStr);
    }

    public List<String> spawnCompleter(CommandSender sender, Arguments arguments) {
        List<String> completeStr = new ArrayList<>();
        switch (arguments.remains()) {
            case 1:
                completeStr.addAll(getMobNames());
                break;
            case 2:
                completeStr.addAll(Bukkit.getWorlds().stream().map(World::getName).collect(Collectors.toList()));
                break;
            case 3:
                completeStr.add("x");
                if (sender instanceof Player) {
                    completeStr.add(String.valueOf(((Player) sender).getLocation().getX()));
                } else if (sender instanceof BlockCommandSender) {
                    completeStr.add(String.valueOf(((BlockCommandSender) sender).getBlock().getX()));
                }
                break;
            case 4:
                completeStr.add("y");
                if (sender instanceof Player) {
                    completeStr.add(String.valueOf(((Player) sender).getLocation().getY()));
                } else if (sender instanceof BlockCommandSender) {
                    completeStr.add(String.valueOf(((BlockCommandSender) sender).getBlock().getY()));
                }
                break;
            case 5:
                completeStr.add("z");
                if (sender instanceof Player) {
                    completeStr.add(String.valueOf(((Player) sender).getLocation().getZ()));
                } else if (sender instanceof BlockCommandSender) {
                    completeStr.add(String.valueOf(((BlockCommandSender) sender).getBlock().getZ()));
                }
                break;
            case 6:
                completeStr.add("level");
                break;
        }
        return filtered(arguments, completeStr);
    }

    public List<String> getLootCompleter(CommandSender sender, Arguments arguments) {
        List<String> completeStr = new ArrayList<>();
        switch (arguments.remains()) {
            case 1:
                completeStr.addAll(LootManager.instance().getLootNames());
                break;
        }
        return filtered(arguments, completeStr);
    }

    public List<String> modifyCompleter(CommandSender sender, Arguments arguments) {
        List<String> completeStr = new ArrayList<>();
        switch (arguments.remains()) {
            case 1:
                completeStr.add("loot");
                break;
            case 2:
                completeStr.addAll(LootManager.instance().getLootNames());
                break;
        }
        return filtered(arguments, completeStr);
    }

    public List<String> setDropCompleter(CommandSender sender, Arguments arguments) {
        List<String> completeStr = new ArrayList<>();
        switch (arguments.remains()) {
            case 1:
                completeStr.addAll(LootManager.instance().getLootNames());
                break;
            case 2:
                completeStr.add("level");
                break;
            case 3:
                completeStr.add("weight");
                break;
        }
        return filtered(arguments, completeStr);
    }

    public List<String> sampleCompleter(CommandSender sender, Arguments arguments) {
        List<String> completeStr = new ArrayList<>();
        switch (arguments.remains()) {
            case 1:
                break;
        }
        return filtered(arguments, completeStr);
    }

    private Set<String> getMobNames() {
        return MobManager.instance().getMobConfigNames();
    }

    private static List<String> filtered(Arguments arguments, List<String> completeStr) {
        String next = arguments.at(arguments.length() - 1);
        return completeStr.stream().filter(s -> s.startsWith(next)).collect(Collectors.toList());
    }

    private static List<String> filtered(String lastArg, List<String> completeStr) {
        String finalNext = lastArg;
        return completeStr.stream().filter(s -> s.startsWith(finalNext)).collect(Collectors.toList());
    }

    private static List<String> filtered(List<String> excluded, String lastArg, List<String> completeStr) {
        String finalNext = lastArg;
        return completeStr.stream().filter(s -> s.startsWith(finalNext))
                .filter(s -> !excluded.contains(s)).collect(Collectors.toList());
    }

    //todo: add language information in this section
    //<editor-fold>
    public static class InspectCommand extends CommandReceiver {

        /**
         * @param plugin for logging purpose only
         * @param _i18n
         */
        public InspectCommand(Plugin plugin, ILocalizer _i18n) {
            super(plugin, _i18n);
        }

        @Override
        public String getHelpPrefix() {
            return "";
        }

        @SubCommand(isDefaultCommand = true)
        public void inspectLocation(CommandSender sender, Arguments arguments) {
            Location location = null;
            if (sender instanceof Player) {
                location = ((Player) sender).getLocation();
            } else if (sender instanceof BlockCommandSender) {
                location = ((BlockCommandSender) sender).getBlock().getLocation();
            }
            if (location == null) {
                return;
            }
            List<MobConfig> spawnableMob = MobManager.instance().getSpawnableMob(location).stream()
                    .map(WeightedPair::getKey)
                    .collect(Collectors.toList());
            if (spawnableMob.size() == 0) {
                new Message(I18n.format("inspect.error.no_spawnable_mob"))
                        .send(sender);
                return;
            }
            sendMobInfo(sender, spawnableMob, sender.isOp());
        }

        @SubCommand(value = "biome", permission = "im.inspect.biome", tabCompleter = "biomeCompleter")
        public void biomeCommand(CommandSender sender, Arguments arguments) {
            Biome biome = arguments.nextEnum(Biome.class);
            boolean detailed = isDetailed(arguments);

            List<MobConfig> collect = MobManager.instance().getMobConfigs().stream()
                    .filter(mobConfig -> mobConfig.spawn.biomes.contains(biome))
                    .collect(Collectors.toList());
            sendMobInfo(sender, collect, detailed);
        }

        public List<String> biomeCompleter(CommandSender sender, Arguments arguments) {
            List<String> completeStr = new ArrayList<>();
            switch (arguments.remains()) {
                case 1:
                    completeStr.addAll(Arrays.stream(Biome.values()).map(Enum::name).collect(Collectors.toList()));
                    break;
                case 2:
                    completeStr.add("detailed");
                    completeStr.add("true");
                    completeStr.add("false");
                    break;
            }
            return filtered(arguments, completeStr);
        }

        @SubCommand(value = "region", permission = "im.inspect.region", tabCompleter = "regionCompleter")
        public void regionCommand(CommandSender sender, Arguments arguments) {
            String regionStr = arguments.nextString();
            boolean detailed = isDetailed(arguments);

            RegionConfig region = InfPlugin.plugin.config().regionConfigs.get(regionStr);
            if (region == null) throw new IllegalArgumentException();
            NamedDirConfigs<MobConfig> mobConfigs = InfPlugin.plugin.config().mobConfigs;
            List<MobConfig> collect = region.mobs.stream()
                    .map(s -> s.contains(":") ? s.substring(0, s.indexOf(":")) : s)
                    .map(mobConfigs::get).collect(Collectors.toList());
            sendMobInfo(sender, collect, detailed);
        }

        public List<String> regionCompleter(CommandSender sender, Arguments arguments) {
            List<String> completeStr = new ArrayList<>();
            switch (arguments.remains()) {
                case 1:
                    completeStr.addAll(InfPlugin.plugin.config().regionConfigs.keys());
                    break;
                case 2:
                    completeStr.add("detailed");
                    completeStr.add("true");
                    completeStr.add("false");
                    break;
            }
            return filtered(arguments, completeStr);
        }

        @SubCommand(value = "mob", permission = "im.inspect.mob", tabCompleter = "mobCompleter")
        public void mobCommand(CommandSender sender, Arguments arguments) {
            String mobName = arguments.nextString();

            MobConfig mobConfig = InfPlugin.plugin.config().mobConfigs.get(mobName);
            NamedDirConfigs<RegionConfig> regionConfigs = InfPlugin.plugin.config.regionConfigs;
            if (mobConfig == null) throw new IllegalArgumentException();
            List<RegionConfig> regionsContainsMob = regionConfigs.values().stream()
                    .filter(regionConfig ->
                            regionConfig.mobs.stream().map(s -> s.substring(0, s.indexOf(":")))
                                    .anyMatch(s -> s.equals(mobName)))
                    .collect(Collectors.toList());
            sendMobInfo(sender, Arrays.asList(mobConfig), true);
            regionsContainsMob.stream()
                    .forEach(regionConfig -> {
                        new Message(I18n.format("inspect.info.region_spawn_name", regionConfig.name)).send(sender);
                        regionConfig.mobs.stream().filter(s -> s.startsWith(mobName))
                                .forEach(s -> new Message(I18n.format("inspect.info.region_spawn_info", s)).send(sender));
                    });
        }

        public List<String> mobCompleter(CommandSender sender, Arguments arguments) {
            List<String> completeStr = new ArrayList<>();
            switch (arguments.remains()) {
                case 1:
                    completeStr.addAll(MobManager.instance().getMobConfigNames());
                    break;
            }
            return filtered(arguments, completeStr);
        }

        @SubCommand(value = "ability", permission = "im.inspect.ability", tabCompleter = "abilityCompleter")
        public void abilityCommand(CommandSender sender, Arguments arguments) {
            String ability = arguments.nextString();

            AbilitySetConfig abilitySetConfig = InfPlugin.plugin.config().abilityConfigs.get(ability);
            if (abilitySetConfig == null) throw new IllegalArgumentException();
            new Message(I18n.format("inspect.info.ability_set_name", ability)).send(sender);
            new Message(I18n.format("inspect.info.ability_set_weight", abilitySetConfig.weight)).send(sender);
            abilitySetConfig.abilities.entrySet().stream().forEach(entry -> {
                String name = entry.getKey();
                IAbility value = entry.getValue();
                YamlConfiguration section = new YamlConfiguration();
                value.serialize(section);
                new Message(String.format("%s\n%s", name, section.saveToString())).send(sender);
            });
        }

        public List<String> abilityCompleter(CommandSender sender, Arguments arguments) {
            List<String> completeStr = new ArrayList<>();
            switch (arguments.remains()) {
                case 1:
                    completeStr.addAll(InfPlugin.plugin.config().abilityConfigs.keys());
                    break;
            }
            return filtered(arguments, completeStr);
        }

        @SubCommand(value = "level", permission = "im.inspect.level", tabCompleter = "levelCompleter")
        public void levelCommand(CommandSender sender, Arguments arguments) {
            int level = arguments.nextInt();

            LevelConfig levelConfig = InfPlugin.plugin.config().levelConfigs.get(level);
            List<MobConfig> mobsForLevel = MobManager.instance().getMobsForLevel(level);
            if (levelConfig == null || mobsForLevel == null) throw new IllegalArgumentException();
            sendLevelConfig(sender, level, levelConfig, mobsForLevel);
        }

        public List<String> levelCompleter(CommandSender sender, Arguments arguments) {
            List<String> completeStr = new ArrayList<>();
            switch (arguments.remains()) {
                case 1:
                    completeStr.addAll(InfPlugin.plugin.config().levelConfigs.keys().stream().map(String::valueOf).collect(Collectors.toList()));
                    break;
            }
            return filtered(arguments, completeStr);
        }

        @SubCommand(value = "item", permission = "im.inspect.item", tabCompleter = "itemCompleter")
        public void itemCommand(CommandSender sender, Arguments arguments) {
            String id = arguments.nextString();
            ILootItem loot = LootManager.instance().getLoot(id);
            if (loot == null) {
                new Message("").append(I18n.format("inspect.item.no_item"))
                        .send(sender);
            } else {
                new Message("").append(I18n.format("inspect.item.success", id, loot.isDynamic()), loot.getItemStack())
                        .send(sender);
            }
        }

        public List<String> itemCompleter(CommandSender sender, Arguments arguments) {
            List<String> completeStr = new ArrayList<>();
            switch (arguments.remains()) {
                case 1:
                    completeStr.addAll(LootManager.instance().getLootNames());
                    break;
            }
            return filtered(arguments, completeStr);
        }

        @SubCommand(value = "loot", permission = "im.inspect.loot", tabCompleter = "lootCompleter")
        public void lootCommand(CommandSender sender, Arguments arguments) {
            String id = arguments.nextString();
            int level = Integer.parseInt(id);
            List<ILootItem> loots = LootManager.instance().getLoots(level);
            if (!loots.isEmpty()) {
                new Message("").append(I18n.format("inspect.level.success", level))
                        .send(sender);
                loots.forEach(lootItem -> {
                    int weight = lootItem.getWeight(level);
                    new Message("").append(I18n.format("inspect.level.info", weight, lootItem.isDynamic()), lootItem.getItemStack())
                            .send(sender);
                });
            } else {
                new Message("").append(I18n.format("inspect.level.no_level", level))
                        .send(sender);
            }
        }

        public List<String> lootCompleter(CommandSender sender, Arguments arguments) {
            List<String> completeStr = new ArrayList<>();
            switch (arguments.remains()) {
                case 1:
                    completeStr.addAll(InfPlugin.plugin.config().levelConfigs.keys().stream().map(String::valueOf).collect(Collectors.toList()));
                    break;
            }
            return filtered(arguments, completeStr);
        }

        //      @SubCommand(value = "sample", permission = "im.inspect.", tabCompleter = "sampleCompleter")
        public void sampleCommand(CommandSender sender, Arguments arguments) {

        }

        public List<String> sampleCompleter(CommandSender sender, Arguments arguments) {
            List<String> completeStr = new ArrayList<>();
            switch (arguments.remains()) {
                case 1:
                    break;
            }
            return filtered(arguments, completeStr);
        }

        private void sendMobInfo(CommandSender sender, List<MobConfig> spawnableMob, boolean detailed) {
            new Message("inspect.success").send(sender);
            spawnableMob.forEach(config -> {
                String name = config.getName();
                new Message(I18n.format("inspect.info.normal", name)).send(sender);
                if (detailed) {
                    YamlConfiguration section = new YamlConfiguration();
                    config.serialize(section);
                    new Message(section.saveToString()).send(sender);
                }
            });
        }

        private void sendLevelConfig(CommandSender sender, int level, LevelConfig levelConfig, List<MobConfig> mobsForLevel) {
            new Message(I18n.format("inspect.info.level", level)).send(sender);
            YamlConfiguration section = new YamlConfiguration();
            levelConfig.serialize(section);
            new Message(section.saveToString()).send(sender);
            mobsForLevel.stream().forEach(mobConfig -> {
                new Message(I18n.format("inspect.info.level_mob", mobConfig)).send(sender);
            });
        }

        private boolean isDetailed(Arguments arguments) {
            boolean detailed = false;
            String top = arguments.top();
            if (top != null) {
                detailed = Boolean.parseBoolean(top);
            }
            return detailed;
        }
    }

    public static class CreateCommand extends CommandReceiver {
        public CreateCommand(JavaPlugin plugin, ILocalizer i18n) {
            super(plugin, i18n);
        }

        @Override
        public String getHelpPrefix() {
            return "";
        }


        @SubCommand(value = "mob", permission = "im.create.mob", tabCompleter = "mobCompleter")
        public void mobCommand(CommandSender sender, Arguments arguments) {
            String id = arguments.nextString();
            EntityType entityType = arguments.nextEnum(EntityType.class);
            String displayName = arguments.nextString();
            boolean autoSpawn = arguments.nextBoolean();
            int remains = arguments.remains();
            List<String> abilities = new ArrayList<>();
            MobConfig mobConfig1 = InfPlugin.plugin.config().mobConfigs.get(id);
            if (mobConfig1 != null) {
                new Message(I18n.format("create.mob.exists", id)).send(sender);
                return;
            }
            for (int i = 0; i < remains; i++) {
                String ability = arguments.nextString();
                AbilitySetConfig abilitySetConfig = InfPlugin.plugin.config().abilityConfigs.get(ability);
                if (abilitySetConfig == null) {
                    new Message(I18n.format("create.error.invalid_ability", ability)).send(sender);
                    return;
                }
                abilities.add(ability);
            }
            MobConfig mobConfig = new MobConfig(id);
            mobConfig.abilities = abilities;
            mobConfig.type = entityType;
            mobConfig.spawn.autoSpawn = autoSpawn;
            mobConfig.name = displayName;
            mobConfig.spawn.biomes = Arrays.stream(Biome.values()).map(Enum::name).collect(Collectors.toList());
            mobConfig.spawn.worlds = Bukkit.getWorlds().stream().map(World::getName).collect(Collectors.toList());
            int max = MobManager.instance().getLevels().stream().mapToInt(Integer::intValue)
                    .max().orElse(1);
            ArrayList<String> levels = new ArrayList<>();
            if (max == 1) {
                levels.add("1");
            } else {
                levels.add("1-" + max);
            }
            mobConfig.spawn.levels = levels;
            InfPlugin.plugin.config().mobConfigs.add(id, mobConfig);
            mobConfig.save();
            MobManager.instance().load();
        }

        public List<String> mobCompleter(CommandSender sender, Arguments arguments) {
            List<String> completeStr = new ArrayList<>();
            switch (arguments.remains()) {
                case 1:
                    completeStr.add("id");
                    break;
                case 2:
                    completeStr.addAll(getMobs());
                    break;
                case 3:
                    completeStr.add("displayName");
                    break;
                case 4:
                    completeStr.add("autoSpawn");
                    completeStr.add("true");
                    completeStr.add("false");
                    break;
                default:
                    if (arguments.length() > 6) {
                        completeStr.addAll(InfPlugin.plugin.config().abilityConfigs.keys());
                    }

            }
            return filtered(arguments, completeStr);
        }

        @SubCommand(value = "ability", permission = "im.create.ability", tabCompleter = "abilityCompleter")
        public void abilityCommand(CommandSender sender, Arguments arguments) {
            String s = arguments.nextString();

            NamedDirConfigs<AbilitySetConfig> abilityConfigs = InfPlugin.plugin.config().abilityConfigs;
            AbilitySetConfig config1 = abilityConfigs.get(s);
            if (config1 != null) {
                new Message(I18n.format("create.error.ability_exists", s)).send(sender);
                return;
            }
            AbilitySetConfig config = new AbilitySetConfig(s);
            abilityConfigs.add(s, config);
            abilityConfigs.saveToDir();
            new Message(I18n.format("create.ability.success", s)).send(sender);
        }

        public List<String> abilityCompleter(CommandSender sender, Arguments arguments) {
            List<String> completeStr = new ArrayList<>();
            switch (arguments.remains()) {
                case 1:
                    completeStr.add("id");
                    break;
            }
            return filtered(arguments, completeStr);
        }

        @SubCommand(value = "region", permission = "im.create.region", tabCompleter = "regionCompleter")
        public void regionCommand(CommandSender sender, Arguments arguments) {
            String s = arguments.nextString();

            NamedDirConfigs<RegionConfig> regionConfigs = InfPlugin.plugin.config().regionConfigs;
            RegionConfig config1 = regionConfigs.get(s);
            if (config1 != null) {
                new Message(I18n.format("create.error.ability_exists", s)).send(sender);
                return;
            }
            if (!(sender instanceof Player)) {
                new Message(I18n.format("error.not_player", s)).send(sender);
                return;
            }
            try {
                WorldEditPlugin worldEditPlugin = WorldEditPlugin.getPlugin(WorldEditPlugin.class);
                if (!worldEditPlugin.isEnabled()) {
                    new Message(I18n.format("error.we_not_enabled", s)).send(sender);
                    return;
                }
                LocalSession session = worldEditPlugin.getSession((Player) sender);
                World world = ((Player) sender).getWorld();
                Region selection = session.getSelection(new BukkitWorld(world));
                BlockVector3 minimumPoint = selection.getMinimumPoint();
                BlockVector3 maximumPoint = selection.getMaximumPoint();
                RegionConfig.Region region = new RegionConfig.Region(world,
                        minimumPoint.getX(), maximumPoint.getX(),
                        minimumPoint.getY(), maximumPoint.getY(),
                        minimumPoint.getZ(), maximumPoint.getZ());
                RegionConfig config = new RegionConfig(s, region);
                regionConfigs.add(s, config);
                regionConfigs.saveToDir();
                new Message(I18n.format("create.success", s)).send(sender);
            } catch (LinkageError e) {
                new Message(I18n.format("error.we_not_enabled", s)).send(sender);
            } catch (IncompleteRegionException e) {
                new Message(I18n.format("create.error.no_selection")).send(sender);
            }
        }

        public List<String> regionCompleter(CommandSender sender, Arguments arguments) {
            List<String> completeStr = new ArrayList<>();
            switch (arguments.remains()) {
                case 1:
                    completeStr.add("id");
                    break;
            }
            return filtered(arguments, completeStr);
        }


    }

    private static Collection<String> getMobs() {
        return MOB_TYPES;
    }

    public static class ModifyCommand extends CommandReceiver {

        /**
         * @param plugin for logging purpose only
         * @param _i18n
         */
        public ModifyCommand(Plugin plugin, ILocalizer _i18n) {
            super(plugin, _i18n);
        }

        @Override
        public String getHelpPrefix() {
            return "";
        }

        List<String> mobAction = Arrays.asList(
                "type",
                "name",
                "nbt",
                "ability",
                "spawn",
                "loot"
        );

        @SubCommand(value = "mob", permission = "im.modify.mob", tabCompleter = "mobCompleter")
        public void mobCommand(CommandSender sender, Arguments arguments) {
            String id = arguments.nextString();
            NamedDirConfigs<MobConfig> mobConfigs = InfPlugin.plugin.config().mobConfigs;
            MobConfig mobConfig = mobConfigs.get(id);
            boolean modified = false;
            if (mobConfig == null) {
                new Message(I18n.format("modify.mob.dont_exist", id)).send(sender);
                return;
            }
            String action = arguments.nextString();
            switch (action) {
                case "type":
                    EntityType entityType = arguments.nextEnum(EntityType.class);
                    mobConfig.type = entityType;
                    modified = true;
                    new Message(I18n.format("modify.mob.type.success", entityType.name())).send(sender);
                    break;
                case "name":
                    String name = arguments.nextString();
                    mobConfig.name = name;
                    modified = true;
                    new Message(I18n.format("modify.mob.name.success", name)).send(sender);
                    break;
                case "nbt":
                    String nbt = arguments.nextString();
                    mobConfig.nbtTags = nbt;
                    modified = true;
                    new Message(I18n.format("modify.mob.nbt.success")).send(sender);
                    break;
                case "ability":
                    modified = mobCommandAbility(sender, arguments, mobConfig);
                    break;
                case "spawn":
                    modified = mobCommandSpawn(sender, arguments, mobConfig, id);
                    break;
                case "loot":
                    modified = mobCommandLoot(sender, arguments, mobConfig, id);
                    break;
            }
            if (modified) {
                mobConfig.save();
            }
        }

        // modify mob command implementations
        //<editor-fold>
        private boolean mobCommandSpawn(CommandSender sender, Arguments arguments, MobConfig mobConfig, String id) {
            String action = arguments.nextString();
            boolean modified = false;
            switch (action) {
                case "auto":
                    boolean b = arguments.nextBoolean();
                    mobConfig.spawn.autoSpawn = b;
                    modified = true;
                    break;
                case "weight":
                    int weight = arguments.nextInt();
                    mobConfig.spawn.weight = weight;
                    modified = true;
                    break;
                case "level":
                    String level = arguments.nextString();
                    String[] split = level.split(",");
                    mobConfig.spawn.levels.clear();
                    mobConfig.spawn.levels.addAll(Arrays.asList(split));
                    modified = true;
                    break;
                case "world":
                    String action1 = arguments.nextString();
                    List<String> worlds = mobConfig.spawn.worlds;
                    String worldName = arguments.nextString();
                    World world;
                    switch (action1) {
                        case "list":
                            if (worlds.isEmpty()) {
                                new Message(I18n.format("modify.mob.spawn.world.error_empty", id)).send(sender);
                                break;
                            }
                            worlds.forEach(s -> {
                                new Message(I18n.format("modify.mob.spawn.world.info", s)).send(sender);
                            });
                            break;
                        case "add":
                            world = Bukkit.getWorld(worldName);
                            if (world == null) {
                                new Message(I18n.format("modify.mob.spawn.world.not_exist", worldName)).send(sender);
                                break;
                            }
                            if (!worlds.contains(worldName)) {
                                worlds.add(world.getName());
                                modified = true;
                                new Message(I18n.format("modify.mob.spawn.world.success", worldName)).send(sender);
                            } else {
                                new Message(I18n.format("modify.mob.spawn.world.included", worldName)).send(sender);
                            }
                            break;
                        case "remove":
                            if (worlds.remove(worldName)) {
                                modified = true;
                                new Message(I18n.format("modify.mob.spawn.world.success", worldName)).send(sender);
                            } else {
                                new Message(I18n.format("modify.mob.spawn.world.not_included", worldName)).send(sender);
                            }
                            break;
                    }
                    break;
                case "biome":
                    String action2 = arguments.nextString();
                    Biome biome;
                    switch (action2) {
                        case "list":
                            List<String> biomes = mobConfig.spawn.biomes;
                            if (biomes.isEmpty()) {
                                new Message(I18n.format("modify.mob.spawn.biome.empty", id)).send(sender);
                            } else {
                                biomes.forEach(s -> {
                                    new Message(I18n.format("modify.mob.spawn.biome.info", s)).send(sender);
                                });
                            }

                            break;
                        case "add":
                            biome = arguments.nextEnum(Biome.class);
                            String name = biome.name();
                            if (!mobConfig.spawn.biomes.contains(name)) {
                                mobConfig.spawn.biomes.add(name);
                                new Message(I18n.format("modify.biome.add.success", name)).send(sender);
                            } else {
                                new Message(I18n.format("modify.biome.add.exists", name)).send(sender);
                            }
                            break;
                        case "remove":
                            biome = arguments.nextEnum(Biome.class);
                            String name1 = biome.name();
                            if (mobConfig.spawn.biomes.contains(name1)) {
                                mobConfig.spawn.biomes.remove(name1);
                                new Message(I18n.format("modify.biome.remove.success", name1)).send(sender);
                            } else {
                                new Message(I18n.format("modify.biome.remove.not_exist", name1)).send(sender);
                            }
                            break;
                    }
                    break;
            }
            return modified;
        }

        private boolean mobCommandAbility(CommandSender sender, Arguments arguments, MobConfig mobConfig) {
            boolean modified = false;
            String action = arguments.nextString();
            String target = "";
            NamedDirConfigs<AbilitySetConfig> abilityConfigs = InfPlugin.plugin.config().abilityConfigs;
            switch (action) {
                case "list":
                    List<String> abilities = mobConfig.abilities;
                    abilities.stream().forEach(s -> new Message(I18n.format("modify.mob.ability.list_info", s)).send(sender));
                    break;
                case "add":
                    target = arguments.nextString();
                    if (abilityConfigs.get(target) == null) {
                        new Message(I18n.format("modify.mob.ability.add.no_ability", target)).send(sender);
                        break;
                    }
                    mobConfig.abilities.add(target);
                    modified = true;
                    new Message(I18n.format("modify.mob.ability.add.success", target)).send(sender);
                    break;
                case "remove":
                    target = arguments.nextString();
                    if (!mobConfig.abilities.contains(target)) {
                        new Message(I18n.format("modify.mob.ability.remove.no_ability", target)).send(sender);
                        break;
                    }
                    mobConfig.abilities.remove(target);
                    modified = true;
                    new Message(I18n.format("modify.mob.ability.remove.success", target)).send(sender);
                    break;
                default:
                    new Message(I18n.format("modify.mob.ability.unknown_action")).send(sender);
            }
            return modified;
        }

        private boolean mobCommandLoot(CommandSender sender, Arguments arguments, MobConfig mobConfig, String id) {
            boolean modified = false;
            String action = arguments.nextString();
            switch (action) {
                case "vanilla":
                    boolean isVanilla = arguments.nextBoolean();
                    new Message(I18n.format("modify.mob.loot.vanilla", isVanilla)).send(sender);
                    mobConfig.loot.vanilla = isVanilla;
                    modified = true;
                    break;
                case "imloot":
                    boolean isImLoot = arguments.nextBoolean();
                    mobConfig.loot.imLoot = isImLoot;
                    new Message(I18n.format("modify.mob.loot.imLoot", isImLoot)).send(sender);
                    modified = true;
                    break;
                case "expOverride":
                    int expOverride = arguments.nextInt();
                    mobConfig.loot.expOverride = expOverride;
                    new Message(I18n.format("modify.mob.loot.expOverride", expOverride)).send(sender);
                    modified = true;
                    break;
                case "special":
                    String action1 = arguments.nextString();
                    List<String> specialList;
                    String lootName;
                    switch (action1) {
                        case "chance":
                            double chance = arguments.nextDouble();
                            mobConfig.loot.special.chance = chance;
                            new Message(I18n.format("modify.mob.loot.special.chance", chance)).send(sender);
                            modified = true;
                            break;
                        case "list":
                            List<String> list = mobConfig.loot.special.list;
                            if (list.isEmpty()) {
                                new Message(I18n.format("modify.mob.loot.special.list.empty", id)).send(sender);
                            } else {
                                list.forEach(s -> {
                                    new Message(I18n.format("modify.mob.loot.special.list.info", s)).send(sender);
                                });
                            }
                            break;
                        case "add":
                            lootName = arguments.nextString();
                            specialList = mobConfig.loot.special.list;
                            if (checkLootFormat(lootName)) {
                                specialList.add(lootName);
                                modified = true;
                                new Message(I18n.format("modify.mob.loot.special.add.success", lootName)).send(sender);
                            } else {
                                new Message(I18n.format("modify.mob.loot.special.add.invalid", lootName)).send(sender);
                            }
                            break;
                        case "remove":
                            lootName = arguments.nextString();
                            specialList = mobConfig.loot.special.list;
                            if (specialList.remove(lootName)) {
                                new Message(I18n.format("modify.mob.loot.special.add.success", lootName)).send(sender);
                                modified = true;
                            } else {
                                new Message(I18n.format("modify.mob.loot.special.add.not_included", lootName)).send(sender);
                            }
                            break;
                    }
                    break;
            }
            return modified;
        }

        private boolean checkLootFormat(String lootName) {
            String[] split = lootName.split(":");
            try {
                String name = split[0];
                int weight = Integer.parseInt(split[1]);
                ILootItem loot = LootManager.instance().getLoot(name);
                return loot != null;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        public List<String> mobCompleter(CommandSender sender, Arguments arguments) {
            List<String> completeStr = new ArrayList<>();
            switch (arguments.remains()) {
                case 1:
                    completeStr.addAll(InfPlugin.plugin.config().mobConfigs.keys());
                    break;
                case 2:
                    completeStr.addAll(mobAction);
                    break;
                case 3:
                case 4:
                case 5:
                default:
                    String id = arguments.nextString();
                    String action = arguments.nextString();
                    switch (action) {
                        case "type":
                            completeStr.addAll(getMobs());
                            break;
                        case "name":
                            completeStr.add("name");
                            break;
                        case "nbt":
                            completeStr.add("nbt");
                            break;
                        case "ability":
                            completeStr.addAll(mobAbilityCompleter(sender, arguments, id));
                            break;
                        case "spawn":
                            completeStr.addAll(mobSpawnCompleter(sender, arguments, id));
                            break;
                        case "loot":
                            completeStr.addAll(mobLootCompleter(sender, arguments, id));
                            break;
                    }
            }
            return filtered(arguments, completeStr);
        }

        private Collection<String> mobLootCompleter(CommandSender sender, Arguments arguments, String id) {
            List<String> completeStr = new ArrayList<>();
            switch (arguments.remains()) {
                case 1:
                    completeStr.add("vanilla");
                    completeStr.add("imloot");
                    completeStr.add("expOverride");
                    completeStr.add("special");
                    break;
                case 2:
                    String action = arguments.nextString();
                    switch (action) {
                        case "vanilla":
                        case "imloot":
                            completeStr.add("true");
                            completeStr.add("false");
                            break;
                        case "expOverride":
                            completeStr.add("amount");
                            break;
                        case "special":
                            completeStr.add("chance");
                            completeStr.add("add");
                            completeStr.add("remove");
                            break;
                    }
                    break;
                case 3:
                    String action1 = arguments.nextString();
                    if (!"special".equals(action1)) break;
                    String action2 = arguments.nextString();
                    String target = arguments.nextString();
                    switch (action2) {
                        case "chance":
                            completeStr.add("chance");
                            break;
                        case "add":
                            if (target.endsWith(":")) {
                                completeStr.add("weight");
                            } else {
                                completeStr.addAll(LootManager.instance().getLootNames().stream()
                                        .map(s -> s.concat(":"))
                                        .collect(Collectors.toList()));
                            }
                            break;
                        case "remove":
                            MobConfig mobConfig = InfPlugin.plugin.config().mobConfigs.get(id);
                            if (mobConfig != null) {
                                completeStr.addAll(mobConfig.loot.special.list);
                            }
                            break;
                    }
            }
            return completeStr;
        }

        List<String> mobSpawnActions = Arrays.asList(
                "auto",
                "weight",
                "level",
                "biome",
                "world");

        private Collection<String> mobSpawnCompleter(CommandSender sender, Arguments arguments, String id) {
            List<String> completeString = new ArrayList<>();
            int remains = arguments.remains();
            String action;
            switch (remains) {
                case 1:
                    completeString.addAll(mobSpawnActions);
                case 2:
                    action = arguments.nextString();
                    switch (action) {
                        case "auto":
                            completeString.add("true");
                            completeString.add("false");
                            break;
                        case "weight":
                            completeString.add("weight");
                            break;
                        case "level":
                            completeString.add("level");
                            break;
                        case "biome":
                        case "world":
                            completeString.add("list");
                            completeString.add("add");
                            completeString.add("remove");
                            break;
                    }
                    break;
                case 3:
                    action = arguments.nextString();
                    MobConfig mobConfig = InfPlugin.plugin.config().mobConfigs.get(id);
                    String action1 = arguments.nextString();
                    if (mobConfig == null) break;
                    switch (action) {
                        case "biome":
                            if ("remove".equals(action1)) {
                                completeString.addAll(mobConfig.spawn.biomes);
                            } else {
                                completeString.addAll(Arrays.stream(Biome.values()).map(Enum::name).collect(Collectors.toList()));
                            }
                            break;
                        case "world":
                            if ("remove".equals(action1)) {
                                completeString.addAll(mobConfig.spawn.worlds);
                            } else {
                                completeString.addAll(Bukkit.getWorlds().stream().map(World::getName).collect(Collectors.toList()));
                            }
                    }
            }
            return completeString;
        }

        private List<String> mobAbilityCompleter(CommandSender sender, Arguments arguments, String id) {
            List<String> completeStr = new ArrayList<>();
            switch (arguments.remains()) {
                case 1:
                    completeStr.add("list");
                    completeStr.add("add");
                    completeStr.add("remove");
                    break;
                case 2:
                    MobConfig mobConfig = InfPlugin.plugin.config().mobConfigs.get(id);
                    String action = arguments.nextString();
                    if ("add".equals(action)) {
                        completeStr.addAll(InfPlugin.plugin.config().abilityConfigs.keys());
                    } else if ("remove".equals(action)) {
                        if (mobConfig != null) {
                            completeStr.addAll(mobConfig.abilities);
                        }
                    }
                    break;
            }
            return completeStr;
        }
        //</editor-fold>

        @SubCommand(value = "region", permission = "im.modify.region", tabCompleter = "regionCompleter")
        public void regionCommand(CommandSender sender, Arguments arguments) {
            String s;
            String action;
            s = arguments.nextString();
            action = arguments.nextString();
            RegionConfig regionConfig = InfPlugin.plugin.config().regionConfigs.get(s);
            boolean modified = false;
            if (regionConfig == null) {
                new Message(I18n.format("modify.region.not_exist", s)).send(sender);
                return;
            }
            switch (action) {
                case "mobs":
                    String nextAction = arguments.nextString();
                    String mob = arguments.next();
                    switch (nextAction) {
                        case "add":
                            if (!checkMobFormat(mob)) {
                                new Message(I18n.format("modify.region.mobs.not_exist")).send(sender);
                                return;
                            }
                            String mobName = mob.substring(0, mob.indexOf(":"));
                            MobConfig mobConfig = InfPlugin.plugin.config().mobConfigs.get(mobName);
                            if (mobConfig == null) {
                                new Message(I18n.format("modify.region.mob_not_exist", mob)).send(sender);
                            } else {
                                regionConfig.mobs.add(mob);
                                modified = true;
                                new Message(I18n.format("modify.region.success", mob)).send(sender);
                            }
                            break;
                        case "remove":
                            if (regionConfig.mobs.remove(mob)) {
                                modified = true;
                                new Message(I18n.format("modify.region.mobs.remove.success", mob)).send(sender);
                            } else {
                                new Message(I18n.format("modify.region.mobs.remove.not_exist", mob)).send(sender);
                            }
                            break;
                    }
                    break;
            }
            if (modified) {
                regionConfig.save();
            }
        }

        private boolean checkMobFormat(String mob) {
            String[] split = mob.split(":");
            try {
                String name = split[0];
                int weight = Integer.parseInt(split[1]);
                MobConfig mobConfig = InfPlugin.plugin.config().mobConfigs.get(name);
                return mobConfig != null;
            } catch (Exception e) {
                return false;
            }
        }

        public List<String> regionCompleter(CommandSender sender, Arguments arguments) {
            List<String> completeStr = new ArrayList<>();
            String s;
            String action;
            switch (arguments.remains()) {
                case 1:
                    completeStr.addAll(InfPlugin.plugin.config().regionConfigs.keys());
                    break;
                case 2:
                    completeStr.add("mobs");
                    break;
                case 3:
//                    completeStr.add("list");
                    completeStr.add("add");
                    completeStr.add("remove");
                    break;
                case 4:
                    s = arguments.nextString();
                    arguments.next();
                    action = arguments.nextString();
                    RegionConfig regionConfig = InfPlugin.plugin.config().regionConfigs.get(s);
                    if ("remove".equals(action)) {
                        if (regionConfig != null) {
                            completeStr.addAll(regionConfig.mobs);
                        }
                    } else if ("add".equals(action)) {
                        completeStr.addAll(MobManager.instance().getMobConfigNames());
                    }
                    break;
                case 5:
                    s = arguments.nextString();
                    action = arguments.nextString();
                    if ("add".equals(action)) {
                        completeStr.add("weight");
                    }
            }
            return filtered(arguments, completeStr);
        }

        @SubCommand(value = "ability", permission = "im.modify.ability", tabCompleter = "abilityCompleter")
        public void abilityCommand(CommandSender sender, Arguments arguments) {
            String id;
            String action;
            AbilitySetConfig abilitySetConfig;
            id = arguments.nextString();
            action = arguments.nextString();
            abilitySetConfig = InfPlugin.plugin.config().abilityConfigs.get(id);
            if (abilitySetConfig == null) {
                new Message(I18n.format("modify.ability.add.unknown_ability_set", id)).send(sender);
                return;
            }
            String targetAbility;
            final boolean[] modified = {false};
            switch (action) {
                case "add":
                    targetAbility = arguments.nextString();
                    Class<? extends IAbility> aClass = AbilityCollection.ABILITY_NAMES.get(targetAbility);
                    if (aClass == null) {
                        new Message(I18n.format("modify.ability.add.unknown_ability", targetAbility)).send(sender);
                        break;
                    }
                    try {
                        IAbility iAbility = (IAbility) setField(aClass, arguments, "");
                        Map<String, IAbility> abilities = abilitySetConfig.abilities;
                        String baseName = iAbility.getName();
                        int i = 1;
                        String name = baseName;
                        while (abilities.containsKey(name)) {
                            name = baseName.concat("-" + i++);
                        }
                        abilities.put(name, iAbility);
                        new Message(I18n.format("modify.ability.add.success", id, name)).send(sender);
                        modified[0] = true;
                    } catch (InstantiationException | IllegalAccessException ignored) {
                        throw new BadCommandException();
                    } catch (ClassCastException castEx) {
                        StringBuilder sb = new StringBuilder("unsupported command: /");
                        int length = arguments.length();
                        for (int i = 0; i < length; i++) {
                            sb.append(arguments.at(i)).append(" ");
                        }
                        Bukkit.getLogger().log(Level.WARNING, sb.toString(), castEx);
                        new Message(I18n.format("modify.ability.add.unsupported_command")).send(sender);
                    } catch (UnsupportedOperationException unsupported) {
                        StringBuilder sb = new StringBuilder("unsupported command: /");
                        int length = arguments.length();
                        for (int i = 0; i < length; i++) {
                            sb.append(arguments.at(i)).append(" ");
                        }
                        new Message(I18n.format("modify.ability.add.unsupported_command")).send(sender);
                        Bukkit.getLogger().log(Level.WARNING, sb.toString(), unsupported);
                    }
                    break;
                case "set":
                    targetAbility = arguments.nextString();
                    String property = arguments.nextString();
                    String val = arguments.top();
                    IAbility iAbility = abilitySetConfig.abilities.get(targetAbility);
                    if (iAbility == null) {
                        new Message(I18n.format("modify.ability.set.unknown_ability", targetAbility)).send(sender);
                        break;
                    }
                    reflectField(property.concat(":"), iAbility.getClass(), iAbility, (field, iSerializable) -> {
                        try {
                            modified[0] = setField(field, iSerializable, val);
                            if (modified[0]) {
                                new Message(I18n.format("modify.ability.set.success", targetAbility, property, val)).send(sender);
                            } else {
                                new Message(I18n.format("modify.ability.set.failed", targetAbility, property, val)).send(sender);
                            }
                        } catch (IllegalAccessException e) {
                            throw new BadCommandException();
                        }
                    });
                    break;
                case "remove":
                    targetAbility = arguments.nextString();
                    IAbility remove = abilitySetConfig.abilities.remove(targetAbility);
                    if (remove == null) {
                        new Message(I18n.format("modify.ability.remove.not_exist", targetAbility)).send(sender);
                    } else {
                        new Message(I18n.format("modify.ability.remove.success", targetAbility)).send(sender);
                    }
                    break;
            }
            if (modified[0]) {
                abilitySetConfig.save();
            }
        }

        private boolean setField(Field field, ISerializable iAbility, String arg) throws IllegalAccessException {
            if (arg == null) {
                return false;
            }
            if (field.getAnnotation(ISerializable.Serializable.class) != null) {
                field.setAccessible(true);
                Class<?> fieldType = field.getType();
                if (Integer.class.isAssignableFrom(fieldType) || int.class.isAssignableFrom(fieldType)) {
                    int i = Integer.parseInt(arg);
                    field.set(iAbility, i);
                } else if (Boolean.class.isAssignableFrom(fieldType) || boolean.class.isAssignableFrom(fieldType)) {
                    boolean i = Boolean.parseBoolean(arg);
                    field.set(iAbility, i);
                } else if (String.class.isAssignableFrom(fieldType)) {
                    field.set(iAbility, arg);
                } else if (Enum.class.isAssignableFrom(fieldType)) {
                    Enum t = Enum.valueOf(((Class<Enum>) fieldType), arg);
                    field.set(iAbility, t);
                } else if (Double.class.isAssignableFrom(fieldType) || double.class.isAssignableFrom(fieldType)) {
                    double a = Double.parseDouble(arg);
                    field.set(iAbility, a);
                } else if (Float.class.isAssignableFrom(fieldType) || float.class.isAssignableFrom(fieldType)) {
                    float a = Float.parseFloat(arg);
                    field.set(iAbility, a);
                } else if (List.class.isAssignableFrom(fieldType)) {
                    ArrayList<String> o = new Gson().fromJson(arg, new TypeToken<ArrayList<String>>() {
                    }.getType());
                    List o1 = ((List) field.get(iAbility));
                    Object ref = o1.stream().findAny().orElse(null);
                    if (ref != null) {
                        Class<?> aClass1 = ref.getClass();
                        if (Number.class.isAssignableFrom(aClass1)) {
                            try {
                                List<Number> collect = o.stream().map(s1 -> ((Number) Double.parseDouble(s1)))
                                        .collect(Collectors.toList());
                                ((List<Number>) o1).addAll(collect);
                            } catch (Exception e) {
                                throw new UnsupportedOperationException();
                            }
                        }
                        if (String.class.isAssignableFrom(aClass1)) {
                            ((List<String>) o1).addAll(o);
                        }
                    } else {
                        throw new UnsupportedOperationException();
                    }
                } else {
                    return false;
                }
            }
            return true;
        }

        private ISerializable setField(Class<? extends ISerializable> aClass, Arguments arguments, String prefix) throws IllegalAccessException, InstantiationException {
            ISerializable iAbility = aClass.newInstance();
            Field[] declaredFields = aClass.getDeclaredFields();
            if (declaredFields.length == 0) return iAbility;
            for (Field field : declaredFields) {
                Class<?> fieldType = field.getType();
                if (ISerializable.class.isAssignableFrom(fieldType)) {
                    field.set(iAbility, setField((Class<? extends ISerializable>) fieldType, arguments, prefix.concat(field.getName())));
                }
                String arg;
                try {
                    arg = arguments.argString(prefix.concat(field.getName()));
                } catch (BadCommandException e) {
                    arg = null;
                }
                setField(field, iAbility, arg);
            }
            return iAbility;
        }

        public List<String> abilityCompleter(CommandSender sender, Arguments arguments) {
            final List<String> completeStr = new ArrayList<>();
            String id;
            String action;
            AbilitySetConfig abilitySetConfig;
            switch (arguments.remains()) {
                case 1:
                    completeStr.addAll(InfPlugin.plugin.config().abilityConfigs.keys());
                    break;
                case 2:
                    completeStr.add("add");
                    completeStr.add("set");
                    completeStr.add("remove");
                    break;
                case 3:
                    id = arguments.nextString();
                    action = arguments.nextString();
                    abilitySetConfig = InfPlugin.plugin.config().abilityConfigs.get(id);
                    if (abilitySetConfig == null) break;
                    switch (action) {
                        case "add":
                            completeStr.addAll(AbilityCollection.ABILITY_NAMES.keySet());
                            break;
                        case "set":
                        case "remove":
                            completeStr.addAll(abilitySetConfig.abilities.keySet());
                            break;
                    }
                    break;
                default:
                    if (arguments.remains() < 4) break;
                    id = arguments.nextString();
                    action = arguments.nextString();
                    String basicAbility;
                    String target;
                    switch (action) {
                        case "add":
                            abilitySetConfig = InfPlugin.plugin.config().abilityConfigs.get(id);
                            basicAbility = arguments.nextString();
                            int remains = arguments.remains();
                            List<String> excluded = new ArrayList<>();
                            for (int i = 0; i < remains - 1; i++) {
                                String next = arguments.next();
                                excluded.add(next.substring(0, next.lastIndexOf(":") + 1));
                            }
                            Class<? extends IAbility> aClass = AbilityCollection.ABILITY_NAMES.get(basicAbility);
                            target = arguments.nextString();
                            if (target.endsWith(":")) {
                                try {
                                    IAbility iAbility = aClass.newInstance();
                                    reflectField(target, aClass, iAbility, (field, iSerializable) -> {
                                        if (field == null) return;
                                        if (Enum.class.isAssignableFrom(field.getType())) {
                                            reflectEnum(field.getType(), anEnum -> completeStr.add(target.concat(anEnum.name())));
                                        }
                                        if (ISerializable.class.isAssignableFrom(field.getType())) {
                                            reflectISerializables(field.getType(), (field1, prefix) -> {
                                                completeStr.add(prefix.concat(field1.getName()).concat(":"));
                                            }, "");
                                        } else {
                                            completeStr.add(target.concat(field.getType().getName()));
                                        }
                                    });
                                } catch (InstantiationException | IllegalAccessException ignored) {
                                }
                                return completeStr;
                            } else {
                                String prefix = "";
                                if (target.contains(":")) {
                                    prefix = target.substring(0, target.lastIndexOf(":") + 1);
                                }
                                String finalPrefix = prefix;
                                reflectISerializables(aClass, (field1, prefix1) -> {
                                    completeStr.add(prefix1.concat(field1.getName()).concat(":"));
                                }, "");
                            }
                            break;
                        case "set":
                            int remains1 = arguments.remains();
                            abilitySetConfig = InfPlugin.plugin.config().abilityConfigs.get(id);
                            if (abilitySetConfig == null) break;
                            String targetAbi;
                            String targetStr;
                            IAbility iAbility;
                            switch (remains1) {
                                case 1:
                                    completeStr.addAll(abilitySetConfig.abilities.keySet());
                                    break;
                                case 2:
                                    targetAbi = arguments.nextString();
                                    targetStr = arguments.nextString();
                                    iAbility = abilitySetConfig.abilities.get(targetAbi);
                                    if (iAbility == null) break;
                                    reflectISerializables(iAbility.getClass(), (field, prefix) -> {
                                        if (field == null) return;
                                        if (ISerializable.class.isAssignableFrom(field.getType())) {
                                            reflectISerializables(field.getType(), (field1, prefix1) -> {
                                                completeStr.add(prefix1.concat(field1.getName()));
                                            }, "");
                                        } else {
                                            completeStr.add(prefix.concat(field.getName()));
                                        }
                                    }, "");
                                    return filtered(targetStr, completeStr);
                                case 3:
                                    targetAbi = arguments.nextString();
                                    targetStr = arguments.nextString();
                                    iAbility = abilitySetConfig.abilities.get(targetAbi);
                                    if (iAbility == null) break;
                                    reflectField(targetStr.concat(":"), iAbility.getClass(), iAbility, (field, iSerializable) -> {
                                        if (Enum.class.isAssignableFrom(field.getType())) {
                                            reflectEnum(field.getType(), (anEnum) -> {
                                                completeStr.add(anEnum.name());
                                            });
                                        } else {
                                            completeStr.add(field.getType().getName());
                                        }
                                    });
                                    return completeStr;
                            }
                    }
            }
            return filtered(arguments, completeStr);
        }

        private boolean reflectField(String target, Class<?> aClass, ISerializable instance, BiConsumer<Field, ISerializable> func) {
            int ind = target.lastIndexOf(":");
            String[] split = target.substring(0, ind > 0 ? ind : target.length()).split(":", 2);
            try {
                String name = split[0];
                if (aClass == null) return false;
                Field declaredField = aClass.getDeclaredField(name);
                if (split.length > 1) {
                    return reflectField(split[1], declaredField.getType(), (ISerializable) declaredField.get(instance), func);
                } else {
                    func.accept(declaredField, instance);
                    return true;
                }
            } catch (Exception ignored) {
                return false;
            }
        }

        private void reflectEnum(Class<?> aClass, Consumer<Enum> action) {
            Class<? extends Enum> type = (Class<? extends Enum>) aClass;
            try {
                Field en = type.getDeclaredField("ENUM$VALUES");
                en.setAccessible(true);
                Enum[] o = ((Enum[]) en.get(null));
                if (o != null && o.length > 0) {
                    for (Enum anEnum : o) {
                        action.accept(anEnum);
                    }
                }
            } catch (NoSuchFieldException | IllegalAccessException ignored) {
            }
        }

        private void reflectISerializables(Class<?> aClass, BiConsumer<Field, String> action, String prefix) {
            if (aClass == null) return;
            Field[] declaredFields = aClass.getDeclaredFields();
            if (declaredFields.length > 0) {
                for (Field declaredField : declaredFields) {
                    try {
                        ISerializable.Serializable annotation = declaredField.getAnnotation(ISerializable.Serializable.class);
                        if (annotation != null) {
                            if (ISerializable.class.isAssignableFrom(declaredField.getType())) {
                                reflectISerializables(declaredField.getType(), action, declaredField.getName().concat(":"));
                            } else {
                                action.accept(declaredField, prefix);
                            }
                        }
                    } catch (Exception ig) {
                        System.out.println();
                    }
                }
            }
        }

        //      @SubCommand(value = "", permission = "im.modify.", tabCompleter = "Completer")
        public void sampleCommand(CommandSender sender, Arguments arguments) {
        }

        public List<String> sampleCompleter(CommandSender sender, Arguments arguments) {
            List<String> completeStr = new ArrayList<>();
            switch (arguments.remains()) {
                case 1:
                    break;
            }
            return filtered(arguments, completeStr);
        }


    }

    public static class DeleteCommand extends CommandReceiver {
        /**
         * @param plugin for logging purpose only
         * @param _i18n
         */
        public DeleteCommand(Plugin plugin, ILocalizer _i18n) {
            super(plugin, _i18n);
        }

        @Override
        public String getHelpPrefix() {
            return "";
        }

        Map<CommandSender, String> commandBuffer = new LinkedHashMap<>(10);

        @SubCommand(value = "mob", permission = "im.delete.mob", tabCompleter = "mobCompleter")
        public void mobCommand(CommandSender sender, Arguments arguments) {
            String cmd = getFullCommand(arguments);
            String id = arguments.nextString();
            MobConfig mobConfig = InfPlugin.plugin.config().mobConfigs.get(id);
            String s = commandBuffer.get(sender);
            if (commandBuffer.containsKey(sender) && s != null && s.equals(cmd)) {
                commandBuffer.remove(sender);
                executeDeleteMob(sender, mobConfig, id);
            } else {
                printDeleteInfo(sender, mobConfig, id);
                commandBuffer.put(sender, cmd);
            }
        }

        private void printDeleteInfo(CommandSender sender, MobConfig mobConfig, String id) {
            if (mobConfig == null) {
                new Message(I18n.format("delete.no_mob", id)).send(sender);
                return;
            }
            NamedDirConfigs<RegionConfig> regionConfigs = InfPlugin.plugin.config().regionConfigs;
            new Message(I18n.format("delete.mob.info", id)).send(sender);
            findInRegions(regionConfigs, id, regionConfig -> new Message(I18n.format("delete.info.influenced_config", regionConfig.name)).send(sender));
        }

        private void executeDeleteMob(CommandSender sender, MobConfig mobConfig, String id) {
            if (mobConfig == null) {
                new Message(I18n.format("delete.no_mob", id)).send(sender);
                return;
            }
            NamedDirConfigs<RegionConfig> regionConfigs = InfPlugin.plugin.config().regionConfigs;
            NamedDirConfigs<MobConfig> mobConfigs = InfPlugin.plugin.config().mobConfigs;
            MobConfig remove = mobConfigs.remove(id);
            if (remove == null) {
                new Message(I18n.format("delete.mob.failed", id)).send(sender);
                return;
            }
            new Message(I18n.format("delete.mob.success", id)).send(sender);
            mobConfigs.saveToDir();
            final boolean[] modified = {false};
            findInRegions(regionConfigs, id, regionConfig -> {
                List<String> collect = regionConfig.mobs.stream().filter(s -> {
                    String[] split = s.split(":", 2);
                    String s1 = split[0];
                    return s1.equals(id);
                }).collect(Collectors.toList());
                modified[0] = modified[0] || regionConfig.mobs.removeAll(collect);
            });
            if (modified[0]) {
                mobConfigs.saveToDir();
            }
        }

        private void findInRegions(NamedDirConfigs<RegionConfig> regionConfigs, String id, Consumer<RegionConfig> consumer) {
            regionConfigs.values().stream().filter(regionConfig -> regionConfig.mobs.stream().anyMatch(s -> {
                String[] split = s.split(":", 2);
                String s1 = split[0];
                return s1.equals(id);
            })).forEach(consumer);
        }

        public List<String> mobCompleter(CommandSender sender, Arguments arguments) {
            List<String> completeStr = new ArrayList<>();
            switch (arguments.remains()) {
                case 1:
                    completeStr.addAll(InfPlugin.plugin.config().mobConfigs.keys());
                    break;
            }
            return filtered(arguments, completeStr);
        }

        @SubCommand(value = "region", permission = "im.delete.region", tabCompleter = "regionCompleter")
        public void regionCommand(CommandSender sender, Arguments arguments) {
            String fullCommand = getFullCommand(arguments);
            String id = arguments.nextString();
            RegionConfig regionConfig = InfPlugin.plugin.config().regionConfigs.get(id);
            if (regionConfig == null) {
                new Message(I18n.format("delete.region.no_region", id)).send(sender);
                return;
            }
            String s = commandBuffer.get(sender);
            if (s != null && s.equals(fullCommand)) {
                commandBuffer.remove(sender);
                RegionConfig remove = InfPlugin.plugin.config().regionConfigs.remove(id);
                if (remove == null) {
                    new Message(I18n.format("delete.region.failed", id)).send(sender);
                    return;
                }
                new Message(I18n.format("delete.region.success", id)).send(sender);
            } else {
                commandBuffer.put(sender, fullCommand);
                new Message(I18n.format("delete.region.info", id)).send(sender);
            }
        }

        private String getFullCommand(Arguments arguments) {
            int length = arguments.length();
            StringBuilder builder = new StringBuilder("/");
            for (int i = 0; i < length; i++) {
                String at = arguments.at(i);
                builder.append(at).append(" ");
            }
            return builder.toString();
        }

        public List<String> regionCompleter(CommandSender sender, Arguments arguments) {
            List<String> completeStr = new ArrayList<>();
            switch (arguments.remains()) {
                case 1:
                    completeStr.addAll(InfPlugin.plugin.config().regionConfigs.keys());
                    break;
            }
            return filtered(arguments, completeStr);
        }

        @SubCommand(value = "ability", permission = "im.delete.ability", tabCompleter = "abilityCompleter")
        public void abilityCommand(CommandSender sender, Arguments arguments) {
            String fullCommand = getFullCommand(arguments);
            String id = arguments.nextString();
            NamedDirConfigs<AbilitySetConfig> abilityConfigs = InfPlugin.plugin.config().abilityConfigs;
            AbilitySetConfig config = abilityConfigs.get(id);
            if (config == null) {
                new Message(I18n.format("delete.ability.no_ability_set", id)).send(sender);
                return;
            }
            String s = commandBuffer.get(sender);
            if (s != null && s.equals(fullCommand)) {
                new Message(I18n.format("delete.ability.success", id)).send(sender);
                commandBuffer.remove(sender);
                executeDeleteAbility(sender, config, id);
            } else {
                new Message(I18n.format("delete.mob.confirm", id)).send(sender);
                commandBuffer.put(sender, fullCommand);
                printAbilityInfo(sender, config, id);
            }
        }

        private void printAbilityInfo(CommandSender sender, AbilitySetConfig config, String id) {
            new Message(I18n.format("delete.ability.confirm", id)).send(sender);
            findInMobs(sender, id, mobConfig -> {
                new Message(I18n.format("delete.ability.info", mobConfig.name, mobConfig)).send(sender);
            });
        }


        private void executeDeleteAbility(CommandSender sender, AbilitySetConfig config, String id) {
            NamedDirConfigs<AbilitySetConfig> abilityConfigs = InfPlugin.plugin.config().abilityConfigs;
            boolean[] modified = {false};
            AbilitySetConfig remove = abilityConfigs.remove(id);
            if (remove == null) {
                new Message(I18n.format("delete.ability.fail", id)).send(sender);
                return;
            }
            findInMobs(sender, id, mobConfig -> {
                List<String> collect = mobConfig.abilities.stream().filter(s -> {
                    String[] split = s.split(":", 2);
                    String s1 = split[0];
                    return s1.equals(id);
                }).collect(Collectors.toList());
                modified[0] = modified[0] || mobConfig.abilities.removeAll(collect);
            });
            abilityConfigs.saveToDir();
            new Message(I18n.format("delete.ability.success", id)).send(sender);
            if (modified[0]) {
                InfPlugin.plugin.config().mobConfigs.saveToDir();
            }
        }

        private void findInMobs(CommandSender sender, String id, Consumer<MobConfig> func) {
            NamedDirConfigs<MobConfig> mobConfigs = InfPlugin.plugin.config().mobConfigs;
            mobConfigs.values().stream().filter(mobConfig -> mobConfig.abilities.stream().anyMatch(s -> {
                String[] split = s.split(":", 2);
                String s1 = split[0];
                return s1.equals(id);
            })).forEach(func);
        }

        public List<String> abilityCompleter(CommandSender sender, Arguments arguments) {
            List<String> completeStr = new ArrayList<>();
            switch (arguments.remains()) {
                case 1:
                    completeStr.addAll(InfPlugin.plugin.config().abilityConfigs.keys());
                    break;
            }
            return filtered(arguments, completeStr);
        }

        //@SubCommand(value = "", permission = "im.delete.", tabCompleter = "Completer")
        public void sampleCommand(CommandSender sender, Arguments arguments) {
        }

        public List<String> sampleCompleter(CommandSender sender, Arguments arguments) {
            List<String> completeStr = new ArrayList<>();
            switch (arguments.remains()) {
                case 1:
                    break;
            }
            return filtered(arguments, completeStr);
        }

    }
    //</editor-fold>
}
