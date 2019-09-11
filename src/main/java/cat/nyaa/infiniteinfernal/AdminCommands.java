package cat.nyaa.infiniteinfernal;

import cat.nyaa.infiniteinfernal.ability.AbilityCollection;
import cat.nyaa.infiniteinfernal.ability.IAbility;
import cat.nyaa.infiniteinfernal.configs.*;
import cat.nyaa.infiniteinfernal.loot.ILootItem;
import cat.nyaa.infiniteinfernal.loot.LootManager;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.mob.MobManager;
import cat.nyaa.infiniteinfernal.utils.Utils;
import cat.nyaa.infiniteinfernal.utils.WeightedPair;
import cat.nyaa.nyaacore.ILocalizer;
import cat.nyaa.nyaacore.Message;
import cat.nyaa.nyaacore.cmdreceiver.Arguments;
import cat.nyaa.nyaacore.cmdreceiver.CommandReceiver;
import cat.nyaa.nyaacore.cmdreceiver.SubCommand;
import cat.nyaa.nyaacore.configuration.ISerializable;
import cat.nyaa.nyaacore.utils.InventoryUtils;
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

    public AdminCommands(InfPlugin plugin, ILocalizer _i18n) {
        super(plugin, _i18n);
        this.plugin = plugin;
        this.i18n = _i18n;
        inspectCommand = new InspectCommand(plugin, i18n);
        createCommand = new CreateCommand(plugin, i18n);
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

    @SubCommand(value = "modify", permission = "im.modify", tabCompleter = "modifyCompleter")
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
        String next = "";
        int remains = arguments.remains();
        for (int i = 0; i < remains; i++) {
            String next1 = arguments.next();
            next = next1 == null ? next : next1;
        }
        String finalNext = next;
        return completeStr.stream().filter(s -> s.startsWith(finalNext)).collect(Collectors.toList());
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
                    .map(s -> s.substring(0, s.indexOf(":")))
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
            new Message(I18n.format("create.error.ability_exists", s)).send(sender);
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
                                    new Message(I18n.format("modify.mob.spawn.biome.info", s));
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
                    mobConfig.loot.vanilla = isVanilla;
                    modified = true;
                    break;
                case "imloot":
                    boolean isImLoot = arguments.nextBoolean();
                    mobConfig.loot.imLoot = isImLoot;
                    modified = true;
                    break;
                case "expOverride":
                    int expOverride = arguments.nextInt();
                    mobConfig.loot.expOverride = expOverride;
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
                    completeStr.addAll(mobAction);
                    break;
                case 2:
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
                            String action1 = arguments.nextString();
                            switch (action1) {
                                case "chance":
                                    completeStr.add("chance");
                                    break;
                                case "add":
                                    completeStr.addAll(InfPlugin.plugin.config().abilityConfigs.keys().stream()
                                            .map(s -> s.concat(":"))
                                            .collect(Collectors.toList()));
                                    break;
                                case "remove":
                                    MobConfig mobConfig = InfPlugin.plugin.config().mobConfigs.get(id);
                                    if (mobConfig != null) {
                                        completeStr.addAll(mobConfig.abilities);
                                    }
                                    break;
                            }
                            break;
                    }
                    break;
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
                            completeString.add("world");
                            break;
                    }
                    break;
                case 3:
                    action = arguments.nextString();
                    switch (action) {
                        case "biome":
                            completeString.addAll(Arrays.stream(Biome.values()).map(Enum::name).collect(Collectors.toList()));
                            break;
                        case "world":
                            completeString.addAll(Bukkit.getWorlds().stream().map(World::getName).collect(Collectors.toList()));
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
                    String action = arguments.nextString();
                    if ("add".equals(action) || "remove".equals(action)) {
                        completeStr.addAll(InfPlugin.plugin.config().abilityConfigs.keys());
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
            if (regionConfig == null){
                new Message(I18n.format("modify.region.not_exist", s)).send(sender);
                return;
            }
            switch (action){
                case "mobs":
                    String nextAction = arguments.nextString();
                    String mob = arguments.next();
                    if (!checkMobFormat(mob)){
                        new Message(I18n.format("modify.region.mobs.not_exist")).send(sender);
                    }
                    String mobName = mob.substring(0, mob.indexOf(":"));
                    switch (nextAction){
                        case "add":
                            MobConfig mobConfig = InfPlugin.plugin.config().mobConfigs.get(mobName);
                            if (mobConfig == null){
                                new Message(I18n.format("modify.region.not_exist")).send(sender);
                            }
                            else {
                                regionConfig.mobs.add(mobName);
                                modified = true;
                                new Message(I18n.format("modify.region.not_exist")).send(sender);
                            }
                            break;
                        case "remove":
                            if (regionConfig.mobs.remove(mob)) {
                                modified = true;
                                new Message(I18n.format("modify.region.mobs.remove.success", mobName)).send(sender);
                            }else {
                                new Message(I18n.format("modify.region.mobs.remove.not_exist", mobName)).send(sender);
                            }
                            break;
                    }
                    break;
            }
            if (modified){
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
            } catch (NumberFormatException e) {
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
                    s = arguments.nextString();
                    arguments.next();
                    action = arguments.nextString();
                    RegionConfig regionConfig = InfPlugin.plugin.config().regionConfigs.get(s);
                    if ("remove".equals(action)) {
                        if (regionConfig != null) {
                            completeStr.addAll(regionConfig.mobs);
                        }
                    }else if ("add".equals(action)){
                        completeStr.addAll(MobManager.instance().getMobConfigNames());
                    }
                    break;
                case 4:
                    s = arguments.nextString();
                    action = arguments.nextString();
                    if ("add".equals(action)){
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
            switch (action){
                case "add":
                    //todo
            }
        }

        public List<String> abilityCompleter(CommandSender sender, Arguments arguments) {
            List<String> completeStr = new ArrayList<>();
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
                case 4:
                    id = arguments.nextString();
                    action = arguments.nextString();
                    abilitySetConfig = InfPlugin.plugin.config().abilityConfigs.get(id);
                    int remains = arguments.remains();
                    List<String> excluded = new ArrayList<>();
                    for (int i = 0; i < remains - 1; i++) {
                        String next = arguments.next();
                        excluded.add(next.substring(next.lastIndexOf(":")));
                    }
                    String basicAbility = arguments.nextString();
                    Class<? extends IAbility> aClass = AbilityCollection.ABILITY_NAMES.get(basicAbility);
                    String target = arguments.nextString();
                    if (target.endsWith(":")) {
                        Field field = reflectField(target, aClass);
                        completeStr.add(field.getType().getName());
                        return filtered(excluded, basicAbility, completeStr);
                    } else {
                        reflectISerializables(aClass, completeStr, "");
                        return filtered(excluded, basicAbility, completeStr);
                    }
            }
            return filtered(arguments, completeStr);
        }

        private Field reflectField(String target, Class<?> aClass) {
            String[] split = target.split(":", 2);
            try {
                String name = split[0];
                if (aClass == null)return null;
                Field declaredField = aClass.getDeclaredField(name);
                if (split.length > 1) {
                    return reflectField(split[1], declaredField.getType());
                } else {
                    return declaredField;
                }
            } catch (Exception ignored) {
                return null;
            }
        }

        private void reflectISerializables(Class<?> aClass, List<String> completeStr, String prefix) {
            if (aClass == null) return;
            Field[] declaredFields = aClass.getDeclaredFields();
            if (declaredFields.length > 0) {
                for (Field declaredField : declaredFields) {
                    try {
                        ISerializable.Serializable annotation = declaredField.getAnnotation(ISerializable.Serializable.class);
                        if (annotation != null) {
                            if (ISerializable.class.isAssignableFrom(declaredField.getType())) {
                                reflectISerializables(declaredField.getType(), completeStr, declaredField.getName().concat(":"));
                            } else {
                                completeStr.add(prefix.concat(declaredField.getName()).concat(":"));
                            }
                        }
                    } catch (Exception ignored) {
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
    //</editor-fold>
}
