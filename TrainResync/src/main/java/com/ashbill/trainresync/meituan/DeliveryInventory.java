package com.ashbill.trainresync.meituan;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Plan all removals before the game adapter mutates a single stack. */
public final class DeliveryInventory {
    private DeliveryInventory() {}
    public record Stock(String item, int count, boolean plain) {}

    public static int[] plan(List<Stock> inventory, Map<String, Integer> required) {
        Map<String, Integer> remaining = new LinkedHashMap<>(required);
        int[] removal = new int[inventory.size()];
        for (int i = 0; i < inventory.size(); i++) {
            Stock stock = inventory.get(i);
            if (!stock.plain()) continue;
            int take = Math.min(Math.max(0, stock.count()), remaining.getOrDefault(stock.item(), 0));
            removal[i] = take;
            if (take > 0) remaining.put(stock.item(), remaining.get(stock.item()) - take);
        }
        return remaining.values().stream().allMatch(count -> count == 0) ? removal : null;
    }
}
