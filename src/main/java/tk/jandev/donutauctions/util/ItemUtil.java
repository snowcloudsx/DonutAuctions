package tk.jandev.donutauctions.util;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;

public class ItemUtil {
    public static boolean isShulkerBox(Item item) {
        return (Registries.ITEM.getId(item).getPath().toUpperCase().contains("SHULKER_BOX")); // super inefficient - but cleaner
    }
}
