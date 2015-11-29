/**
 * 
 */
package org.mines.cs565.dccs.sampler;

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
	
	
}
