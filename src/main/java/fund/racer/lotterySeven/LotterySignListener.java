package fund.racer.lotterySeven;

import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public final class LotterySignListener implements Listener {

    private final LotterySignManager signManager;
    private final LotteryGUI gui;

    public LotterySignListener(
            LotterySignManager signManager,
            LotteryGUI gui
    ) {
        this.signManager = signManager;
        this.gui = gui;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        // PlayerInteractEvent fires once for each hand.
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (!(event.getClickedBlock().getState() instanceof Sign sign)) {
            return;
        }

        if (!signManager.isEntrySign(sign)) {
            return;
        }

        event.setCancelled(true);

        gui.displayMainPage(event.getPlayer());
    }
}