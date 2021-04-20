package cat.nyaa.infiniteinfernal.mob.bossbar;

import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.mob.MobManager;
import cat.nyaa.infiniteinfernal.utils.AngledEntity;
import cat.nyaa.nyaacore.Pair;
import org.bukkit.Bukkit;
import org.bukkit.boss.KeyedBossBar;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.stream.Collectors;

class BossbarRefreshTask extends BukkitRunnable {
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
                    Bukkit.removeBossBar(keyedBossBar.getKey());
                }
            });
            BarUpdater updater = new BarUpdater();
            if (!mobs.isEmpty()) {
                mobs.forEach(iMob -> {
                    LivingEntity entity = iMob.getEntity();
                    List<AngledEntity> nearbyPlayers = MobManager.instance().getPlayersNearMob(iMob).stream()
                            .map(player -> AngledEntity.of(entity, player))
                            .filter(angledEntity -> angledEntity.getAngle() >= 0)
                            .sorted(AngledEntity::compareTo)
                            .collect(Collectors.toList());
                    KeyedBossBar bossBar = iMob.getBossBar();
                    if (bossBar == null) return;
                    if (!nearbyPlayers.isEmpty()) {
                        updater.registerIMob(iMob, nearbyPlayers);
                    }
                    iMob.updateBossBar(bossBar, entity);
                });
                updater.commit();
            }
        }

        class BarUpdater {
            List<IMob> iMobs;
            private Map<Player, List<Pair<IMob, AngledEntity>>> playerCounter;
            private Map<IMob, List<Player>> barPlayerMap;

            BarUpdater() {
                this.playerCounter = new LinkedHashMap<>();
                barPlayerMap = new LinkedHashMap<>();
                iMobs = new ArrayList<>();
            }

            void registerIMob(IMob iMob, List<AngledEntity> nearbyPlayers) {
                iMobs.add(iMob);
                nearbyPlayers.stream()
                        .forEach(angledEntity -> {
                            LivingEntity livingEntity = angledEntity.getEntity();
                            if (!(livingEntity instanceof Player)) return;
                            this.add(iMob, (Player) livingEntity);
                        });
            }

            void add(IMob iMob, Player player) {
                List<Player> players = barPlayerMap.computeIfAbsent(iMob, iMob1 -> new ArrayList<>());
                List<Pair<IMob, AngledEntity>> pairs = playerCounter.computeIfAbsent(player, player1 -> new ArrayList<>());
                Pair<IMob, AngledEntity> bar = new Pair<>(iMob, AngledEntity.of(iMob.getEntity(), player));
                if (pairs.size() < 5) {
                    pairs.add(bar);
                    players.add(player);
                } else {
                    for (int i = 0; i < pairs.size(); i++) {
                        AngledEntity targetEntity = bar.getValue();
                        if (targetEntity.compareTo(pairs.get(i).getValue()) < 0) {
                            IMob key = bar.getKey();
                            pairs.set(i, bar);
                            players.add(player);
                            List<Player> players1 = barPlayerMap.computeIfAbsent(key, iMob1 -> new ArrayList<>());
                            if (targetEntity.getEntity() instanceof Player) {
                                players1.remove(targetEntity.getEntity());
                            }
                            break;
                        }
                    }
                }
            }

            void commit() {
                barPlayerMap.entrySet().stream()
                        .forEach(entry -> {
                            List<Player> watchers = entry.getValue();
                            KeyedBossBar bossBar = entry.getKey().getBossBar();
                            bossBar.getPlayers().parallelStream()
                                    .filter(player -> !watchers.contains(player))
                                    .forEach(bossBar::removePlayer);
                            watchers.parallelStream()
                                    .forEach(bossBar::addPlayer);
                        });
            }
        }

//        void add(List<Pair<BossBar, AngledEntity>> pairs, Pair<BossBar, AngledEntity> bar) {
//            bar.getKey().addPlayer(((Player) bar.getValue().livingEntity));
//            pairs.add(bar);
//        }
//
//        void replace(List<Pair<BossBar, AngledEntity>> pairs, int origin, Pair<BossBar, AngledEntity> replacement) {
////            replacement.getKey().addPlayer(((Player) replacement.getValue().livingEntity));
//            pairs.add(origin, replacement);
//            Pair<BossBar, AngledEntity> remove = pairs.remove(pairs.size() - 1);
////            remove.getKey().removePlayer(((Player) remove.getValue().livingEntity));
//        }
    }