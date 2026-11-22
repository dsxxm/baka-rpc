package com.baka.web;

import com.baka.dto.RpcReq;
import com.baka.dto.RpcResp;
import com.baka.protocol.RpcMessage;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * RpcMessageEncoder
 */
public final class RpcMessageEncoder extends MessageToByteEncoder<Object> {

	private final byte codec;

	public RpcMessageEncoder(byte codec) {
		this.codec = codec;
	}

	@Override
	public boolean acceptOutboundMessage(Object message) {
		return message instanceof RpcReq || message instanceof RpcResp || message instanceof RpcMessage;
	}

	@Override
	protected void encode(ChannelHandlerContext ctx, Object payload, ByteBuf out) {
		RpcMessage message;
		if (payload instanceof RpcMessage) {
			message = (RpcMessage) payload;
		} else {
			byte messageType;
			if (payload instanceof RpcReq) {
				messageType = RpcMessage.MessageType.REQUEST;
			} else if (payload instanceof RpcResp) {
				messageType = RpcMessage.MessageType.RESPONSE;
			} else {
				throw new EncoderException("unsupported RPC payload: " + payload.getClass().getName());
			}

			byte[] body = RpcSerialization.serialize(codec, payload);
			message = new RpcMessage(messageType, codec, body);
		}

		out.writeInt(message.getMagic());
		out.writeByte(message.getVersion());
		out.writeByte(message.getMessageType());
		out.writeByte(message.getCodec());
		out.writeInt(message.getBodyLength());
		out.writeBytes(message.getBody());
	}
}
