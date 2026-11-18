package com.baka.dto;

import java.io.Serializable;

/**
 * RpcResp
 */
public class RpcResp<T> implements Serializable {

	private static final long serialVersionUID = 1L;

	private String reqId;
	private long code;
	private String msg;
	private T data;

	public RpcResp(String reqId, long code, String msg, T data) {
		this.reqId = reqId;
		this.code = code;
		this.msg = msg;
		this.data = data;
	}

	public RpcResp() {
	}

	public static <T> RpcResp<T> success(String reqId) {
		return new RpcResp<T>(reqId, 200L, "", null);
	}

	public static <T> RpcResp<T> success(String reqId, T data) {
		return new RpcResp<T>(reqId, 200L, "", data);
	}

	public static <T> RpcResp<T> fail(String reqId, String msg) {
		return new RpcResp<T>(reqId, 400L, msg, null);
	}

	public static <T> boolean isFail(RpcResp<T> resp) {
		return resp.getCode() == 400;
	}

	public static <T> boolean isSuccess(RpcResp<T> resp) {
		return resp.getCode() == 200;
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

	public long getCode() {
		return code;
	}

	public void setCode(long code) {
		this.code = code;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public T getData() {
		return data;
	}

	public void setData(T data) {
		this.data = data;
	}

	@Override
	public String toString() {
		return "RpcResp{" +
				"reqId='" + reqId + '\'' +
				", code=" + code +
				", msg='" + msg + '\'' +
				", data=" + data +
				'}';
	}
}
