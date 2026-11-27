package com.ashbill.trainresync.client;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "trainresync", value = Dist.CLIENT)
public final class DeliveryLabels {
    private static final String LABEL = "美团送达点";
    private static List<BlockPos> points = List.of();

    public static void update(List<BlockPos> visible) { points = visible; }

    @SubscribeEvent public static void unload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) points = List.of();
    }

    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (points.isEmpty() || event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.level.dimension() != Level.OVERWORLD || mc.options.hideGui) return;
        var camera = event.getCamera();
        var eye = camera.getPosition();
        var pose = event.getPoseStack();
        var buffers = mc.renderBuffers().bufferSource();
        float left = -mc.font.width(LABEL) / 2f;
        int background = (int) (mc.options.getBackgroundOpacity(0.25f) * 255) << 24;
        for (BlockPos point : points) {
            pose.pushPose();
            pose.translate(point.getX() + 0.5 - eye.x, point.getY() + 1.8 - eye.y, point.getZ() + 0.5 - eye.z);
            pose.mulPose(camera.rotation());
            pose.scale(-0.025f, -0.025f, 0.025f);
            mc.font.drawInBatch(LABEL, left, 0, 0xffffffff, false, pose.last().pose(), buffers,
                    Font.DisplayMode.NORMAL, background, LightTexture.FULL_BRIGHT);
            pose.popPose();
        }
    }
}
