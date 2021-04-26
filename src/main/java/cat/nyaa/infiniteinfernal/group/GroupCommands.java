package cat.nyaa.infiniteinfernal.group;

import cat.nyaa.infiniteinfernal.I18n;
import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.utils.RandomUtil;
import cat.nyaa.infiniteinfernal.utils.Utils;
import cat.nyaa.nyaacore.ILocalizer;
import cat.nyaa.nyaacore.Message;
import cat.nyaa.nyaacore.cmdreceiver.Arguments;
import cat.nyaa.nyaacore.cmdreceiver.BadCommandException;
import cat.nyaa.nyaacore.cmdreceiver.CommandReceiver;
import cat.nyaa.nyaacore.cmdreceiver.SubCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GroupCommands extends CommandReceiver {

    /**
     * @param plugin for logging purpose only
     * @param _i18n
     */
    public GroupCommands(Plugin plugin, ILocalizer _i18n) {
        super(plugin, _i18n);
        manageCommand = new ManageCommands(plugin, _i18n);
    }

    @SubCommand(value = "join", permission = "im.group.admin", tabCompleter = "joinCompleter")
    public void onJoin(CommandSender sender, Arguments arguments) {
        String groupName = arguments.nextString();
        Group group = GroupManager.getInstance().getGroup(groupName);
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
        GroupManager.getInstance().join(player, group);
        new Message("").append(I18n.format("group.join.success", player.getName(), groupName)).send(sender);
        if (sender != player) {
            Message append = new Message("").append(I18n.format("group.join.success", player.getName(), groupName));
            group.broadcast(append);
        }
    }

    public List<String> joinCompleter(CommandSender sender, Arguments arguments) {
        List<String> completeStr = new ArrayList<>();
        GroupManager groupManager = GroupManager.getInstance();
        switch (arguments.remains()) {
            case 1:
                completeStr.add("<teamName>");
                completeStr.addAll(groupManager.getGroupNames());
                break;
            case 2:
                String s = arguments.nextString();
                Group group = groupManager.getGroup(s);
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

    @SubCommand(value = "leave", permission = "im.group", tabCompleter = "leaveCompleter")
    public void onLeave(CommandSender sender, Arguments arguments) {
        Player player = null;
        if (sender.hasPermission("im.group.admin") && arguments.top() != null) {
            player = arguments.nextPlayer();
        } else {
            player = asPlayer(sender);
        }
        Group group = GroupManager.getInstance().getPlayerGroup(player);
        if (group == null) {
            new Message("").append(I18n.format("group.leave.no_group", player.getName())).send(sender);
            return;
        }
        GroupManager.getInstance().checkAndLeave(player);
        Message append = new Message("").append(I18n.format("group.leave.success", player.getName()));
        group.broadcast(append);
        append.send(player);
    }

    public List<String> leaveCompleter(CommandSender sender, Arguments arguments) {
        List<String> completeStr = new ArrayList<>();
        switch (arguments.remains()) {
            case 1:
                if (sender.hasPermission("im.group.admin")) {
                    completeStr.addAll(GroupManager.getInstance().getPlayerNames());
                }
                break;
        }
        return filtered(arguments, completeStr);
    }

    @SubCommand(value = "invite", permission = "im.group", tabCompleter = "inviteCompleter")
    public void onInvite(CommandSender sender, Arguments arguments) {
        Player player = arguments.nextPlayer();
        Group target = null;
        Player inviter = null;
        GroupManager instance = GroupManager.getInstance();
        if (sender.isOp() || sender.hasPermission("im.group.admin")) {
            if (arguments.top() != null) {
                target = instance.getGroup(arguments.nextString());
            } else {
                if (sender instanceof Player) {
                    target = instance.getPlayerGroup(((Player) sender));
                    inviter = ((Player) sender);
                }
            }
        } else if (sender instanceof Player) {
                target = instance.getPlayerGroup(((Player) sender));
                inviter = ((Player) sender);
        }
        if (target == null) {
            new Message("").append(I18n.format("group.invite.no_group")).send(sender);
            return;
        }
        GroupManager.getInstance().invite(inviter, player, target);
        new Message("").append(I18n.format("group.invite.requested", player.getName(), target.getName())).send(sender);
    }

    public List<String> inviteCompleter(CommandSender sender, Arguments arguments) {
        List<String> completeStr = new ArrayList<>();
        GroupManager instance = GroupManager.getInstance();
        switch (arguments.remains()) {
            case 1:
                if (sender instanceof Player) {
                    Group playerGroup = instance.getPlayerGroup(((Player) sender));
                    if (playerGroup!=null) {
                        completeStr.addAll(Bukkit.getOnlinePlayers().stream().filter(player -> !playerGroup.containsMember(player)).map(pl -> pl.getName()).collect(Collectors.toSet()));
                    }
                }
                if (sender.isOp() || sender.hasPermission("im.group.admin")) {
                    if (filtered(arguments, completeStr).size() == 0){
                        completeStr.addAll(instance.getGroupNames());
                    }
                }
                break;
            case 2:
                if (sender.isOp() || sender.hasPermission("im.group.admin")) {
                    Group playerGroup;
                    if (sender instanceof Player) {
                        playerGroup = instance.getPlayerGroup(((Player) sender));
                    }else {
                        playerGroup = instance.getGroup(arguments.top());
                    }
                    if (playerGroup != null){
                        completeStr.addAll(Bukkit.getOnlinePlayers().stream().filter(player -> !playerGroup.containsMember(player)).map(pl -> pl.getName()).collect(Collectors.toSet()));
                    }
                }
                break;
        }
        return filtered(arguments, completeStr);
    }

    @SubCommand(value = "accept", permission = "im.group")
    public void onAccept(CommandSender sender, Arguments arguments) {
        Player player = asPlayer(sender);
        GroupManager.InviteSession session = GroupManager.getInstance().getSession(player.getUniqueId());
        if (session == null) {
            new Message("").append(I18n.format("group.invite.no_session")).send(sender);
            return;
        }
        if (session.player.equals(player)) {
            Group target = session.target;
            GroupManager.getInstance().join(player, target);
            GroupManager.getInstance().removeSession(player.getUniqueId());
        }
    }

    @SubCommand(value = "deny", permission = "im.group")
    public void onDeny(CommandSender sender, Arguments arguments) {
        Player player = asPlayer(sender);
        GroupManager.InviteSession session = GroupManager.getInstance().getSession(player.getUniqueId());
        if (session == null) {
            new Message("").append(I18n.format("group.invite.no_session")).send(sender);
            return;
        }
        if (session.player.equals(player)) {
            Group target = session.target;
            deny(session);
        }
    }

    private void deny(GroupManager.InviteSession session) {
        new Message("").append(I18n.format("group.invite.denied_inviter", session.player.getName(), session.target.getName())).send(session.inviter);
        new Message("").append(I18n.format("group.invite.denied_player", session.target.getName())).send(session.player);
        GroupManager.getInstance().removeSession(session.player.getUniqueId());
    }

    @SubCommand(value = "addPlayer", permission = "im.group.admin", tabCompleter = "addPlayerCompleter")
    public void onAddPlayer(CommandSender sender, Arguments arguments) {
        String s = arguments.nextString();
        Group group = GroupManager.getInstance().getGroup(s);
        Player player = arguments.nextPlayer();
        if (group == null) {
            new Message("").append(I18n.format("group.add_player.no_group", s)).send(sender);
            return;
        }
        GroupManager.getInstance().join(player, group);
    }

    public List<String> addPlayerCompleter(CommandSender sender, Arguments arguments) {
        List<String> completeStr = new ArrayList<>();
        switch (arguments.remains()) {
            case 1:
                completeStr.addAll(GroupManager.getInstance().getGroupNames());
                break;
            case 2:
                String s = arguments.nextString();
                Group group = GroupManager.getInstance().getGroup(s);
                if (group != null) {
                    completeStr.addAll(Bukkit.getOnlinePlayers().stream().filter(player -> !group.containsMember(player)).map(HumanEntity::getName).collect(Collectors.toList()));
                }
                break;
        }
        return filtered(arguments, completeStr);
    }

    @SubCommand(value = "create", permission = "im.group")
    public void onCreateGroup(CommandSender sender, Arguments arguments) {
        String name = arguments.nextString();
        GroupManager instance = GroupManager.getInstance();
        if (instance.getGroup(name) != null) {
            new Message("").append(I18n.format("group.create.exists", name)).send(sender);
            return;
        }
        Player player = asPlayer(sender);
        instance.createGroup(name, player);
        new Message("").append(I18n.format("group.create.success", name)).send(sender);
    }

    @SubCommand(value = "list", permission = "im.group")
    public void onList(CommandSender sender, Arguments arguments) {
        if (sender instanceof Player) {
            Group playerGroup = GroupManager.getInstance().getPlayerGroup((Player) sender);
            if (playerGroup == null) {
                listGroup(sender);
                return;
            }
            listPlayersInGroup(playerGroup, sender);
        } else {
            listGroup(sender);
        }
    }

    @SubCommand(value = "tp", permission = "im.group.admin")
    public void onTp(CommandSender sender, Arguments arguments){
        Player player = asPlayer(sender);
        Group playerGroup = GroupManager.getInstance().getPlayerGroup(player);
        if (playerGroup==null)return;
        if (playerGroup.getMembers().size() <= 1)return;
        List<Player> collect = playerGroup.getMembers().stream().filter(player1 -> !player1.equals(player)).collect(Collectors.toList());
        Player player1 = RandomUtil.randomPick(collect);
        player.teleport(player1);
    }

    private void listPlayersInGroup(Group playerGroup, CommandSender sender) {
        new Message("").append(I18n.format("group.list.message.players", playerGroup.getName())).send(sender);
        Message message = new Message("");
        Collection<? extends String> groupNames = playerGroup.getMemberNames();
        groupNames.forEach(s -> message.append(String.format("\"%s\"", s)).append(" "));
        message.send(sender);
    }

    private void listGroup(CommandSender sender) {
        new Message("").append(I18n.format("group.list.message.groups")).send(sender);
        Message message = new Message("");
        Collection<? extends String> groupNames = GroupManager.getInstance().getGroupNames();
        groupNames.forEach(s -> message.append(String.format("\"%s\"", s)).append(" "));
        message.send(sender);
    }

    @SubCommand(value = "manage", permission = "im.group")
    public ManageCommands manageCommand;


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

    @Override
    public String getHelpPrefix() {
        return null;
    }

    public static class ManageCommands extends CommandReceiver {

        /**
         * @param plugin for logging purpose only
         * @param _i18n
         */
        public ManageCommands(Plugin plugin, ILocalizer _i18n) {
            super(plugin, _i18n);
        }

        Set<CommandSender> disbandSet = new HashSet<>();

        @SubCommand(value = "admin", permission = "im.group", tabCompleter = "adminCompleter")
        public void onAdmin(CommandSender sender, Arguments arguments) {
            String action = arguments.nextString();
            Group group;
            switch (action) {
                case "add":
                case "remove":
                    group = GroupManager.getInstance().getPlayerGroup(asPlayer(sender));
                    break;
                default:
                    group = GroupManager.getInstance().getGroup(action);
                    if (group == null) {
                        new Message("").append(I18n.format("group.admin.no_group_name", action));
                    }
                    action = arguments.nextString();
            }
            if (group == null) {
                throw new IllegalArgumentException();
            }
            if (!sender.isOp() && !group.isAdmin(asPlayer(sender))) {
                new Message("").append(I18n.format("error.permission")).send(sender);
                return;
            }
            Player player = arguments.nextPlayer();
            switch (action) {
                case "add":
                    group.addAdmin(player);
                    new Message("").append(I18n.format("group.manage.admin.add_success", player.getName(), group.name)).send(sender);
                    if (sender != player) {
                        new Message("").append(I18n.format("group.manage.admin.add_success", player.getName(), group.name)).send(player);
                    }
                    break;
                case "remove":
                    group.removeAdmin(player);
                    new Message("").append(I18n.format("group.manage.admin.remove_success", player.getName(), group.name)).send(sender);
                    if (sender != player) {
                        new Message("").append(I18n.format("group.manage.admin.add_success", player.getName(), group.name)).send(player);
                    }
                    break;
                default:
                    throw new BadCommandException();
            }
        }

        public List<String> adminCompleter(CommandSender sender, Arguments arguments) {
            List<String> completeStr = new ArrayList<>();
            switch (arguments.remains()) {
                case 1:
                    completeStr.add("add");
                    completeStr.add("remove");
                    if (sender.isOp() || sender.hasPermission("im.group.admin")) {
                        completeStr.add("<groupName>");
                        if (filtered(arguments, completeStr).size() == 0) {
                            completeStr.addAll(GroupManager.getInstance().getGroupNames());
                        }
                    }
                    break;
                case 2:
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        Group group = GroupManager.getInstance().getPlayerGroup(player);
                        if (group != null) {
                            completeStr.addAll(group.getMemberNames());
                        } else {
                            completeStr.addAll(GroupManager.getInstance().getGroupNames());
                        }
                    }
                    break;
            }
            return filtered(arguments, completeStr);
        }

        @SubCommand(value = "kick", permission = "im.group", tabCompleter = "kickCompleter")
        public void onKick(CommandSender sender, Arguments arguments) {
            Player player = asPlayer(sender);
            Group group = GroupManager.getInstance().getPlayerGroup(player);
            if (group == null) {
                new Message("").append(I18n.format("group.manage.no_group")).send(sender);
                return;
            }
            if (!sender.isOp() && !group.isAdmin(asPlayer(sender))) {
                new Message("").append(I18n.format("error.permission")).send(sender);
                return;
            }
            GroupManager.getInstance().kick(player, group);
        }

        public List<String> kickCompleter(CommandSender sender, Arguments arguments) {
            List<String> completeStr = new ArrayList<>();
            switch (arguments.remains()) {
                case 1:
                    if (sender instanceof Player) {
                        Group group = GroupManager.getInstance().getPlayerGroup((Player) sender);
                        if (group != null) {
                            completeStr.addAll(group.getMemberNames());
                        }
                    }
                    break;
            }
            return filtered(arguments, completeStr);
        }

        @SubCommand(value = "expDropMode", permission = "im.group", tabCompleter = "expDropModeCompleter")
        public void onDropMode(CommandSender sender, Arguments arguments) {
            Player player = asPlayer(sender);
            Group group = GroupManager.getInstance().getPlayerGroup(player);

            if (arguments.top() == null) {
                new Message(group.dropMode.name()).send(sender);
                return;
            }
            Group.ExpDropMode expDropMode = arguments.nextEnum(Group.ExpDropMode.class);
            group.setDropMode(expDropMode);
            new Message("").append(I18n.format("group.manage.expdropmode_change", expDropMode.name())).send(sender);
        }

        public List<String> expDropModeCompleter(CommandSender sender, Arguments arguments) {
            List<String> completeStr = new ArrayList<>();
            switch (arguments.remains()) {
                case 1:
                    completeStr.addAll(Stream.of(Group.ExpDropMode.values()).map(Enum::name).collect(Collectors.toList()));
                    break;
            }
            return filtered(arguments, completeStr);
        }

        @SubCommand(value = "lootMode", permission = "im.group", tabCompleter = "lootModeCompleter")
        public void onLootMode(CommandSender sender, Arguments arguments) {
            Player player = asPlayer(sender);
            Group group = GroupManager.getInstance().getPlayerGroup(player);

            if (arguments.top() == null) {
                new Message(group.dropMode.name()).send(sender);
                return;
            }
            Group.LootMode lootMode = arguments.nextEnum(Group.LootMode.class);
            group.setLootMode(lootMode);
            new Message("").append(I18n.format("group.manage.lootmode_change", lootMode.name())).send(sender);
        }

        public List<String> lootModeCompleter(CommandSender sender, Arguments arguments) {
            List<String> completeStr = new ArrayList<>();
            switch (arguments.remains()) {
                case 1:
                    completeStr.addAll(Stream.of(Group.LootMode.values()).map(Enum::name).collect(Collectors.toList()));
                    break;
            }
            return filtered(arguments, completeStr);
        }

        @SubCommand(value = "disband", permission = "im.group", tabCompleter = "disbandCompleter")
        public void onDisband(CommandSender sender, Arguments arguments) {
            if (arguments.top() != null) {
                String s = arguments.nextString();
                Group group = GroupManager.getInstance().getGroup(s);
                if (group == null) {
                    new Message("").append(I18n.format("group.disband.no_group")).send(sender);
                    return;
                }
                if (sender.isOp() || sender.hasPermission("im.group.admin")) {
                    if (!disbandSet.contains(sender)) {
                        sendConfirmMessage(sender, group.getName());
                    } else {
                        disband(group, sender.getName());
                    }
                }
                return;
            }
            Player player = asPlayer(sender);
            Group playerGroup = GroupManager.getInstance().getPlayerGroup(player);
            if (playerGroup == null) {
                new Message("").append(I18n.format("group.disband.no_group")).send(sender);
                return;
            }
            if (!disbandSet.contains(sender)) {
                sendConfirmMessage(sender, playerGroup.getName());
            } else {
                disband(playerGroup, sender.getName());
            }
        }

        private void disband(Group group, String name) {
            GroupManager.getInstance().disband(group);
            group.disband();
        }

        private void sendConfirmMessage(CommandSender sender, String name) {
            new Message("").append(I18n.format("group.disband.confirm_message", name)).send(sender);
            disbandSet.add(sender);
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (disbandSet.remove(sender)) {
                        new Message("").append(I18n.format("group.disband.aborted")).send(sender);
                    }
                }
            }.runTaskLater(InfPlugin.plugin, 200);
        }

        public List<String> disbandCompleter(CommandSender sender, Arguments arguments) {
            List<String> completeStr = new ArrayList<>();
            switch (arguments.remains()) {
                case 1:
                    if (sender.isOp() || sender.hasPermission("im.group.admin")) {
                        completeStr.addAll(GroupManager.getInstance().getGroupNames());
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

