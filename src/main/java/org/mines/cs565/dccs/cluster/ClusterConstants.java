package org.mines.cs565.dccs.cluster;

import java.time.Duration;

public class ClusterConstants {

	// Defaults
	public static final String DEFAULT_HOST = "127.0.0.1";
	public static final int DEFAULT_PORT= 5000;
	public static final int DEFAULT_GOSSIP_PORT= 55555;
	public static final String DEFAULT_DELIMITER = ",";
	public static final String DEFAULT_NAME = "dccs";
	public static final String DEFAULT_LOG_DIRECTORY = "/logs/";
	public static final int DEFAULT_GOSSIP_INTERVAL = 1000;
	public static final int DEFAULT_GOSSIP_CLEANUP_INTERVAL = 10000;
	public static final int DEFAULT_MIN_QUORUM = 1;
	public static final int DEFAULT_ELECTION_TIMEOUT = 1000;
	public static final int DEFAULT_HEARTBEAT = 150;
	
}
