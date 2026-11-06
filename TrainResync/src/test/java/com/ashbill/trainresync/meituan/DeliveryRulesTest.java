package com.ashbill.trainresync.meituan;

import com.google.gson.Gson;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import static com.ashbill.trainresync.meituan.DeliveryBook.*;

public final class DeliveryRulesTest {
    private static int assertions;
    private static void check(boolean ok, String name) {
        assertions++;
        if (!ok) throw new AssertionError(name);
    }
    private static BigDecimal decimal(String value) { return new BigDecimal(value); }
    private static Menu simpleMenu() { return new Menu(List.of(new Line("test:pie", 3, decimal("16"), Map.of())), BigDecimal.ONE); }
    private static Order animal(Point point, long expiry) { return new Order(UUID.randomUUID(), null, "绵羊-0001", point, simpleMenu(), expiry); }
    private static void fill(DeliveryBook book, UUID viewer, long expiry) {
        while (book.courier(viewer).animals.size() < 5) book.addAnimal(viewer, animal(new Point(20, 70, 20), expiry));
    }
    public static void main(String[] args) throws Exception {
        MeituanConfig config = new MeituanConfig();
        config.validate();
        check(config.foods.size() == 3 && config.animals.size() == 27, "approved initial food/name counts");
        for (int n = 0; n < 12; n++) check(target(n) == Math.max(0, Math.min(7 - n, 5)), "rolling target " + n);
        long now = 100 * DAY;
        UUID viewer = UUID.randomUUID(), owner = UUID.randomUUID();
        DeliveryBook book = new DeliveryBook();
        fill(book, viewer, now + 1000);
        Request request = book.request(owner, "Owner", new Point(100, 60, 100));
        book.request(viewer, "Self", new Point(1, 60, 1));
        Random random = new Random(123);
        Preview before = book.preview(viewer, 0, 0, now, config, random);
        check(before.rows().size() == 5 && before.rows().stream().noneMatch(q -> viewer.equals(q.order().owner())), "own requests filtered before display");
        Preview repeated = book.preview(viewer, 0, 0, now, config, random);
        check(before.equals(repeated), "repeat preview preserves exact menu, hot result, order and price");
        book.request(owner, "Owner", new Point(101, 250, 100));
        Preview moved = book.preview(viewer, 0, 0, now, config, random);
        check(sameSelection(before, moved), "same near/far side allows changed destination and Y");
        Quote movedPlayer = moved.rows().stream().filter(q -> owner.equals(q.order().owner())).findFirst().orElseThrow();
        check(movedPlayer.order().point().y() == 250, "latest actual destination used");
        Menu retained = movedPlayer.order().menu();
        book.request(owner, "Owner", new Point(900, 250, 100));
        Preview crossed = book.preview(viewer, 0, 0, now, config, random);
        check(!sameSelection(moved, crossed), "owner crossing threshold invalidates selection");
        Quote farPlayer = crossed.rows().stream().filter(q -> owner.equals(q.order().owner())).findFirst().orElseThrow();
        check(farPlayer.order().menu().equals(retained), "reprice does not reroll menu or hot bonus");
        check(!sameSelection(crossed, book.preview(viewer, 850, 100, now, config, random)), "courier moving across threshold invalidates selection");
        fill(book, UUID.randomUUID(), now + 1000);
        check(sameSelection(crossed, book.preview(viewer, 0, 0, now, config, random)), "other courier animal pool does not reshuffle");
        book.request(UUID.randomUUID(), "Other", new Point(5, 60, 5));
        check(!sameSelection(crossed, book.preview(viewer, 0, 0, now, config, random)), "player membership invalidates old selection");
        check(book.accept(viewer, farPlayer, config), "accept request");
        Active active = book.courier(viewer).active;
        check(active.deadline() == book.runningMillis + 12_000_000L, "200 minute deadline uses server running time");
        check(!book.requests.containsKey(owner) && !book.accept(viewer, farPlayer, config), "remove request and reject second active order");
        book.request(owner, "Owner", new Point(10, 40, 10));
        check(!book.requests.get(owner).id().equals(request.id()) && book.courier(viewer).active.equals(active), "new owner request cannot mutate accepted order");
        check(book.cancel(owner) && book.courier(viewer).active.equals(active), "cancel only waiting request");
        DeliveryBook restored = new Gson().fromJson(new Gson().toJson(book), DeliveryBook.class);
        check(restored.courier(viewer).active.equals(active) && restored.runningMillis == book.runningMillis, "restart preserves accepted contents and remaining running time");
        check(restored.courier(viewer).animals.size() == 5, "restart preserves private animal pool");
        check(book.expireAnimals(viewer, now + 1000) && book.courier(viewer).animals.isEmpty(), "expiry includes exact deadline");

        for (int n = 0; n <= 10; n++) {
            DeliveryBook pool = new DeliveryBook();
            fill(pool, viewer, now + 5000);
            for (int i = 0; i < 4; i++) pool.request(UUID.randomUUID(), "Customer", new Point(10, 60, 10));
            for (int i = 0; i < n; i++) pool.courier(viewer).completed.add(now - 1);
            pool.courier(viewer).completed.add(now - DAY); // Outside the rolling window at the exact edge.
            Preview rows = pool.preview(viewer, 0, 0, now, config, random);
            check(rows.rows().size() == Math.max(3, target(n)), "player requests remain at N=" + n);
            check(rows.rows().stream().filter(q -> q.order().owner() != null).count() == 3, "at most three players at N=" + n);
        }
        DeliveryBook empty = new DeliveryBook();
        fill(empty, viewer, now + 5000);
        for (int i = 0; i < 7; i++) empty.courier(viewer).completed.add(now);
        check(empty.preview(viewer, 0, 0, now, config, random).rows().isEmpty(), "zero player orders plus N7 produces zero rows");
        check(empty.courier(viewer).animals.size() == 5, "invisible animals remain in pool");

        Menu priced = new Menu(List.of(new Line("test:a", 1, decimal("0.7"), Map.of()),
                new Line("test:b", 1, decimal("0.7"), Map.of("新品", decimal("1.5")))), decimal("1.5"));
        Order special = new Order(UUID.randomUUID(), owner, "Owner", new Point(900, 60, 0), priced, Long.MAX_VALUE);
        Quote quote = quote(special, 0, 0, config);
        check(quote.subtotal().equals(decimal("1.75")) && quote.reward() == 5, "item-specific fixed bonuses and final-only floor");
        check(quote(animal(new Point(0, 1000, 0), now), 0.5, 0.5, config).distance() == 0, "list ignores Y");
        check(new Point(0, 1000, 0).squaredDistance(0.5, 0, 0.5) == 1_000_000, "delivery includes Y");
        config.hotProbability = 1;
        config.foods.values().forEach(food -> food.quantityMultiplier = 4);
        for (int i = 0; i < 500; i++) {
            Menu menu = menu(config, random);
            check(menu.hot().compareTo(decimal("1.5")) >= 0 && menu.hot().compareTo(decimal("2.0")) <= 0 && menu.hot().scale() == 2, "hot bounds/precision");
            check(menu.lines().size() >= 1 && menu.lines().size() <= 3 && menu.lines().stream().map(Line::item).distinct().count() == menu.lines().size(), "distinct food types");
            for (Line line : menu.lines()) check(line.count() >= 12 && line.count() <= 28 && line.count() % 4 == 0, "quantity multiplier");
        }
        var stock = List.of(new DeliveryInventory.Stock("a", 2, true), new DeliveryInventory.Stock("a", 64, false),
                new DeliveryInventory.Stock("a", 3, true), new DeliveryInventory.Stock("b", 2, true));
        check(DeliveryInventory.plan(stock, Map.of("a", 6, "b", 1)) == null, "NBT-altered stock excluded; insufficient total removes nothing");
        check(DeliveryInventory.plan(stock, Map.of("a", 4, "b", 3)) == null, "second food shortage is atomic");
        int[] plan = DeliveryInventory.plan(stock, Map.of("a", 4, "b", 2));
        check(java.util.Arrays.equals(plan, new int[]{2, 0, 2, 2}), "split-stack exact removal leaves surplus");
        check(stock.get(0).count() == 2 && stock.get(2).count() == 3, "planning does not mutate inventory");
        var path = Files.createTempFile("meituan-config-test-", ".json");
        try {
            Files.writeString(path, "{\"railwayDataProtection\":true}");
            check(!MeituanConfig.load(path).enabled, "missing section disabled without changing railway settings");
            Files.writeString(path, "{\"meituan\":{\"enabled\":true,\"foods\":{\"test:pie\":{\"价格\":16,\"数量乘数\":4,\"加成\":{\"新品\":1.2}}}}}");
            MeituanConfig loaded = MeituanConfig.load(path);
            check(loaded.enabled && loaded.foods.size() == 1 && loaded.foods.get("test:pie").quantityMultiplier == 4, "custom JSON replaces food whitelist");
            Files.writeString(path, "{\"meituan\":{\"farProbability\":2}}");
            boolean rejected = false;
            try { MeituanConfig.load(path); } catch (java.io.IOException expected) { rejected = true; }
            check(rejected, "invalid probability rejected");
        } finally { Files.deleteIfExists(path); }
        System.out.println("Meituan rules PASS: " + assertions + " assertions (selection, pricing, persistence, inventory, config)");
    }
}
