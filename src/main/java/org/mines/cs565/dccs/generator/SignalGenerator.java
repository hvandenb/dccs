package org.mines.cs565.dccs.generator;

import java.nio.ByteBuffer;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
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
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.stereotype.Service;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.AbstractScheduledService;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SignalGenerator  {


	private AudioFormat format;
	private SourceDataLine line;
	private int packageSize = 0;
	private boolean exitThread = false;

	@Autowired
	private SignalProperties properties;
	
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
//		ThreadPoolTaskScheduler scheduler = (ThreadPoolTaskScheduler) appContext.getBean("scheduler");

		// Create a scheduled trigger that fires based on the samplingRate of the generator.
		// We're doing a conversion of cycles per second to miliseconds. Note that this might get rounded
		// due to the trigger only being able to represent whole numbers
//		generatorTrigger = new PeriodicTrigger( (long) (1000 / properties.getSamplingRate()),TimeUnit.MILLISECONDS); 
//		generatorTrigger.setFixedRate(true);
//		generatorTrigger.setInitialDelay(1000L);
//		ScheduledFuture<Object> scedulefuture= scheduler.schedule(taskObject, generatorTrigger );

		
		log.info("Initialize the Signal Generator");
//		packageSize = (int)(((float)properties.getBufferDuration() / 1000) * properties.getSamplingRate() * properties.getSampleSize()) * 2;
		
		
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
		stopwatch.stop();
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
    public float getSample(float time) {
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
    
    /**
     * Provides the current elapsed time
     * @return elasped time
     * 
     */
    public float getTime()
    {
    	return (float)(stopwatch.elapsed(TimeUnit.NANOSECONDS));
    }
    
    public float getValue()
    {
        float time = this.getTime();

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
		log.info("Running the Signal Generator");
		
		// Restart the stop watch
		stopwatch.reset();
		stopwatch.start();
				
	}


}
