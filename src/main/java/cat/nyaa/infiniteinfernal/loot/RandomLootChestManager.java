package cat.nyaa.infiniteinfernal.loot;

import cat.nyaa.infiniteinfernal.Config;
import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.configs.MobConfig;
import cat.nyaa.infiniteinfernal.configs.RandomLootChestConfig;
import cat.nyaa.infiniteinfernal.configs.RegionConfig;
import cat.nyaa.infiniteinfernal.mob.MobManager;
import cat.nyaa.infiniteinfernal.utils.Utils;
import cat.nyaa.infiniteinfernal.utils.WeightedPair;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.TileState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class RandomLootChestManager implements Listener {
    private static final int SINGLE_CHEST_SIZE = 27;
    private static final NamespacedKey DEATH_CHEST_KEY = new NamespacedKey("deathchest", "death_chest");

    private final InfPlugin plugin;
    private final NamespacedKey lastRefillKey;
    private final NamespacedKey nextRefillKey;
    private final Random random = new Random();
    private final Deque<ChunkKey> chunkQueue = new ArrayDeque<>();
    private final Set<ChunkKey> chunkSet = new HashSet<>();
    private BukkitRunnable scanTask;

    public RandomLootChestManager(InfPlugin plugin) {
        this.plugin = plugin;
        this.lastRefillKey = new NamespacedKey(plugin, "loot_refill_last");
        this.nextRefillKey = new NamespacedKey(plugin, "loot_refill_next");
    }

    public void start() {
        stop();
        rebuildQueue();
        RandomLootChestConfig config = plugin.config().randomLootChest;
        if (!config.enabled) {
            return;
        }
        int interval = Math.max(20, config.scanIntervalTicks);
        scanTask = new BukkitRunnable() {
            @Override
            public void run() {
                scanChunks();
            }
        };
        scanTask.runTaskTimer(plugin, interval, interval);
    }

    public void stop() {
        if (scanTask != null) {
            scanTask.cancel();
            scanTask = null;
        }
        chunkQueue.clear();
        chunkSet.clear();
    }

    @EventHandler(ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!plugin.config().randomLootChest.enabled) {
            return;
        }
        addChunk(event.getChunk());
    }

    @EventHandler(ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent event) {
        removeChunk(event.getChunk());
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        RandomLootChestConfig config = plugin.config().randomLootChest;
        if (!config.enabled) {
            return;
        }
        Inventory inventory = event.getInventory();
        ContainerInfo info = resolveContainer(inventory);
        if (info == null) {
            return;
        }
        if (!plugin.config().isEnabledInWorld(info.container.getWorld())) {
            return;
        }
        refillIfDue(info, config);
    }

    private void rebuildQueue() {
        chunkQueue.clear();
        chunkSet.clear();
        Bukkit.getWorlds().forEach(world -> {
            for (Chunk chunk : world.getLoadedChunks()) {
                addChunk(chunk);
            }
        });
    }

    private void addChunk(Chunk chunk) {
        ChunkKey key = new ChunkKey(chunk);
        if (chunkSet.add(key)) {
            chunkQueue.add(key);
        }
    }

    private void removeChunk(Chunk chunk) {
        chunkSet.remove(new ChunkKey(chunk));
    }

    private void scanChunks() {
        RandomLootChestConfig config = plugin.config().randomLootChest;
        if (!config.enabled) {
            return;
        }
        if (chunkQueue.isEmpty()) {
            rebuildQueue();
        }
        int chunksToScan = Math.max(1, config.scanChunksPerRun);
        for (int i = 0; i < chunksToScan; i++) {
            ChunkKey key = pollNextChunk();
            if (key == null) {
                break;
            }
            Chunk chunk = key.getLoadedChunk();
            if (chunk == null) {
                continue;
            }
            processChunk(chunk, config);
            if (chunkSet.contains(key)) {
                chunkQueue.add(key);
            }
        }
    }

    private ChunkKey pollNextChunk() {
        while (!chunkQueue.isEmpty()) {
            ChunkKey key = chunkQueue.poll();
            if (chunkSet.contains(key)) {
                return key;
            }
        }
        return null;
    }

    private void processChunk(Chunk chunk, RandomLootChestConfig config) {
        World world = chunk.getWorld();
        if (!plugin.config().isEnabledInWorld(world)) {
            return;
        }
        int processed = 0;
        int limit = Math.max(1, config.maxContainersPerChunk);
        for (var state : chunk.getTileEntities()) {
            if (!(state instanceof Container)) {
                continue;
            }
            ContainerInfo info = resolveContainer((Container) state);
            if (info == null) {
                continue;
            }
            if (!refillIfDue(info, config)) {
                continue;
            }
            if (++processed >= limit) {
                break;
            }
        }
    }

    private ContainerInfo resolveContainer(Inventory inventory) {
        if (inventory == null) {
            return null;
        }
        InventoryHolder holder = inventory.getHolder();
        if (holder instanceof DoubleChest) {
            InventoryHolder left = ((DoubleChest) holder).getLeftSide();
            if (left instanceof Container) {
                return new ContainerInfo((Container) left, inventory);
            }
            return null;
        }
        if (holder instanceof Container) {
            return new ContainerInfo((Container) holder, inventory);
        }
        return null;
    }

    private ContainerInfo resolveContainer(Container container) {
        Inventory inventory = container.getInventory();
        if (container instanceof Chest) {
            InventoryHolder holder = inventory.getHolder();
            if (holder instanceof DoubleChest) {
                InventoryHolder left = ((DoubleChest) holder).getLeftSide();
                if (left instanceof Chest) {
                    Chest leftChest = (Chest) left;
                    if (!leftChest.getLocation().equals(container.getLocation())) {
                        return null;
                    }
                    return new ContainerInfo(leftChest, inventory);
                }
            }
        }
        return new ContainerInfo(container, inventory);
    }

    private boolean refillIfDue(ContainerInfo info, RandomLootChestConfig config) {
        Container container = info.container;
        if (!(container instanceof TileState)) {
            return false;
        }
        TileState tileState = (TileState) container;
        PersistentDataContainer data = tileState.getPersistentDataContainer();
        if (data.has(DEATH_CHEST_KEY, PersistentDataType.BYTE)) {
            return false;
        }
        if (shouldSkipLootChest(container.getLocation())) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (!data.has(lastRefillKey, PersistentDataType.LONG)) {
            refillContainer(info, config, now);
            return true;
        }
        Long nextRefill = data.get(nextRefillKey, PersistentDataType.LONG);
        if (nextRefill != null && now < nextRefill) {
            return false;
        }
        refillContainer(info, config, now);
        return true;
    }

    private void refillContainer(ContainerInfo info, RandomLootChestConfig config, long now) {
        Inventory inventory = info.inventory;
        inventory.clear();
        int itemCount = getItemCount(inventory, config);
        if (itemCount > 0) {
            List<ItemStack> items = generateLoot(info.container.getLocation(), itemCount);
            placeItems(inventory, items);
        }
        long nextRefill = now + randomResetMillis(config);
        TileState updateState = (TileState) info.container.getBlock().getState();
        PersistentDataContainer updateData = updateState.getPersistentDataContainer();
        updateData.set(lastRefillKey, PersistentDataType.LONG, now);
        updateData.set(nextRefillKey, PersistentDataType.LONG, nextRefill);
        updateState.update(true, false);
    }

    private int getItemCount(Inventory inventory, RandomLootChestConfig config) {
        int minItems = Math.max(0, config.minItems);
        int maxItems = Math.max(minItems, config.maxItems);
        int base = randomRange(minItems, maxItems);
        int sizeFactor = Math.max(1, inventory.getSize() / SINGLE_CHEST_SIZE);
        int count = base * sizeFactor;
        return Math.min(count, inventory.getSize());
    }

    private void placeItems(Inventory inventory, List<ItemStack> items) {
        if (items.isEmpty()) {
            return;
        }
        List<Integer> slots = new ArrayList<>(inventory.getSize());
        for (int i = 0; i < inventory.getSize(); i++) {
            slots.add(i);
        }
        Collections.shuffle(slots, random);
        int limit = Math.min(items.size(), slots.size());
        for (int i = 0; i < limit; i++) {
            inventory.setItem(slots.get(i), items.get(i));
        }
    }

    private List<ItemStack> generateLoot(Location location, int itemCount) {
        List<WeightedPair<MobConfig, Integer>> spawnable = getSpawnCandidates(location);
        if (spawnable == null || spawnable.isEmpty()) {
            return Collections.emptyList();
        }
        Map<ILootItem, Integer> lootPool = buildLootPool(spawnable);
        if (lootPool.isEmpty()) {
            return Collections.emptyList();
        }
        List<ItemStack> items = new ArrayList<>(itemCount);
        for (int i = 0; i < itemCount; i++) {
            ILootItem loot = Utils.weightedRandomPick(lootPool);
            if (loot == null) {
                break;
            }
            ItemStack item = loot.getItemStack();
            if (item == null || item.getType().isAir()) {
                continue;
            }
            items.add(item);
        }
        return items;
    }

    private List<WeightedPair<MobConfig, Integer>> getSpawnCandidates(Location location) {
        Config config = plugin.config();
        List<RegionConfig> regions = config.getRegionsForLocation(location);
        MobManager mobManager = MobManager.instance();
        if (!regions.isEmpty() && regions.stream().anyMatch(region -> region.mobs.isEmpty())) {
            return mobManager.getBiomeSpawnableMob(location);
        }
        List<WeightedPair<MobConfig, Integer>> spawnable = mobManager.getSpawnableMob(location);
        if ((spawnable == null || spawnable.isEmpty()) && !regions.isEmpty()) {
            return mobManager.getNaturalSpawnableMob(location);
        }
        return spawnable;
    }

    private boolean shouldSkipLootChest(Location location) {
        if (location == null) {
            return false;
        }
        List<RegionConfig> regions = plugin.config().getRegionsForLocation(location);
        if (regions.isEmpty()) {
            return false;
        }
        return regions.stream().anyMatch(region -> region.skipLootChest);
    }

    private Map<ILootItem, Integer> buildLootPool(List<WeightedPair<MobConfig, Integer>> spawnable) {
        Map<ILootItem, Integer> lootPool = new LinkedHashMap<>();
        for (WeightedPair<MobConfig, Integer> candidate : spawnable) {
            if (candidate == null || candidate.getKey() == null || candidate.getValue() == null) {
                continue;
            }
            // Skip non-naturally spawning mobs (e.g., bosses) from loot chest drops
            if (!candidate.getKey().spawn.autoSpawn) {
                continue;
            }
            int level = candidate.getValue();
            int mobWeight = Math.max(1, candidate.getWeight());
            Map<ILootItem, Integer> mobLoot = getLootForMob(candidate.getKey(), level);
            if (mobLoot.isEmpty()) {
                continue;
            }
            mobLoot.forEach((loot, weight) -> addWeight(lootPool, loot, (long) weight * mobWeight));
        }
        return lootPool;
    }

    private Map<ILootItem, Integer> getLootForMob(MobConfig mobConfig, int level) {
        Map<ILootItem, Integer> lootMap = new LinkedHashMap<>();
        LootManager lootManager = LootManager.instance();
        if (mobConfig.loot.imLoot) {
            for (ILootItem lootItem : lootManager.getLevelDrops(level)) {
                int weight = lootItem.getWeight(level);
                if (weight > 0) {
                    addWeight(lootMap, lootItem, weight);
                }
            }
        }
        double specialChance = mobConfig.loot.special.chance / 100d;
        if (specialChance > 0 && !mobConfig.loot.special.list.isEmpty()) {
            mobConfig.loot.special.list.forEach(entry -> {
                String[] split = entry.split(":", 2);
                if (split.length != 2) {
                    return;
                }
                ILootItem lootItem = lootManager.getLoot(split[0]);
                if (lootItem == null) {
                    return;
                }
                try {
                    int weight = Integer.parseInt(split[1]);
                    int adjusted = (int) Math.round(weight * specialChance);
                    if (adjusted > 0) {
                        addWeight(lootMap, lootItem, adjusted);
                    }
                } catch (NumberFormatException ignored) {
                }
            });
        }
        return lootMap;
    }

    private void addWeight(Map<ILootItem, Integer> lootMap, ILootItem lootItem, long weight) {
        if (weight <= 0) {
            return;
        }
        long current = lootMap.getOrDefault(lootItem, 0);
        long next = Math.min(Integer.MAX_VALUE, current + weight);
        lootMap.put(lootItem, (int) next);
    }

    private int randomRange(int min, int max) {
        if (max <= min) {
            return min;
        }
        return min + random.nextInt(max - min + 1);
    }

    private long randomResetMillis(RandomLootChestConfig config) {
        int minHours = Math.max(0, config.minResetHours);
        int maxHours = Math.max(minHours, config.maxResetHours);
        long minMillis = minHours * 3600_000L;
        long maxMillis = maxHours * 3600_000L;
        if (maxMillis <= minMillis) {
            return minMillis;
        }
        long range = maxMillis - minMillis;
        long offset = (long) Math.floor(random.nextDouble() * range);
        return minMillis + offset;
    }

    private static class ContainerInfo {
        private final Container container;
        private final Inventory inventory;

        private ContainerInfo(Container container, Inventory inventory) {
            this.container = container;
            this.inventory = inventory;
        }
    }

    private static class ChunkKey {
        private final UUID worldId;
        private final int x;
        private final int z;

        private ChunkKey(Chunk chunk) {
            this.worldId = chunk.getWorld().getUID();
            this.x = chunk.getX();
            this.z = chunk.getZ();
        }

        private Chunk getLoadedChunk() {
            World world = Bukkit.getWorld(worldId);
            if (world == null) {
                return null;
            }
            if (!world.isChunkLoaded(x, z)) {
                return null;
            }
            return world.getChunkAt(x, z);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ChunkKey chunkKey = (ChunkKey) o;
            return x == chunkKey.x && z == chunkKey.z && worldId.equals(chunkKey.worldId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(worldId, x, z);
        }
    }
}
