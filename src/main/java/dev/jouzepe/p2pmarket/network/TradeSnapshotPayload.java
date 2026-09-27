package dev.jouzepe.p2pmarket.network;

import dev.jouzepe.p2pmarket.P2PMarketMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record TradeSnapshotPayload(String json) implements CustomPacketPayload {
    public static final Type<TradeSnapshotPayload> TYPE = new Type<>(Identifier.parse(P2PMarketMod.MOD_ID + ":trade_snapshot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TradeSnapshotPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            TradeSnapshotPayload::json,
            TradeSnapshotPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
