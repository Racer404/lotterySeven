package fund.racer.lotterySeven;

import org.bukkit.NamespacedKey;
import org.bukkit.block.Sign;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class LotterySignManager {

    private static final String ENTRY_KEY = "entry_sign";

    private final NamespacedKey entryKey;

    public LotterySignManager(JavaPlugin plugin) {
        this.entryKey = new NamespacedKey(plugin, ENTRY_KEY);
    }

    public boolean isEntrySign(Sign sign) {
        PersistentDataContainer data =
                sign.getPersistentDataContainer();

        return data.has(
                entryKey,
                PersistentDataType.BYTE
        );
    }

    public void setEntrySign(Sign sign) {
        sign.getPersistentDataContainer().set(
                entryKey,
                PersistentDataType.BYTE,
                (byte) 1
        );

        sign.update(true, false);
    }

    public void removeEntrySign(Sign sign) {
        sign.getPersistentDataContainer().remove(entryKey);
        sign.update(true, false);
    }
}