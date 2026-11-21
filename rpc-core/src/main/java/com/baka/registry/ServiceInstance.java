package com.baka.registry;

import com.baka.dto.ServiceKey;

/**
 * ServiceInstance
 */
public class ServiceInstance {

	private ServiceKey serviceKey;
	private String instanceId;
	private String host;
	private int port;

	public ServiceInstance() {}

	public ServiceInstance(ServiceKey serviceKey, String instanceId, String host, int port) {
		this.serviceKey = serviceKey;
		this.instanceId = instanceId;
		this.host = host;
		this.port = port;
	}

	public ServiceKey getServiceKey() {
		return serviceKey;
	}

	public void setServiceKey(ServiceKey serviceKey) {
		this.serviceKey = serviceKey;
	}

	public String getInstanceId() {
		return instanceId;
	}

	public void setInstanceId(String instanceId) {
		this.instanceId = instanceId;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		ServiceInstance other = (ServiceInstance) obj;
		if (serviceKey == null ? other.serviceKey != null : !serviceKey.equals(other.serviceKey)) {
			return false;
		}
		return instanceId == null ? other.instanceId == null : instanceId.equals(other.instanceId);
	}

	@Override
	public int hashCode() {
		int result = serviceKey == null ? 0 : serviceKey.hashCode();
		return 31 * result + (instanceId == null ? 0 : instanceId.hashCode());
	}

	@Override
	public String toString() {
		return "ServiceInstance{" + "serviceKey=" + serviceKey + ", instanceId='" + instanceId + '\'' + ", host='" + host + '\'' + ", port=" + port + '}';
	}
}
