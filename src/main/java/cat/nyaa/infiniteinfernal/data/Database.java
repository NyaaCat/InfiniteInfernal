package cat.nyaa.infiniteinfernal.data;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.nyaacore.configuration.FileConfigure;
import cat.nyaa.nyaacore.orm.DatabaseUtils;
import cat.nyaa.nyaacore.orm.NonUniqueResultException;
import cat.nyaa.nyaacore.orm.WhereClause;
import cat.nyaa.nyaacore.orm.backends.BackendConfig;
import cat.nyaa.nyaacore.orm.backends.IConnectedDatabase;
import cat.nyaa.nyaacore.orm.backends.ITypedTable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.UUID;

public class Database extends FileConfigure {
    private static Database INSTANCE;

    @Serializable
    public BackendConfig backendConfig;
    @Serializable
    public int databaseVersion = 0;

    private IConnectedDatabase connect;

    private Database(){
    }

    public void init() throws SQLException, ClassNotFoundException {
        InfPlugin plugin = InfPlugin.plugin;
        connect = DatabaseUtils.connect(plugin, backendConfig);
        int newDatabaseVersion = DatabaseUpdater.updateDatabase(Database.this, databaseVersion);
        if (newDatabaseVersion != databaseVersion) {
            databaseVersion = newDatabaseVersion;
            save();
        }
    }

    public static Database getInstance(){
        if(INSTANCE == null){
            synchronized (Database.class){
                if (INSTANCE == null){
                    INSTANCE = new Database();
                }
            }
        }
        return INSTANCE;
    }

    @Override
    public void deserialize(ConfigurationSection config) {
        backendConfig = BackendConfig.sqliteBackend(InfPlugin.plugin.getName() + ".db");
        if (config.isConfigurationSection("database")){
            backendConfig.deserialize(config);
        }
    }

    @Override
    protected String getFileName() {
        return "database.yml";
    }

    @Override
    protected JavaPlugin getPlugin() {
        return InfPlugin.plugin;
    }

    @Override
    public void save() {
        super.save();
    }

    public PlayerData getPlayerData(Player player){
        UUID uniqueId = player.getUniqueId();
        return getPlayerData(uniqueId);
    }

    public void setPlayerData(PlayerData playerData) {
        insertOrUpdate(playerData.uuid.toString(), "uuid", PlayerData.class, playerData);
    }

    private <T> void insertOrUpdate(Object uid, String key, Class<T> tableClass, T newRecord) {
        WhereClause where = WhereClause.EQ(key, uid);
        ITypedTable<T> table = connect.getUnverifiedTable(tableClass);
        T oldRecord = table.selectUniqueUnchecked(where);
        if (oldRecord == null) {
            table.insert(newRecord);
        } else {
            table.update(newRecord, where);
        }
    }

    public PlayerData getPlayerData(UUID uuid) {
        PlayerData playerData = connect.getUnverifiedTable(PlayerData.class).selectUniqueUnchecked(WhereClause.EQ("uuid", uuid.toString()));
        if (playerData == null){
            playerData = new PlayerData();
            playerData.uuid = uuid.toString();
        }
        return playerData;
    }
}
