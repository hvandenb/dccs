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

	private String members = "";
	private String hostName = ClusterConstants.DEFAULT_HOST;
	private int port= ClusterConstants.DEFAULT_PORT;
	private String name = ClusterConstants.DEFAULT_NAME;
	private String logLocation = ClusterConstants.DEFAULT_LOG_DIRECTORY;
	
}
