package com.baka.registry;

import java.util.List;

/**
 * LoadBalancer 服务注册中心负载均衡器 负责获取服务
 */
public interface LoadBalancer {

	ServiceInstance balance(List<ServiceInstance> instances);
}
