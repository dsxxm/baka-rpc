package com.baka.web;

import com.baka.protocol.RpcMessage;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * ClientHeartbeatHandler 维持复用客户端 Channel 的存活状态。
 */
@Slf4j
public final class ClientHeartbeatHandler extends ChannelInboundHandlerAdapter {

    private static final byte[] EMPTY_BODY = new byte[0];

    private final byte codec;

    public ClientHeartbeatHandler(byte codec) {
        this.codec = codec;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object event) {
        if (event instanceof IdleStateEvent) {
            IdleState state = ((IdleStateEvent) event).state();
            // 没有业务请求写出时，使用自定义 RPC 协议维持长连接。
            if (state == IdleState.WRITER_IDLE) {
                log.info("sending RPC heartbeat request to {}", ctx.channel().remoteAddress());
                ctx.writeAndFlush(new RpcMessage(
                        RpcMessage.MessageType.HEARTBEAT_REQUEST,
                        codec,
                        EMPTY_BODY
                ));
                return;
            }
            // 长时间收不到业务响应或心跳响应，丢弃失效连接。
            if (state == IdleState.READER_IDLE) {
                log.warn("closing idle RPC client channel to {}", ctx.channel().remoteAddress());
                ctx.close();
                return;
            }
        }
        ctx.fireUserEventTriggered(event);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object message) {
        if (message instanceof RpcMessage) {
            RpcMessage rpcMessage = (RpcMessage) message;
            if (rpcMessage.getMessageType() == RpcMessage.MessageType.HEARTBEAT_RESPONSE) {
                log.info("received RPC heartbeat response from {}", ctx.channel().remoteAddress());
                return;
            }
            if (rpcMessage.getMessageType() == RpcMessage.MessageType.HEARTBEAT_REQUEST) {
                log.info("received RPC heartbeat request from {}; sending response", ctx.channel().remoteAddress());
                ctx.writeAndFlush(new RpcMessage(
                        RpcMessage.MessageType.HEARTBEAT_RESPONSE,
                        codec,
                        EMPTY_BODY
                ));
                return;
            }
        }
        ctx.fireChannelRead(message);
    }
}
