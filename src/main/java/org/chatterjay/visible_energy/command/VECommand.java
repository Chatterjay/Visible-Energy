package org.chatterjay.visible_energy.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import net.neoforged.neoforge.network.PacketDistributor;

import org.chatterjay.visible_energy.config.VEConfig;
import org.chatterjay.visible_energy.network.payloads.S2CDeviceHighlightData;
import org.chatterjay.visible_energy.network.payloads.S2CDeviceHighlightData.DeviceInfo;

import sonar.fluxnetworks.api.FluxConstants;
import sonar.fluxnetworks.api.device.FluxDeviceType;
import sonar.fluxnetworks.api.gui.EnumNetworkColor;
import sonar.fluxnetworks.common.connection.FluxNetwork;
import sonar.fluxnetworks.common.connection.NetworkStatistics;
import sonar.fluxnetworks.common.data.FluxPlayerData;
import sonar.fluxnetworks.common.device.TileFluxDevice;
import sonar.fluxnetworks.register.DataAttachments;

public class VECommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var cmd = Commands.literal("visible_energy")
                .executes(ctx -> executeScan(ctx.getSource(), VEConfig.SCAN_RADIUS.get()))
                .then(Commands.literal("scan")
                        .executes(ctx -> executeScan(ctx.getSource(), VEConfig.SCAN_RADIUS.get()))
                        .then(Commands.argument("radius",
                                IntegerArgumentType.integer(5, 128))
                                .executes(ctx -> executeScan(ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "radius")))));

        dispatcher.register(cmd);

        var alias = Commands.literal("ve")
                .executes(ctx -> executeScan(ctx.getSource(), VEConfig.SCAN_RADIUS.get()))
                .then(Commands.literal("scan")
                        .executes(ctx -> executeScan(ctx.getSource(), VEConfig.SCAN_RADIUS.get()))
                        .then(Commands.argument("radius",
                                IntegerArgumentType.integer(5, 128))
                                .executes(ctx -> executeScan(ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "radius")))));

        dispatcher.register(alias);
    }

    private static int executeScan(CommandSourceStack source, int radius) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        BlockPos center = player.blockPosition();

        final int wirelessNetworkId = resolveWirelessNetwork(player);

        int chunkRadius = (int) Math.ceil(radius / 16.0);
        List<DeviceInfo> results = new ArrayList<>();

        for (int cx = -chunkRadius; cx <= chunkRadius; cx++) {
            for (int cz = -chunkRadius; cz <= chunkRadius; cz++) {
                int chunkX = (center.getX() >> 4) + cx;
                int chunkZ = (center.getZ() >> 4) + cz;

                if (!level.hasChunk(chunkX, chunkZ)) continue;

                LevelChunk chunk = level.getChunk(chunkX, chunkZ);

                chunk.getBlockEntities().forEach((pos, be) -> {
                    if (!(be instanceof TileFluxDevice device)) return;
                    if (pos.distSqr(center) > (long) radius * radius) return;

                    FluxNetwork network = device.getNetwork();
                    if (network == null || !network.isValid()) return;

                    NetworkStatistics stats = network.getStatistics();
                    float usagePercent = 0f;
                    if (stats != null && stats.totalEnergy > 0) {
                        usagePercent = (float) stats.totalBuffer / stats.totalEnergy * 100f;
                    }

                    boolean isCurrent;
                    if (wirelessNetworkId != FluxConstants.INVALID_NETWORK_ID) {
                        isCurrent = network.getNetworkID() == wirelessNetworkId;
                    } else if (device.getDeviceType() == FluxDeviceType.CONTROLLER
                            && network.canPlayerAccess(player)) {
                        isCurrent = true;
                    } else {
                        isCurrent = false;
                    }

                    String customName = device.getCustomName();
                    if (customName == null || customName.isEmpty()) {
                        customName = device.getDeviceType().name();
                    }

                    results.add(new DeviceInfo(
                            pos,
                            device.getDeviceType().ordinal(),
                            customName,
                            network.getNetworkName(),
                            resolveNetworkColor(network.getNetworkColor()),
                            usagePercent,
                            isCurrent));
                });
            }
        }

        PacketDistributor.sendToPlayer(player, new S2CDeviceHighlightData(results));

        source.sendSuccess(
                () -> Component.literal(
                        "Found " + results.size() + " Flux devices within " + radius + " blocks"),
                false);

        return results.size();
    }

    private static int resolveWirelessNetwork(ServerPlayer player) {
        try {
            FluxPlayerData data = player.getData(DataAttachments.PLAYER_DATA.get());
            if (data != null) return data.getWirelessNetwork();
        } catch (Exception ignored) {
        }
        return FluxConstants.INVALID_NETWORK_ID;
    }

    private static int resolveNetworkColor(int colorIndex) {
        try {
            EnumNetworkColor[] colors = EnumNetworkColor.VALUES;
            if (colorIndex >= 0 && colorIndex < colors.length) {
                return colors[colorIndex].getRGB();
            }
        } catch (Exception ignored) {
        }
        // Fallback: generate a distinct color from the index
        float hue = (colorIndex * 0.618033988749895f) % 1f;
        int r = (int) (Math.abs(Math.cos(hue * Math.PI * 2)) * 192 + 32);
        int g = (int) (Math.abs(Math.cos((hue + 0.333f) * Math.PI * 2)) * 192 + 32);
        int b = (int) (Math.abs(Math.cos((hue + 0.667f) * Math.PI * 2)) * 192 + 32);
        return (r << 16) | (g << 8) | b;
    }
}
