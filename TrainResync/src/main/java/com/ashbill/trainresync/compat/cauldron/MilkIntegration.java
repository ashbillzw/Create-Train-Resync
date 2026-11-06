package com.ashbill.trainresync.compat.cauldron;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.registries.ForgeRegistries;
import net.hibiscus.naturespirit.registration.NSTags;

/** Container bridge; pre-existing vanilla filled cauldrons are left intact. */
public final class MilkIntegration {
    /** Four equal rain portions take the same expected time as vanilla's three. */
    public static boolean collectRain(Level world) {
        return world.random.nextFloat() < 0.05F * 4 / 3;
    }

    private static final ResourceLocation CAULDRON = new ResourceLocation("amendments:liquid_cauldron");
    public static MilkAccess tank(Level world, BlockPos pos) {
        BlockEntity tile = world.getBlockEntity(pos);
        return tile instanceof MilkAccess access && access.trainresync$milkAmount() >= 0 ? access : null;
    }
    public static boolean plain(BlockState state) { return state.is(Blocks.CAULDRON); }
    /** -1 leaves unrelated Create open-pipe behavior alone. */
    public static int fillAt(Level world, BlockPos pos, FluidStack stack, FluidAction action) {
        String type = CauldronContents.type(stack);
        if (type.isEmpty()) {
            var soft = MappedFluids.fromForge(stack);
            if (soft.isEmpty() || !MappedFluids.managed(CauldronContents.type(soft))) return -1;
            if (world == null || !world.isLoaded(pos)) return -1;
            boolean empty = plain(world.getBlockState(pos));
            MilkAccess access = tank(world, pos);
            if (!empty && access == null) return -1;
            int stored = empty ? 0 : access.trainresync$milkAmount();
            if (CauldronPotions.different(access, soft))
                return stack.getAmount() >= 250 ? CauldronPotions.mix(access, soft, action) : 0;
            if (stored > 0 && !MappedFluids.contents(access).isFluidEqual(stack)) return 0;
            int accepted = MilkAmounts.fill(stored, stack.getAmount());
            if (accepted == 0 || action.simulate()) return accepted;
            if (world.isClientSide) return 0;
            if (empty) {
                access = createTank(world, pos);
                if (access == null) return 0;
            }
            access.trainresync$setMappedFluid(soft, stored + accepted);
            return accepted;
        }
        return fillContentAt(world, pos, type, stack.getAmount(), action);
    }
    public static int fillContentAt(Level world, BlockPos pos, String type, int amount, FluidAction action) {
        if (world == null || !world.isLoaded(pos)) return -1;
        if (!CauldronContents.liquid(type) && !CauldronContents.shake(type)) return -1;
        boolean empty = plain(world.getBlockState(pos));
        MilkAccess access = tank(world, pos);
        if (!empty && access == null) return -1;
        int stored = empty ? 0 : access.trainresync$milkAmount();
        if (stored > 0 && !type.equals(access.trainresync$content())) return 0;
        int accepted = MilkAmounts.fill(stored, amount);
        if (accepted == 0 || action.simulate()) return accepted;
        if (world.isClientSide) return 0;
        if (empty) {
            access = createTank(world, pos);
            if (access == null) return 0;
        }
        access.trainresync$setContent(type, stored + accepted, 1000);
        return accepted;
    }
    private static MilkAccess createTank(Level world, BlockPos pos) {
        if (!world.setBlock(pos, ForgeRegistries.BLOCKS.getValue(CAULDRON).defaultBlockState(), 3)) return null;
        var access = tank(world, pos);
        if (access == null) throw new IllegalStateException("Created cauldron has no integration bridge");
        return access;
    }
    public static ItemStack item(String id) { return new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(id))); }
    public static String inputType(String id, ItemStack stack) {
        for (String flavor : CauldronContents.FLAVORS)
            if (id.equals(CauldronContents.milkshakeId(flavor))) return "shake:" + flavor;
        return switch (id) {
            case "minecraft:milk_bucket", "farmersdelight:milk_bottle", "neapolitan:milk_bottle" -> "milk";
            case "minecraft:water_bucket" -> "water";
            case "minecraft:lava_bucket" -> "lava";
            case "minecraft:potion" -> stack.getTag() != null
                && "minecraft:water".equals(stack.getTag().getString("Potion"))
                && !stack.getTag().contains("CustomPotionEffects") ? "water" : "";
            default -> "";
        };
    }
    public static ItemStack output(String type, boolean bucket) {
        if (bucket) return item("minecraft:" + type + "_bucket");
        if (CauldronContents.shake(type)) return item(CauldronContents.milkshakeId(type.substring(6)));
        if (type.equals("milk")) return item("farmersdelight:milk_bottle");
        ItemStack water = item("minecraft:potion");
        water.getOrCreateTag().putString("Potion", "minecraft:water");
        return water;
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void interact(PlayerInteractEvent.RightClickBlock event) {
        Level world = event.getLevel();
        BlockPos pos = event.getPos();
        if (event.getItemStack().is(CauldronGlow.INGREDIENTS)
                && world.getBlockEntity(pos) instanceof net.mehvahdjukaar.amendments.common.tile.LiquidCauldronBlockTile tile
                && tile.getSoftFluidTank().getFluid().is(net.mehvahdjukaar.amendments.reg.ModTags.CAN_GLOW)) {
            if (!world.isClientSide && !tile.isGlowing()) {
                tile.setGlowing(true);
                if (!event.getEntity().isCreative()) event.getItemStack().shrink(1);
                world.gameEvent(event.getEntity(), GameEvent.BLOCK_CHANGE, pos);
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(world.isClientSide));
            return;
        }
        boolean empty = plain(world.getBlockState(pos));
        MilkAccess access = tank(world, pos);
        if (!empty && access == null) return;
        ItemStack held = event.getItemStack();
        if (!empty && MappedFluids.managed(access.trainresync$content())) {
            if (!MappedFluids.containerFits(access, held)) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.FAIL);
            }
            return;
        }
        String id = String.valueOf(ForgeRegistries.ITEMS.getKey(held.getItem()));
        String input = inputType(id, held);
        String type = empty ? "" : access.trainresync$content();
        int stored = empty ? 0 : access.trainresync$milkAmount();
        int amount = CauldronContainers.containerAmount(id, input);
        boolean cheese = type.equals("milk") && held.is(NSTags.Items.CHEESE_MAKER);
        String flavor = "";
        for (String candidate : CauldronContents.FLAVORS) {
            if (id.equals(CauldronContents.iceCreamId(candidate))) flavor = candidate;
        }
        boolean shake = type.equals("milk") && !flavor.isEmpty();
        if (amount == 0 && !cheese && !shake) {
            InteractionResult result = WaterInteractions.interact(event, access);
            if (result != InteractionResult.PASS) {
                event.setCanceled(true);
                event.setCancellationResult(result);
            }
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        if (cheese || shake) {
            if (stored != 1000) return;
            if (!world.isClientSide) {
                ItemStack remainder = MilkConversions.apply(world, pos, held);
                if (remainder == null) return;
                if (remainder.isEmpty()) {
                    if (!event.getEntity().isCreative()) held.shrink(1);
                } else event.getEntity().setItemInHand(event.getHand(), ItemUtils.createFilledResult(held, event.getEntity(), remainder));
                world.gameEvent(event.getEntity(), GameEvent.BLOCK_CHANGE, pos);
            }
        } else {
            if (!CauldronContainers.canTransfer(input, type, stored, amount)) return;
            if (!world.isClientSide) {
                ItemStack result = CauldronContainers.transfer(world, pos, held, event.getEntity());
                if (result == null) return;
                event.getEntity().setItemInHand(event.getHand(), ItemUtils.createFilledResult(held, event.getEntity(), result));
            }
        }
        event.setCancellationResult(InteractionResult.sidedSuccess(world.isClientSide));
    }
}
