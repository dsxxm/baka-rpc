package com.baka.dto;

import java.io.Serializable;

/**
 * ServiceKey
 */

public class ServiceKey implements Serializable {
	private static final long serialVersionUID = 1L;

	public static final String DEFAULT_GROUP = "default";
	public static final String DEFAULT_VERSION = "default-version";

	private String serviceName;
	private String groupName;
	private String version;

	public ServiceKey(String serviceName, String groupName, String version) {
		this.serviceName = serviceName;
		this.groupName = groupName;
		this.version = version;
	}

	public ServiceKey() {
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((serviceName == null) ? 0 : serviceName.hashCode());
		result = prime * result + ((groupName == null) ? 0 : groupName.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ServiceKey other = (ServiceKey) obj;
		if (serviceName == null) {
			if (other.serviceName != null)
				return false;
		} else if (!serviceName.equals(other.serviceName))
			return false;
		if (groupName == null) {
			if (other.groupName != null)
				return false;
		} else if (!groupName.equals(other.groupName))
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ServiceKey{" +
				"serviceName='" + serviceName + '\'' +
				", groupName='" + groupName + '\'' +
				", version='" + version + '\'' +
				'}';
	}
}
