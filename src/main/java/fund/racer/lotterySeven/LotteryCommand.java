package fund.racer.lotterySeven;

import org.bukkit.FluidCollisionMode;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class LotteryCommand implements CommandExecutor {

    private static final String ADMIN_PERMISSION = "lottery.admin";
    private static final int SIGN_RANGE = 5;

    private final LotteryGUI gui;
    private final LotterySignManager signManager;
    private final LotteryMessages messages;

    public LotteryCommand(
            LotteryGUI gui,
            LotterySignManager signManager,
            LotteryMessages messages
    ) {
        this.gui = gui;
        this.signManager = signManager;
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
            sender.sendMessage(
                    messages.get("players-only")
            );
            return true;
        }

        if (args.length == 0) {
            gui.displayMainPage(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "setentry" -> setEntrySign(player);
            case "removeentry" -> removeEntrySign(player);
            default -> player.sendMessage(
                    messages.get("command-usage")
            );
        }

        return true;
    }

    private void setEntrySign(Player player) {
        if (!hasAdminPermission(player)) {
            return;
        }

        Sign sign = getTargetSign(player);

        if (sign == null) {
            player.sendMessage(
                    messages.get("entry-not-looking-at-sign")
            );
            return;
        }

        if (signManager.isEntrySign(sign)) {
            player.sendMessage(
                    messages.get("entry-already-set")
            );
            return;
        }

        signManager.setEntrySign(sign);

        player.sendMessage(
                messages.get("entry-set")
        );
    }

    private void removeEntrySign(Player player) {
        if (!hasAdminPermission(player)) {
            return;
        }

        Sign sign = getTargetSign(player);

        if (sign == null) {
            player.sendMessage(
                    messages.get("entry-not-looking-at-sign")
            );
            return;
        }

        if (!signManager.isEntrySign(sign)) {
            player.sendMessage(
                    messages.get("entry-not-set")
            );
            return;
        }

        signManager.removeEntrySign(sign);

        player.sendMessage(
                messages.get("entry-removed")
        );
    }

    private boolean hasAdminPermission(Player player) {
        if (player.hasPermission(ADMIN_PERMISSION)) {
            return true;
        }

        player.sendMessage(
                messages.get("no-permission")
        );

        return false;
    }

    private Sign getTargetSign(Player player) {
        var block = player.getTargetBlockExact(
                SIGN_RANGE,
                FluidCollisionMode.NEVER
        );

        if (block == null) {
            return null;
        }

        if (!(block.getState() instanceof Sign sign)) {
            return null;
        }

        return sign;
    }
}