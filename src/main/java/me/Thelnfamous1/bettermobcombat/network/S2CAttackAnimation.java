package me.Thelnfamous1.bettermobcombat.network;

import me.Thelnfamous1.bettermobcombat.Constants;
import net.bettercombat.logic.AnimatedHand;
import net.bettercombat.network.Packets;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record S2CAttackAnimation(int mobId, AnimatedHand animatedHand, String animationName, float length, float upswing) implements CustomPacketPayload {
    public static final Type<S2CAttackAnimation> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "attack_animation"));

    public static final StreamCodec<FriendlyByteBuf, S2CAttackAnimation> STREAM_CODEC = StreamCodec.of(
            (buffer, msg) -> msg.write(buffer),
            S2CAttackAnimation::read
    );

    public static S2CAttackAnimation read(FriendlyByteBuf buffer) {
        int mobId = buffer.readInt();
        AnimatedHand animatedHand = AnimatedHand.values()[buffer.readInt()];
        String animationName = buffer.readUtf();
        float length = buffer.readFloat();
        float upswing = buffer.readFloat();
        return new S2CAttackAnimation(mobId, animatedHand, animationName, length, upswing);
    }

    public static S2CAttackAnimation stop(int mobId, int length) {
        return new S2CAttackAnimation(mobId, AnimatedHand.MAIN_HAND, Packets.AttackAnimation.StopSymbol, (float) length, 0.0F);
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeInt(this.mobId);
        buffer.writeInt(this.animatedHand.ordinal());
        buffer.writeUtf(this.animationName);
        buffer.writeFloat(this.length);
        buffer.writeFloat(this.upswing);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
