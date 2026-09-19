package fund.racer.lotterySeven;

import java.util.HashMap;
import java.util.Map;

public class LotteryManager {

    public long next_draw_time = 1;

    private final LotteryDatabase database;
    private final Map<String, int[]> localMap;

    public LotteryManager() {
        this.localMap = new HashMap<>();
        this.database = new LotteryDatabase("settledBets.json");
    }

    public void playerSetNumber(String UUID, int numberPos, int selectedNumber){
        if(!localMap.containsKey(UUID)){
            localMap.put(UUID, new int[7]);
        }

        int[] update_numbers = localMap.get(UUID);
        update_numbers[numberPos] = selectedNumber;
        localMap.put(UUID, update_numbers);
    }

    public void playerRandomNumber(String UUID, int numberPos){
        int RANDOM_MAX = 33;
        if(!localMap.containsKey(UUID)){
            localMap.put(UUID, new int[7]);
        }

        int[] update_numbers = localMap.get(UUID);
        if(numberPos == 6){
            RANDOM_MAX = 16;
        }
        int random_number = (int) Math.ceil(Math.random() * RANDOM_MAX);
        update_numbers[numberPos] = random_number;
        localMap.put(UUID, update_numbers);
    }

    public void playerRandomAll(String UUID){
        if(!localMap.containsKey(UUID)){
            localMap.put(UUID, new int[7]);
        }
        int[] update_numbers = localMap.get(UUID);
        update_numbers[0] = (int) Math.ceil(Math.random() * 33);
        update_numbers[1] = (int) Math.ceil(Math.random() * 33);
        update_numbers[2] = (int) Math.ceil(Math.random() * 33);
        update_numbers[3] = (int) Math.ceil(Math.random() * 33);
        update_numbers[4] = (int) Math.ceil(Math.random() * 33);
        update_numbers[5] = (int) Math.ceil(Math.random() * 33);
        update_numbers[6] = (int) Math.ceil(Math.random() * 16);
        localMap.put(UUID, update_numbers);
    }

    public void playerClearLocal(String UUID){
        localMap.remove(UUID);
    }



    public void newRound(){
        localMap.clear();
        playerRandomAll("DEALER");
        playerSettleBet("DEALER");
    }

    public void playerSettleBet(String UUID){
        if (!localMap.containsKey(UUID)){
            return;
        }
        int[] finalBet = localMap.get(UUID);

        boolean allNonZero = true;
        for (int entry : finalBet) {
            if (entry == 0) {
                allNonZero = false;
                break;
            }
        }

        if(allNonZero){
            database.putPlayerBet(next_draw_time, UUID, finalBet);
            database.writeFile();
            playerClearLocal(UUID);
        }

    }


}
