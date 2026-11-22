package com.baka.web;

import com.baka.dto.RpcReq;
import com.baka.dto.RpcResp;
import com.baka.dto.ServiceKey;
import com.baka.protocol.RpcMessage;
import com.baka.protocol.protobuf.RpcProtocolProto;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

/**
 * RpcSerialization 根据协议 codec 执行序列化。
 */
public final class RpcSerialization {

	private RpcSerialization() {
	}

	public static byte[] serialize(byte codec, Object value) {
		if (codec != RpcMessage.Codec.PROTOBUF) {
			throw new IllegalArgumentException("unsupported RPC codec: " + codec);
		}
		if (value instanceof RpcReq) {
			return serializeRequest((RpcReq) value).toByteArray();
		}
		if (value instanceof RpcResp) {
			return serializeResponse((RpcResp<?>) value).toByteArray();
		}
		throw new IllegalArgumentException("unsupported RPC payload: " + value.getClass().getName());
	}

	public static <T> T deserialize(byte codec, byte[] bytes, Class<T> type) {
		if (codec != RpcMessage.Codec.PROTOBUF) {
			throw new IllegalArgumentException("unsupported RPC codec: " + codec);
		}
		try {
			if (type == RpcReq.class) {
				return type.cast(deserializeRequest(RpcProtocolProto.RpcRequest.parseFrom(bytes)));
			}
			if (type == RpcResp.class) {
				return type.cast(deserializeResponse(RpcProtocolProto.RpcResponse.parseFrom(bytes)));
			}
			throw new IllegalArgumentException("unsupported RPC payload type: " + type.getName());
		} catch (InvalidProtocolBufferException e) {
			throw new IllegalStateException("failed to deserialize protobuf RPC payload", e);
		}
	}

	private static RpcProtocolProto.RpcRequest serializeRequest(RpcReq req) {
		RpcProtocolProto.RpcRequest.Builder builder = RpcProtocolProto.RpcRequest.newBuilder()
				.setRequestId(req.getReqId())
				.setServiceName(req.getServiceKey().getServiceName())
				.setGroupName(req.getServiceKey().getGroupName())
				.setVersion(req.getServiceKey().getVersion())
				.setMethodName(req.getMethodName());

		Object[] params = req.getParams();
		Class<?>[] paramTypes = req.getParamTypes();
		if (params == null) {
			return builder.build();
		}
		if (paramTypes == null || params.length != paramTypes.length) {
			throw new IllegalArgumentException("RPC parameter values and types must match");
		}

		for (int index = 0; index < params.length; index++) {
			if (!(params[index] instanceof Message)) {
				throw new IllegalArgumentException("protobuf RPC parameter must implement Message: " + paramTypes[index].getName());
			}
			builder.addParameters(Any.pack((Message) params[index]));
			builder.addParameterTypeNames(paramTypes[index].getName());
		}
		return builder.build();
	}

	private static RpcProtocolProto.RpcResponse serializeResponse(RpcResp<?> resp) {
		RpcProtocolProto.RpcResponse.Builder builder = RpcProtocolProto.RpcResponse.newBuilder()
				.setRequestId(resp.getReqId())
				.setCode(resp.getCode())
				.setMessage(resp.getMsg());

		Object data = resp.getData();
		if (data != null) {
			if (!(data instanceof Message)) {
				throw new IllegalArgumentException("protobuf RPC response data must implement Message: " + data.getClass().getName());
			}
			builder.setData(Any.pack((Message) data));
			builder.setDataTypeName(data.getClass().getName());
		}
		return builder.build();
	}

	private static RpcReq deserializeRequest(RpcProtocolProto.RpcRequest request) {
		int parameterCount = request.getParametersCount();
		if (parameterCount != request.getParameterTypeNamesCount()) {
			throw new IllegalArgumentException("protobuf RPC parameter values and types must match");
		}

		Object[] params = new Object[parameterCount];
		Class<?>[] paramTypes = new Class<?>[parameterCount];
		for (int index = 0; index < parameterCount; index++) {
			Class<?> type = loadMessageType(request.getParameterTypeNames(index));
			paramTypes[index] = type;
			params[index] = unpack(request.getParameters(index), type);
		}

		return new RpcReq(
				request.getRequestId(),
				new ServiceKey(request.getServiceName(), request.getGroupName(), request.getVersion()),
				request.getMethodName(),
				params,
				paramTypes
		);
	}

	private static RpcResp<?> deserializeResponse(RpcProtocolProto.RpcResponse response) {
		Object data = null;
		if (!response.getDataTypeName().isEmpty()) {
			data = unpack(response.getData(), loadMessageType(response.getDataTypeName()));
		}
		return new RpcResp<Object>(response.getRequestId(), response.getCode(), response.getMessage(), data);
	}

	private static Class<?> loadMessageType(String typeName) {
		try {
			Class<?> type = Class.forName(typeName);
			if (!Message.class.isAssignableFrom(type)) {
				throw new IllegalArgumentException("protobuf type does not implement Message: " + typeName);
			}
			return type;
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException("unknown protobuf type: " + typeName, e);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Message unpack(Any value, Class<?> type) {
		try {
			return value.unpack((Class) type);
		} catch (InvalidProtocolBufferException e) {
			throw new IllegalArgumentException("failed to unpack protobuf value: " + type.getName(), e);
		}
	}
}
