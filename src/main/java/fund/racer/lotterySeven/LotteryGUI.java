package fund.racer.lotterySeven;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
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

    /**
     * Opens the betting page.
     *
     * The lottery numbers are now entered directly into the
     * AnvilWindow rename text.
     *
     * Paper and diamonds are only required materials:
     * - 1 paper per ticket
     * - 2 diamonds per ticket
     */
    public void displayBetMakingPage(Player player) {
        Item empty = createEmptyItem();
        Item invalidBet = createInvalidBetItem();

        Gui gui = Gui.builder()
                .setStructure("i i #")
                .build();

        VirtualInventory betPlace = new VirtualInventory(2);
        gui.setInventory('i', betPlace);

        AnvilWindow window = AnvilWindow.builder()
                .setUpperGui(gui)
                .setTitle(messages.get("bet-making-title"))
                .setViewer(player)
                .addCloseHandler(close -> returnBetItems(player, betPlace))
                .build();

        /*
         * The rename text is the actual lottery number input.
         *
         * Example:
         * 1 5 12 20 28 33 7
         */
        window.addRenameHandler(text -> {
            updateBetButton(
                    player,
                    gui,
                    betPlace,
                    window,
                    text
            );
        });

        /*
         * Paper/diamond changes can also affect whether the
         * purchase button should be available.
         */
        betPlace.addPreUpdateHandler(event -> {
            if (event.isAdd() || event.isRemove()) {
                updateBetButton(
                        player,
                        gui,
                        betPlace,
                        window,
                        window.getRenameText()
                );
            }

            if (event.isRemove()) {
                /*
                 * If the materials become invalid, updateBetButton()
                 * will replace the button with the invalid item.
                 *
                 * This also keeps the old empty-slot behavior.
                 */
                if (betPlace.getItem(0) == null
                        && betPlace.getItem(1) == null) {
                    gui.setItem('#', empty);
                }
            }
        });

        /*
         * Start with an invalid/empty state.
         */
        gui.setItem('#', invalidBet);

        window.open();
    }

    /**
     * Updates the purchase button based on:
     *
     * 1. Anvil rename text
     * 2. Paper
     * 3. Diamonds
     */
    private void updateBetButton(
            Player player,
            Gui gui,
            VirtualInventory betPlace,
            AnvilWindow window,
            String renameText
    ) {
        int[] numbers = parseBetText(renameText);

        ItemStack paper = betPlace.getItem(0);
        ItemStack diamonds = betPlace.getItem(1);

        if (numbers == null
                || !isValidPaper(paper)
                || !isValidPayment(diamonds)) {

            gui.setItem('#', createInvalidBetItem());
            return;
        }

        gui.setItem(
                '#',
                createSettleBetItem(
                        player,
                        betPlace,
                        window
                )
        );
    }

    private Item createEmptyItem() {
        return Item.builder()
                .setItemProvider(
                        new ItemBuilder(Material.AIR)
                )
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

    /**
     * Creates the purchase button.
     *
     * The actual rename text is read again when the player clicks
     * the button instead of relying on the value used to create
     * the button.
     */
    private Item createSettleBetItem(
            Player player,
            VirtualInventory betPlace,
            AnvilWindow window
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
                                window
                        )
                )
                .build();
    }

    private void settleBet(
            Player player,
            VirtualInventory betPlace,
            AnvilWindow window
    ) {
        /*
         * Read the rename text again when purchasing.
         *
         * This guarantees that the actual current anvil text is
         * used, even if the text changed after the button was created.
         */
        int[] placedNumbers = parseBetText(
                window.getRenameText()
        );

        if (placedNumbers == null) {
            return;
        }

        ItemStack paper = betPlace.getItem(0);
        ItemStack diamonds = betPlace.getItem(1);

        /*
         * Paper is now only a material requirement.
         */
        if (!isValidPaper(paper)
                || !isValidPayment(diamonds)) {
            return;
        }

        player.getWorld().playSound(
                player.getLocation(),
                Sound.ENTITY_SHEEP_SHEAR,
                1.0f,
                1.0f
        );

        int betCount = Math.min(
                paper.getAmount(),
                diamonds.getAmount() / 2
        );

        if (betCount <= 0) {
            return;
        }

        /*
         * All tickets are added and the database is written once.
         */
        manager.playerSettleBets(
                player,
                placedNumbers,
                betCount
        );

        paper.setAmount(
                paper.getAmount() - betCount
        );

        diamonds.setAmount(
                diamonds.getAmount() - betCount * 2
        );

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
                InventoryUtils.addToInventoryOrDrop(
                        player,
                        item
                );
            }
        }
    }

    /**
     * Paper is now validated only by its material.
     *
     * No custom name is required.
     */
    private boolean isValidPaper(ItemStack paper) {
        return paper != null
                && paper.getType() == Material.PAPER;
    }

    /**
     * Requires at least two diamonds for one ticket.
     */
    private boolean isValidPayment(ItemStack diamonds) {
        return diamonds != null
                && diamonds.getType() == Material.DIAMOND
                && diamonds.getAmount() >= 2;
    }

    /**
     * Parses the AnvilWindow rename text.
     *
     * Expected:
     *
     * 1 5 12 20 28 33 7
     *
     * First 6 numbers:
     * 1-33, unique
     *
     * Last number:
     * 1-16
     */
    private int[] parseBetText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        String[] parts = text.trim().split("\\s+");

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

        return isValidBlueNumber(
                numbers[RED_NUMBER_COUNT]
        )
                ? numbers
                : null;
    }

    private boolean isValidRedNumbers(int[] numbers) {
        for (int i = 0; i < RED_NUMBER_COUNT; i++) {
            if (numbers[i] < 1
                    || numbers[i] > RED_NUMBER_MAX) {
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
        return number >= 1
                && number <= BLUE_NUMBER_MAX;
    }

    public void build_your_bet(
            long timeKey,
            Player player,
            Gui gui
    ) {
        int[] bet = manager.get_last_bet(
                timeKey,
                player
        );

        List<int[]> draw = manager.getDraw(timeKey);

        if (bet == null || draw.isEmpty()) {
            gui.setItem(1, null);
            return;
        }

        int[] drawNumbers = draw.getFirst();

        boolean[] duplicates =
                manager.findDuplicates(
                        drawNumbers,
                        bet
                );

        int prize = manager.calculatePrize(
                duplicates
        );

        givePrize(player, prize);

        manager.clear_last_bet(
                timeKey,
                player
        );

        int newSize = manager
                .getPlayerBets(timeKey, player)
                .size();

        if (newSize <= 0) {
            gui.setItem(1, null);
            return;
        }

        int[] nextBet = manager.get_last_bet(
                timeKey,
                player
        );

        boolean[] nextDuplicates =
                manager.findDuplicates(
                        drawNumbers,
                        nextBet
                );

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

    private void givePrize(
            Player player,
            int prize
    ) {
        if (prize > 0) {
            InventoryUtils.addToInventoryOrDrop(
                    player,
                    new ItemStack(
                            Material.DIAMOND,
                            prize
                    )
            );

            player.getWorld().playSound(
                    player.getLocation(),
                    Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST,
                    1.0f,
                    1.0f
            );

            return;
        }

        if (prize == 0) {
            InventoryUtils.addToInventoryOrDrop(
                    player,
                    new ItemStack(
                            Material.SPIDER_EYE,
                            1
                    )
            );

            player.getWorld().playSound(
                    player.getLocation(),
                    Sound.ENTITY_VILLAGER_NO,
                    1.0f,
                    1.0f
            );

            return;
        }

        player.getWorld().playSound(
                player.getLocation(),
                Sound.ENTITY_ENDER_DRAGON_DEATH,
                1.0f,
                1.0f
        );

        player.sendMessage(
                messages.get("first-prize")
        );
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

        VirtualInventory prizePlace =
                new VirtualInventory(1);

        gui.setInventory('i', prizePlace);

        String nextDrawTime =
                manager.nextDrawTime_String();

        MerchantWindow window =
                MerchantWindow.builder()
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

        List<MerchantWindow.Trade> tradeList =
                new ArrayList<>();

        long[] allKeys = manager.getAllKeys();

        boolean[] betAvailable =
                new boolean[allKeys.length];

        for (int keyIndex = allKeys.length - 1;
             keyIndex >= 0;
             keyIndex--) {

            long timeKey = allKeys[keyIndex];

            List<int[]> bets =
                    manager.getPlayerBets(
                            timeKey,
                            player
                    );

            boolean isNextDraw =
                    timeKey == manager.nextDrawTime_Key();

            boolean hasBets =
                    !bets.isEmpty();

            betAvailable[keyIndex] =
                    hasBets;

            Item ticket =
                    createHistoryTicket(
                            bets.size(),
                            hasBets
                    );

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

            tradeList.add(
                    tradeBuilder.build()
            );
        }

        window.setTrades(tradeList);

        window.setTradeSelectHandlers(
                List.of(
                        (something, tradeIndex) -> {
                            int realIndex =
                                    (allKeys.length - 1)
                                            - tradeIndex;

                            long timeKey =
                                    allKeys[realIndex];

                            if (timeKey
                                    == manager.nextDrawTime_Key()) {
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
                                .setName(
                                        messages.get(messageKey)
                                )
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
                                .setAmount(
                                        hasBets
                                                ? betCount
                                                : 1
                                )
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
        List<int[]> draw =
                manager.getDraw(timeKey);

        if (draw.isEmpty()) {
            return;
        }

        int[] drawNumbers =
                draw.getFirst();

        gui.setItem(
                0,
                createDrawResultItem(drawNumbers)
        );

        if (!hasBets) {
            return;
        }

        int[] bet =
                manager.get_last_bet(
                        timeKey,
                        player
                );

        if (bet == null) {
            gui.setItem(1, null);
            return;
        }

        boolean[] duplicates =
                manager.findDuplicates(
                        drawNumbers,
                        bet
                );

        int count =
                manager.getPlayerBets(
                        timeKey,
                        player
                ).size();

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

    private Item createDrawResultItem(
            int[] draw
    ) {
        return Item.builder()
                .setItemProvider(
                        new ItemBuilder(
                                Material.FILLED_MAP
                        )
                                .setName(
                                        Component.text(
                                                messages.get(
                                                        "draw-result"
                                                )
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
        return IntStream.range(
                        0,
                        numbers.length
                )
                .mapToObj(i -> {
                    Component number =
                            Component.text(
                                    String.valueOf(
                                            numbers[i]
                                    )
                            ).color(
                                    i < RED_NUMBER_COUNT
                                            ? NamedTextColor.RED
                                            : NamedTextColor.BLUE
                            );

                    if (!decorateDuplicates) {
                        return number.decorate(
                                TextDecoration.BOLD
                        );
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