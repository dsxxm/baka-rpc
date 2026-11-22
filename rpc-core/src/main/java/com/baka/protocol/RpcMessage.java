package com.baka.protocol;

/**
 * RPC wire message.
 *
 * <pre>
 * 0                   1                   2                   3
 * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                         magic (BRPC)                          |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * | version | messageType | codec |          bodyLength           |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                            body                               |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 */
public final class RpcMessage {

	// 固定协议标识，ASCII 为 BRPC。
	public static final int MAGIC = 0x42525043;
	// 当前协议版本。
	public static final byte VERSION = 1;
	// 固定 Header 长度：4 + 1 + 1 + 1 + 4。
	public static final int HEADER_LENGTH = 11;
	// 单条消息体允许的最大字节数。
	public static final int MAX_BODY_LENGTH = 8 * 1024 * 1024;

	private final int magic;
	// 协议版本。
	private final byte version;
	// 请求、响应或心跳等消息类型。
	private final byte messageType;
	// Body 的序列化方式。
	private final byte codec;
	// Body 的字节长度。
	private final int bodyLength;
	// 序列化后的 RPC 请求或响应数据。
	private final byte[] body;

	public RpcMessage(byte messageType, byte codec, byte[] body) {
		this(VERSION, messageType, codec, body);
	}

	public RpcMessage(byte version, byte messageType, byte codec, byte[] body) {
		if (body == null) {
			throw new IllegalArgumentException("body must not be null");
		}
		if (body.length > MAX_BODY_LENGTH) {
			throw new IllegalArgumentException("body exceeds maximum size");
		}
		this.magic = MAGIC;
		this.version = version;
		this.messageType = messageType;
		this.codec = codec;
		this.bodyLength = body.length;
		this.body = body;
	}

	public int getMagic() {
		return magic;
	}

	public byte getVersion() {
		return version;
	}

	public byte getMessageType() {
		return messageType;
	}

	public byte getCodec() {
		return codec;
	}

	public int getBodyLength() {
		return bodyLength;
	}

	public byte[] getBody() {
		return body;
	}


	@Override
	public String toString() {
		return "RpcMessage{" +
				"magic=0x" + Integer.toHexString(magic) +
				", version=" + version +
				", messageType=" + messageType +
				", codec=" + codec +
				", bodyLength=" + bodyLength +
				'}';
	}
	public static final class MessageType {
		public static final byte REQUEST = 0x01;
		public static final byte RESPONSE = 0x02;
		public static final byte HEARTBEAT_REQUEST = 0x03;
		public static final byte HEARTBEAT_RESPONSE = 0x04;

		private MessageType() {
		}
	}

	public static final class Codec {
		public static final byte JAVA = 0x01;
		public static final byte JSON = 0x02;
		public static final byte PROTOBUF = 0x03;

		private Codec() {
		}
	}
}
