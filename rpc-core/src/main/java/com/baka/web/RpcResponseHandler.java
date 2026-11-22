package com.baka.web;

import com.baka.dto.RpcResp;
import com.baka.trans.ChannelManager;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * RpcResponseHandler
 */
public final class RpcResponseHandler extends SimpleChannelInboundHandler<RpcResp<?>> {

	private final ChannelManager channelManager;

	public RpcResponseHandler(ChannelManager channelManager) {
		this.channelManager = channelManager;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, RpcResp<?> resp) {
		channelManager.completeResponse(resp);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		ctx.close();
	}
}
