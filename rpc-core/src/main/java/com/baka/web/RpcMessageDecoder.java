package com.baka.web;

import java.util.List;

import com.baka.dto.RpcReq;
import com.baka.dto.RpcResp;
import com.baka.protocol.RpcMessage;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.MessageToMessageDecoder;

import lombok.extern.slf4j.Slf4j;

/**
 * RpcMessageDecoder
 */
@Slf4j
public final class RpcMessageDecoder extends MessageToMessageDecoder<ByteBuf> {

	private final byte expectedMessageType;

	public RpcMessageDecoder(byte expectedMessageType) {
		this.expectedMessageType = expectedMessageType;
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
		int magic = in.readInt();
		if (magic != RpcMessage.MAGIC) {
			throw new DecoderException("invalid RPC magic number");
		}

		byte version = in.readByte();
		if (version != RpcMessage.VERSION) {
			throw new DecoderException("unsupported RPC version: " + version);
		}

		byte messageType = in.readByte();
		if (messageType != expectedMessageType) {
			throw new DecoderException("unexpected RPC message type: " + messageType);
		}

		byte codec = in.readByte();

		int bodyLength = in.readInt();
		if (bodyLength < 0 || bodyLength > RpcMessage.MAX_BODY_LENGTH || bodyLength != in.readableBytes()) {
			throw new DecoderException("invalid RPC body length: " + bodyLength);
		}

		byte[] body = new byte[bodyLength];
		in.readBytes(body);
		RpcMessage message = new RpcMessage(version, messageType, codec, body);

		// 记录完整协议 Header；body 是序列化字节，不直接打印。
		log.info("received {}", message);

		try {
			if (messageType == RpcMessage.MessageType.REQUEST) {
				out.add(RpcSerialization.deserialize(codec, message.getBody(), RpcReq.class));
			} else {
				out.add(RpcSerialization.deserialize(codec, message.getBody(), RpcResp.class));
			}
		} catch (IllegalArgumentException e) {
			throw new DecoderException(e.getMessage(), e);
		}
	}
}
