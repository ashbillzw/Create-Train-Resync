package com.ashbill.trainresync.meituan;

import java.math.BigDecimal;
import java.util.List;

/** Plan all removals before the game adapter mutates a single stack. */
public final class DeliveryInventory {
    private DeliveryInventory() {}
    /** Negative for ineligible stacks; otherwise the quality premium in half-price units. */
    public record Stock(String item, int count, int bonusHalves) {}
    public record Plan(int[] removal, BigDecimal premium) {}

    public static Plan plan(List<Stock> inventory, List<DeliveryBook.Line> lines) {
        int[] removal = new int[inventory.size()];
        BigDecimal twicePremium = BigDecimal.ZERO;
        for (var line : lines) {
            int needed = line.count();
            long halfUnits = 0;
            for (int i = 0; i < inventory.size() && needed > 0; i++) {
                Stock stock = inventory.get(i);
                if (stock.bonusHalves() < 0 || !stock.item().equals(line.item())) continue;
                int take = Math.min(needed, Math.max(0, stock.count() - removal[i]));
                removal[i] += take;
                needed -= take;
                halfUnits += (long) take * stock.bonusHalves();
            }
            if (needed > 0) return null;
            twicePremium = twicePremium.add(line.adjustedUnitPrice().multiply(BigDecimal.valueOf(halfUnits)));
        }
        return new Plan(removal, twicePremium.divide(BigDecimal.valueOf(2)));
    }
}
