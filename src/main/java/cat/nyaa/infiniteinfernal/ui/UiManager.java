package cat.nyaa.infiniteinfernal.ui;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.utils.ticker.TickEvent;
import cat.nyaa.infiniteinfernal.utils.ticker.TickTask;
import cat.nyaa.infiniteinfernal.utils.ticker.Ticker;
//import cat.nyaa.nyaacore.utils.ClassPathUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.function.Predicate;

public class UiManager {
    private static UiManager INSTANCE;

    RegenerationTask tickTask;

    private UiManager(){
        tickTask = new RegenerationTask(tickEvent -> false);
        Ticker.getInstance().register(tickTask);
//        Class<? extends BaseVar<?>>[] classes = ClassPathUtils.scanSubclasses(InfPlugin.plugin, "cat.nyaa.infiniteinfernal.ui.impl", BaseVar.class);
//        for (Class<? extends BaseVar<?>> aClass : classes) {
//            iVarMap.put(aClass.getSimpleName().substring(3), aClass);
//        }
    }

    public static UiManager getInstance() {
        if (INSTANCE == null){
            synchronized (UiManager.class){
                if (INSTANCE == null){
                    INSTANCE = new UiManager();
                }
            }
        }
        return INSTANCE;
    }

//    Map<Player, Map<String, IVar<?>>> variableMap = new LinkedHashMap<>();
//    Map<String, Class<? extends BaseVar<?>>> iVarMap = new LinkedHashMap<>();
//
//    public <T extends IVar<?>> T getValue(Player player, String variable, T defaultVal){
//        Map<String, IVar<?>> stringIVarMap = variableMap.computeIfAbsent(player, player1 -> new LinkedHashMap<>());
//        T iVar = (T) stringIVarMap.computeIfAbsent(variable, variable1 -> defaultVal);
//        return iVar;
//    }

    Map<UUID, BaseUi> uiMap = new LinkedHashMap<>();

    public BaseUi getUi(Player player) {
        return uiMap.computeIfAbsent(player.getUniqueId(), uuid -> new BaseUi(uuid));
    }

    public int getTick() {
        return tickTask.getTicked();
    }

    public class RegenerationTask extends TickTask {
        Queue<Player> playerQueue = new LinkedList<>();

        public RegenerationTask(Predicate<TickEvent> shouldRemove) {
            super(shouldRemove);
        }

        @Override
        public void run(int ticked) {
            if (!InfPlugin.plugin.config().enableActionbarInfo)return;
            while (!playerQueue.isEmpty()){
                Player poll = playerQueue.poll();
                BaseUi baseUi = uiMap.computeIfAbsent(poll.getUniqueId(), BaseUi::new);
                baseUi.regeneration(poll,ticked);
                baseUi.refreshUi(poll);
            }
            playerQueue.addAll(Bukkit.getOnlinePlayers());
        }
    }
}
