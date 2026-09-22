package fund.racer.lotterySeven;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.inventory.VirtualInventory;
import xyz.xenondevs.invui.inventory.event.ItemPreUpdateEvent;
import xyz.xenondevs.invui.inventory.event.PlayerUpdateReason;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemBuilder;
import xyz.xenondevs.invui.util.InventoryUtils;
import xyz.xenondevs.invui.window.AnvilWindow;
import xyz.xenondevs.invui.window.MerchantWindow;
import xyz.xenondevs.invui.window.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public final class LotteryGUI {

    private static final int BET_SIZE = 7;
    private static final int RED_NUMBER_COUNT = 6;
    private static final int RED_NUMBER_MAX = 33;
    private static final int BLUE_NUMBER_MAX = 16;

    private final LotteryManager manager;
    private final LotteryMessages messages;

    public LotteryGUI(
            LotteryManager manager,
            LotteryMessages messages
    ) {
        this.manager = manager;
        this.messages = messages;
    }

    public void displayMainPage(Player player) {
        Item pastBet = Item.builder()
                .setItemProvider(
                        new ItemBuilder(Material.FLOW_BANNER_PATTERN)
                                .setName(messages.get("main-history"))
                )
                .addClickHandler(click -> displayBetHistoryPage(player))
                .build();

        Item newBet = Item.builder()
                .setItemProvider(
                        new ItemBuilder(Material.MAP)
                                .setName(messages.get("main-new-bet"))
                )
                .addClickHandler(click -> displayBetMakingPage(player))
                .build();

        Gui gui = Gui.builder()
                .setStructure(
                        "# # # # # # # # #",
                        "# # a # # # b # #",
                        "# # # # # # # # #"
                )
                .addIngredient('a', pastBet)
                .addIngredient('b', newBet)
                .build();

        Window window = Window.builder()
                .setTitle(messages.get("main-title"))
                .setUpperGui(gui)
                .setViewer(player)
                .build();

        window.open();
    }

    public void displayBetMakingPage(Player player) {
        Item empty = createEmptyItem();
        Item invalidBet = createInvalidBetItem();

        Gui gui = Gui.builder()
                .setStructure("i i #")
                .build();

        VirtualInventory betPlace = new VirtualInventory(2);

        betPlace.addPreUpdateHandler(event -> {
            if (event.isAdd()) {
                handleBetAdded(
                        player,
                        gui,
                        betPlace,
                        invalidBet,
                        event
                );
                return;
            }

            if (event.isRemove()) {
                gui.setItem('#', empty);
            }
        });

        gui.setInventory('i', betPlace);

        AnvilWindow window = AnvilWindow.builder()
                .setUpperGui(gui)
                .setTitle(messages.get("bet-making-title"))
                .setViewer(player)
                .addCloseHandler(close -> returnBetItems(player, betPlace))
                .build();

        window.open();
    }

    private void handleBetAdded(
            Player player,
            Gui gui,
            VirtualInventory betPlace,
            Item invalidBet,
            ItemPreUpdateEvent event
    ) {
        int[] placedNumbers = parseBetPlace(event);

        if (placedNumbers == null) {
            gui.setItem('#', invalidBet);
            return;
        }

        gui.setItem(
                '#',
                createSettleBetItem(player, betPlace, placedNumbers)
        );
    }

    private Item createEmptyItem() {
        return Item.builder()
                .setItemProvider(new ItemBuilder(Material.AIR))
                .build();
    }

    private Item createInvalidBetItem() {
        return Item.builder()
                .setItemProvider(
                        new ItemBuilder(Material.BARRIER)
                                .setName(messages.get("bet-invalid"))
                )
                .build();
    }

    private Item createSettleBetItem(
            Player player,
            VirtualInventory betPlace,
            int[] placedNumbers
    ) {
        String drawTime = manager.nextDrawTime_String();

        return Item.builder()
                .setItemProvider(
                        new ItemBuilder(Material.FILLED_MAP)
                                .setName(
                                        messages.get(
                                                "bet-draw-time",
                                                "time",
                                                drawTime
                                        )
                                )
                )
                .addClickHandler(
                        click -> settleBet(
                                player,
                                betPlace,
                                placedNumbers
                        )
                )
                .build();
    }

    private void settleBet(
            Player player,
            VirtualInventory betPlace,
            int[] placedNumbers
    ) {
        player.playSound(
                player,
                Sound.ENTITY_SHEEP_SHEAR,
                1,
                1
        );

        ItemStack paper = betPlace.getItem(0);
        ItemStack diamonds = betPlace.getItem(1);

        if (paper == null || diamonds == null) {
            return;
        }

        int betCount = Math.min(
                paper.getAmount(),
                diamonds.getAmount() / 2
        );

        if (betCount <= 0) {
            return;
        }

        // All tickets are added and the database is written once.
        manager.playerSettleBets(
                player,
                placedNumbers,
                betCount
        );

        paper.setAmount(paper.getAmount() - betCount);
        diamonds.setAmount(diamonds.getAmount() - betCount * 2);

        betPlace.setItem(
                PlayerUpdateReason.SUPPRESSED,
                0,
                paper.getAmount() > 0
                        ? paper
                        : new ItemStack(Material.AIR)
        );

        betPlace.setItem(
                PlayerUpdateReason.SUPPRESSED,
                1,
                diamonds.getAmount() > 0
                        ? diamonds
                        : new ItemStack(Material.AIR)
        );

        player.closeInventory();
    }

    private void returnBetItems(
            Player player,
            VirtualInventory betPlace
    ) {
        for (int slot = 0; slot < 2; slot++) {
            ItemStack item = betPlace.getItem(slot);

            if (item != null && !item.getType().isAir()) {
                InventoryUtils.addToInventoryOrDrop(player, item);
            }
        }
    }

    private int[] parseBetPlace(ItemPreUpdateEvent event) {
        ItemStack paper = getResultingItem(event, 0);
        ItemStack diamonds = getResultingItem(event, 1);

        if (!isValidPaper(paper) || !isValidPayment(diamonds)) {
            return null;
        }

        return parseBetNumbers(paper);
    }

    private ItemStack getResultingItem(
            ItemPreUpdateEvent event,
            int slot
    ) {
        if (event.getSlot() == slot) {
            return event.getNewItem();
        }

        return event.getInventory().getItem(slot);
    }

    private boolean isValidPaper(ItemStack paper) {
        if (paper == null || paper.getType() != Material.PAPER) {
            return false;
        }

        ItemMeta meta = paper.getItemMeta();
        return meta != null && meta.hasDisplayName();
    }

    private boolean isValidPayment(ItemStack diamonds) {
        return diamonds != null
                && diamonds.getType() == Material.DIAMOND
                && diamonds.getAmount() >= 2;
    }

    private int[] parseBetNumbers(ItemStack paper) {
        ItemMeta meta = paper.getItemMeta();

        String customText = PlainTextComponentSerializer.plainText()
                .serialize(meta.displayName())
                .trim();

        String[] parts = customText.split("\\s+");

        if (parts.length != BET_SIZE) {
            return null;
        }

        int[] numbers = new int[BET_SIZE];

        try {
            for (int i = 0; i < BET_SIZE; i++) {
                numbers[i] = Integer.parseInt(parts[i]);
            }
        } catch (NumberFormatException e) {
            return null;
        }

        if (!isValidRedNumbers(numbers)) {
            return null;
        }

        return isValidBlueNumber(numbers[RED_NUMBER_COUNT])
                ? numbers
                : null;
    }

    private boolean isValidRedNumbers(int[] numbers) {
        for (int i = 0; i < RED_NUMBER_COUNT; i++) {
            if (numbers[i] < 1 || numbers[i] > RED_NUMBER_MAX) {
                return false;
            }

            for (int j = 0; j < i; j++) {
                if (numbers[i] == numbers[j]) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean isValidBlueNumber(int number) {
        return number >= 1 && number <= BLUE_NUMBER_MAX;
    }

    public void build_your_bet(
            long timeKey,
            Player player,
            Gui gui
    ) {
        int[] bet = manager.get_last_bet(timeKey, player);
        List<int[]> draw = manager.getDraw(timeKey);

        if (bet == null || draw.isEmpty()) {
            gui.setItem(1, null);
            return;
        }

        int[] drawNumbers = draw.getFirst();
        boolean[] duplicates =
                manager.findDuplicates(drawNumbers, bet);
        int prize = manager.calculatePrize(duplicates);

        givePrize(player, prize);
        manager.clear_last_bet(timeKey, player);

        int newSize = manager
                .getPlayerBets(timeKey, player)
                .size();

        if (newSize <= 0) {
            gui.setItem(1, null);
            return;
        }

        int[] nextBet = manager.get_last_bet(timeKey, player);
        boolean[] nextDuplicates =
                manager.findDuplicates(drawNumbers, nextBet);

        gui.setItem(
                1,
                createYourBetItem(
                        timeKey,
                        player,
                        gui,
                        nextBet,
                        newSize,
                        nextDuplicates
                )
        );
    }

    private void givePrize(Player player, int prize) {
        if (prize > 0) {
            InventoryUtils.addToInventoryOrDrop(
                    player,
                    new ItemStack(Material.DIAMOND, prize)
            );

            player.playSound(
                    player,
                    Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST,
                    1,
                    1
            );
            return;
        }

        if (prize == 0) {
            InventoryUtils.addToInventoryOrDrop(
                    player,
                    new ItemStack(Material.SPIDER_EYE, 1)
            );

            player.playSound(
                    player,
                    Sound.ENTITY_VILLAGER_NO,
                    1,
                    1
            );
            return;
        }

        player.playSound(
                player,
                Sound.ENTITY_ENDER_DRAGON_DEATH,
                1,
                1
        );

        player.sendMessage(messages.get("first-prize"));
    }

    private Item createYourBetItem(
            long timeKey,
            Player player,
            Gui gui,
            int[] bet,
            int amount,
            boolean[] duplicates
    ) {
        return Item.builder()
                .setItemProvider(
                        new ItemBuilder(Material.MAP)
                                .setAmount(amount)
                                .setName(
                                        Component.text(
                                                messages.get("your-bet")
                                        )
                                )
                                .setLore(
                                        createNumberLore(
                                                bet,
                                                duplicates,
                                                true
                                        )
                                )
                )
                .addClickHandler(
                        click -> build_your_bet(
                                timeKey,
                                player,
                                gui
                        )
                )
                .build();
    }

    public void displayBetHistoryPage(Player player) {
        Item betFinished = createHistoryStateItem(
                Material.LEVER,
                "history-finished"
        );

        Item betDrawn = createHistoryStateItem(
                Material.REDSTONE_TORCH,
                "history-drawn"
        );

        Item betFuture = createHistoryStateItem(
                Material.LEVER,
                "history-future"
        );

        Gui gui = Gui.builder()
                .setStructure(3, 1, "# # i")
                .build();

        VirtualInventory prizePlace = new VirtualInventory(1);
        gui.setInventory('i', prizePlace);

        String nextDrawTime = manager.nextDrawTime_String();

        MerchantWindow window = MerchantWindow.builder()
                .setTitle(
                        messages.get(
                                "history-title",
                                "time",
                                nextDrawTime
                        )
                )
                .setUpperGui(gui)
                .setViewer(player)
                .build();

        List<MerchantWindow.Trade> tradeList = new ArrayList<>();

        long[] allKeys = manager.getAllKeys();
        boolean[] betAvailable = new boolean[allKeys.length];

        for (int keyIndex = allKeys.length - 1;
             keyIndex >= 0;
             keyIndex--) {

            long timeKey = allKeys[keyIndex];
            List<int[]> bets = manager.getPlayerBets(timeKey, player);

            boolean isNextDraw = timeKey == manager.nextDrawTime_Key();
            boolean hasBets = !bets.isEmpty();

            betAvailable[keyIndex] = hasBets;

            Item ticket = createHistoryTicket(bets.size(), hasBets);

            MerchantWindow.Trade.Builder tradeBuilder =
                    MerchantWindow.Trade.builder()
                            .setFirstInput(ticket)
                            .setAvailable(!isNextDraw);

            if (isNextDraw) {
                tradeBuilder.setResult(betFuture);
            } else if (hasBets) {
                tradeBuilder.setResult(betDrawn);
            } else {
                tradeBuilder.setResult(betFinished);
            }

            tradeList.add(tradeBuilder.build());
        }

        window.setTrades(tradeList);
        window.setTradeSelectHandlers(
                List.of(
                        (something, tradeIndex) -> {
                            int realIndex =
                                    (allKeys.length - 1) - tradeIndex;

                            long timeKey = allKeys[realIndex];

                            if (timeKey == manager.nextDrawTime_Key()) {
                                return;
                            }

                            showSelectedHistory(
                                    timeKey,
                                    player,
                                    gui,
                                    betAvailable[realIndex]
                            );
                        }
                )
        );

        window.open();
    }

    private Item createHistoryStateItem(
            Material material,
            String messageKey
    ) {
        return Item.builder()
                .setItemProvider(
                        new ItemBuilder(material)
                                .setName(messages.get(messageKey))
                )
                .build();
    }

    private Item createHistoryTicket(
            int betCount,
            boolean hasBets
    ) {
        return Item.builder()
                .setItemProvider(
                        new ItemBuilder(Material.FILLED_MAP)
                                .setAmount(hasBets ? betCount : 1)
                                .setName(
                                        messages.get(
                                                "history-ticket-count",
                                                "count",
                                                betCount
                                        )
                                )
                )
                .build();
    }

    private void showSelectedHistory(
            long timeKey,
            Player player,
            Gui gui,
            boolean hasBets
    ) {
        List<int[]> draw = manager.getDraw(timeKey);

        if (draw.isEmpty()) {
            return;
        }

        int[] drawNumbers = draw.getFirst();
        gui.setItem(0, createDrawResultItem(drawNumbers));

        if (!hasBets) {
            return;
        }

        int[] bet = manager.get_last_bet(timeKey, player);

        if (bet == null) {
            gui.setItem(1, null);
            return;
        }

        boolean[] duplicates =
                manager.findDuplicates(drawNumbers, bet);

        int count = manager.getPlayerBets(timeKey, player).size();

        gui.setItem(
                1,
                createYourBetItem(
                        timeKey,
                        player,
                        gui,
                        bet,
                        count,
                        duplicates
                )
        );
    }

    private Item createDrawResultItem(int[] draw) {
        return Item.builder()
                .setItemProvider(
                        new ItemBuilder(Material.FILLED_MAP)
                                .setName(
                                        Component.text(
                                                messages.get("draw-result")
                                        )
                                )
                                .setLore(
                                        createNumberLore(
                                                draw,
                                                null,
                                                false
                                        )
                                )
                )
                .build();
    }

    private List<Component> createNumberLore(
            int[] numbers,
            boolean[] duplicates,
            boolean decorateDuplicates
    ) {
        return IntStream.range(0, numbers.length)
                .mapToObj(i -> {
                    Component number = Component.text(
                            String.valueOf(numbers[i])
                    ).color(
                            i < RED_NUMBER_COUNT
                                    ? NamedTextColor.RED
                                    : NamedTextColor.BLUE
                    );

                    if (!decorateDuplicates) {
                        return number.decorate(TextDecoration.BOLD);
                    }

                    return number.decorate(
                            duplicates[i]
                                    ? TextDecoration.STRIKETHROUGH
                                    : TextDecoration.BOLD
                    );
                })
                .toList();
    }
}
