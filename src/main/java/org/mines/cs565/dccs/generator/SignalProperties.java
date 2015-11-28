/**
 * 
 */
package org.mines.cs565.dccs.generator;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * @author Henri M.B. van den Bulk
 *
 */
@Data
@ConfigurationProperties(prefix = "generator", ignoreUnknownFields = false)
@Component
public class SignalProperties {
	enum WaveShape {
	    SINE,
	    SQUARE,
	    TRIANGLE,
	    SAWTOOTH
	}
	
	private WaveShape shape = WaveShape.SINE;

	/**
	 * is the frequency of the source that you'd want to sample
	 */
	private float frequency = GeneratorConstants.DEFAULT_FREQUENCY;
	private float phase = GeneratorConstants.DEFAULT_PHASE;
	private float amplitude = GeneratorConstants.DEFAULT_AMPLITUDE;
	private int bits = GeneratorConstants.DEFAULT_BITS;
	private int samplingRate = GeneratorConstants.DEFAULT_SAMPLING_RATE;
	private int sampleSize = GeneratorConstants.DEFAULT_SAMPLE_SIZE; // Sample size in bytes
	private int bufferDuration = GeneratorConstants.DEFAULT_BUFFER_DURATION; // About a 100ms buffer
	private float offset = GeneratorConstants.DEFAULT_OFFSET;
	private float invert = GeneratorConstants.DEFAULT_INVERT; // Yes=-1, No=1
	private String outputFileName = GeneratorConstants.DEFAULT_OUTPUT_FILENAME;
	private boolean enableOutput = GeneratorConstants.DEFAULT_ENABLE_OUTPUT;
	
	
}
