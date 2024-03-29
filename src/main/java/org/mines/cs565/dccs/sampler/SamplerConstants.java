package org.mines.cs565.dccs.sampler;

public class SamplerConstants {

	// Defaults
	public static final String DEFAULT_MEASUREMENT_QUEUE = "/measurements";
	public static final int DEFAULT_BITS = 10; // Default number of bits sampling
	/**
	 * Indicates the multiplier of the sampling time, e.g. T=multiplier * (1/f)
	 */
	public static final int DEFAULT_MULTIPLIER = 1;
	public static final float DEFAULT_FREQUENCY = 500;
	public static final int DEFAULT_TIMING_VECTOR_SIZE = 20; 
	
	public static final String INVOCATIONS_COUNTER = "sampler.invocations";
	public static final String SAMPLES_COUNTER = "sampler.samples";
	public static final int DEFAULT_BUFFER_SIZE = 100;
	
	public static final String DEFAULT_OUTPUT_FILENAME ="sample.csv";
	public static final boolean DEFAULT_ENABLE_OUTPUT = false;
	public static final String MEASUREMENT_NAME = "wave.guage.sampler";
	public static final String DEFAULT_VECTOR = "true,true,false";
	public static final String DEFAULT_DELIMITER = ",";
	public static final boolean DEFAULT_LOCAL_RTV = false;

}
