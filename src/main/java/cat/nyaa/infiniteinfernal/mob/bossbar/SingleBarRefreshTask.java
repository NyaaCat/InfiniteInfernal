package cat.nyaa.infiniteinfernal.mob.bossbar;

import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.mob.MobManager;
import cat.nyaa.infiniteinfernal.utils.AngledEntity;
import org.bukkit.Bukkit;
import org.bukkit.boss.KeyedBossBar;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

class SingleBarRefreshTask extends BukkitRunnable {
        @Override
        public synchronized void cancel() throws IllegalStateException {
            super.cancel();
            Collection<IMob> mobs = MobManager.instance().getMobs();
            mobs.parallelStream().forEach(iMob -> iMob.getBossBar().removeAll());
        }

        @Override
        public void run() {
            Collection<IMob> mobs = MobManager.instance().getMobs();
            Iterator<KeyedBossBar> bossBars = Bukkit.getBossBars();
            bossBars.forEachRemaining(keyedBossBar -> {
                if (!MobManager.instance().isMobBar(keyedBossBar)) {
                    keyedBossBar.removeAll();
                    Bukkit.removeBossBar(keyedBossBar.getKey());
                }
            });
            Map<Player, List<AngledEntity>> maps = new LinkedHashMap<>();
            mobs.stream().forEach(iMob -> {
                LivingEntity entity = iMob.getEntity();
                List<Player> playersNearMob = MobManager.instance().getPlayersNearMob(iMob);
                playersNearMob.stream().forEach(player -> {
                    AngledEntity of = AngledEntity.of(entity, player);
                    List<AngledEntity> angledEntities = maps.computeIfAbsent(player, player1 -> new ArrayList<>());
                    angledEntities.add(of);
                });
            });
            Map<IMob, List<Player>> barMap = new LinkedHashMap<>();
            maps.entrySet().stream().forEach(playerListEntry -> {
                AngledEntity angledEntity = playerListEntry.getValue().stream().min(AngledEntity::compareTo).orElse(null);
                if (angledEntity != null) {
                    IMob iMob = MobManager.instance().toIMob(angledEntity.getEntity());
                    if (iMob != null) {
                        List<Player> players = barMap.computeIfAbsent(iMob, iMob1 -> new ArrayList<>());
                        players.add(playerListEntry.getKey());
                    }
                }
            });
            commit(barMap);
        }

        private void commit(Map<IMob, List<Player>> barMap) {
            Collection<IMob> mobs = MobManager.instance().getMobs();
            mobs.stream().forEach(iMob -> {
                List<Player> players = barMap.get(iMob);
                KeyedBossBar bossBar = iMob.getBossBar();
                if (bossBar == null) {
                    return;
                }
                BossbarManager.update(iMob);
                if (players == null) {
                    bossBar.removeAll();
                    return;
                }
                List<Player> watchers = bossBar.getPlayers();
                watchers.stream().forEach(player -> {
                    if (!players.contains(player)) {
                        bossBar.removePlayer(player);
                    }
                });
                players.stream().forEach(player -> {
                    if (!watchers.contains(player)) {
                        bossBar.addPlayer(player);
                    }
                });
            });

        }
    }