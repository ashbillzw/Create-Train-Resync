package com.ashbill.trainresync.meituan;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Delivery settings; older installations store these under a meituan key. */
public final class MeituanConfig {
    public int nearRadius = 500;
    public int farStart = 500;
    public int farEnd = 1000;
    public double prankItemChance = 0.001;
    public double preferredPoolChance = 0.5;
    public double farProbability = 0.25;
    public BigDecimal basePriceMultiplier = new BigDecimal("1.5");
    public boolean eventActive;
    public static final BigDecimal EVENT_MULTIPLIER = BigDecimal.valueOf(2);
    public BigDecimal deliveryBaseRate = BigDecimal.ZERO;
    public BigDecimal playerMultiplier = new BigDecimal("1.5");
    public BigDecimal farMultiplier = new BigDecimal("1.5");
    public double hotProbability = 0.10;
    public int hotMinHundredths = 150;
    public int hotMaxHundredths = 250;
    public int deliveryRadius = 10;
    public int deliveryTimeoutSeconds = 12000;
    public int animalMinSeconds = 300;
    public int animalMaxSeconds = 1800;
    public List<String> animals = List.of("美西螈", "骆驼", "猫", "鸡", "牛", "苦力怕", "驴", "末影人",
            "狐狸", "山羊", "守卫者", "马", "铁傀儡", "羊驼", "哞菇", "骡", "豹猫", "熊猫", "鹦鹉",
            "猪", "北极熊", "兔子", "绵羊", "嗅探兽", "雪傀儡", "监守者", "狼");
    public Map<String, Food> foods = new LinkedHashMap<>();

    public MeituanConfig() {
        for (String name : List.of("apple_pie", "sweet_berry_cheesecake", "chocolate_pie"))
            foods.put("farmersdelight:" + name, new Food());
    }

    public static final class Food {
        @SerializedName("价格") public BigDecimal price = new BigDecimal("16");
        @SerializedName("整蛊") public boolean prank;
        @SerializedName("好") public boolean preferred;
        @SerializedName("副餐品") public boolean sideDish;
        @SerializedName("数量乘数") public int quantityMultiplier = 1;
        @SerializedName("加成") public Map<String, BigDecimal> bonuses = new LinkedHashMap<>();
    }

    public static MeituanConfig load(Path path) throws IOException {
        if (!Files.exists(path)) return new MeituanConfig();
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (!path.getFileName().toString().equals("meituan.json")) {
                if (!root.has("meituan")) return new MeituanConfig();
                root = root.getAsJsonObject("meituan");
            }
            MeituanConfig config = new Gson().fromJson(root, MeituanConfig.class);
            if (config == null) throw new IllegalArgumentException("meituan must be an object");
            config.validate();
            return config;
        } catch (RuntimeException invalid) {
            throw new IOException("Invalid meituan configuration", invalid);
        }
    }

    public void validate() {
        require(nearRadius > 0 && farStart >= nearRadius && farEnd > farStart && farEnd <= 100000, "radii");
        require(probability(prankItemChance), "prank item chance");
        require(probability(preferredPoolChance), "preferred pool chance");
        require(probability(farProbability) && probability(hotProbability), "probabilities");
        require(positive(basePriceMultiplier), "base price multiplier");
        require(deliveryBaseRate != null && deliveryBaseRate.signum() >= 0 && deliveryBaseRate.compareTo(BigDecimal.valueOf(1_000_000)) <= 0, "delivery base rate");
        require(positive(playerMultiplier) && positive(farMultiplier), "order multipliers");
        require(hotMinHundredths >= 100 && hotMaxHundredths >= hotMinHundredths && hotMaxHundredths <= 10000, "hot range");
        require(deliveryRadius > 0 && deliveryRadius <= 1000 && deliveryTimeoutSeconds > 0, "delivery limits");
        require(animalMinSeconds > 0 && animalMaxSeconds >= animalMinSeconds && animalMaxSeconds <= 86400, "animal expiry");
        require(animals != null && !animals.isEmpty() && animals.stream().allMatch(s -> s != null && !s.isBlank()), "animals");
        require(foods != null && !foods.isEmpty(), "foods");
        foods.forEach((id, food) -> {
            require(id != null && id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+") && food != null, "food ID");
            require(positive(food.price) && food.quantityMultiplier > 0 && food.quantityMultiplier <= 64, "food price/quantity");
            require(food.bonuses != null, "food bonuses");
            food.bonuses.forEach((name, value) -> require(name != null && !name.isBlank() && positive(value), "food bonus"));
        });
        require(foods.values().stream().anyMatch(food -> !food.sideDish && !food.prank), "foods must include a non-side dish");
    }

    private static boolean probability(double value) { return Double.isFinite(value) && value >= 0 && value <= 1; }
    private static boolean positive(BigDecimal value) { return value != null && value.signum() > 0 && value.compareTo(new BigDecimal("1000000")) <= 0; }
    private static void require(boolean valid, String field) { if (!valid) throw new IllegalArgumentException("Invalid " + field); }
}
