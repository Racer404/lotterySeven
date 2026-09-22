package fund.racer.lotterySeven;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class Main extends JavaPlugin implements Listener {

    private LotteryManager manager;
    private LotteryGUI lotteryGUI;
    private LotteryMessages messages;
    private LotterySignManager signManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        messages = new LotteryMessages(this);
        signManager = new LotterySignManager(this);

        manager = new LotteryManager(
                this,
                messages
        );

        lotteryGUI = new LotteryGUI(
                manager,
                messages
        );

        Bukkit.getPluginManager().registerEvents(
                this,
                this
        );

        Bukkit.getPluginManager().registerEvents(
                new LotterySignListener(
                        signManager,
                        lotteryGUI
                ),
                this
        );

        Objects.requireNonNull(getCommand("lottery"))
                .setExecutor(
                        new LotteryCommand(
                                lotteryGUI,
                                signManager,
                                messages
                        )
                );

        getLogger().info("LotterySeven enabled!");
    }

    @Override
    public void onDisable() {
        if (manager != null) {
            manager.save();
        }
    }

}