package fund.racer.lotterySeven;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class Main extends JavaPlugin implements Listener {

    private LotteryManager manager;
    private LotteryGUI lotteryGUI;
    private LotteryMessages messages;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        messages = new LotteryMessages(this);
        manager = new LotteryManager(this, messages);
        lotteryGUI = new LotteryGUI(manager, messages);

        Bukkit.getPluginManager().registerEvents(this, this);

        Objects.requireNonNull(getCommand("lottery"))
                .setExecutor(new LotteryCommand(lotteryGUI, messages));

        getLogger().info("LotterySeven enabled!");
    }

    @Override
    public void onDisable() {
        if (manager != null) {
            manager.save();
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String message = messages.get(
                "join",
                "player",
                event.getPlayer().getName()
        );

        event.getPlayer().sendMessage(Component.text(message));
    }
}
