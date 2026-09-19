package fund.racer.lotterySeven;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.inventory.ReferencingInventory;
import xyz.xenondevs.invui.inventory.VirtualInventory;
import xyz.xenondevs.invui.inventory.event.ItemPreUpdateEvent;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemBuilder;
import xyz.xenondevs.invui.util.InventoryUtils;
import xyz.xenondevs.invui.window.AnvilWindow;
import xyz.xenondevs.invui.window.Window;

import java.util.Arrays;

public class LotteryGUI {

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

        // Numbers 1-6: 1 ~ 33
        for (int i = 0; i < 6; i++) {
            if (numbers[i] < 1 || numbers[i] > 33) {
                return null;
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
                .addClickHandler(click -> player.sendMessage("开奖结果？"))
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
        Item settle_bet = Item.builder()
                .setItemProvider(new ItemBuilder(Material.FILLED_MAP))
                .addClickHandler(click -> player.playSound(player, Sound.ENTITY_SHEEP_SHEAR,1,1))
                .build();

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




}
