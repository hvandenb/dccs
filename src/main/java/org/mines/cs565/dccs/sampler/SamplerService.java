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

import com.google.common.collect.Lists;
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
	  
	private List<Boolean> timingVector = new ArrayList<Boolean>();
	private int sampleIndex = 0;
	
	List<Measurement<Double>> measurements;
	
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
		// Create the vector with size of n
		List<Boolean> vector = Lists.newArrayListWithCapacity(n);
		
		log.debug("Creating RTV of size [{}]", n);
		if (n < m) 
			return null;
						
		for (int i = 1; i <= n; i++) {
			vector.add(new Boolean( i <= (n-m) ? true : false ));
		}
		
//		for (int i = 1 ; i < m -1; i++) {
//			vector.add(new Boolean(true));
//		}
//		for (int i = m ; i < n -1; i++) {
//			vector.add(new Boolean(false));
//		}
		
		Collections.shuffle(vector, new Random(seed));
		
		return vector;
		
	}
	

	@PostConstruct
	void init() {

		log.info("Initialize the Sampler");
		
	    samplingRate = calculateSamplingInterval(properties.getFrequency());
	    		
		// Ensure we have a timing vector
		if (timingVector != null)
			timingVector.clear();
//		timingVector = new ArrayList<Boolean>();
		
		// Just generate the Timer
		// TODO: Timing vector needs to be provided by the cluster
		timingVector = generateTimingVector(properties.getBufferSize(), properties.getBufferSize() * 2, 1234);
		
		// Initialize a list of measurements, which will be a local buffer
		measurements = Lists.newArrayListWithCapacity(properties.getBufferSize());
		
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

		sample();

	}
	
	/**
	 * Take a sample and store it in the local buffer
	 * @return true if we actually sampled, false if not.
	 */
	private boolean sample() {
		boolean sampled = false;
				
		if (timingVector.size() > 0) {
			sampleIndex++;
			
			// Reset the index when we've reached the end
			if (sampleIndex > timingVector.size())
				sampleIndex = 1;
			
	
			// Only when the vector tells us to sample we'll sample
			if (timingVector.get(sampleIndex-1).booleanValue() == true) {
				// Get a sample from our sampler
				Measurement<Double> m = sampler.sample();
				measurements.add(m);
				sampled = true;
				
//				if (queue != null) {
//					//queue.add("");
//				}
				
			}
		}
		else {
			log.warn("The timing vector is empty, so we'll skip the sampling");
		}
		return sampled;
	}

	@Override
	protected Scheduler scheduler() {
		log.info("Creating a fixed scheduler with a sample rate [{}] ms", samplingRate);
		return Scheduler.newFixedRateSchedule(0, samplingRate, TimeUnit.MILLISECONDS);
	}
	
}
