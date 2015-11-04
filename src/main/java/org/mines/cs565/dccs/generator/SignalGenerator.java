package org.mines.cs565.dccs.generator;

import java.nio.ByteBuffer;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import org.mines.cs565.dccs.sampler.SamplerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.google.common.base.Stopwatch;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SignalGenerator {


	private AudioFormat format;
	private SourceDataLine line;
	private int packageSize = 0;
	private boolean exitThread = false;

	@Autowired
	private SignalProperties properties;
	
	@Autowired
	private SignalWriter writer;
	
    /// Time the signal generator was started
    private long startTime;
    private final Stopwatch stopwatch = Stopwatch.createUnstarted();

    /// Ticks per second on this CPU
//    private long ticksPerSecond = Stopwatch.Frequency;
	
	public AudioFormat createAudioFormat() {
		return new AudioFormat(1 / properties.getFrequency(), properties.getBits(), 1, true, true);
	}


	@PostConstruct
	void init() {
		log.info("Initialize the signal generator");
		packageSize = (int)(((float)properties.getBufferDuration() / 1000) * properties.getSamplingRate() * properties.getSampleSize()) * 2;
	
//		try {
//			format = createAudioFormat();
//			DataLine.Info info = new DataLine.Info(SourceDataLine.class, format, sinePackageSize);
//
//			if (!AudioSystem.isLineSupported(info))
//				throw new LineUnavailableException();
//
//            line = (SourceDataLine)AudioSystem.getLine(info);
//            line.open(format);  
//            line.start();	
//		
//		}
//		catch (LineUnavailableException e) {
//			log.warn("Line of that type is not available: [{}]", e.getCause().getMessage());
//		}
		
	
		log.info("Requested buffer size = " + packageSize);            
//	    log.info("Actual line buffer size = " + line.getBufferSize());
		
	    // Start the thread
//	    this.run();
//		this.getSamples();
	}
	
	/**
	 * Close things down before the object gets destroyed.
	 */
	@PreDestroy
	public void done() {
		log.info("Stopping the generator");
		if (line!=null) {
		 line.drain();
         line.close();
		}
		stopwatch.start();
	}
	
	public void exit() {
		exitThread = true;
	}

	/**
	 * Get the number of queued samples in the SourceDataLine buffer
	 * @return
	 */
    private int getLineSampleCount() {
       return line.getBufferSize() - line.available();
    }
    
    /**
     * Returns the next sample based on the position in the cycle.
     * @param cyclePosition
     * @return
     */
    private double getSample(double cyclePosition) {
    	double value = 0;
    	
    	switch (properties.getShape()) {
		case SAWTOOTH:
			value = 2.0 * (cyclePosition - Math.floor(cyclePosition + 0.5));
			break;
		case SINE:
			value = Math.sin(2*Math.PI * cyclePosition);
			break;
		case SQUARE:
			break;
		default:
			break;
    	
    	}
    	
    	return value;
    }
	
    /**
     * Returns the next sample based on a time
     * @param time
     * @return
     */
    private float getSample(float time) {
        float value = 0f;
        float t = properties.getFrequency() * time + properties.getPhase();
    	
    	switch (properties.getShape()) {
		case SAWTOOTH:
//			value = 2.0 * (cyclePosition - Math.floor(cyclePosition + 0.5));
			// 2 * ( t/a - floor( t/a + 1/2 ) )
            value = 2f*(t-(float)Math.floor(t+0.5f));
			break;
		case SINE: // sin( 2 * pi * t )
//			value = Math.sin(2*Math.PI * cyclePosition);
			value = (float)Math.sin(2 * Math.PI * t);
			break;
		case SQUARE: 
			value = (float) Math.signum(Math.sin(2f * Math.PI * t));
			break;
		case TRIANGLE:
			// 2 * abs( t - 2 * floor( t / 2 ) - 1 ) - 1
            value = 1f-4f*(float)Math.abs
                ( Math.round(t-0.25f)-(t-0.25f) );
		default:
			break;
    	
    	}
    	
    	return value;
    }    
    
    
    public float getValue()
    {
        float time = (float)(stopwatch.elapsed(TimeUnit.NANOSECONDS));
//                        / ticksPerSecond;
        return getSample(time);
    }
    /**
     * Get a buffer of oscillator samples
     * @return Buffer of samples
     */
    public ByteBuffer getSamples() {
		ByteBuffer signalBuffer = ByteBuffer.allocate(packageSize);
		// Position through the sine wave as a percentage (i.e. 0-1 is 0-2*PI)
		double cyclePosition = 0;
		
		double cylce = properties.getFrequency() / properties.getSamplingRate();   // Fraction of cycle between samples

		signalBuffer.clear();                             //Toss out samples from previous pass

        // Generate sinePackageSize samples based on the current cycle from frequency
        for (int i=0; i < packageSize/ properties.getSampleSize(); i++) {
        	signalBuffer.putShort((short)(Short.MAX_VALUE * getSample(cyclePosition)));

        	cyclePosition += cylce;
           if (cyclePosition > 1)
        	   cyclePosition -= 1;
        }
        
        return signalBuffer;
    }
    
    public void reset() {
    	stopwatch.reset();
    }
    
//	@Async
	public void run() {
		log.info("Running the Generator");
		
		// Restart the stop watch
		stopwatch.reset();
		stopwatch.start();
		
//		ByteBuffer signalBuffer = null;
		
//		// Keep running..
//		while(exitThread==false) {
//			double cylce = properties.getFrequency() / properties.getSamplingRate();   //Fraction of cycle between samples
//
//			signalBuffer.clear();                             //Toss out samples from previous pass
//			signalBuffer = getSamples();
//			
//            // Write sine samples to the line buffer
//            // If the audio buffer is full, this would block until there is enough room,
//            // but we are not writing unless we know there is enough space.
////            line.write(signalBuffer.array(), 0, signalBuffer.position());    
//            log.info("Created signal buffer of size [{}]", signalBuffer.position());
//
//            // Wait here until there are less than SINE_PACKET_SIZE samples in the buffer
//            // (Buffer size is 2*SINE_PACKET_SIZE at least, so there will be room for 
//            // at least SINE_PACKET_SIZE samples when this is true)
//            try {
//               while (getLineSampleCount() > packageSize) 
//                  Thread.sleep(1);                          // Give the system some time to run
//            }
//            catch (InterruptedException e) {                // We don't care about this
//            }
//		}
//		
	}
}
