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
        var veNode = Commands.literal("ve")
                .executes(ctx -> executeScan(ctx.getSource(), VEConfig.SCAN_RADIUS.get()))
                .then(Commands.literal("scan")
                        .executes(ctx -> executeScan(ctx.getSource(), VEConfig.SCAN_RADIUS.get()))
                        .then(Commands.argument("radius",
                                IntegerArgumentType.integer(5, 128))
                                .executes(ctx -> executeScan(ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "radius")))))
                .then(Commands.literal("stop")
                        .executes(ctx -> executeStop(ctx.getSource())))
                .then(Commands.literal("duration")
                        .then(Commands.argument("seconds",
                                IntegerArgumentType.integer(1, 3600))
                                .executes(ctx -> executeDuration(ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "seconds")))));

        dispatcher.register(veNode);

        var fullNode = Commands.literal("visible_energy")
                .executes(ctx -> executeScan(ctx.getSource(), VEConfig.SCAN_RADIUS.get()))
                .then(Commands.literal("scan")
                        .executes(ctx -> executeScan(ctx.getSource(), VEConfig.SCAN_RADIUS.get()))
                        .then(Commands.argument("radius",
                                IntegerArgumentType.integer(5, 128))
                                .executes(ctx -> executeScan(ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "radius")))))
                .then(Commands.literal("stop")
                        .executes(ctx -> executeStop(ctx.getSource())))
                .then(Commands.literal("duration")
                        .then(Commands.argument("seconds",
                                IntegerArgumentType.integer(1, 3600))
                                .executes(ctx -> executeDuration(ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "seconds")))));

        dispatcher.register(fullNode);
    }

    private static int executeScan(CommandSourceStack source, int radius) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int duration = VEConfig.DISPLAY_DURATION.get();
        ScanSessionTracker.INSTANCE.startSession(player, radius, duration);

        int count = scanAndSend(player, radius);

        source.sendSuccess(
                () -> Component.literal(
                        "Scanning " + count + " Flux devices within " + radius
                                + " blocks for " + duration + "s"),
                false);

        return count;
    }

    private static int executeStop(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ScanSessionTracker.INSTANCE.stopSession(player.getUUID());
        source.sendSuccess(() -> Component.literal("Stopped scanning."), false);
        return 1;
    }

    private static int executeDuration(CommandSourceStack source, int seconds)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        VEConfig.DISPLAY_DURATION.set(seconds);
        source.sendSuccess(
                () -> Component.literal("Display duration set to " + seconds + "s"),
                false);
        return seconds;
    }

    public static int scanAndSend(ServerPlayer player, int radius) {
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
                    String energyStatus = formatDeviceEnergyStatus(stats, device);

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
                            energyStatus,
                            isCurrent));
                });
            }
        }

        PacketDistributor.sendToPlayer(player, new S2CDeviceHighlightData(results));
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

    private static String formatDeviceEnergyStatus(NetworkStatistics stats, TileFluxDevice device) {
        if (stats == null) return "---";

        long deviceChange = Math.abs(device.getTransferChange());
        FluxDeviceType type = device.getDeviceType();
        StringBuilder sb = new StringBuilder();

        if (type == FluxDeviceType.PLUG) {
            sb.append("← ");
            sb.append(compactEnergy(deviceChange)).append("/t");
            if (stats.energyInput > 0) {
                int pct = (int) (deviceChange * 100L / stats.energyInput);
                sb.append(" (").append(Math.min(pct, 100)).append("%)");
            }
        } else if (type == FluxDeviceType.POINT) {
            sb.append(compactEnergy(deviceChange)).append("/t →");
            if (stats.energyOutput > 0) {
                int pct = (int) (deviceChange * 100L / stats.energyOutput);
                sb.append(" (").append(Math.min(pct, 100)).append("%)");
            }
        } else if (type == FluxDeviceType.STORAGE) {
            long change = device.getTransferChange();
            if (change > 0) {
                sb.append("← ").append(compactEnergy(change)).append("/t in");
            } else if (change < 0) {
                sb.append(compactEnergy(-change)).append("/t → out");
            } else {
                sb.append("0 FE/t");
            }
        } else {
            sb.append(compactEnergy(deviceChange)).append("/t");
        }

        if (stats.totalEnergy > 0) {
            sb.append(" | ").append(compactEnergy(stats.totalEnergy));
        }

        return sb.toString();
    }

    private static String compactEnergy(long value) {
        if (value <= 0) return "0 FE";
        if (value >= 1_000_000_000_000L) {
            return String.format("%.1f TFE", value / 1_000_000_000_000f);
        } else if (value >= 1_000_000_000L) {
            return String.format("%.1f GFE", value / 1_000_000_000f);
        } else if (value >= 1_000_000L) {
            return String.format("%.1f MFE", value / 1_000_000f);
        } else if (value >= 1_000L) {
            return String.format("%.1f kFE", value / 1_000f);
        }
        return String.format("%d FE", value);
    }

    private static int resolveNetworkColor(int colorIndex) {
        try {
            EnumNetworkColor[] colors = EnumNetworkColor.VALUES;
            if (colorIndex >= 0 && colorIndex < colors.length) {
                return colors[colorIndex].getRGB();
            }
        } catch (Exception ignored) {
        }
        float hue = (colorIndex * 0.618033988749895f) % 1f;
        int r = (int) (Math.abs(Math.cos(hue * Math.PI * 2)) * 192 + 32);
        int g = (int) (Math.abs(Math.cos((hue + 0.333f) * Math.PI * 2)) * 192 + 32);
        int b = (int) (Math.abs(Math.cos((hue + 0.667f) * Math.PI * 2)) * 192 + 32);
        return (r << 16) | (g << 8) | b;
    }
}
