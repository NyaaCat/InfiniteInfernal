package cat.nyaa.infiniteinfernal.group;

import cat.nyaa.infiniteinfernal.I18n;
import cat.nyaa.nyaacore.ILocalizer;
import cat.nyaa.nyaacore.Message;
import cat.nyaa.nyaacore.cmdreceiver.Arguments;
import cat.nyaa.nyaacore.cmdreceiver.CommandReceiver;
import cat.nyaa.nyaacore.cmdreceiver.SubCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GroupCommands extends CommandReceiver {
    Map<String, Group> groupMap = new LinkedHashMap<>();

    Map<Player, Group> playerGroupMap = new LinkedHashMap<>();

    /**
     * @param plugin for logging purpose only
     * @param _i18n
     */
    public GroupCommands(Plugin plugin, ILocalizer _i18n) {
        super(plugin, _i18n);
        manageCommand = new ManageCommands(plugin, _i18n);
    }

    @SubCommand(value = "join", permission = "im.group", tabCompleter = "joinCompleter")
    public void onJoin(CommandSender sender, Arguments arguments) {
        String groupName = arguments.nextString();
        Group group = groupMap.get(groupName);
        if (group == null) {
            new Message("").append(I18n.format("group.join.no_group", groupName)).send(sender);
            return;
        }
        Player player = null;
        if (sender.hasPermission("im.group.admin") && arguments.top() != null) {
            player = arguments.nextPlayer();
        } else {
            player = asPlayer(sender);
        }
        if (group.containsMember(player)) {
            new Message("").append(I18n.format("group.join.already_in", groupName)).send(sender);
            return;
        }
        join(player, group);
        new Message("").append(I18n.format("group.join.success", player.getName(), groupName)).send(sender);
    }

    @SubCommand(value = "leave", permission = "im.group", tabCompleter = "leaveCompleter")
    public void onLeave(CommandSender sender, Arguments arguments) {
        if (sender.hasPermission("im.group.admin") && arguments.top() != null){
            Player player = arguments.nextPlayer();
            new Message("").append(I18n.format("group.leave.cleared", player.getName())).send(sender);
            return;
        }
        Player player = asPlayer(sender);
        Group group = playerGroupMap.get(player);
        if (group == null){
            new Message("").append(I18n.format("group.leave.no_group")).send(player);
            return;
        }
        group.leaveMember(player);
        new Message("").append(I18n.format("group.leave.success", group.getName())).send(sender);

    }

    @SubCommand(value = "manage", permission = "im.group", tabCompleter = "leaveCompleter")
    ManageCommands manageCommand;

    public List<String> leaveCompleter(CommandSender sender, Arguments arguments) {
        List<String> completeStr = new ArrayList<>();
        switch (arguments.remains()) {
            case 1:
                if (sender.hasPermission("im.group.admin")){
                    completeStr.addAll(playerGroupMap.keySet().stream().map(HumanEntity::getName).collect(Collectors.toList()));
                }
                break;
        }
        return filtered(arguments, completeStr);
    }

    private void join(Player player, Group group) {
        checkAndLeave(player);
        group.joinMember(player);
    }

    private void checkAndLeave(Player player) {
        Group group1 = playerGroupMap.get(player);
        if (group1 != null){
            group1.leaveMember(player);
        }
    }

    public List<String> joinCompleter(CommandSender sender, Arguments arguments) {
        List<String> completeStr = new ArrayList<>();
        switch (arguments.remains()) {
            case 1:
                completeStr.add("<teamName>");
                completeStr.addAll(groupMap.keySet());
                break;
            case 2:
                String s = arguments.nextString();
                Group group = groupMap.get(s);
                if (sender.hasPermission("im.group.admin")) {
                    List<String> collect = Bukkit.getServer().getOnlinePlayers().stream()
                            .filter(player -> group == null ? true : !group.containsMember(player))
                            .map(HumanEntity::getName)
                            .collect(Collectors.toList());
                    completeStr.addAll(collect);
                }
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

    private static List<String> filtered(Arguments arguments, List<String> completeStr) {
        String next = arguments.at(arguments.length() - 1);
        return completeStr.stream().filter(s -> s.startsWith(next)).collect(Collectors.toList());
    }


    public Group newGroup(String name) {
        return groupMap.computeIfAbsent(name, s -> new Group(name));
    }

    public Group getGroup(String name) {
        return groupMap.get(name);
    }


    @Override
    public String getHelpPrefix() {
        return null;
    }

    public class ManageCommands extends CommandReceiver {

        /**
         * @param plugin for logging purpose only
         * @param _i18n
         */
        public ManageCommands(Plugin plugin, ILocalizer _i18n) {
            super(plugin, _i18n);
        }

        @SubCommand(value = "admin", permission = "im.group", tabCompleter = "adminCompleter")
        public void onAdmin(CommandSender sender, Arguments arguments) {
            String action = arguments.nextString();
            Group group;

            switch (action){
                case "add":
                case "remove":
                    group = playerGroupMap.get(asPlayer(sender));
                    break;
                default:
                    group = groupMap.get(action);
                    if (group == null){
                        new Message("").append(I18n.format("group.admin.no_group_name"));
                    }
                    action = arguments.nextString();
            }
            if (group == null){
                throw new IllegalArgumentException();
            }
            if (!sender.isOp() && !group.isAdmin(asPlayer(sender))){
                throw new IllegalAccessException()
            }
            switch (action){
                case "add":
                    group
                    break;
                case "remove":
                    break;
                default:
                    throw new IllegalArgumentException();
            }
        }

        public List<String> adminCompleter(CommandSender sender, Arguments arguments) {
            List<String> completeStr = new ArrayList<>();
            switch (arguments.remains()) {
                case 1:
                    if (sender instanceof Player){
                        Player player = (Player) sender;
                        Group group = playerGroupMap.get(player);
                        if (group != null){
                            completeStr.addAll(group.getMemberNames());
                        }else {
                            completeStr.addAll(groupMap.keySet());
                        }
                    }
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

        @Override
        public String getHelpPrefix() {
            return null;
        }
    }
}

