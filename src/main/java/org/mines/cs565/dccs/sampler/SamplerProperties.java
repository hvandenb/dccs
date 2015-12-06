/**
 * 
 */
package org.mines.cs565.dccs.sampler;

import org.mines.cs565.dccs.generator.GeneratorConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * @author Henri M.B. van den Bulk
 *
 */
@Data
@ConfigurationProperties(prefix = "sampler", ignoreUnknownFields = false)
@Component
public class SamplerProperties {
	
	private float frequency = SamplerConstants.DEFAULT_FREQUENCY;
	private int bits = SamplerConstants.DEFAULT_BITS;
	private int timingVectorSize = SamplerConstants.DEFAULT_TIMING_VECTOR_SIZE;
	private String queueName = SamplerConstants.DEFAULT_MEASUREMENT_QUEUE;
	private int multiplier = SamplerConstants.DEFAULT_MULTIPLIER;
	private int bufferSize = SamplerConstants.DEFAULT_BUFFER_SIZE;

	private boolean enableOutput = SamplerConstants.DEFAULT_ENABLE_OUTPUT; 

	/** The Random Timing Vector to be used for local purposes {@link #useLocalRTV} */
	private String vector = SamplerConstants.DEFAULT_VECTOR;
	
	/** Indicates if a local vector is to be used, good for testing */
	private boolean uselocalrtv = SamplerConstants.DEFAULT_LOCAL_RTV;
	
}
