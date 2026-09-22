package fund.racer.lotterySeven;

import org.bukkit.plugin.java.JavaPlugin;

public final class LotteryMessages {

    private final JavaPlugin plugin;

    public LotteryMessages(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public String get(String key) {
        return plugin.getConfig().getString(
                "messages." + key,
                key
        );
    }

    public String get(String key, String placeholder, Object value) {
        return get(key).replace(
                "{" + placeholder + "}",
                String.valueOf(value)
        );
    }

    public String error(String key) {
        return plugin.getConfig().getString(
                "errors." + key,
                key
        );
    }
}
