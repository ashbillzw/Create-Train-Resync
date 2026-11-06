package com.ashbill.trainresync.meituan;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.ashbill.trainresync.meituan.DeliveryBook.*;

@Mod.EventBusSubscriber(modid = "trainresync")
public final class MeituanService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<MinecraftServer, MeituanService> SERVICES = new IdentityHashMap<>();
    private static final String PREFIX = "【美团】";
    private static final String ERROR = "暂时无法完成操作，请稍后重试。";
    private static final String CHANGED = "选单发生变化，请重新选择。";
    private static final String[] COINS = {"sun", "crown", "cog", "sprocket", "bevel", "spur"};
    private static final int[] DENOMINATIONS = {4096, 512, 64, 16, 8, 1};

    private final MinecraftServer server;
    private final ServerLevel level;
    private final MeituanConfig config;
    private final DeliveryData data;
    private final DeliveryBook book;
    private final Random random = new Random();
    private final Map<UUID, Preview> previews = new HashMap<>();
    private final LinkedHashMap<UUID, FillJob> jobs = new LinkedHashMap<>();
    private long lastNanos = System.nanoTime();
    private int ticks;

    private MeituanService(MinecraftServer server, MeituanConfig config) {
        this.server = server;
        this.level = server.overworld();
        this.config = config;
        data = level.getDataStorage().computeIfAbsent(DeliveryData::load, DeliveryData::new, "trainresync_meituan");
        if (data.readError != null) throw new IllegalStateException("Meituan save is unreadable; preserved without overwrite", data.readError);
        book = data.book;
        for (String id : config.foods.keySet()) item(id);
        for (String coin : COINS) item("numismatics:" + coin);
    }

    @SubscribeEvent public static void start(ServerStartedEvent event) {
        try {
            MeituanConfig config = MeituanConfig.load(FMLPaths.CONFIGDIR.get().resolve("trainresync.json"));
            if (config.enabled) SERVICES.put(event.getServer(), new MeituanService(event.getServer(), config));
        } catch (Exception error) { LOGGER.error("Meituan disabled: configuration, required items or saved data unavailable", error); }
    }

    @SubscribeEvent public static void stopping(ServerStoppingEvent event) {
        MeituanService service = SERVICES.get(event.getServer());
        if (service != null) { service.advanceClock(); service.data.setDirty(); }
    }

    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { SERVICES.remove(event.getServer()); }

    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MeituanService service = SERVICES.get(event.getServer());
        if (service != null) service.tick();
    }

    @SubscribeEvent public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("meituan")
                .requires(source -> SERVICES.containsKey(source.getServer()))
                .executes(context -> run(context.getSource(), "jiedan", null))
                .then(Commands.literal("diancan").executes(context -> run(context.getSource(), "diancan", null)))
                .then(Commands.literal("quxiaodiancan").executes(context -> run(context.getSource(), "quxiaodiancan", null)))
                .then(Commands.literal("songda").executes(context -> run(context.getSource(), "songda", null)))
                .then(Commands.literal("jiedan").executes(context -> run(context.getSource(), "jiedan", null))
                        .then(Commands.argument("id", StringArgumentType.greedyString())
                                .executes(context -> run(context.getSource(), "jiedan", StringArgumentType.getString(context, "id"))))));
    }

    private static int run(CommandSourceStack source, String action, String id) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        MeituanService service = SERVICES.get(source.getServer());
        if (service == null) { message(player, ERROR); return 0; }
        if (!player.level().dimension().equals(Level.OVERWORLD)) { message(player, "只能在主世界送餐！"); return 0; }
        try {
            service.advanceClock();
            service.expireActive();
            switch (action) {
                case "diancan" -> service.request(player);
                case "quxiaodiancan" -> service.cancel(player);
                case "songda" -> service.deliver(player);
                default -> service.listOrAccept(player, id);
            }
            service.data.setDirty();
            return 1;
        } catch (RuntimeException error) {
            LOGGER.error("Meituan command failed: {} for {}", action, player.getUUID(), error);
            message(player, ERROR);
            return 0;
        }
    }

    private void advanceClock() {
        long now = System.nanoTime();
        long millis = (now - lastNanos) / 1_000_000L;
        if (millis > 0) { book.runningMillis += millis; lastNanos += millis * 1_000_000L; }
    }

    private void tick() {
        advanceClock();
        if (++ticks % 20 == 0) { expireActive(); data.setDirty(); }
        if (ticks % 10 == 0) markers();
        // A bounded round-robin step; chunk futures complete through Minecraft's own chunk pipeline.
        if (!jobs.isEmpty()) {
            UUID viewer = jobs.keySet().iterator().next();
            FillJob job = jobs.remove(viewer);
            try {
                if (!job.step()) jobs.put(viewer, job);
                else {
                    ServerPlayer player = server.getPlayerList().getPlayer(viewer);
                    if (player != null && player.level() == level && book.courier(viewer).active == null) show(player);
                }
            } catch (RuntimeException error) {
                LOGGER.error("Meituan terrain search failed for {}", viewer, error);
                notifyOnline(viewer, ERROR);
            }
        }
    }

    private void request(ServerPlayer player) {
        BlockPos origin = player.blockPosition();
        BlockPos chosen = available(origin) ? origin : null;
        for (int dy = 0; chosen == null && dy <= 1; dy++)
            for (int dx = -1; chosen == null && dx <= 1; dx++)
                for (int dz = -1; chosen == null && dz <= 1; dz++) {
                    if (dy == 0 && dx == 0 && dz == 0) continue;
                    BlockPos candidate = origin.offset(dx, dy, dz);
                    if (available(candidate)) chosen = candidate;
                }
        if (chosen == null) { message(player, "外卖箱放置空间不足！"); return; }
        Point point = new Point(chosen.getX(), chosen.getY(), chosen.getZ());
        boolean fresh = !book.requests.containsKey(player.getUUID());
        book.request(player.getUUID(), player.getGameProfile().getName(), point);
        message(player, ownerPrefix(player.getUUID()) + "已成功下单，定位 " + xyz(point) + "，等待接单。");
        MutableComponent cancel = Component.literal("/meituan quxiaodiancan").withStyle(style -> style
                .withColor(ChatFormatting.YELLOW).withClickEvent(new net.minecraft.network.chat.ClickEvent(
                        net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/meituan quxiaodiancan")));
        player.sendSystemMessage(cancel);
        if (fresh) broadcast(player);
    }

    private boolean available(BlockPos point) {
        return !level.isOutsideBuildHeight(point) && level.getWorldBorder().isWithinBounds(point)
                && level.getBlockState(point).isAir() && level.getFluidState(point).isEmpty();
    }

    private void broadcast(ServerPlayer owner) {
        long today = LocalDate.now(ZoneId.of("Asia/Shanghai")).toEpochDay();
        long start = LocalDate.ofEpochDay(today - 6).atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID id = player.getUUID();
            if (id.equals(owner.getUUID())) continue;
            Courier courier = book.couriers.get(id);
            boolean recentSuccess = courier != null && courier.completed.stream().anyMatch(time -> time >= start);
            if (book.lastBroadcastDay.getOrDefault(id, Long.MIN_VALUE) < today - 6 || recentSuccess) {
                message(player, "收到" + owner.getGameProfile().getName() + "的点餐订单！接单：/meituan jiedan。");
                book.lastBroadcastDay.put(id, today);
            }
        }
    }

    private String ownerPrefix(UUID owner) {
        long count = book.couriers.values().stream().filter(c -> c.active != null && owner.equals(c.active.order().owner())).count();
        return count > 0 ? "（" + count + " 单 送货中）" : "";
    }

    private void cancel(ServerPlayer player) {
        message(player, book.cancel(player.getUUID()) ? "已取消点餐。" : ownerPrefix(player.getUUID()) + "没有可以取消的点餐。");
    }

    private void listOrAccept(ServerPlayer player, String rawId) {
        UUID viewer = player.getUUID();
        Courier courier = book.courier(viewer);
        if (courier.active != null) { activeMessage(player, "当前还有未完成订单！", courier.active); return; }
        Preview previous = previews.get(viewer);
        int index = -1;
        if (rawId != null) try { index = Integer.parseInt(rawId.trim()) - 1; } catch (NumberFormatException ignored) { }
        boolean selecting = previous != null && index >= 0 && index < previous.rows().size();
        boolean expired = book.expireAnimals(viewer, System.currentTimeMillis());
        if (jobs.containsKey(viewer)) return;
        if (courier.animals.size() < 5) {
            if (selecting && (expired || previous.animals() != courier.animalRevision || previous.players() != book.playerRevision))
                message(player, CHANGED);
            previews.remove(viewer);
            jobs.put(viewer, new FillJob(viewer, player.getX(), player.getZ()));
            return;
        }
        Preview current = preview(player);
        if (!selecting) { show(player, current); return; }
        if (!DeliveryBook.sameSelection(previous, current)) { message(player, CHANGED); show(player, current); return; }
        Quote selected = current.rows().get(index);
        if (!book.accept(viewer, selected, config)) { message(player, CHANGED); show(player); return; }
        previews.remove(viewer);
        activeMessage(player, "已接单！", courier.active);
        if (selected.order().owner() != null)
            notifyOnline(selected.order().owner(), "订单已被" + player.getGameProfile().getName() + "接单！");
    }

    private Preview preview(ServerPlayer player) {
        return book.preview(player.getUUID(), player.getX(), player.getZ(), System.currentTimeMillis(), config, random);
    }

    private void show(ServerPlayer player) { show(player, preview(player)); }

    private void show(ServerPlayer player, Preview preview) {
        previews.put(player.getUUID(), preview);
        data.setDirty();
        if (preview.rows().isEmpty()) { message(player, "暂无可接订单。"); return; }
        message(player, "当前可接取订单如下！用/meituan jiedan <id>接单！");
        for (int i = 0; i < preview.rows().size(); i++) {
            Quote quote = preview.rows().get(i);
            Order order = quote.order();
            MutableComponent row = Component.literal((i + 1) + " | " + order.name() + " | " + number(quote.base()) + "元");
            // Item-specific bonuses stay attached to their food; they must not look like whole-order multipliers.
            if (quote.base().compareTo(quote.subtotal()) != 0) row.append(" → " + number(quote.subtotal()) + "元");
            if (order.menu().hot().compareTo(BigDecimal.ONE) != 0) row.append(" ×" + number(order.menu().hot()) + "火爆");
            if (quote.far()) row.append(" ×" + number(config.farMultiplier) + "远单");
            if (order.owner() != null) row.append(" ×" + number(config.playerMultiplier) + "玩家单");
            MutableComponent reward = Component.literal(" → " + quote.reward() + "元");
            if (quote.subtotal().multiply(quote.orderMultiplier()).compareTo(quote.base().multiply(BigDecimal.valueOf(2))) > 0)
                reward.withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD);
            row.append(reward).append(" | ").append(contents(order.menu(), true))
                    .append(" | " + Math.round(quote.distance()) + "米");
            String command = "/meituan jiedan " + (i + 1);
            row.withStyle(style -> style.withClickEvent(new net.minecraft.network.chat.ClickEvent(
                    net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, command)));
            player.sendSystemMessage(row);
        }
    }

    private static MutableComponent contents(Menu menu, boolean bonuses) {
        MutableComponent text = Component.empty();
        for (Line line : menu.lines()) {
            if (!text.getString().isEmpty()) text.append(" ");
            text.append(new ItemStack(item(line.item())).getHoverName()).append(" ×" + line.count());
            if (bonuses) line.bonuses().forEach((name, value) -> {
                if (value.compareTo(BigDecimal.ONE) != 0) text.append("（" + name + "×" + number(value) + "）");
            });
        }
        return text;
    }

    private static void activeMessage(ServerPlayer player, String prefix, Active active) {
        player.sendSystemMessage(Component.literal(PREFIX + prefix + "餐品 ").append(contents(active.order().menu(), false))
                .append("，定位 " + xyz(active.order().point())));
    }

    private void deliver(ServerPlayer player) {
        Courier courier = book.courier(player.getUUID());
        Active active = courier.active;
        if (active == null) { message(player, "当前没有配送中的订单。"); return; }
        Point point = active.order().point();
        if (point.squaredDistance(player.getX(), player.getY(), player.getZ()) > (double) config.deliveryRadius * config.deliveryRadius) {
            message(player, "距离送达定位过远！（送达定位：" + xyz(point) + "）"); return;
        }
        List<ItemStack> inventory = new ArrayList<>(player.getInventory().items);
        inventory.addAll(player.getInventory().offhand);
        List<ItemStack> foods = new ArrayList<>();
        List<DeliveryInventory.Stock> stock = new ArrayList<>();
        for (ItemStack stack : inventory) {
            String id = ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
            boolean plain = !stack.isEmpty() && ItemStack.isSameItemSameTags(stack, new ItemStack(stack.getItem()));
            stock.add(new DeliveryInventory.Stock(id, stack.getCount(), plain));
        }
        Map<String, Integer> needed = new LinkedHashMap<>();
        for (Line line : active.order().menu().lines()) {
            needed.merge(line.item(), line.count(), Math::addExact);
            split(foods, item(line.item()), line.count());
        }
        int[] removal = DeliveryInventory.plan(stock, needed);
        if (removal == null) { message(player, "背包中没有足够的餐品！"); return; }
        List<ItemStack> money = payout(active.reward()); // Resolve all items before changing any inventory slot.
        for (int i = 0; i < removal.length; i++) inventory.get(i).shrink(removal[i]);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        // Commands and expiration run on the server thread. Consume the active state before side effects.
        courier.active = null;
        courier.completed.add(System.currentTimeMillis());
        data.setDirty();
        if (active.order().owner() != null) {
            deliverFood(point, foods);
            notifyOnline(active.order().owner(), "您的外卖已由" + player.getGameProfile().getName() + "送达！（外卖员："
                    + player.getGameProfile().getName() + "，位置：" + xyz(point) + "）");
        } else level.sendParticles(player, ParticleTypes.HAPPY_VILLAGER, true,
                point.x() + 0.5, point.y() + 1, point.z() + 0.5, 25, 0.5, 0.5, 0.5, 0.05);
        for (ItemStack stack : money) drop(stack, player.getX(), player.getY(), player.getZ());
        message(player, "成功送达！报酬" + active.reward() + "元已发放。");
    }

    private void deliverFood(Point point, List<ItemStack> foods) {
        BlockPos pos = new BlockPos(point.x(), point.y(), point.z());
        ChestBlockEntity chest = null;
        if (available(pos) && level.setBlock(pos, Blocks.CHEST.defaultBlockState(), 3)
                && level.getBlockEntity(pos) instanceof ChestBlockEntity created) chest = created;
        int slot = 0;
        for (ItemStack stack : foods) {
            if (chest != null && slot < chest.getContainerSize()) chest.setItem(slot++, stack);
            else drop(stack, point.x() + 0.5, point.y() + 0.2, point.z() + 0.5);
        }
        if (chest != null) chest.setChanged();
    }

    private static List<ItemStack> payout(long amount) {
        List<ItemStack> result = new ArrayList<>();
        for (int i = 0; i < COINS.length; i++) {
            long count = amount / DENOMINATIONS[i];
            amount %= DENOMINATIONS[i];
            split(result, item("numismatics:" + COINS[i]), Math.toIntExact(count));
        }
        return result;
    }

    private static void split(List<ItemStack> result, Item item, int count) {
        int limit = new ItemStack(item).getMaxStackSize();
        while (count > 0) { int take = Math.min(limit, count); result.add(new ItemStack(item, take)); count -= take; }
    }

    private void drop(ItemStack stack, double x, double y, double z) {
        ItemEntity entity = new ItemEntity(level, x, y, z, stack);
        entity.setDefaultPickUpDelay();
        if (!level.addFreshEntity(entity)) LOGGER.error("Meituan item entity rejected at {},{},{}: {}", x, y, z, stack);
    }

    private void expireActive() {
        for (Map.Entry<UUID, Courier> entry : book.couriers.entrySet()) {
            Active active = entry.getValue().active;
            if (active == null || active.deadline() > book.runningMillis) continue;
            entry.getValue().active = null;
            data.setDirty();
            notifyOnline(entry.getKey(), "当前订单已超时失败。");
            if (active.order().owner() != null) {
                String name = server.getProfileCache() == null ? entry.getKey().toString()
                        : server.getProfileCache().get(entry.getKey()).map(com.mojang.authlib.GameProfile::getName).orElse(entry.getKey().toString());
                notifyOnline(active.order().owner(), "您的外卖送货失败！（外卖员：" + name + "）");
            }
        }
    }

    private void markers() {
        for (Request request : book.requests.values()) marker(request.owner(), request.point());
        for (Map.Entry<UUID, Courier> entry : book.couriers.entrySet()) {
            Active active = entry.getValue().active;
            if (active == null) continue;
            marker(entry.getKey(), active.order().point());
            if (active.order().owner() != null) marker(active.order().owner(), active.order().point());
        }
    }

    private void marker(UUID viewer, Point point) {
        ServerPlayer player = server.getPlayerList().getPlayer(viewer);
        if (player == null || player.level() != level || point.squaredDistance(player.getX(), player.getY(), player.getZ()) > 128 * 128) return;
        level.sendParticles(player, ParticleTypes.END_ROD, true, point.x() + 0.5, point.y() + 1.2, point.z() + 0.5,
                3, 0.15, 0.25, 0.15, 0.005);
    }

    private void notifyOnline(UUID target, String text) {
        ServerPlayer player = server.getPlayerList().getPlayer(target);
        if (player != null) message(player, text);
        else LOGGER.info("Meituan offline notification not delivered (Inbox integration deferred), player={}, message={}", target, text);
    }

    private static void message(ServerPlayer player, String text) { player.sendSystemMessage(Component.literal(PREFIX + text)); }
    private static String xyz(Point point) { return point.x() + " " + point.y() + " " + point.z(); }
    private static String number(BigDecimal value) { return value.stripTrailingZeros().toPlainString(); }
    private static Item item(String id) {
        ResourceLocation key = new ResourceLocation(id);
        if (!ForgeRegistries.ITEMS.containsKey(key)) throw new IllegalArgumentException("Unknown Meituan item: " + id);
        Item item = ForgeRegistries.ITEMS.getValue(key);
        if (item == null || item == net.minecraft.world.item.Items.AIR) throw new IllegalArgumentException("Invalid Meituan item: " + id);
        return item;
    }

    private final class FillJob {
        final UUID viewer;
        final double centerX, centerZ;
        Search search;
        FillJob(UUID viewer, double x, double z) { this.viewer = viewer; centerX = x; centerZ = z; }
        boolean step() {
            if (book.courier(viewer).animals.size() >= 5) return true;
            if (search == null) search = new Search(centerX, centerZ, random.nextDouble() < config.farProbability);
            Point point = search.step();
            if (point == null) return false;
            long expires = System.currentTimeMillis() + 1000L * (config.animalMinSeconds + random.nextInt(config.animalMaxSeconds - config.animalMinSeconds + 1));
            String name = config.animals.get(random.nextInt(config.animals.size())) + "-" + String.format(Locale.ROOT, "%04d", random.nextInt(10000));
            book.addAnimal(viewer, new Order(UUID.randomUUID(), null, name, point, menu(config, random), expires));
            data.setDirty();
            search = null;
            return book.courier(viewer).animals.size() >= 5;
        }
    }

    private final class Search {
        final double centerX, centerZ;
        final boolean far;
        int attempts, x, z;
        CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> pending;
        Search(double x, double z, boolean far) { centerX = x; centerZ = z; this.far = far; }
        Point step() {
            if (pending == null) {
                attempts++;
                double min = far ? config.farStart : 0, max = far ? config.farEnd : config.nearRadius;
                double radius = Math.sqrt(min * min + random.nextDouble() * (max * max - min * min));
                double angle = random.nextDouble() * Math.PI * 2;
                x = (int) Math.floor(Math.max(level.getWorldBorder().getMinX() + 1,
                        Math.min(level.getWorldBorder().getMaxX() - 1, centerX + Math.cos(angle) * radius)));
                z = (int) Math.floor(Math.max(level.getWorldBorder().getMinZ() + 1,
                        Math.min(level.getWorldBorder().getMaxZ() - 1, centerZ + Math.sin(angle) * radius)));
                var source = level.getChunkSource();
                int chunkX = x >> 4, chunkZ = z >> 4;
                // This public API schedules its work on the main thread when called off-thread.
                // Its main-thread branch managedBlocks; do not invoke that branch from a tick.
                pending = CompletableFuture.supplyAsync(() -> source.getChunkFuture(chunkX, chunkZ, ChunkStatus.FULL, true))
                        .thenCompose(future -> future);
                return null;
            }
            if (!pending.isDone()) return null;
            ChunkAccess chunk = pending.join().left().orElseThrow(() -> new IllegalStateException("Chunk unavailable during Meituan search"));
            pending = null;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, level.getMaxBuildHeight() - 1, z);
            for (int y = level.getMaxBuildHeight() - 1; y >= level.getMinBuildHeight(); y--) {
                pos.setY(y);
                BlockState state = chunk.getBlockState(pos);
                if (state.isAir() && state.getFluidState().isEmpty()) continue;
                boolean allowed = attempts <= 32 ? state.isSolid() : attempts <= 64 ? state.getFluidState().isEmpty() : true;
                return allowed ? new Point(x, Math.min(y + 1, level.getMaxBuildHeight() - 1), z) : null;
            }
            return new Point(x, level.getMinBuildHeight(), z);
        }
    }
}
