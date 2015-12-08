/**
 * 
 */
package org.mines.cs565.dccs.sampler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.mines.cs565.dccs.cluster.ClusterManager;
import org.mines.cs565.dccs.generator.SignalWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AbstractScheduledService;

import io.atomix.variables.DistributedValue;
import lombok.extern.slf4j.Slf4j;

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
	
	private Splitter splitter;
	
	@Autowired
	private SignalWriter writer;
	
	@Autowired
	private ClusterManager clusterManager;
	  
	private List<Boolean> timingVector = new ArrayList<Boolean>();
	private int sampleIndex = 0;
	
	List<Measurement<Double>> measurements;
	
	// this will be our distributed 
	Optional<DistributedValue<List<Boolean>>> rtv = Optional.absent();
	
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
	 * Build an actual vector list based on a delimiter based string. {@see BooleanUtils}
	 * @param 
	 */
	private List<Boolean> buildVector(String vs) {
		List<String> list = Lists.newArrayList(this.splitter.split(Strings.nullToEmpty(vs)));
		List<Boolean> vector = Lists.newArrayListWithCapacity(list.size());

		for (String e : list) {
			if (e.length() > 1) // We'll assume its the text			
				vector.add(BooleanUtils.toBooleanObject(e));
			else if (StringUtils.isNumeric(e))
			{
				vector.add(BooleanUtils.toBooleanObject(Integer.parseInt(e)));
			}
				
		}
		
		return vector;
		
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

		log.info("Initialize the Sampler Service");
		
	    samplingRate = calculateSamplingInterval(properties.getFrequency());
	    
	    this.splitter = Splitter.onPattern(SamplerConstants.DEFAULT_DELIMITER).omitEmptyStrings().trimResults();
	    
		// Ensure we have a timing vector
		if (timingVector != null)
			timingVector.clear();
				
		// TODO: Timing vector needs to be provided by the cluster		
		if (properties.isUselocalrtv()) // We'll use a provided vector
			timingVector = buildVector(properties.getVector());
		else
			timingVector = generateTimingVector(properties.getBufferSize(), properties.getBufferSize() * 2, 1234);
		
		log.info("Vector: [{}] has been created of size {}", timingVector, timingVector.size());
		// Check if we have a distributed vector available. 
		rtv = Optional.fromNullable(clusterManager.createValue("vector"));
		
		if (rtv.isPresent()) 
		{
			rtv.get().set(timingVector).thenRun(() -> {
			      rtv.get().get().thenAccept(result -> {
			        System.out.println("Value is: " + result);
//			        rtv.get().context().schedule(Duration.ofSeconds(1), () -> recursiveSet(value));
			      });
			    });
		}

		// Initialize a list of measurements, which will be a local buffer
		measurements = Lists.newArrayListWithCapacity(properties.getBufferSize());
		
		// Start the scheduler
//		startAsync();	
	    
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
		log.info("Time to sample");
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
				log.info("Took sample {}", m);

				if (properties.isEnableOutput())
					writer.write(m);
				
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
