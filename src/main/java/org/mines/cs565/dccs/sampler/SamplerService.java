/**
 * 
 */
package org.mines.cs565.dccs.sampler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.common.util.concurrent.AbstractScheduledService.Scheduler;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.slf4j.Logger;

/**
 * @author hvandenb
 *
 */
@Component
@Slf4j
public class SamplerService extends AbstractScheduledService {
	
	private long samplingRate = 0;
    
	@Autowired
	private SamplerProperties properties;
	
	@Autowired
    private Sampler sampler;
	
	@Autowired
	private CounterService samplerCounter;
  
	private List<Boolean> timingVector = new ArrayList<Boolean>();
	private int sampleIndex = 0;
	
	/**
	 * Calculate the sampling interval based on a frequency in Hz. 
	 * Using the formula T = 1 / f 
	 * @return Sampling Interval in ms
	 */
	public long calculateSamplingInterval(float frequency) {
		long T=0;
		T= (long) (properties.getMultiplier() * (1 / frequency) * 1000);
		
		return T;
	}
	
	/**
	 * Generate the timing vector that a sampler will need. The size of the timing vector (n) should
	 * be larger then the size of the internal memory (m).
	 * @param m Size of internal buffer for storing the samples
	 * @param n Size of the timing vector
	 * @param seed
	 * @return a Timing vector, null if m >= n
	 */
	public List<Boolean> generateTimingVector(int m, int n, long seed) {
		List<Boolean> vector = new ArrayList<Boolean>(m);
		
		if (n < m) 
			return null;
					
		for (int i = 0 ; i < m -1; i++) {
			vector.set(i, new Boolean(true));
		}
		for (int i = m ; i < n -1; i++) {
			vector.set(i, new Boolean(false));
		}
		
		Collections.shuffle(vector, new Random(seed));
		
		return vector;
		
	}
	

	@PostConstruct
	void init() {

		log.info("Initialize the Sampler");
		
	    samplingRate = calculateSamplingInterval(properties.getFrequency());
	    
		// Reset our actuators
		samplerCounter.reset(SamplerConstants.INVOCATIONS_COUNTER);
		samplerCounter.reset(SamplerConstants.SAMPLES_COUNTER);
		
		// Ensure we have a timing vector
		if (timingVector != null)
			timingVector.clear();
		timingVector = new ArrayList<Boolean>();
		
		
//		timingVector = generateTimingVector()
		
		// Start the scheduler
		startAsync();	
	    
	}
	/**
	 * Close things down before the object gets destroyed.
	 */
	@PreDestroy
	public void stop() {
		log.info("Stopping the sampler");
		
		stopAsync();
	}

	/**
	 * This gets called on the specific scheduler
	 */
	@Override
	protected void runOneIteration() throws Exception {
	
		log.debug("Invoking the Sampler");

		samplerCounter.increment(SamplerConstants.INVOCATIONS_COUNTER);
		
		if (timingVector.size() > 0) {
			sampleIndex++;
			
			// Reset the index when we've reached the end
			if (sampleIndex > timingVector.size())
				sampleIndex = 1;
			
	
			// Only when the vector tells us to sample we'll sample
			if (timingVector.get(sampleIndex-1).booleanValue()) {
				samplerCounter.increment(SamplerConstants.SAMPLES_COUNTER);

				// Get a sample from our sampler
				Measurement<Double> m = sampler.sample();

//				if (queue != null) {
//					//queue.add("");
//				}
				
			}
		}
		else {
			log.warn("The timing vector is empty, so we'll skip the sampling");
		}
		
		
	}

	@Override
	protected Scheduler scheduler() {
		log.debug("Creating a fixed scheduler");
		return Scheduler.newFixedRateSchedule(0, samplingRate, TimeUnit.MILLISECONDS);
	}
	
}
