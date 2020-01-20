package cat.nyaa.infiniteinfernal.ui;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.ui.impl.VarMana;
import cat.nyaa.infiniteinfernal.ui.impl.VarRage;
import cat.nyaa.infiniteinfernal.utils.ticker.TickEvent;
import cat.nyaa.infiniteinfernal.utils.ticker.TickTask;
import cat.nyaa.infiniteinfernal.utils.ticker.Ticker;
//import cat.nyaa.nyaacore.utils.ClassPathUtils;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.function.Predicate;

public class InfVarManager {
    private static InfVarManager INSTANCE;

    private InfVarManager(){
        Ticker.getInstance().register(new RegenerationTask(tickEvent -> true));
//        Class<? extends BaseVar<?>>[] classes = ClassPathUtils.scanSubclasses(InfPlugin.plugin, "cat.nyaa.infiniteinfernal.ui.impl", BaseVar.class);
//        for (Class<? extends BaseVar<?>> aClass : classes) {
//            iVarMap.put(aClass.getSimpleName().substring(3), aClass);
//        }
    }

    public static InfVarManager getInstance() {
        if (INSTANCE == null){
            synchronized (InfVarManager.class){
                if (INSTANCE == null){
                    INSTANCE = new InfVarManager();
                }
            }
        }
        return INSTANCE;
    }

    Queue<Player> playerQueue = new LinkedList<>();
//    Map<Player, Map<String, IVar<?>>> variableMap = new LinkedHashMap<>();
//    Map<String, Class<? extends BaseVar<?>>> iVarMap = new LinkedHashMap<>();
//
//    public <T extends IVar<?>> T getValue(Player player, String variable, T defaultVal){
//        Map<String, IVar<?>> stringIVarMap = variableMap.computeIfAbsent(player, player1 -> new LinkedHashMap<>());
//        T iVar = (T) stringIVarMap.computeIfAbsent(variable, variable1 -> defaultVal);
//        return iVar;
//    }

    Map<Player, VarMana> manaMap = new LinkedHashMap<>();
    Map<Player, VarRage> rageMap = new LinkedHashMap<>();

    public VarMana getMana(Player player){
        return manaMap.get(player);
    }

    public VarRage getRage(Player player){
        return rageMap.get(player);
    }

    public class RegenerationTask extends TickTask {
        public RegenerationTask(Predicate<TickEvent> shouldRemove) {
            super(shouldRemove);
        }

        @Override
        public void run(int ticked) {
            if (playerQueue.isEmpty()) {
                fillPlayerQueue();
            }
            int online = InfPlugin.plugin.getServer().getOnlinePlayers().size();
            int playersPerTick = online;
            for (int i = 0; i < playersPerTick; i++) {
                if (playerQueue.isEmpty()) {
                    break;
                }
                Player poll = playerQueue.poll();
                refreshUi(poll);
            }
        }

        private void refreshUi(Player poll) {
            VarMana mana = getMana(poll);
            VarRage rage = getRage(poll);

        }

        private void regeneration(IVar<Double> ivar) {
        }

        private void fillPlayerQueue() {
            Collection<? extends Player> onlines = InfPlugin.plugin.getServer().getOnlinePlayers();
            playerQueue.addAll(onlines);
        }
    }
}
