/**
 * 
 */
package org.mines.cs565.dccs.sampler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.slf4j.Logger;

/**
 * @author hvandenb
 *
 */
@Service
@Slf4j
public class SamplerService {
	
	@Value("${sampler.frequency}")
	private float sourceFrequency = 0;
	private long samplingRate = 0;
    
	@Value("${sampler.multiplier}")
	private int multiplier = SamplerConstants.DEFAULT_MULTIPLIER;
	
    ThreadPoolTaskScheduler threadPoolTaskScheduler;
        
	@Autowired
    Sampler sampler;
    
	/**
	 * Calculate the sampling interval based on a frequency in Hz. 
	 * Using the formula T = 1 / f 
	 * @return Sampling Interval in ms
	 */
	public long calculateSamplingInterval(float frequency) {
		long T=0;
		T= (long) (multiplier * (1 / frequency) * 1000);
		
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
	
	/**
	 * This changes the scheduler's sampling
	 * @param rate
	 */
	public void changeSchedule(long rate) {
		this.samplingRate = rate;
		threadPoolTaskScheduler.scheduleWithFixedDelay(sampler, samplingRate); // schedule in ms
	
	}

	@PostConstruct
	void init() {

		log.info("Initialize the Sampler");
		
		threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
	    threadPoolTaskScheduler.setThreadNamePrefix("SamplerJob");
	    threadPoolTaskScheduler.initialize();
	    
	    samplingRate = calculateSamplingInterval(this.sourceFrequency);
//	    changeSchedule(samplingRate);
	}
	/**
	 * Close things down before the object gets destroyed.
	 */
	@PreDestroy
	public void stop() {
		log.info("Stopping the sampler");
	    ScheduledExecutorService scheduledExecutorService = threadPoolTaskScheduler.getScheduledExecutor();
	    scheduledExecutorService.shutdown();
	}
	
}
