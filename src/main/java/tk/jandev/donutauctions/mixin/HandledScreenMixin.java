package tk.jandev.donutauctions.mixin;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public class HandledScreenMixin {

    private static final Pattern WORTH_PATTERN =
            Pattern.compile("Worth: \\$(\\d+(?:\\.\\d+)?)([kKmMbB]?)");
    private static final Pattern AUCTION_PATTERN =
            Pattern.compile("Auction.?Value: \\$(\\d+(?:\\.\\d+)?)([kKmMbB]?)");

    @Inject(method = "drawForeground(Lnet/minecraft/client/gui/DrawContext;II)V", at = @At("TAIL"))
    private void drawOverlay(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();

        // Check if current screen is a container screen (chest or shulker box)
        if (!(client.currentScreen instanceof GenericContainerScreen)
                && !(client.currentScreen instanceof ShulkerBoxScreen)) {
            return;
        }

        if (client.player == null) {
            return;
        }

        HandledScreen<?> screen = (HandledScreen<?>) client.currentScreen;

        double totalSellWorth = 0;
        double totalAuctionWorth = 0;
        Map<String, double[]> itemMap = new HashMap<>(); // [count, sellWorth, auctionWorth]

        ClientPlayerEntity player = client.player;
        Item.TooltipContext tooltipContext = Item.TooltipContext.DEFAULT;

        // Process only container items (not player inventory)
        // Generic containers and shulker boxes have container slots first, then player inventory
        int containerSlots;
        if (client.currentScreen instanceof ShulkerBoxScreen) {
            containerSlots = 27; // Shulker boxes have 27 slots
        } else {
            containerSlots = screen.getScreenHandler().slots.size() - 36; // 36 = player inventory slots
        }

        for (int i = 0; i < containerSlots; i++) {
            ItemStack stack = screen.getScreenHandler().slots.get(i).getStack();
            if (stack.isEmpty()) {
                continue;
            }

            double itemSellWorth = 0;
            double itemAuctionWorth = 0;
            String itemName = stack.getName().getString();

            List<Text> tooltips = stack.getTooltip(tooltipContext, player, TooltipType.BASIC);
            for (Text line : tooltips) {
                String raw = line.getString();

                Matcher m1 = WORTH_PATTERN.matcher(raw);
                Matcher m2 = AUCTION_PATTERN.matcher(raw);

                if (m1.find()) {
                    itemSellWorth += parseWorth(m1.group(1), m1.group(2));
                }
                if (m2.find()) {
                    itemAuctionWorth += parseWorth(m2.group(1), m2.group(2));
                }
            }

            if (itemSellWorth > 0 || itemAuctionWorth > 0) {
                totalSellWorth += itemSellWorth;
                totalAuctionWorth += itemAuctionWorth;

                // Group items by name
                if (itemMap.containsKey(itemName)) {
                    double[] existing = itemMap.get(itemName);
                    existing[0]++; // count
                    existing[1] += itemSellWorth; // total sell worth
                    existing[2] += itemAuctionWorth; // total auction worth
                } else {
                    itemMap.put(itemName, new double[] {1, itemSellWorth, itemAuctionWorth});
                }
            }
        }

        // Convert to list and sort by maximum worth (descending)
        List<Map.Entry<String, double[]>> itemWorths = new ArrayList<>(itemMap.entrySet());
        itemWorths.sort(
                (a, b) -> {
                    double maxA = Math.max(a.getValue()[1], a.getValue()[2]);
                    double maxB = Math.max(b.getValue()[1], b.getValue()[2]);
                    return Double.compare(maxB, maxA);
                });

        int lineHeight = 10;

        // Draw totals in top left corner (outside chest GUI)
        int totalX = -150;
        int totalY = -80;

        context.drawText(
                client.textRenderer, Text.literal("Total Worth:"), totalX, totalY, 0xFFFFFF, true);
        totalY += lineHeight;

        context.drawText(
                client.textRenderer,
                Text.literal("/Sell: $" + formatValue(totalSellWorth)),
                totalX,
                totalY,
                0xFFFFFF,
                true);
        totalY += lineHeight;

        context.drawText(
                client.textRenderer,
                Text.literal("Auction: $" + formatValue(totalAuctionWorth)),
                totalX,
                totalY,
                0xFFFFFF,
                true);
        totalY += lineHeight;

        // Draw breakdown below the totals with some spacing
        int breakdownX = totalX;
        int breakdownY = totalY + 10; // Add some spacing between totals and breakdown

        context.drawText(
                client.textRenderer,
                Text.literal("Item Breakdown:"),
                breakdownX,
                breakdownY,
                0xFFFFFF,
                true);
        int currentY = breakdownY + lineHeight;

        // Draw top items (limit to prevent overflow)
        int maxItems = Math.min(itemWorths.size(), 6);
        for (int i = 0; i < maxItems; i++) {
            Map.Entry<String, double[]> entry = itemWorths.get(i);
            String itemName = entry.getKey();
            double[] data = entry.getValue();
            int count = (int) data[0];
            double maxWorth = Math.max(data[1], data[2]);

            // Truncate item name if too long
            String displayName = itemName;
            if (displayName.length() > 15) {
                displayName = displayName.substring(0, 12) + "...";
            }

            String itemLine;
            if (count > 1) {
                itemLine = displayName + " x" + count + " [" + formatValue(maxWorth) + "]";
            } else {
                itemLine = displayName + " [" + formatValue(maxWorth) + "]";
            }

            context.drawText(
                    client.textRenderer, Text.literal(itemLine), breakdownX, currentY, 0xFFFFFF, true);
            currentY += lineHeight;
        }

        // Show "..." if there are more items
        if (itemWorths.size() > maxItems) {
            context.drawText(
                    client.textRenderer,
                    Text.literal("... and " + (itemWorths.size() - maxItems) + " more items"),
                    breakdownX,
                    currentY,
                    0xAAAAAA,
                    true);
        }
    }

    private double parseWorth(String number, String suffix) {
        double base = Double.parseDouble(number);
        return switch (suffix.toLowerCase()) {
            case "k" -> base * 1_000;
            case "m" -> base * 1_000_000;
            case "b" -> base * 1_000_000_000;
            default -> base;
        };
    }

    private String formatValue(double value) {
        if (value >= 1_000_000_000) {
            return new DecimalFormat("#.##").format(value / 1_000_000_000) + "B";
        }
        if (value >= 1_000_000) {
            return new DecimalFormat("#.##").format(value / 1_000_000) + "M";
        }
        if (value >= 1_000) {
            return new DecimalFormat("#.##").format(value / 1_000) + "K";
        }
        return new DecimalFormat("#.##").format(value);
    }
}