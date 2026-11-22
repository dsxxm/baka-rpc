package com.baka.trans;

import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import com.baka.dto.RpcResp;
import com.baka.protocol.RpcMessage;
import com.baka.registry.ServiceInstance;
import com.baka.web.ClientHeartbeatHandler;
import com.baka.web.RpcMessageDecoder;
import com.baka.web.RpcMessageEncoder;
import com.baka.web.RpcResponseHandler;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * ChannelManager 为每个服务实例复用一条客户端 Channel。
 */
public final class ChannelManager implements AutoCloseable {

    public static final class PendingRequest {
        private final Channel channel;
        private final CompletableFuture<RpcResp<?>> responseFuture;

        private PendingRequest(Channel channel, CompletableFuture<RpcResp<?>> responseFuture) {
            this.channel = channel;
            this.responseFuture = responseFuture;
        }
    }

    private final EventLoopGroup eventLoopGroup;
    private final byte codec;
    private final ConcurrentMap<ServiceInstance, CompletableFuture<Channel>> channels = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, PendingRequest> pendingRequests = new ConcurrentHashMap<>();

    public ChannelManager(EventLoopGroup eventLoopGroup, byte codec) {
        this.eventLoopGroup = eventLoopGroup;
        this.codec = codec;
    }

    public CompletableFuture<Channel> getOrConnect(ServiceInstance instance) {
        while (true) {
            CompletableFuture<Channel> existing = channels.get(instance);
            if (existing != null) {
                if (!existing.isDone()) {
                    return existing;
                }
                if (existing.isCompletedExceptionally() || existing.isCancelled()) {
                    channels.remove(instance, existing);
                    continue;
                }

                Channel channel = existing.getNow(null);
                if (channel != null && channel.isActive()) {
                    return existing;
                }
                channels.remove(instance, existing);
                continue;
            }

            CompletableFuture<Channel> connecting = new CompletableFuture<>();
            if (channels.putIfAbsent(instance, connecting) == null) {
                connect(instance, connecting);
                return connecting;
            }
        }
    }

    public CompletableFuture<RpcResp<?>> registerRequest(String requestId, Channel channel) {
        CompletableFuture<RpcResp<?>> responseFuture = new CompletableFuture<>();
        PendingRequest request = new PendingRequest(channel, responseFuture);
        if (pendingRequests.putIfAbsent(requestId, request) != null) {
            throw new IllegalStateException("duplicate RPC request ID: " + requestId);
        }
        return responseFuture;
    }

    public void completeResponse(RpcResp<?> response) {
        PendingRequest request = pendingRequests.remove(response.getReqId());
        if (request != null) {
            request.responseFuture.complete(response);
        }
    }

    public void failRequest(String requestId, CompletableFuture<RpcResp<?>> responseFuture, Throwable cause) {
        PendingRequest request = pendingRequests.remove(requestId);
        if (request != null) {
            request.responseFuture.completeExceptionally(cause);
        } else {
            responseFuture.completeExceptionally(cause);
        }
    }

    public void removeRequest(String requestId, CompletableFuture<RpcResp<?>> responseFuture) {
        PendingRequest request = pendingRequests.get(requestId);
        if (request != null && request.responseFuture == responseFuture) {
            pendingRequests.remove(requestId, request);
        }
    }

    @Override
    public void close() {
        for (CompletableFuture<Channel> future : channels.values()) {
            Channel channel = future.getNow(null);
            if (channel != null) {
                channel.close();
            }
        }
        channels.clear();
        failPendingRequests(null, new IllegalStateException("RPC client is closed"));
    }

    private void connect(ServiceInstance instance, CompletableFuture<Channel> connecting) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(EpollSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel channel) {
                        channel.pipeline()
                                .addLast(new LengthFieldBasedFrameDecoder(
                                        RpcMessage.HEADER_LENGTH + RpcMessage.MAX_BODY_LENGTH,
                                        7,
                                        4,
                                        0,
                                        0
                                ))
                                .addLast(new RpcMessageDecoder())
                                .addLast(new RpcMessageEncoder(codec))
                                .addLast(new IdleStateHandler(30, 10, 0, TimeUnit.SECONDS))
                                .addLast(new ClientHeartbeatHandler(codec))
                                .addLast(new RpcResponseHandler(ChannelManager.this));
                    }
                });

        ChannelFuture connectFuture = bootstrap.connect(instance.getHost(), instance.getPort());
        connectFuture.addListener(future -> {
            if (!future.isSuccess()) {
                channels.remove(instance, connecting);
                connecting.completeExceptionally(future.cause());
                return;
            }

            Channel channel = ((ChannelFuture) future).channel();
            channel.closeFuture().addListener(closeFuture -> {
                channels.remove(instance, connecting);
                failPendingRequests(channel, new IllegalStateException("RPC channel closed"));
            });
            connecting.complete(channel);
        });
    }

    private void failPendingRequests(Channel channel, Throwable cause) {
        for (Entry<String, PendingRequest> entry : pendingRequests.entrySet()) {
            PendingRequest request = entry.getValue();
            if (channel == null || request.channel == channel) {
                if (pendingRequests.remove(entry.getKey(), request)) {
                    request.responseFuture.completeExceptionally(cause);
                }
            }
        }
    }
}
