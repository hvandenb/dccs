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
	/** Heartbeat message timeout in ms */
	private int heartBeat = ClusterConstants.DEFAULT_HEARTBEAT;
	private String hostName = ClusterConstants.DEFAULT_HOST;
	/** Main port for clusster communications */
	private int port= ClusterConstants.DEFAULT_PORT;
	private String name = ClusterConstants.DEFAULT_NAME;
	private String logLocation = ClusterConstants.DEFAULT_LOG_DIRECTORY;
	private int gossipPort = ClusterConstants.DEFAULT_GOSSIP_PORT;
	private int gossipInterval = ClusterConstants.DEFAULT_GOSSIP_INTERVAL;
	private int gossipCleanupInterval = ClusterConstants.DEFAULT_GOSSIP_CLEANUP_INTERVAL;
	/** Minimum quorum we need to ensure we can do leader election etc. (n/2)+1 */
	private int minimumQuorum = ClusterConstants.DEFAULT_MIN_QUORUM;
	/** Timeout as to when to start an election in (s)econds */
	private int electinTimeout = ClusterConstants.DEFAULT_ELECTION_TIMEOUT;
}
