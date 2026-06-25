package org.chatterjay.visible_energy.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

import net.neoforged.neoforge.network.PacketDistributor;

import org.chatterjay.visible_energy.config.VEConfig;
import org.chatterjay.visible_energy.network.payloads.S2CDeviceHighlightData;
import org.chatterjay.visible_energy.network.payloads.S2CDeviceHighlightData.DeviceInfo;

import sonar.fluxnetworks.api.FluxConstants;
import sonar.fluxnetworks.api.device.FluxDeviceType;
import sonar.fluxnetworks.api.device.IFluxDevice;
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

        reportCrossDimension(player, radius);

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

    private static void reportCrossDimension(ServerPlayer player, int radius) {
        ServerLevel level = player.serverLevel();
        ResourceKey<Level> playerDim = level.dimension();
        BlockPos center = player.blockPosition();
        int chunkRadius = (int) Math.ceil(radius / 16.0);

        Set<Integer> seenNetworks = new HashSet<>();
        List<Component> alerts = new ArrayList<>();

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
                    if (!seenNetworks.add(network.getNetworkID())) return;

                    for (IFluxDevice conn : network.getAllConnections()) {
                        GlobalPos gp = conn.getGlobalPos();
                        if (gp.dimension().equals(playerDim)) continue;

                        BlockPos p = gp.pos();
                        ServerLevel dimLevel = player.server.getLevel(gp.dimension());
                        boolean loaded = dimLevel != null && dimLevel.hasChunk(p.getX() >> 4, p.getZ() >> 4);
                        String dimName = gp.dimension().location().toString();

                        Component line = Component.literal("  ")
                                .append(Component.literal(conn.getDeviceType().name())
                                        .withStyle(ChatFormatting.GOLD))
                                .append(Component.literal(" at ").withStyle(ChatFormatting.GRAY))
                                .append(Component.literal(p.getX() + "," + p.getY() + "," + p.getZ())
                                        .withStyle(ChatFormatting.AQUA))
                                .append(Component.literal(" in ").withStyle(ChatFormatting.GRAY))
                                .append(Component.literal(dimName)
                                        .withStyle(ChatFormatting.GREEN))
                                .append(Component.literal(" [")
                                        .withStyle(ChatFormatting.GRAY))
                                .append(Component.literal(loaded ? "loaded" : "unloaded")
                                        .withStyle(loaded ? ChatFormatting.GREEN : ChatFormatting.RED))
                                .append(Component.literal("]")
                                        .withStyle(ChatFormatting.GRAY));
                        alerts.add(line);
                    }
                });
            }
        }

        if (!alerts.isEmpty()) {
            player.sendSystemMessage(Component.literal(
                    "Cross-dimension devices on found networks:").withStyle(ChatFormatting.YELLOW));
            for (Component alert : alerts) {
                player.sendSystemMessage(alert);
            }
        }
    }

    public static int scanAndSend(ServerPlayer player, int radius) {
        ServerLevel level = player.serverLevel();
        BlockPos center = player.blockPosition();
        final int wirelessNetworkId = resolveWirelessNetwork(player);

        int chunkRadius = (int) Math.ceil(radius / 16.0);

        // First pass: collect raw device data
        record RawDevice(TileFluxDevice device, BlockPos pos, FluxNetwork network,
                         boolean isCurrent) {}
        List<RawDevice> rawDevices = new ArrayList<>();

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

                    boolean isCurrent;
                    if (wirelessNetworkId != FluxConstants.INVALID_NETWORK_ID) {
                        isCurrent = network.getNetworkID() == wirelessNetworkId;
                    } else if (device.getDeviceType() == FluxDeviceType.CONTROLLER
                            && network.canPlayerAccess(player)) {
                        isCurrent = true;
                    } else {
                        isCurrent = false;
                    }

                    rawDevices.add(new RawDevice(device, pos.immutable(), network, isCurrent));
                });
            }
        }

        // Compute per-network totals
        // Map: networkId -> [totalInput, totalOutput]
        Map<Integer, long[]> networkTotals = new HashMap<>();
        for (RawDevice rd : rawDevices) {
            FluxDeviceType type = rd.device.getDeviceType();
            long change = rd.device.getTransferChange();
            int netId = rd.network.getNetworkID();
            long[] totals = networkTotals.computeIfAbsent(netId, k -> new long[2]);
            if (type == FluxDeviceType.PLUG && change > 0) {
                totals[0] += change;
            } else if (type == FluxDeviceType.POINT && change < 0) {
                totals[1] += -change;
            }
        }

        // Second pass: build DeviceInfo with per-network proportions
        List<DeviceInfo> results = new ArrayList<>();
        for (RawDevice rd : rawDevices) {
            TileFluxDevice device = rd.device;
            FluxNetwork network = rd.network;
            long[] totals = networkTotals.getOrDefault(network.getNetworkID(), new long[2]);
            int proportion = computeProportionFromTotals(device, totals[0], totals[1]);
            NetworkStatistics stats = network.getStatistics();
            String energyStatus = formatDeviceEnergyStatus(stats, device, proportion);

            String customName = device.getCustomName();
            if (customName == null || customName.isEmpty()) {
                customName = device.getDeviceType().name();
            }

            results.add(new DeviceInfo(
                    rd.pos,
                    device.getDeviceType().ordinal(),
                    customName,
                    network.getNetworkName(),
                    resolveNetworkColor(network.getNetworkColor(), network.getNetworkName()),
                    energyStatus,
                    rd.isCurrent,
                    proportion,
                    network.getNetworkID()));
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

    private static int computeProportionFromTotals(TileFluxDevice device,
                                                    long totalInput, long totalOutput) {
        long change = device.getTransferChange();
        FluxDeviceType type = device.getDeviceType();
        if (type == FluxDeviceType.PLUG && totalInput > 0 && change > 0) {
            return Math.min((int) (change * 100L / totalInput), 100);
        } else if (type == FluxDeviceType.POINT && totalOutput > 0 && change < 0) {
            return Math.min((int) (-change * 100L / totalOutput), 100);
        }
        return 0;
    }

    private static String formatDeviceEnergyStatus(NetworkStatistics stats, TileFluxDevice device,
                                                    int proportion) {
        if (stats == null) return "---";

        long deviceChange = Math.abs(device.getTransferChange());
        FluxDeviceType type = device.getDeviceType();
        StringBuilder sb = new StringBuilder();

        if (type == FluxDeviceType.PLUG) {
            sb.append("↑ ");
            sb.append(compactEnergy(deviceChange)).append("/t");
            if (proportion > 0) sb.append(" (").append(proportion).append("%)");
        } else if (type == FluxDeviceType.POINT) {
            sb.append(compactEnergy(deviceChange)).append("/t ↓");
            if (proportion > 0) sb.append(" (").append(proportion).append("%)");
        } else if (type == FluxDeviceType.STORAGE) {
            long change = device.getTransferChange();
            if (change > 0) {
                sb.append("↑ ").append(compactEnergy(change)).append("/t in");
            } else if (change < 0) {
                sb.append(compactEnergy(-change)).append("/t ↓ out");
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

    private static int resolveNetworkColor(int networkColor, String networkName) {
        // FluxNetwork.getNetworkColor() returns an RGB value, or -1 if unset.
        int rgb = networkColor & 0xFFFFFF;
        // Recognized FluxNetworks color: use directly
        for (EnumNetworkColor c : EnumNetworkColor.values()) {
            if (c.getRGB() == rgb) {
                return rgb;
            }
        }
        // Unset (-1 → 0xFFFFFF) or unknown: generate hue from network name
        float hue = (Math.abs(networkName.hashCode()) * 0.618033988749895f) % 1f;
        int r = (int) ((Math.cos(hue * Math.PI * 2) * 0.5 + 0.5) * 192 + 32);
        int g = (int) ((Math.cos((hue + 0.333f) * Math.PI * 2) * 0.5 + 0.5) * 192 + 32);
        int b = (int) ((Math.cos((hue + 0.667f) * Math.PI * 2) * 0.5 + 0.5) * 192 + 32);
        return (r << 16) | (g << 8) | b;
    }
}
