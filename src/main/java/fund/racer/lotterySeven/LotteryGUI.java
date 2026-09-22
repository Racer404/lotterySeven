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


public class LotteryGUI {


    private final LotteryManager manager;

    public LotteryGUI(LotteryManager manager) {
        this.manager = manager;
    }

    private int[] bet_place_checker(ItemPreUpdateEvent event) {
        ItemStack paper;
        ItemStack diamonds;

        // Get the resulting item in slot 0
        if (event.getSlot() == 0) {
            paper = event.getNewItem();
        } else {
            paper = event.getInventory().getItem(0);
        }

        // Get the resulting item in slot 1
        if (event.getSlot() == 1) {
            diamonds = event.getNewItem();
        } else {
            diamonds = event.getInventory().getItem(1);
        }

        // Check slot 0: PAPER
        if (paper == null || paper.getType() != Material.PAPER) {
            return null;
        }

        // Check slot 1: at least 2 DIAMOND
        if (diamonds == null
                || diamonds.getType() != Material.DIAMOND
                || diamonds.getAmount() < 2) {
            return null;
        }

        // Check PAPER custom name
        ItemMeta meta = paper.getItemMeta();

        if (meta == null || !meta.hasDisplayName()) {
            return null;
        }

        String customText = PlainTextComponentSerializer.plainText()
                .serialize(meta.displayName())
                .trim();

        String[] parts = customText.split("\\s+");

        // Must contain exactly 7 numbers
        if (parts.length != 7) {
            return null;
        }

        int[] numbers = new int[7];

        try {
            for (int i = 0; i < 7; i++) {
                numbers[i] = Integer.parseInt(parts[i]);
            }
        } catch (NumberFormatException e) {
            return null;
        }

        for (int i = 0; i < 6; i++) {
            // Number 1-6: 1 ~ 33
            if (numbers[i] < 1 || numbers[i] > 33) {
                return null;
            }

            // Check for duplicates
            for (int j = 0; j < i; j++) {
                if (numbers[i] == numbers[j]) {
                    return null;
                }
            }
        }

        // Number 7: 1 ~ 16
        if (numbers[6] < 1 || numbers[6] > 16) {
            return null;
        }

        return numbers;
    }


    public void displayMainPage(Player player){
        Item past_bet = Item.builder()
                .setItemProvider(new ItemBuilder(Material.FLOW_BANNER_PATTERN).setName("开奖结果"))
                .addClickHandler(click -> displayBetHistoryPage(player))
                .build();

        Item new_bet = Item.builder()
                .setItemProvider(new ItemBuilder(Material.MAP).setName("买张新的"))
                .addClickHandler(click -> displayBetMakingPage(player))
                .build();

        Gui gui = Gui.builder()
                .setStructure(
                        "# # # # # # # # #",
                        "# # a # # # b # #",
                        "# # # # # # # # #")
                .addIngredient('a', past_bet)
                .addIngredient('b', new_bet)
                .build();

        Window window = Window.builder()
                .setTitle("欢迎拉杆服务器彩票小站!")
                .setUpperGui(gui)
                .setViewer(player)
                .build();
        window.open();
    }

    public void displayBetMakingPage(Player player){
        Item empty = Item.builder()
                .setItemProvider(new ItemBuilder(Material.AIR))
                .build();

        Item wrong_format = Item.builder()
                .setItemProvider(new ItemBuilder(Material.BARRIER).setName("放钻石了吗?彩票格式对吗?"))
                .build();

        Gui gui = Gui.builder()
                .setStructure("i i #")
                .build();

        VirtualInventory bet_place = new VirtualInventory(2);

        bet_place.addPreUpdateHandler(event  -> {
            if(event.isAdd()){

                int[] placed_number = bet_place_checker(event);

                if(placed_number==null){
                    gui.setItem('#', wrong_format);

                }else{
                    Item settle_bet = Item.builder()
                            .setItemProvider(new ItemBuilder(Material.FILLED_MAP).setName("开奖时间:"+manager.nextDrawTime_String()))
                            .addClickHandler(click -> {
                                player.playSound(player, Sound.ENTITY_SHEEP_SHEAR,1,1);
                                ItemStack paper = bet_place.getItem(0);
                                ItemStack diamonds = bet_place.getItem(1);

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

                                for (int i = 0; i < betCount; i++) {
                                    manager.playerSettleBet(player, placed_number);
                                }

                                paper.setAmount(paper.getAmount() - betCount);
                                diamonds.setAmount(diamonds.getAmount() - betCount * 2);

                                bet_place.setItem(
                                        PlayerUpdateReason.SUPPRESSED,
                                        0,
                                        paper.getAmount() > 0 ? paper : new ItemStack(Material.AIR)
                                );

                                bet_place.setItem(
                                        PlayerUpdateReason.SUPPRESSED,
                                        1,
                                        diamonds.getAmount() > 0 ? diamonds : new ItemStack(Material.AIR)
                                );

                                click.player().closeInventory();
                            })
                            .build();
                    gui.setItem('#', settle_bet);
                }
            }
            else if(event.isRemove()){
                gui.setItem('#', empty);
            }
        });

        gui.setInventory('i', bet_place);

        AnvilWindow window = AnvilWindow.builder()
                .setUpperGui(gui)
                .setTitle("请把新编号码放入左侧")
                .setViewer(player)
                .addCloseHandler(close->{
                    for (int i = 0; i < 2; i++) {
                        ItemStack item = bet_place.getItem(i);
                        if (item != null && !item.getType().isAir()) {
                            InventoryUtils.addToInventoryOrDrop(player, item);
                        }
                    }
                })
                .build();
        window.open();
    }

    public void build_your_bet(long time_key, Player player, Gui gui){
        int[] bet = manager.get_last_bet(time_key, player);
        List<int[]> draw = manager.getDraw(time_key);
        boolean[] duplicates = manager.findDuplicates(draw.getFirst(), bet);
        int prize = manager.calculatePrize(duplicates);

        if (prize > 0) {
            ItemStack item = new ItemStack(Material.DIAMOND, prize);
            InventoryUtils.addToInventoryOrDrop(player, item);
            player.playSound(player, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST,1, 1);
        }else if(prize == 0){
            ItemStack item = new ItemStack(Material.SPIDER_EYE, 1);
            InventoryUtils.addToInventoryOrDrop(player, item);
            player.playSound(player, Sound.ENTITY_VILLAGER_NO,1, 1);
        }else{
            player.playSound(player,Sound.ENTITY_ENDER_DRAGON_DEATH, 1, 1);
            player.sendMessage("一等奖！概率111万分之一，请截图并联系服务器管理员");
        }
        manager.clear_last_bet(time_key, player);

        int new_size = manager.getPlayerBets(time_key, player).size();


        if(new_size>0){
            int[] new_bet = manager.get_last_bet(time_key, player);
            boolean[] new_duplicates = manager.findDuplicates(draw.getFirst(), new_bet);

            Item your_bet = Item.builder()
                    .setItemProvider(new ItemBuilder(Material.MAP).setAmount(new_size)
                            .setName(Component.text("你的投注"))
                            .setLore(
                                    IntStream.range(0, new_bet.length)
                                            .mapToObj(i ->
                                                    Component.text(String.valueOf(new_bet[i]))
                                                            .color(i < 6 ? NamedTextColor.RED : NamedTextColor.BLUE)
                                                            .decorate(new_duplicates[i] ? TextDecoration.STRIKETHROUGH : TextDecoration.BOLD)
                                            )
                                            .toList()
                            )
                    ).addClickHandler(click -> this.build_your_bet(time_key, player, gui))
                    .build();
            gui.setItem(1, your_bet);
        }else{
            gui.setItem(1, null);
        }

    }

    public void displayBetHistoryPage(Player player){
        Item bet_empty = Item.builder()
                .setItemProvider(new ItemBuilder(Material.LEVER).setName("已结束"))
                .build();

        Item bet_ok = Item.builder()
                .setItemProvider(new ItemBuilder(Material.REDSTONE_TORCH).setName("已开奖！"))
                .build();

        Item bet_future = Item.builder()
                .setItemProvider(new ItemBuilder(Material.LEVER).setName("未开奖"))
                .build();

        Gui gui = Gui.builder()
                .setStructure(3, 1, "# # i")
                .build();

        VirtualInventory prize_place = new VirtualInventory(1);
        gui.setInventory('i', prize_place);

        MerchantWindow window = MerchantWindow.builder()
                .setTitle("距离下一次开奖: " + manager.nextDrawTime_String())
                .setUpperGui(gui)
                .setViewer(player)
                .build();

        List<MerchantWindow.Trade> tradelist = new ArrayList<>();


        long[] allKeys = manager.getAllKeys();
        boolean[] bet_available = new boolean[allKeys.length];

        for (int key_idx = allKeys.length-1; key_idx >=0; key_idx--) {
            long key = allKeys[key_idx];

            List<int[]> betList = manager.getPlayerBets(key, player);
            boolean isNextDraw = key == manager.nextDrawTime_Key();
            boolean hasBets = !betList.isEmpty();
            bet_available[key_idx] = hasBets;

            Item ticket = Item.builder()
                    .setItemProvider(
                            new ItemBuilder(Material.FILLED_MAP)
                                    .setAmount(hasBets ? betList.size() : 1)
                                    .setName("已购彩票:"+betList.size()+"张")
                    )
                    .build();

            MerchantWindow.Trade.Builder tradeBuilder = MerchantWindow.Trade.builder()
                    .setFirstInput(ticket)
                    .setAvailable(!isNextDraw);

            if (hasBets) {
                tradeBuilder.setResult(bet_ok);
            } else {
                tradeBuilder.setResult(bet_empty);
            }
            if(isNextDraw){
                tradeBuilder.setResult(bet_future);
            }

            MerchantWindow.Trade trade = tradeBuilder.build();

            tradelist.add(trade);
        }

        window.setTrades(tradelist);
        window.setTradeSelectHandlers(List.of(
                (something, tradeIndex) -> {
                    int tradeIndex_real = (allKeys.length - 1) - tradeIndex;
                    long time_key = allKeys[tradeIndex_real];

                    if(bet_available[tradeIndex_real]){
                        if(time_key != manager.nextDrawTime_Key()){
                            List<int[]> draw = manager.getDraw(time_key);
                            if(!draw.isEmpty()){

                                Item draw_reveal = Item.builder()
                                        .setItemProvider(new ItemBuilder(Material.FILLED_MAP)
                                                .setName(Component.text("开奖结果"))
                                                .setLore(
                                                        IntStream.range(0, draw.getFirst().length)
                                                                .mapToObj(i ->
                                                                        Component.text(String.valueOf(draw.getFirst()[i]))
                                                                                .color(i < 6 ? NamedTextColor.RED : NamedTextColor.BLUE)
                                                                                .decorate(TextDecoration.BOLD)
                                                                )
                                                                .toList()
                                                )
                                        )
                                        .build();

                                gui.setItem(0, draw_reveal);

                                int[] bet = manager.get_last_bet(time_key, player);
                                boolean[] duplicates = manager.findDuplicates(draw.getFirst(), bet);
                                int size = manager.getPlayerBets(time_key, player).size();

                                Item your_bet = Item.builder()
                                        .setItemProvider(new ItemBuilder(Material.MAP).setAmount(size)
                                                .setName(Component.text("你的投注"))
                                                .setLore(
                                                        IntStream.range(0, bet.length)
                                                                .mapToObj(i ->
                                                                        Component.text(String.valueOf(bet[i]))
                                                                                .color(i < 6 ? NamedTextColor.RED : NamedTextColor.BLUE)
                                                                                .decorate(duplicates[i] ? TextDecoration.STRIKETHROUGH : TextDecoration.BOLD)
                                                                )
                                                                .toList()
                                                )
                                        ).addClickHandler(click -> build_your_bet(time_key, player, gui))
                                        .build();

                                gui.setItem(1, your_bet);
                            }
                        }
                    }else{
                        if(time_key != manager.nextDrawTime_Key()) {
                            List<int[]> draw = manager.getDraw(time_key);
                            if (!draw.isEmpty()) {

                                Item draw_reveal = Item.builder()
                                        .setItemProvider(new ItemBuilder(Material.FILLED_MAP)
                                                .setName(Component.text("开奖结果"))
                                                .setLore(
                                                        IntStream.range(0, draw.getFirst().length)
                                                                .mapToObj(i ->
                                                                        Component.text(String.valueOf(draw.getFirst()[i]))
                                                                                .color(i < 6 ? NamedTextColor.RED : NamedTextColor.BLUE)
                                                                                .decorate(TextDecoration.BOLD)
                                                                )
                                                                .toList()
                                                )
                                        )
                                        .build();

                                gui.setItem(0, draw_reveal);
                            }
                        }
                    }
                }
        ));
        window.open();
    }


}
