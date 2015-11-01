package org.mines.cs565.dccs.cluster;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "cluster", ignoreUnknownFields = false)
@Component
public class ClusterProperties {

	private String members;
	private String hostName = ClusterConstants.DEFAULT_HOST;
	private int port= ClusterConstants.DEFAULT_PORT;
	private String name = ClusterConstants.DEFAULT_NAME;
	private String logLocation = ClusterConstants.DEFAULT_LOG_DIRECTORY;

    public String getMembers() {
        return this.members;
    }
    
    public void setMembers(String members) {
    	this.members = members;
    }

	/**
	 * @return the hostName
	 */
	public String getHostName() {
		return hostName;
	}

	/**
	 * @param hostName the hostName to set
	 */
	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	/**
	 * @return the port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * @param port the port to set
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the logLocation
	 */
	public String getLogLocation() {
		return logLocation;
	}

	/**
	 * @param logLocation the logLocation to set
	 */
	public void setLogLocation(String logLocation) {
		this.logLocation = logLocation;
	}
	
}
