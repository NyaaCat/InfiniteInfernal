package cat.nyaa.infiniteinfernal.group;

import cat.nyaa.infiniteinfernal.I18n;
import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.nyaacore.Message;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
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
        join(creator, group);
        group.addAdmin(creator);
    }

    void join(Player player, Group group) {
        checkAndLeave(player);
        Message append = new Message("").append(I18n.format("group.leave.success", player.getName()));
        group.joinMember(player);
        playerGroupMap.put(player, group);
    }

    void checkAndLeave(Player player) {
        Group group1 = GroupManager.getInstance().getPlayerGroup(player);
        if (group1 != null){
            group1.leaveMember(player.getUniqueId());
            playerGroupMap.remove(player);
        }
    }


    public void disband(Group group) {
        groupMap.remove(group.name);
        quitCache.cleanUp();
    }

    Cache<UUID, Group> quitCache = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    public void autoJoin(Player player) {
        Group group = quitCache.getIfPresent(player.getUniqueId());
        if (group != null){
            join(player, group);
        }
    }

    public void savePlayerState(Player player) {
        Group playerGroup = getPlayerGroup(player);
        if (playerGroup!=null){
            quitCache.put(player.getUniqueId(), playerGroup);
            playerGroup.broadcast(new Message("").append(I18n.format("group.leave.success", player.getName())));
            checkAndLeave(player);
        }
    }

    public void kick(Player player, Group group) {
        group.kick(player);
        quitCache.invalidate(player.getUniqueId());
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
