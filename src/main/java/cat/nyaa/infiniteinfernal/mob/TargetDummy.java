package cat.nyaa.infiniteinfernal.mob;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.ability.IAbilitySet;
import cat.nyaa.infiniteinfernal.configs.MobConfig;
import cat.nyaa.infiniteinfernal.controler.Aggro;
import cat.nyaa.infiniteinfernal.loot.ILootItem;
import cat.nyaa.infiniteinfernal.utils.ticker.TickEvent;
import cat.nyaa.infiniteinfernal.utils.ticker.TickTask;
import cat.nyaa.infiniteinfernal.utils.ticker.Ticker;
import cat.nyaa.nyaacore.utils.NmsUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.KeyedBossBar;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.spigotmc.event.entity.EntityMountEvent;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class TargetDummy implements IMob {
    private static Map<UUID, TargetDummy> targetDummyMap = new HashMap<>();
    private static boolean inited = false;
    private static DpsListener listener = new DpsListener();
    private MobConfig mobConfig;
    private DpsCounter lastUpdatedCounter;
    private Location spawnLocation;
    private MobConfig config;
    private EntityType entityType;
    private LivingEntity trackedEntity;
    private String name;
    private double health;
    private Set<Player> trackedPlayers = new HashSet<>();

    public TargetDummy(MobConfig config, Location location) {
        if (!inited){
            init();
        }
        this.mobConfig = config;
        this.config = config;
        this.spawnLocation = location;
        this.name = config.name;
        generateFromConfig(config, 0);
        this.lastUpdatedCounter = new DpsCounter(this, getMaxHealth());
    }

    private void init() {
        Bukkit.getPluginManager().registerEvents(listener, InfPlugin.plugin);
        Ticker.getInstance().register(new DummyTicker((tickEvent)->false));
        inited = true;
    }

    void respawn() {
        if (trackedEntity !=null) {
            UUID uniqueId = trackedEntity.getUniqueId();
            targetDummyMap.remove(uniqueId);
            trackedEntity.remove();
        }
        trackedEntity = (LivingEntity) spawnLocation.getWorld().spawnEntity(spawnLocation, entityType);
        targetDummyMap.put(trackedEntity.getUniqueId(), this);
        makeInfernal(trackedEntity);
    }


    private void generateFromConfig(MobConfig config, int level) {
        this.entityType = config.type;
        this.health = config.healthOverride > 0 ? config.healthOverride : InfPlugin.plugin.config().dpsDefaultHealth;
    }

    public void remove() {
        targetDummyMap.remove(getEntity().getUniqueId());
        getEntity().remove();
    }

    static class DpsListener implements Listener{
        @EventHandler(priority = EventPriority.MONITOR)
        public void onPostDamage(EntityDamageByEntityEvent event){
            Entity damager = event.getDamager();
            if (!isTargetDummy(event.getEntity())){
                return;
            }
            TargetDummy targetDummy = toTargetDummy(event.getEntity());
            targetDummy.refreshHealth();
        }
        @EventHandler(priority = EventPriority.HIGHEST)
        public void onDamage(EntityDamageByEntityEvent event){
            Entity damager = event.getDamager();
            if (!isTargetDummy(event.getEntity())){
                return;
            }
            TargetDummy targetDummy = toTargetDummy(event.getEntity());
            Player player = null;
            if (damager instanceof Player){
                player = (Player) damager;
            }else if (damager instanceof Projectile){
                ProjectileSource shooter = ((Projectile) damager).getShooter();
                if (shooter instanceof Player) {
                    player = (Player) shooter;
                }
            }
            if (!event.getCause().equals(EntityDamageEvent.DamageCause.VOID)){
//                event.setCancelled(true);
            }
            if (player == null)return;
            submitDamage(player, targetDummy, event);
        }

        @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
        public void onMount(EntityMountEvent event){
            Entity mount = event.getMount();
            Entity entity = event.getEntity();
            if (isTargetDummy(mount) || isTargetDummy(entity)){
                event.setCancelled(true);
            }
        }

    }
    private static void submitDamage(Player player, TargetDummy targetDummy, EntityDamageByEntityEvent event) {
        DpsCounter dpsCounter = targetDummy.getDpsCounter();
        dpsCounter.submitDamage(event.getFinalDamage());
        targetDummy.trackedPlayers.add(player);
        removeLater(targetDummy, player);

        targetDummy.lastUpdatedCounter = dpsCounter;
    }

    private DpsCounter getDpsCounter() {
        return lastUpdatedCounter;
    }

    private static Map<UUID, BukkitRunnable> removeTasks = new WeakHashMap<>();
    private static void removeLater(TargetDummy targetDummy, Player player) {
        BukkitRunnable bukkitRunnable1 = removeTasks.get(player.getUniqueId());
        if (bukkitRunnable1 != null && !bukkitRunnable1.isCancelled()){
            bukkitRunnable1.cancel();
        }
        BukkitRunnable bukkitRunnable = new BukkitRunnable() {
            @Override
            public void run() {
                targetDummy.trackedPlayers.remove(player);
                removeTasks.remove(player.getUniqueId());
            }
        };
        removeTasks.put(player.getUniqueId(), bukkitRunnable);
        bukkitRunnable.runTaskLater(InfPlugin.plugin, InfPlugin.plugin.config().dpsRefreshInterval);
    }

    public static TargetDummy toTargetDummy(Entity damager) {
        return targetDummyMap.get(damager.getUniqueId());
    }

    static class DummyTicker extends TickTask {
        Queue<TargetDummy> dummyQueue = new LinkedList<>();

        public DummyTicker() {
            super();
        }

        public DummyTicker(Predicate<TickEvent> shouldRemove) {
            super(shouldRemove);
        }

        @Override
        public void run(int ticked) {
            if (!InfPlugin.plugin.config().enabled){
                return;
            }
            if (dummyQueue.isEmpty()){
                fillQueue();
            }
            int count = (int) Math.ceil(dummyQueue.size());
            for (int i = 0; i < count; i++) {
                if (dummyQueue.isEmpty()){
                    return;
                }
                TargetDummy poll = dummyQueue.poll();
                if (poll == null){
                    return;
                }
                if (targetDummyMap.get(poll.getEntity().getUniqueId()) == null){
                    return;
                }
                poll.tick(ticked);
            }
        }

        private void fillQueue() {
            dummyQueue.addAll(targetDummyMap.values());
        }

    }

    private void tick(int ticked) {
        refreshHealth();
        if(lastUpdatedCounter == null || !lastUpdatedCounter.isActive()){
            return;
        }
        refreshBossBar();
        if (ticked%10 == 0){
            refreshTitle();
        }
    }

    private void refreshTitle() {
        if (lastUpdatedCounter != null) {
            if (lastUpdatedCounter.isActive()) {
                lastUpdatedCounter.updateTitle();
            }
        }
    }

    private void refreshBossBar() {
        if(lastUpdatedCounter != null){
            lastUpdatedCounter.updateBossbar();
        }
    }

    static class DpsCounter implements IDpsMeter{
        private TargetDummy targetDummy;
        private double total = 0;
        private double dps = 0;
        private double maxDps = 0;
        private double maxVal = 2048;
        private Cleaner cleaner;
        private KeyedBossBar bossBar;
        private boolean active = false;

        public DpsCounter(TargetDummy targetDummy, double max){
            this.targetDummy = targetDummy;
            this.maxVal = max;
            bossBar = Bukkit.createBossBar(CustomMob.CUSTOM_MOB_BOSSBAR, "", BarColor.WHITE, BarStyle.SEGMENTED_10);
            updateTitle();
            MobManager.instance().bossBarIMobMap.put(bossBar, targetDummy);
            startCleaner(InfPlugin.plugin.config().dpsRefreshInterval, 10);
        }

        private void startCleaner(int timeToRefresh, int interval) {
            if (cleaner != null) {
                if (!cleaner.isCancelled()) {
                    cleaner.cancel();
                }
            }
            cleaner = new Cleaner(timeToRefresh, interval);
            cleaner.runTaskTimer(InfPlugin.plugin, 0, interval);
        }

        public boolean isActive() {
            return active;
        }

        private class Cleaner extends BukkitRunnable {
            int current;
            int timeToRefresh;
            int interval;

            Cleaner(int timeToRefresh, int interval){
                this.timeToRefresh = timeToRefresh;
                this.interval = interval;
                reset();
            }

            public void reset(){
                current = timeToRefresh;
            }

            @Override
            public void run() {
                if (current <= 0){
                    clear();
                    reset();
                    return;
                }

                current-=interval;
            }
        }

        public void submitDamage(final double damage){
            total += damage;
            dps += damage;
            maxDps = Math.max(dps, maxDps);
            cleaner.reset();
            active = true;
            new BukkitRunnable(){
                @Override
                public void run() {
                    dps -= damage;
                }
            }.runTaskLater(InfPlugin.plugin, 20);
        }

        private void updateTitle(){
            String dpsTitle = InfPlugin.plugin.config().dpsTitle;
            dpsTitle = dpsTitle.replaceAll("\\{dps}", String.format("%.2f",dps));
            dpsTitle = dpsTitle.replaceAll("\\{total}", String.format("%.2f",total));
            dpsTitle = dpsTitle.replaceAll("\\{max}", String.format("%.2f", maxDps));
            bossBar.setTitle(ChatColor.translateAlternateColorCodes('&', dpsTitle));
        }

        private void updateBossbar() {
            double max = Math.max(0, Math.min(1, dps / this.maxVal));
            bossBar.setProgress(max);
            if (max<0.25){
                bossBar.setColor(BarColor.WHITE);
            }else if (max<0.5){
                bossBar.setColor(BarColor.BLUE);
            }else if (max<0.75){
                bossBar.setColor(BarColor.YELLOW);
            }else {
                bossBar.setColor(BarColor.RED);
            }
        }

        public void clear(){
            total = 0;
            dps = 0;
            maxDps = 0;
            active = false;
        }

        @Override
        public double getDps(Player player) {
            return dps;
        }
    }

    private void refreshHealth() {
        if (trackedEntity != null) {
            if (trackedEntity.isDead()){
                respawn();
            }
            double maxHealth = trackedEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
            trackedEntity.setHealth(maxHealth);
        }
    }

    public static boolean isTargetDummy(Entity entity) {
        return targetDummyMap.containsKey(entity.getUniqueId());
    }

    @Override
    public Map<ILootItem, Integer> getLoots() {
        return new HashMap<>();
    }

    @Override
    public Map<ILootItem, Integer> getSpecialLoots() {
        return new HashMap<>();
    }

    @Override
    public List<IAbilitySet> getAbilities() {
        return new ArrayList<>();
    }

    @Override
    public LivingEntity getEntity() {
        return doSanityCheck();
    }

    private LivingEntity doSanityCheck() {
        if (trackedEntity == null){
            respawn();
        }
        return trackedEntity;
    }

    @Override
    public EntityType getEntityType() {
        return entityType;
    }

    @Override
    public KeyedBossBar getBossBar() {
        return lastUpdatedCounter==null ? null: lastUpdatedCounter.bossBar;
    }

    @Override
    public LivingEntity getTarget() {
        return null;
    }

    @Override
    public int getLevel() {
        return 0;
    }

    @Override
    public double getDamage() {
        return 0;
    }

    @Override
    public double getMaxHealth() {
        return health <= 0 ? 1024 : health;
    }

    @Override
    public double getSpecialChance() {
        return 0;
    }

    @Override
    public int getExp() {
        return 0;
    }

    @Override
    public boolean isAutoSpawn() {
        return false;
    }

    @Override
    public boolean dropVanilla() {
        return false;
    }

    @Override
    public boolean isDynamicHealth() {
        return false;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getTaggedName() {
        return name;
    }

    @Override
    public void showParticleEffect() {

    }

    @Override
    public void makeInfernal(LivingEntity entity) {
        entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(getMaxHealth());
        if (config.nbtTags != null && !config.nbtTags.equals("")) {
            NmsUtils.setEntityTag(entity, config.nbtTags);
        }
        entity.setCustomName(ChatColor.translateAlternateColorCodes('&',getTaggedName()));
        entity.setCustomNameVisible(true);
        InfPlugin.plugin.config().tags.forEach(entity::addScoreboardTag);
        if (!InfPlugin.plugin.config().dpsTag.equals("")) {
            entity.addScoreboardTag(InfPlugin.plugin.config().dpsTag);
        }
        entity.setAI(false);
    }

    @Override
    public void autoRetarget() {

    }

    @Override
    public void retarget(LivingEntity entity) {

    }

    @Override
    public void tweakHealth() {

    }

    @Override
    public Map<LivingEntity, Aggro> getNonPlayerTargets() {
        return null;
    }

    @Override
    public boolean isTarget(LivingEntity target) {
        return false;
    }

    @Override
    public MobConfig getConfig() {
        return config;
    }

    @Override
    public void onDeath() {

    }

    @Override
    public EntityDamageEvent getLastDamageCause() {
        return null;
    }

    @Override
    public void setLastDamageCause(EntityDamageEvent event) {

    }
}
