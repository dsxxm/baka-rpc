package com.baka.web;

import java.util.concurrent.CompletableFuture;

import com.baka.dto.RpcResp;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * RpcResponseHandler
 */
public final class RpcResponseHandler extends SimpleChannelInboundHandler<RpcResp<?>> {

	private final CompletableFuture<RpcResp<?>> responseFuture;

	public RpcResponseHandler(CompletableFuture<RpcResp<?>> responseFuture) {
		this.responseFuture = responseFuture;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, RpcResp<?> resp) {
		responseFuture.complete(resp);
		ctx.close();
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) {
		responseFuture.completeExceptionally(new IllegalStateException("RPC channel closed before response"));
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		responseFuture.completeExceptionally(cause);
		ctx.close();
	}
}
