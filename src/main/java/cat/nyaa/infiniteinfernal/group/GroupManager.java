package cat.nyaa.infiniteinfernal.group;

import cat.nyaa.infiniteinfernal.I18n;
import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.nyaacore.Message;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class GroupManager {
    private static GroupManager INSTANCE;

    Map<String, Group> groupMap = new LinkedHashMap<>();
    Map<Player, Group> playerGroupMap = new LinkedHashMap<>();
    Map<UUID, InviteSession> inviteSessionMap = new LinkedHashMap<>();

    private GroupManager() {

    }

    public static GroupManager getInstance() {
        if (INSTANCE == null) {
            synchronized (GroupManager.class) {
                if (INSTANCE == null){
                    INSTANCE = new GroupManager();
                }
            }
        }
        return INSTANCE;
    }

    public Group getGroup(String groupName) {
        return groupMap.get(groupName);
    }

    public Collection<? extends String> getGroupNames() {
        return groupMap.keySet();
    }

    public Group getPlayerGroup(Player player) {
        return playerGroupMap.get(player);
    }

    public Collection<? extends String> getPlayerNames() {
        return playerGroupMap.keySet().stream().map(HumanEntity::getName).collect(Collectors.toList());
    }

    public InviteSession getSession(UUID uniqueId) {
        return inviteSessionMap.get(uniqueId);
    }

    public void removeSession(UUID uniqueId) {
        inviteSessionMap.remove(uniqueId);
    }

    public void invite(Player inviter, Player player, Group target) {
        String inviterName = inviter == null? "" : inviter.getName();
        InviteSession inviteSession = new InviteSession(inviter, player, target);
        inviteSessionMap.put(player.getUniqueId(), inviteSession);
        new BukkitRunnable(){
            @Override
            public void run() {
                if (inviteSessionMap.containsKey(player.getUniqueId())){
                    InviteSession session = inviteSessionMap.get(player.getUniqueId());
                    if (session.equals(inviteSession)){
                        inviteSessionMap.remove(player.getUniqueId());
                        new Message("").append(I18n.format("group.invite.timeout")).send(player);
                    }
                }
            }
        }.runTaskLater(InfPlugin.plugin, 600);
        new Message("").append(I18n.format("group.invite.message", inviterName, player.getName(), target.getName())).send(player);
    }

    public void createGroup(String groupName, Player creator) {
        Group group = new Group(groupName);
        groupMap.put(groupName, group);
        group.joinMember(creator);
        group.addAdmin(creator);
    }

    public void disband(Group group) {
        groupMap.remove(group.name);
    }


    class InviteSession {
        public final Player inviter;
        public final Player player;
        public final Group target;

        public InviteSession(Player inviter, Player player, Group target) {
            this.inviter = inviter;
            this.player = player;
            this.target = target;
        }
    }
}
