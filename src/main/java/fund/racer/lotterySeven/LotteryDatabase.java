package fund.racer.lotterySeven;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class LotteryDatabase {

    private static final TypeToken<Map<Long, Map<String, List<int[]>>>> DATA_TYPE =
            new TypeToken<>() {
            };

    private final File file;
    private final Gson gson;
    private final LotteryMessages messages;

    private Map<Long, Map<String, List<int[]>>> data = new HashMap<>();

    public LotteryDatabase(
            File dataFolder,
            LotteryMessages messages
    ) {
        this.file = new File(dataFolder, "settledBets.json");
        this.gson = new Gson();
        this.messages = messages;

        load();
    }

    public Map<Long, Map<String, List<int[]>>> getData() {
        return data;
    }

    public void save() {
        try {
            File parent = file.getParentFile();

            if (parent != null) {
                Files.createDirectories(parent.toPath());
            }

            Files.writeString(
                    file.toPath(),
                    gson.toJson(data)
            );

        } catch (IOException e) {
            throw new RuntimeException(
                    messages.error("save-database"),
                    e
            );
        }
    }

    private void load() {
        if (!file.exists()) {
            return;
        }

        try {
            String json = Files.readString(file.toPath());

            Map<Long, Map<String, List<int[]>>> loaded =
                    gson.fromJson(json, DATA_TYPE.getType());

            data = loaded != null
                    ? loaded
                    : new HashMap<>();

        } catch (Exception e) {
            throw new RuntimeException(
                    messages.error("load-database"),
                    e
            );
        }
    }
}