package fund.racer.lotterySeven;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class Main extends JavaPlugin implements Listener {

    public LotteryGUI lotteryGUI;

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);

        getLogger().info("LotterySeven enabled!");

        lotteryGUI = new LotteryGUI();

        Objects.requireNonNull(this.getCommand("lottery")).setExecutor(
                new LotteryCommand(lotteryGUI)
        );
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {

        event.getPlayer().sendMessage(
                Component.text(
                        "Hello, " + event.getPlayer().getName() + "!"
                )
        );
    }
}