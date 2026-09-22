package fund.racer.lotterySeven;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class LotteryCommand implements CommandExecutor {

    private final LotteryGUI gui;
    private final LotteryMessages messages;

    public LotteryCommand(
            LotteryGUI gui,
            LotteryMessages messages
    ) {
        this.gui = gui;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.get("players-only"));
            return true;
        }

        gui.displayMainPage(player);
        return true;
    }
}
