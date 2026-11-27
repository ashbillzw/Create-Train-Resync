package com.ashbill.trainresync.meituan;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/** Persistent order rules, independent of Minecraft so state transitions can be exercised directly. */
public final class DeliveryBook {
    public static final long DAY = 86_400_000L;
    private static final int DAILY_LIMIT = 7;
    public long runningMillis;
    public long playerRevision;
    public Map<UUID, Request> requests = new LinkedHashMap<>();
    public Map<UUID, Courier> couriers = new LinkedHashMap<>();
    public Map<UUID, Long> lastBroadcastDay = new HashMap<>();

    public record Point(int x, int y, int z) {
        public double horizontal(double px, double pz) { return Math.hypot(x + 0.5 - px, z + 0.5 - pz); }
        public double squaredDistance(double px, double py, double pz) {
            return (x + 0.5 - px) * (x + 0.5 - px) + (y - py) * (y - py) + (z + 0.5 - pz) * (z + 0.5 - pz);
        }
    }
    public record Request(UUID id, UUID owner, String name, Point point) {}
    public record Line(String item, int count, BigDecimal unitPrice, Map<String, BigDecimal> bonuses, boolean prank) {
        public BigDecimal base() { return unitPrice.multiply(BigDecimal.valueOf(count)); }
        public BigDecimal adjustedUnitPrice() {
            BigDecimal value = unitPrice;
            for (BigDecimal multiplier : bonuses.values()) value = value.multiply(multiplier);
            return value;
        }
        public BigDecimal subtotal() { return adjustedUnitPrice().multiply(BigDecimal.valueOf(count)); }
    }
    public record Menu(List<Line> lines, BigDecimal hot) {}
    public record Order(UUID id, UUID owner, String name, Point point, Menu menu, long expiresAt) {}
    public record Active(Order order, long reward, long deadline, BigDecimal qualityMultiplier) {
        public long qualityReward(BigDecimal extra, MeituanConfig config) {
            if (extra.signum() == 0) return 0;
            if (qualityMultiplier != null) return extra.multiply(qualityMultiplier).setScale(0, RoundingMode.FLOOR).longValueExact();
            // Older saves kept the whole-coin payment but not the distance and event multipliers.
            BigDecimal subtotal = config.deliveryBaseRate.multiply(BigDecimal.valueOf(order.menu().lines().size() + 1L));
            for (Line line : order.menu().lines()) subtotal = subtotal.add(line.subtotal());
            BigDecimal usual = order.menu().hot();
            if (order.owner() != null) usual = usual.multiply(config.playerMultiplier);
            for (BigDecimal distance : List.of(BigDecimal.ONE, config.farMultiplier)) {
                for (BigDecimal event : List.of(BigDecimal.ONE, MeituanConfig.EVENT_MULTIPLIER)) {
                    BigDecimal factor = usual.multiply(distance).multiply(event);
                    if (subtotal.multiply(factor).setScale(0, RoundingMode.FLOOR).compareTo(BigDecimal.valueOf(reward)) == 0)
                        return extra.multiply(factor).setScale(0, RoundingMode.FLOOR).longValueExact();
                }
            }
            return extra.multiply(BigDecimal.valueOf(reward)).divide(subtotal, 0, RoundingMode.FLOOR).longValueExact();
        }
    }
    public record Quote(Order order, boolean far, double distance, BigDecimal base, BigDecimal subtotal,
                        BigDecimal orderMultiplier, long reward) {
        public boolean highlighted() {
            return subtotal.multiply(orderMultiplier).compareTo(base.multiply(BigDecimal.valueOf(2))) >= 0;
        }
    }
    public record Preview(long players, long animals, List<Quote> rows) {}

    public static final class Courier {
        public long animalRevision;
        public List<Order> animals = new ArrayList<>();
        public Map<UUID, Menu> playerMenus = new HashMap<>();
        public List<Long> completed = new ArrayList<>();
        public Active active;
        // Existing saves use their full rolling history until an operator resets the quota.
        private int completionsSinceReset = DAILY_LIMIT;

        public void resetAnimalCooldown() { completionsSinceReset = 0; }

        public void recordCompletion(long now) {
            completed.add(now);
            completionsSinceReset = Math.min(DAILY_LIMIT, completionsSinceReset + 1);
        }
    }

    public Courier courier(UUID player) { return couriers.computeIfAbsent(player, ignored -> new Courier()); }

    public Request request(UUID owner, String name, Point point) {
        Request old = requests.get(owner);
        Request next = new Request(old == null ? UUID.randomUUID() : old.id(), owner, name, point);
        requests.put(owner, next);
        if (old == null) playerRevision++;
        return next;
    }

    public boolean cancel(UUID owner) {
        Request removed = requests.remove(owner);
        if (removed == null) return false;
        playerRevision++;
        for (Courier courier : couriers.values()) courier.playerMenus.remove(removed.id());
        return true;
    }

    public boolean expireAnimals(UUID viewer, long now) {
        Courier courier = courier(viewer);
        boolean changed = courier.animals.removeIf(order -> order.expiresAt() <= now);
        if (changed) courier.animalRevision++;
        return changed;
    }

    public void addAnimal(UUID viewer, Order order) {
        Courier courier = courier(viewer);
        courier.animals.add(order);
        courier.animalRevision++;
    }

    public static int target(int completions) { return Math.max(Math.min(DAILY_LIMIT - completions, 5), 0); }

    public Preview preview(UUID viewer, double x, double z, long now, MeituanConfig config, Random random) {
        Courier courier = courier(viewer);
        courier.completed.removeIf(time -> time <= now - 7 * DAY);
        int completed = (int) courier.completed.stream().filter(time -> time > now - DAY).count();
        Comparator<UUID> ranking = Comparator.comparing(id -> rank(viewer, playerRevision, courier.animalRevision, id));
        List<Order> selected = new ArrayList<>();
        requests.values().stream().filter(request -> !request.owner().equals(viewer))
                .sorted((a, b) -> ranking.compare(a.id(), b.id())).limit(3).forEach(request -> {
                    Menu menu = courier.playerMenus.computeIfAbsent(request.id(), ignored -> menu(config, random));
                    selected.add(new Order(request.id(), request.owner(), request.name(), request.point(), menu, Long.MAX_VALUE));
                });
        int needed = Math.max(target(Math.min(completed, courier.completionsSinceReset)) - selected.size(), 0);
        courier.animals.stream().sorted((a, b) -> ranking.compare(a.id(), b.id())).limit(needed).forEach(selected::add);
        selected.sort((a, b) -> ranking.compare(a.id(), b.id()));
        return new Preview(playerRevision, courier.animalRevision,
                selected.stream().map(order -> quote(order, x, z, config)).toList());
    }

    public static boolean sameSelection(Preview old, Preview current) {
        if (old == null || old.players() != current.players() || old.animals() != current.animals()
                || old.rows().size() != current.rows().size()) return false;
        for (int i = 0; i < old.rows().size(); i++) {
            Quote a = old.rows().get(i), b = current.rows().get(i);
            if (!a.order().id().equals(b.order().id()) || a.far() != b.far()) return false;
        }
        return true;
    }

    public boolean accept(UUID viewer, Quote quote, MeituanConfig config) {
        Courier courier = courier(viewer);
        if (courier.active != null) return false;
        Order order = quote.order();
        if (order.owner() != null) {
            Request request = requests.get(order.owner());
            if (order.owner().equals(viewer) || request == null || !request.id().equals(order.id())) return false;
            cancel(order.owner());
        } else {
            if (!courier.animals.removeIf(candidate -> candidate.id().equals(order.id()))) return false;
            courier.animalRevision++;
        }
        courier.active = new Active(order, quote.reward(), Math.addExact(runningMillis, config.deliveryTimeoutSeconds * 1000L),
                quote.orderMultiplier().multiply(config.eventActive ? MeituanConfig.EVENT_MULTIPLIER : BigDecimal.ONE));
        return true;
    }

    public static Menu menu(MeituanConfig config, Random random) {
        List<String> choices = new ArrayList<>(config.foods.keySet());
        java.util.Collections.shuffle(choices, random);
        List<String> preferred = new ArrayList<>(), regular = new ArrayList<>();
        List<String> pranks = new ArrayList<>();
        for (String id : choices) {
            MeituanConfig.Food food = config.foods.get(id);
            if (food.prank) pranks.add(id);
            else (food.preferred ? preferred : regular).add(id);
        }
        int kinds = 1 + random.nextInt(Math.min(3, preferred.size() + regular.size()));
        List<String> selected = new ArrayList<>(kinds);
        for (int i = 0; i < kinds; i++) {
            if (!pranks.isEmpty() && random.nextDouble() < config.prankItemChance)
                selected.add(pranks.remove(pranks.size() - 1));
            else selected.add(drawFood(preferred, regular, config.preferredPoolChance, random));
        }
        if (selected.stream().allMatch(id -> config.foods.get(id).sideDish)) {
            preferred.removeIf(id -> config.foods.get(id).sideDish);
            regular.removeIf(id -> config.foods.get(id).sideDish);
            selected.set(kinds - 1, drawFood(preferred, regular, config.preferredPoolChance, random));
        }
        List<Line> lines = new ArrayList<>();
        for (String id : selected) {
            MeituanConfig.Food food = config.foods.get(id);
            double roll = random.nextDouble();
            int baseCount = 1 + (int) (7 * roll * roll);
            lines.add(new Line(id, baseCount * food.quantityMultiplier, food.price.multiply(config.basePriceMultiplier),
                    new LinkedHashMap<>(food.bonuses), food.prank));
        }
        BigDecimal hot = BigDecimal.ONE;
        if (random.nextDouble() < config.hotProbability)
            hot = BigDecimal.valueOf(config.hotMinHundredths + random.nextInt(config.hotMaxHundredths - config.hotMinHundredths + 1), 2);
        return new Menu(List.copyOf(lines), hot);
    }

    private static String drawFood(List<String> preferred, List<String> regular, double chance, Random random) {
        boolean usePreferred = regular.isEmpty() || (!preferred.isEmpty() && random.nextDouble() < chance);
        List<String> pool = usePreferred ? preferred : regular;
        return pool.remove(pool.size() - 1); // Each pool is already shuffled.
    }

    public static Quote quote(Order order, double x, double z, MeituanConfig config) {
        BigDecimal deliveryPay = config.deliveryBaseRate.multiply(BigDecimal.valueOf(order.menu().lines().size() + 1L));
        BigDecimal base = deliveryPay, subtotal = deliveryPay;
        for (Line line : order.menu().lines()) { base = base.add(line.base()); subtotal = subtotal.add(line.subtotal()); }
        double distance = order.point().horizontal(x, z);
        boolean far = distance > config.farStart;
        BigDecimal multiplier = order.menu().hot();
        if (order.owner() != null) multiplier = multiplier.multiply(config.playerMultiplier);
        if (far) multiplier = multiplier.multiply(config.farMultiplier);
        // The event affects payment only; orderMultiplier also drives bonus highlighting.
        BigDecimal payout = subtotal.multiply(multiplier);
        if (config.eventActive) payout = payout.multiply(MeituanConfig.EVENT_MULTIPLIER);
        long reward = payout.setScale(0, RoundingMode.FLOOR).longValueExact();
        if (reward < 0 || reward > 1_000_000_000L) throw new IllegalArgumentException("Order reward exceeds safe payout limit");
        return new Quote(order, far, distance, base, subtotal, multiplier, reward);
    }

    private static UUID rank(UUID viewer, long players, long animals, UUID order) {
        return UUID.nameUUIDFromBytes((viewer + ":" + players + ":" + animals + ":" + order).getBytes(StandardCharsets.UTF_8));
    }
}
