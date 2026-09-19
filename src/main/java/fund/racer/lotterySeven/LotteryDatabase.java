package fund.racer.lotterySeven;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LotteryDatabase {

    private final File file;
    private final Gson gson = new Gson();

    private Map<Long, Map<String, List<int[]>>> data = new HashMap<>();

    public LotteryDatabase(String jsonFile) {
        file = new File(jsonFile);
        reloadFile();
    }

    public List<int[]> getPlayerBets(long timeKey, String playerKey) {
        return data.getOrDefault(timeKey, Map.of())
                .getOrDefault(playerKey, List.of());
    }

    public void putPlayerBet(long timeKey, String playerKey, int[] bet) {
        if (bet.length != 7) {
            throw new IllegalArgumentException("A bet must contain exactly 7 numbers");
        }

        data.computeIfAbsent(timeKey, k -> new HashMap<>())
                .computeIfAbsent(playerKey, k -> new ArrayList<>())
                .add(bet);
    }

    public void reloadFile() {
        try {
            if (!file.exists()) {
                return;
            }

            data = gson.fromJson(
                    Files.readString(file.toPath()),
                    new TypeToken<Map<Long, Map<String, List<int[]>>>>() {}.getType()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to load lottery database", e);
        }
    }

    public void writeFile() {
        try {
            Files.writeString(
                    file.toPath(),
                    gson.toJson(data)
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to write lottery database", e);
        }
    }
}