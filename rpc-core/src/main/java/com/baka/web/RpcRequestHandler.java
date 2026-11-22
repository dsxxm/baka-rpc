package com.baka.web;

import java.util.concurrent.Executor;
import java.util.function.Function;

import com.baka.dto.RpcReq;
import com.baka.dto.RpcResp;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * RpcRequestHandler
 */
public final class RpcRequestHandler extends SimpleChannelInboundHandler<RpcReq> {

	private final Function<RpcReq, RpcResp<?>> dispatcher;
	private final Executor businessExecutor;

	public RpcRequestHandler(Function<RpcReq, RpcResp<?>> dispatcher, Executor businessExecutor) {
		this.dispatcher = dispatcher;
		this.businessExecutor = businessExecutor;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, RpcReq req) {
		businessExecutor.execute(() -> ctx.writeAndFlush(dispatcher.apply(req)));
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		ctx.close();
	}
}
