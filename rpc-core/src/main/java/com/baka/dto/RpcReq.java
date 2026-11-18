package com.baka.dto;

import java.io.Serializable;
import java.util.Arrays;

/**
 * RpcReq
 */
public class RpcReq implements Serializable {

	private static final long serialVersionUID = 1L;

	private String reqId;
	private ServiceKey serviceKey;
	private String methodName;
	private Object[] params;
	private Class<?>[] paramTypes;

	public RpcReq(String reqId, ServiceKey serviceKey, String methodName, Object[] params, Class<?>[] paramTypes) {
		this.reqId = reqId;
		this.serviceKey = serviceKey;
		this.methodName = methodName;
		this.params = params;
		this.paramTypes = paramTypes;
	}

	public RpcReq() {
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public String getReqId() {
		return reqId;
	}

	public void setReqId(String reqId) {
		this.reqId = reqId;
	}

	public ServiceKey getServiceKey() {
		return serviceKey;
	}

	public void setServiceKey(ServiceKey serviceKey) {
		this.serviceKey = serviceKey;
	}

	public String getMethodName() {
		return methodName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public Object[] getParams() {
		return params;
	}

	public void setParams(Object[] params) {
		this.params = params;
	}

	public Class<?>[] getParamTypes() {
		return paramTypes;
	}

	public void setParamTypes(Class<?>[] paramTypes) {
		this.paramTypes = paramTypes;
	}

	@Override
	public String toString() {
		return "RpcReq{" +
				"reqId='" + reqId + '\'' +
				", serviceKey=" + serviceKey +
				", methodName='" + methodName + '\'' +
				", params=" + Arrays.toString(params) +
				", paramTypes=" + Arrays.toString(paramTypes) +
				'}';
	}
}
