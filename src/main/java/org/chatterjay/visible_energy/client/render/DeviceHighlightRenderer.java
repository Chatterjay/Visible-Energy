package org.chatterjay.visible_energy.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;

import org.chatterjay.visible_energy.client.DeviceHighlightCache;
import org.chatterjay.visible_energy.data.DeviceHighlightInfo;
import org.joml.Matrix4f;

import sonar.fluxnetworks.api.device.FluxDeviceType;

@OnlyIn(Dist.CLIENT)
public class DeviceHighlightRenderer {

    @SuppressWarnings("deprecation")
    private static final RenderType OVERLAY_NO_DEPTH = RenderType.create(
            "ve_overlay_no_depth",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            256, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(
                            () -> net.minecraft.client.renderer.GameRenderer.getPositionColorShader()))
                    .setTextureState(new RenderStateShard.TextureStateShard(
                            TextureAtlas.LOCATION_BLOCKS, false, false))
                    .setDepthTestState(new RenderStateShard.DepthTestStateShard("always", 519))
                    .setTransparencyState(new RenderStateShard.TransparencyStateShard(
                            "src_to_one",
                            () -> {
                                RenderSystem.enableBlend();
                                RenderSystem.blendFunc(
                                        GlStateManager.SourceFactor.SRC_ALPHA,
                                        GlStateManager.DestFactor.ONE);
                            },
                            () -> {
                                RenderSystem.disableBlend();
                                RenderSystem.defaultBlendFunc();
                            }))
                    .createCompositeState(true));

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        Font font = mc.font;

        var highlights = DeviceHighlightCache.INSTANCE.getActiveHighlights();
        if (highlights.isEmpty()) return;

        RenderSystem.disableDepthTest();

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        PoseStack.Pose poseEntry = poseStack.last();

        // Pass 1: colored fill overlay (additive blend, no depth test)
        OVERLAY_NO_DEPTH.setupRenderState();
        VertexConsumer fillConsumer = bufferSource.getBuffer(OVERLAY_NO_DEPTH);
        for (DeviceHighlightInfo info : highlights) {
            BlockPos pos = info.getPos();
            int rgb = info.getNetworkColor();
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;
            int a = info.isCurrentNetwork() ? 100 : 50;
            renderBoxFill(fillConsumer, poseEntry, pos, r, g, b, a);
        }
        bufferSource.endBatch(OVERLAY_NO_DEPTH);
        OVERLAY_NO_DEPTH.clearRenderState();

        // Pass 2: line outline
        VertexConsumer lineConsumer = bufferSource.getBuffer(RenderType.lines());
        for (DeviceHighlightInfo info : highlights) {
            BlockPos pos = info.getPos();
            int rgb = info.getNetworkColor();
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;
            int a = info.isCurrentNetwork() ? 220 : 120;
            renderBoxOutline(lineConsumer, poseEntry, pos, r, g, b, a);
        }
        bufferSource.endBatch(RenderType.lines());

        // Pass 2.5: network connection lines (PLUG → other devices only)
        Map<Integer, List<BlockPos>> plugsByNetwork = new HashMap<>();
        Map<Integer, List<BlockPos>> othersByNetwork = new HashMap<>();
        Map<Integer, Integer> networkColors = new HashMap<>();
        for (DeviceHighlightInfo info : highlights) {
            int netId = info.getNetworkId();
            networkColors.putIfAbsent(netId, info.getNetworkColor());
            if (info.getDeviceType() == FluxDeviceType.PLUG) {
                plugsByNetwork.computeIfAbsent(netId, k -> new ArrayList<>()).add(info.getPos());
            } else {
                othersByNetwork.computeIfAbsent(netId, k -> new ArrayList<>()).add(info.getPos());
            }
        }
        VertexConsumer netLineConsumer = bufferSource.getBuffer(RenderType.lines());
        for (int netId : plugsByNetwork.keySet()) {
            List<BlockPos> plugs = plugsByNetwork.get(netId);
            List<BlockPos> others = othersByNetwork.get(netId);
            if (plugs.isEmpty() || others == null || others.isEmpty()) continue;
            int rgb = networkColors.getOrDefault(netId, 0xFFFFFF);
            int lr = (rgb >> 16) & 0xFF;
            int lg = (rgb >> 8) & 0xFF;
            int lb = rgb & 0xFF;
            int la = 140;
            for (BlockPos plugPos : plugs) {
                for (BlockPos otherPos : others) {
                    line(netLineConsumer, poseEntry,
                            plugPos.getX() + 0.5f, plugPos.getY() + 0.5f, plugPos.getZ() + 0.5f,
                            otherPos.getX() + 0.5f, otherPos.getY() + 0.5f, otherPos.getZ() + 0.5f,
                            lr / 255f, lg / 255f, lb / 255f, la / 255f);
                }
            }
        }
        bufferSource.endBatch(RenderType.lines());

        // Pass 3: text labels (deduplicate by position per frame)
        Set<BlockPos> labeledPositions = new HashSet<>();
        for (DeviceHighlightInfo info : highlights) {
            if (labeledPositions.add(info.getPos())) {
                renderLabel(poseStack, font, bufferSource, camera, info);
            }
        }
        bufferSource.endBatch();

        poseStack.popPose();
        RenderSystem.enableDepthTest();
    }

    private static void renderLabel(PoseStack poseStack, Font font,
                                     MultiBufferSource.BufferSource bufferSource,
                                     Camera camera, DeviceHighlightInfo info) {
        BlockPos pos = info.getPos();
        int alpha = info.isCurrentNetwork() ? 0xFF : 0xCC;
        int rgb = getLabelColor(info);
        int textColor = (alpha << 24) | rgb;

        String line1 = info.getDeviceName() + (info.isCurrentNetwork() ? " *" : "");
        String line2 = info.getNetworkName() + " | " + info.getEnergyStatus();

        poseStack.pushPose();
        poseStack.translate(pos.getX() + 0.5, pos.getY() + 2.2, pos.getZ() + 0.5);
        poseStack.mulPose(camera.rotation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180));
        poseStack.scale(-0.025f, -0.025f, 0.025f);

        Matrix4f matrix = poseStack.last().pose();

        font.drawInBatch(
                line1,
                -font.width(line1) / 2f, 0f,
                textColor, false, matrix, bufferSource,
                Font.DisplayMode.SEE_THROUGH, 0, LightTexture.FULL_BRIGHT);

        font.drawInBatch(
                line2,
                -font.width(line2) / 2f, font.lineHeight + 2,
                textColor, false, matrix, bufferSource,
                Font.DisplayMode.SEE_THROUGH, 0, LightTexture.FULL_BRIGHT);

        poseStack.popPose();
    }

    private static int getLabelColor(DeviceHighlightInfo info) {
        FluxDeviceType type = info.getDeviceType();
        int proportion = info.getProportionPercent();

        if (type == FluxDeviceType.PLUG) {
            return 0x55FF55;
        }
        if (type == FluxDeviceType.POINT && proportion >= 50) {
            return 0xFF5555;
        }
        return 0xFFFFFF;
    }

    private static void renderBoxOutline(VertexConsumer consumer, PoseStack.Pose pose,
                                          BlockPos pos,
                                          int r, int g, int b, int a) {
        float x1 = pos.getX() + 0.001f, y1 = pos.getY() + 0.001f, z1 = pos.getZ() + 0.001f;
        float x2 = x1 + 0.998f, y2 = y1 + 0.998f, z2 = z1 + 0.998f;
        float cr = r / 255f, cg = g / 255f, cb = b / 255f, ca = a / 255f;

        line(consumer, pose, x1, y1, z1, x2, y1, z1, cr, cg, cb, ca);
        line(consumer, pose, x2, y1, z1, x2, y1, z2, cr, cg, cb, ca);
        line(consumer, pose, x2, y1, z2, x1, y1, z2, cr, cg, cb, ca);
        line(consumer, pose, x1, y1, z2, x1, y1, z1, cr, cg, cb, ca);
        line(consumer, pose, x1, y2, z1, x2, y2, z1, cr, cg, cb, ca);
        line(consumer, pose, x2, y2, z1, x2, y2, z2, cr, cg, cb, ca);
        line(consumer, pose, x2, y2, z2, x1, y2, z2, cr, cg, cb, ca);
        line(consumer, pose, x1, y2, z2, x1, y2, z1, cr, cg, cb, ca);
        line(consumer, pose, x1, y1, z1, x1, y2, z1, cr, cg, cb, ca);
        line(consumer, pose, x2, y1, z1, x2, y2, z1, cr, cg, cb, ca);
        line(consumer, pose, x2, y1, z2, x2, y2, z2, cr, cg, cb, ca);
        line(consumer, pose, x1, y1, z2, x1, y2, z2, cr, cg, cb, ca);
    }

    private static void renderBoxFill(VertexConsumer consumer, PoseStack.Pose pose,
                                       BlockPos pos, int r, int g, int b, int a) {
        float x1 = pos.getX() - 0.005f, y1 = pos.getY() - 0.005f, z1 = pos.getZ() - 0.005f;
        float x2 = pos.getX() + 1.005f, y2 = pos.getY() + 1.005f, z2 = pos.getZ() + 1.005f;

        quad4(consumer, pose, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, r, g, b, a);
        quad4(consumer, pose, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, r, g, b, a);
        quad4(consumer, pose, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, r, g, b, a);
        quad4(consumer, pose, x2, y1, z2, x2, y2, z2, x1, y2, z2, x1, y1, z2, r, g, b, a);
        quad4(consumer, pose, x1, y1, z2, x1, y2, z2, x1, y2, z1, x1, y1, z1, r, g, b, a);
        quad4(consumer, pose, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, r, g, b, a);
    }

    private static void quad4(VertexConsumer consumer, PoseStack.Pose pose,
                               float x1, float y1, float z1, float x2, float y2, float z2,
                               float x3, float y3, float z3, float x4, float y4, float z4,
                               int r, int g, int b, int a) {
        consumer.vertex(pose.pose(), x1, y1, z1).color(r, g, b, a);
        consumer.vertex(pose.pose(), x2, y2, z2).color(r, g, b, a);
        consumer.vertex(pose.pose(), x3, y3, z3).color(r, g, b, a);
        consumer.vertex(pose.pose(), x4, y4, z4).color(r, g, b, a);
    }

    private static void line(VertexConsumer consumer, PoseStack.Pose pose,
                              float x1, float y1, float z1,
                              float x2, float y2, float z2,
                              float r, float g, float b, float a) {
        consumer.vertex(pose.pose(), x1, y1, z1).color(r, g, b, a).normal(0f, 1f, 0f);
        consumer.vertex(pose.pose(), x2, y2, z2).color(r, g, b, a).normal(0f, 1f, 0f);
    }
}
