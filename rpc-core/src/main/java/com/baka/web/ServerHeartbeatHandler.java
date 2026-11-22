package com.baka.web;

import com.baka.protocol.RpcMessage;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * ServerHeartbeatHandler 响应客户端心跳并关闭长期空闲连接。
 */
@Slf4j
public final class ServerHeartbeatHandler extends ChannelInboundHandlerAdapter {

    private static final byte[] EMPTY_BODY = new byte[0];

    private final byte codec;

    public ServerHeartbeatHandler(byte codec) {
        this.codec = codec;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object event) {
        // 长时间未收到请求或客户端心跳，释放失联客户端连接。
        if (event instanceof IdleStateEvent
                && ((IdleStateEvent) event).state() == IdleState.READER_IDLE) {
            log.warn("closing idle RPC server channel from {}", ctx.channel().remoteAddress());
            ctx.close();
            return;
        }
        ctx.fireUserEventTriggered(event);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object message) {
        if (message instanceof RpcMessage) {
            RpcMessage rpcMessage = (RpcMessage) message;
            if (rpcMessage.getMessageType() == RpcMessage.MessageType.HEARTBEAT_REQUEST) {
                log.info("received RPC heartbeat request from {}; sending response", ctx.channel().remoteAddress());
                ctx.writeAndFlush(new RpcMessage(
                        RpcMessage.MessageType.HEARTBEAT_RESPONSE,
                        codec,
                        EMPTY_BODY
                ));
                return;
            }
            if (rpcMessage.getMessageType() == RpcMessage.MessageType.HEARTBEAT_RESPONSE) {
                log.info("received unexpected RPC heartbeat response from {}", ctx.channel().remoteAddress());
                return;
            }
        }
        ctx.fireChannelRead(message);
    }
}
