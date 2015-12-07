package org.mines.cs565.dccs.cluster;

/**
 * 
 */
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * 
 * @author Henri van den Bulk
 *
 */
@Data
@ConfigurationProperties(prefix = "cluster", ignoreUnknownFields = false)
@Component
public class ClusterProperties {

	//private String members = "";
	private String seeds = "";
	private int heartBeat = 100;
	private String hostName = ClusterConstants.DEFAULT_HOST;
	/** Main port for clusster communications */
	private int port= ClusterConstants.DEFAULT_PORT;
	private String name = ClusterConstants.DEFAULT_NAME;
	private String logLocation = ClusterConstants.DEFAULT_LOG_DIRECTORY;
	private int gossipPort = ClusterConstants.DEFAULT_GOSSIP_PORT;
	private int gossipInterval = ClusterConstants.DEFAULT_GOSSIP_INTERVAL;
	private int gossipCleanupInterval = ClusterConstants.DEFAULT_GOSSIP_CLEANUP_INTERVAL;
}
