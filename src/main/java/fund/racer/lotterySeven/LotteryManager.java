package fund.racer.lotterySeven;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.entity.Player;

import java.io.File;
import java.nio.file.Files;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

public class LotteryManager {

    private final File file;
    private final Gson gson = new Gson();

    private Map<Long, Map<String, List<int[]>>> data = new HashMap<>();

    public LotteryManager() {
        file = new File("settledBets.json");
        reloadFile();
    }

    public boolean[] findDuplicates(int[] draw, int[] bet) {
        boolean[] result = new boolean[bet.length];

        for (int i = 0; i < bet.length; i++) {

            // Last number only compares with the last number
            if (i == bet.length - 1) {
                result[i] = bet[i] == draw[draw.length - 1];
                continue;
            }

            // Other numbers compare with the regular draw numbers
            for (int j = 0; j < draw.length - 1; j++) {
                if (bet[i] == draw[j]) {
                    result[i] = true;
                    break;
                }
            }
        }

        return result;
    }

    public int calculatePrize(boolean[] result) {
        int redMatch = 0;

        for (int i = 0; i < 6; i++) {
            if (result[i]) {
                redMatch++;
            }
        }

        boolean blueMatch = result[6];

        if (redMatch == 6 && blueMatch) {
            return -1; // floating prize
        }

        if (redMatch == 6) {
            return -1; // floating prize
        }

        if (redMatch == 5 && blueMatch) {
            return 2304;
        }

        if ((redMatch == 5 && !blueMatch)
                || (redMatch == 4 && blueMatch)) {
            return 200;
        }

        if ((redMatch == 4 && !blueMatch)
                || (redMatch == 3 && blueMatch)) {
            return 10;
        }

        if ((redMatch == 3 && !blueMatch)
                || (blueMatch && redMatch <= 2)) {
            return 5;
        }

        return 0;
    }

    public long nextDrawTime_Key() {
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime next = Arrays.stream(new DayOfWeek[]{
                        DayOfWeek.TUESDAY,
                        DayOfWeek.FRIDAY,
                        DayOfWeek.SUNDAY
                })
                .map(day -> now
                        .with(TemporalAdjusters.nextOrSame(day))
                        .withHour(20)
                        .withMinute(0)
                        .withSecond(0)
                        .withNano(0))
                .filter(time -> time.isAfter(now))
                .min(LocalDateTime::compareTo)
                .orElseThrow();

        return next.atZone(ZoneId.systemDefault()).toEpochSecond();
    }

    public String nextDrawTime_String() {
        long timestamp = nextDrawTime_Key();

        LocalDateTime time = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(timestamp),
                ZoneId.systemDefault()
        );

        return time.format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        );
    }

    public void playerSettleBet(Player player, int[] bet) {
        String UUID = String.valueOf(player.getUniqueId());
        long time_key = nextDrawTime_Key();

        record_bet(time_key, UUID, bet);

        if(getDraw(time_key).isEmpty()){
            int[] draw_new = random_number_array();
            record_bet(time_key, "DEALER", draw_new);
        }
    }

    public int[] random_number_array() {
        List<Integer> numbers = new ArrayList<>();
        for (int i = 1; i <= 33; i++) {
            numbers.add(i);
        }

        Collections.shuffle(numbers);

        int[] randomBet = new int[7];

        for (int i = 0; i < 6; i++) {
            randomBet[i] = numbers.get(i);
        }

        randomBet[6] = (int) Math.ceil(Math.random() * 16);

        return randomBet;
    }

    public List<int[]> getPlayerBets(long timeKey, Player player) {
        String UUID = String.valueOf(player.getUniqueId());
        return data.getOrDefault(timeKey, Map.of())
                .getOrDefault(UUID, List.of());
    }

    public int[] get_last_bet(long timeKey, Player player) {
        Map<String, List<int[]>> players = data.get(timeKey);
        String UUID = String.valueOf(player.getUniqueId());

        if (players == null) {
            return null;
        }

        List<int[]> bets = players.get(UUID);

        if (bets == null || bets.isEmpty()) {
            return null;
        }

        return bets.getLast();
    }

    public void clear_last_bet(long timeKey, Player player) {
        Map<String, List<int[]>> players = data.get(timeKey);
        String UUID = String.valueOf(player.getUniqueId());

        if (players == null) {
            return;
        }

        List<int[]> bets = players.get(UUID);

        if (bets == null || bets.isEmpty()) {
            return;
        }

        bets.removeLast();

        // Remove empty player entry
        if (bets.isEmpty()) {
            players.remove(UUID);
        }

        // Remove empty time key
        if (players.isEmpty()) {
            data.remove(timeKey);
        }

        writeFile();
    }

    public List<int[]> getDraw(long timeKey) {
        return data.getOrDefault(timeKey, Map.of())
                .getOrDefault("DEALER", List.of());
    }

    public long[] getAllKeys() {
        return data.keySet()
                .stream()
                .mapToLong(Long::longValue)
                .toArray();
    }

    public void record_bet(long timeKey, String playerKey, int[] bet) {
        if (bet.length != 7) {
            throw new IllegalArgumentException(
                    "A bet must contain exactly 7 numbers"
            );
        }

        data.computeIfAbsent(timeKey, k -> new HashMap<>())
                .computeIfAbsent(playerKey, k -> new ArrayList<>())
                .add(bet);

        writeFile();
    }

    private void reloadFile() {
        try {
            if (!file.exists()) {
                return;
            }

            data = gson.fromJson(
                    Files.readString(file.toPath()),
                    new TypeToken<Map<Long, Map<String, List<int[]>>>>() {}.getType()
            );

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to load lottery database",
                    e
            );
        }
    }

    private void writeFile() {
        try {
            Files.writeString(
                    file.toPath(),
                    gson.toJson(data)
            );

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to write lottery database",
                    e
            );
        }
    }
}