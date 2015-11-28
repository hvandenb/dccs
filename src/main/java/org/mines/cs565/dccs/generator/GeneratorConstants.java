package org.mines.cs565.dccs.generator;

public class GeneratorConstants {

	// Defaults
	public static final float DEFAULT_FREQUENCY = 500f; 
	public static final float DEFAULT_PHASE = 0f; 
												
	public static final int DEFAULT_BITS = 16;

	public final static int DEFAULT_SAMPLING_RATE = 44100;
	public final static int DEFAULT_SAMPLE_SIZE = 2; // Sample size in bytes
	public final static int DEFAULT_BUFFER_DURATION = 100; // About a 100ms buffer

	public static final float DEFAULT_AMPLITUDE = 1f;
	public static final float DEFAULT_OFFSET = 0f;
	public static final float DEFAULT_INVERT = 1; // Yes=-1, No=1
	
	public static final String DEFAULT_OUTPUT_FILENAME ="wave.csv";
	public static final boolean DEFAULT_ENABLE_OUTPUT = false;
	public static final String MEASUREMENT_NAME = "wave.guage";

}
