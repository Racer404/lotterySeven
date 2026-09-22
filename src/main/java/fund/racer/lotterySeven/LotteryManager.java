package fund.racer.lotterySeven;

import org.bukkit.entity.Player;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class LotteryManager {

    private static final String DEALER_KEY = "DEALER";

    private static final int BET_SIZE = 7;
    private static final int RED_NUMBER_COUNT = 6;
    private static final int RED_NUMBER_MAX = 33;
    private static final int BLUE_NUMBER_MAX = 16;

    private static final DayOfWeek[] DRAW_DAYS = {
            DayOfWeek.TUESDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SUNDAY
    };

    private static final DateTimeFormatter DRAW_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final LotteryDatabase database;
    private final Map<Long, Map<String, List<int[]>>> data;
    private final LotteryMessages messages;

    public LotteryManager(
            Main plugin,
            LotteryMessages messages
    ) {
        this.messages = messages;
        this.database = new LotteryDatabase(
                plugin.getDataFolder(),
                messages
        );
        this.data = database.getData();
    }

    public boolean[] findDuplicates(int[] draw, int[] bet) {
        boolean[] result = new boolean[bet.length];
        boolean[] redNumbers = new boolean[RED_NUMBER_MAX + 1];

        for (int i = 0; i < RED_NUMBER_COUNT; i++) {
            redNumbers[draw[i]] = true;
        }

        for (int i = 0; i < RED_NUMBER_COUNT; i++) {
            result[i] = redNumbers[bet[i]];
        }

        result[RED_NUMBER_COUNT] =
                bet[RED_NUMBER_COUNT] == draw[RED_NUMBER_COUNT];

        return result;
    }

    public int calculatePrize(boolean[] result) {
        int redMatch = 0;

        for (int i = 0; i < RED_NUMBER_COUNT; i++) {
            if (result[i]) {
                redMatch++;
            }
        }

        boolean blueMatch = result[RED_NUMBER_COUNT];

        if (redMatch == 6) {
            return -1;
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

        LocalDateTime next = Arrays.stream(DRAW_DAYS)
                .map(day -> now
                        .with(TemporalAdjusters.nextOrSame(day))
                        .withHour(20)
                        .withMinute(0)
                        .withSecond(0)
                        .withNano(0))
                .filter(time -> time.isAfter(now))
                .min(LocalDateTime::compareTo)
                .orElseThrow();

        return next
                .atZone(ZoneId.systemDefault())
                .toEpochSecond();
    }

    public String nextDrawTime_String() {
        LocalDateTime time = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(nextDrawTime_Key()),
                ZoneId.systemDefault()
        );

        return time.format(DRAW_TIME_FORMATTER);
    }

    /**
     * Preserves the original single-bet API.
     * A single call still results in exactly one database save.
     */
    public void playerSettleBet(Player player, int[] bet) {
        playerSettleBets(player, bet, 1);
    }

    /**
     * Records multiple identical tickets and saves the database once.
     * This replaces the old loop that performed one JSON write per ticket.
     */
    public void playerSettleBets(
            Player player,
            int[] bet,
            int count
    ) {
        if (count <= 0) {
            return;
        }

        validateBetSize(bet);

        String uuid = String.valueOf(player.getUniqueId());
        long timeKey = nextDrawTime_Key();

        Map<String, List<int[]>> players =
                data.computeIfAbsent(timeKey, key -> new java.util.HashMap<>());

        List<int[]> bets = players.computeIfAbsent(
                uuid,
                key -> new ArrayList<>()
        );

        for (int i = 0; i < count; i++) {
            bets.add(bet);
        }

        if (getDraw(timeKey).isEmpty()) {
            players.put(DEALER_KEY, new ArrayList<>(
                    List.of(random_number_array())
            ));
        }

        save();
    }

    public int[] random_number_array() {
        List<Integer> numbers = new ArrayList<>(RED_NUMBER_MAX);

        for (int i = 1; i <= RED_NUMBER_MAX; i++) {
            numbers.add(i);
        }

        Collections.shuffle(numbers);

        int[] randomBet = new int[BET_SIZE];

        for (int i = 0; i < RED_NUMBER_COUNT; i++) {
            randomBet[i] = numbers.get(i);
        }

        randomBet[RED_NUMBER_COUNT] =
                ThreadLocalRandom.current().nextInt(
                        1,
                        BLUE_NUMBER_MAX + 1
                );

        return randomBet;
    }

    public List<int[]> getPlayerBets(long timeKey, Player player) {
        return getPlayerBets(timeKey, String.valueOf(player.getUniqueId()));
    }

    private List<int[]> getPlayerBets(long timeKey, String uuid) {
        return data.getOrDefault(timeKey, Map.of())
                .getOrDefault(uuid, List.of());
    }

    public int[] get_last_bet(long timeKey, Player player) {
        List<int[]> bets = getPlayerBets(timeKey, player);

        if (bets.isEmpty()) {
            return null;
        }

        return bets.getLast();
    }

    public void clear_last_bet(long timeKey, Player player) {
        Map<String, List<int[]>> players = data.get(timeKey);

        if (players == null) {
            return;
        }

        String uuid = String.valueOf(player.getUniqueId());
        List<int[]> bets = players.get(uuid);

        if (bets == null || bets.isEmpty()) {
            return;
        }

        bets.removeLast();

        if (bets.isEmpty()) {
            players.remove(uuid);
        }

        if (players.isEmpty()) {
            data.remove(timeKey);
        }

        save();
    }

    public List<int[]> getDraw(long timeKey) {
        return data.getOrDefault(timeKey, Map.of())
                .getOrDefault(DEALER_KEY, List.of());
    }

    public long[] getAllKeys() {
        return data.keySet()
                .stream()
                .mapToLong(Long::longValue)
                .toArray();
    }

    public void record_bet(
            long timeKey,
            String playerKey,
            int[] bet
    ) {
        validateBetSize(bet);

        data.computeIfAbsent(
                        timeKey,
                        key -> new java.util.HashMap<>()
                )
                .computeIfAbsent(
                        playerKey,
                        key -> new ArrayList<>()
                )
                .add(bet);

        save();
    }

    public void save() {
        database.save();
    }

    private void validateBetSize(int[] bet) {
        if (bet.length != BET_SIZE) {
            throw new IllegalArgumentException(
                    messages.error("invalid-bet-size")
            );
        }
    }
}
