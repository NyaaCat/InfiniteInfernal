package cat.nyaa.infiniteinfernal.ui;

import cat.nyaa.infiniteinfernal.data.Database;
import cat.nyaa.infiniteinfernal.event.ui.PlayerManaMaxEvent;
import cat.nyaa.infiniteinfernal.event.ui.PlayerRageMaxEvent;
import cat.nyaa.infiniteinfernal.utils.ticker.TickTask;
import cat.nyaa.infiniteinfernal.utils.ticker.Ticker;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Queue;

public class MaxValTicker {
    PlayerTicker task;
    private Queue<Player> playerQueue;

    public MaxValTicker(){

    }

    public void start(){
        if (task != null){
            stop();
        }
        task = new PlayerTicker();
        Ticker.getInstance().register(task);
    }

    public void stop(){
        task.stop();
    }

    class PlayerTicker extends TickTask {
        private boolean stopped = false;

        public PlayerTicker() {
            super();
            super.setPredicate(event -> stopped);
        }

        @Override
        public void run(int ticked) {
            if (playerQueue.isEmpty()){
                fillQueue();
            }
            int count = (int) Math.ceil(playerQueue.size() / 20d);
            for (int i = 0; i < count; i++) {
                if (playerQueue.isEmpty()) {
                    return;
                }
                Player poll = playerQueue.poll();
                BaseUi ui = UiManager.getInstance().getUi(poll);
                double rageBase = ui.getRage().baseMax;
                double manaBase = ui.getMana().baseMax;
                PlayerRageMaxEvent playerRageMaxEvent = new PlayerRageMaxEvent(poll, ui.getRage(), rageBase);
                PlayerManaMaxEvent playerManaMaxEvent = new PlayerManaMaxEvent(poll, ui.getMana(), manaBase);
                Bukkit.getPluginManager().callEvent(playerRageMaxEvent);
                Bukkit.getPluginManager().callEvent(playerManaMaxEvent);

                double rageB = playerRageMaxEvent.getBonus();
                double manaB = playerManaMaxEvent.getBonus();
                ui.getRage().setMaxValue(rageBase + rageB);
                ui.getMana().setMaxValue(manaBase + manaB);
            }
        }

        private void fillQueue() {
            playerQueue.addAll(Bukkit.getOnlinePlayers());
        }

        public void stop() {
            stopped = true;
        }
    }
}
