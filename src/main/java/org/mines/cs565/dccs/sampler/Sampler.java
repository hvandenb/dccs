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

/**
 * The Sampler is responsible for collecting samples from it's measurement source
 *
 * @author <a href="http://github.com/hvandenb">Henri van den Bulk</a>
 *
 */
@Component
@Slf4j
public class Sampler  {
	

	
	@Autowired
	private SamplerProperties properties;
	
//	@Autowired
	private GeneratorService measureSource;
	
	private CompletableFuture<DistributedQueue<String>> queue;
	
//	@Autowired
//	ClusterManager cm=null;
	
	/**
	 * This grabs a sample from the the current sampling source.
	 * @return
	 */
	public Measurement<Double> sample() {
		
		return measureSource.sample(SamplerConstants.MEASUREMENT_NAME);
		
	}
	
	@PostConstruct
	void init() {

		log.info("Initialize the Sampler");

		measureSource = new GeneratorService();
		
				// Let's create the distributed queue
//		queue = cm.createQueue(properties.getQueueName());
//		if (queue != null) {
//			queue.thenAccept(queue -> {
//				log.info("Queue was created");
//			});
//		}
//		else
//			log.error("Unable to create the queue: [{}]", properties.getQueueName());
	}

}
