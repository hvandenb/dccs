package org.mines.cs565.dccs.sampler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.annotation.PostConstruct;

import org.mines.cs565.dccs.cluster.ClusterManager;
import org.mines.cs565.dccs.generator.GeneratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.stereotype.Component;

import io.atomix.collections.DistributedQueue;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class Sampler implements Runnable {
	
	private List<Boolean> timingVector = new ArrayList<Boolean>();
	private int sampleIndex = 0;
	
	@Autowired
	private CounterService samplerCounter;
	
	@Autowired
	private SamplerProperties properties;
	
	@Autowired
	private GeneratorService generator;
	
	private CompletableFuture<DistributedQueue<String>> queue;
	
	@Autowired
	ClusterManager cm=null;
	
	@Override
	public void run() {
		log.info("Time to sample");
		//log.info(Thread.currentThread().getName() + " working ... Time - " + new Date());

		samplerCounter.increment("sampler.counter.invocations");
		
		if (timingVector.size() > 0) {
			sampleIndex++;
			
			// Reset the index when we've reached the end
			if (sampleIndex > timingVector.size())
				sampleIndex = 1;
			
	
			// Only when the vector tells us to sample we'll sample
			if (timingVector.get(sampleIndex-1).booleanValue()) {
				samplerCounter.increment("sampler.counter.samples");
				
				if (queue != null) {
					//queue.add("");
				}
				
			}
		}
		else {
			log.warn("The timing vector is empty, so we can't sample");
		}
	
		
	}
	
	/**
	 * This grabs a sample from the the current sampling source.
	 * @return
	 */
	public Measurement<Double> sample() {
		
		
		
		return null;
		
	}
	
	@PostConstruct
	void init() {

		log.info("Initialize the Sampler");
		if (timingVector != null)
			timingVector.clear();
		timingVector = new ArrayList<Boolean>();

		// Reset our actuators
		samplerCounter.reset("sampler.counter.invocations");
		samplerCounter.reset("sampler.counter.samples");
		
		// Let's create the distributed queue
		queue = cm.createQueue(properties.getQueueName());
		if (queue != null) {
			queue.thenAccept(queue -> {
				log.info("Queue was created");
			});
		}
		else
			log.error("Unable to create the queue: [{}]", properties.getQueueName());
	}

}
