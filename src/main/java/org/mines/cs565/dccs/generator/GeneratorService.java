package org.mines.cs565.dccs.generator;

import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.mines.cs565.dccs.sampler.Measurement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.google.common.base.Ticker;
import com.google.common.util.concurrent.AbstractScheduledService;

import lombok.extern.slf4j.Slf4j;

/**
 * The Generator Service is a wrapper to the actual {@link SignalGenerator}
 * @author Henri van den Bulk
 *
 */
@Component
@Slf4j
public class GeneratorService extends AbstractScheduledService{

	@Autowired
	private SignalProperties properties;

	@Autowired
	private SignalGenerator generator;
	
	@Autowired
	private SignalWriter writer;
	
	// Nice to have a ticker
	private Ticker ticker = Ticker.systemTicker();
	
	@PostConstruct
	void init() {
		log.info("Initialize the Signal Generator Service");
		// Reset our actuators
		
		generator.run();
		try {
//			this.startUp();
			// Only start up the output when it's enable
			if (properties.isEnableOutput()) {
				startAsync();			
			}
		} catch (Exception e) {
			log.error(e.getLocalizedMessage());
		} 
	}
	
	/**
	 * Close things down before the object gets destroyed.
	 */
	@PreDestroy
	public void done() {
		log.info("Stopping the Generator Service");
	}	
	
	/**
	 * Returns a sample of the generator
	 * @return
	 */
	public Measurement<Double> sample() {

		long time = ticker.read();
		float elaspsedTime = generator.getElapsedTime();
		double value = generator.sample(elaspsedTime);
		return new Measurement<Double>(time, GeneratorConstants.MEASUREMENT_NAME, value);
		
	}
		
    /**
     * This method writes out a single sample of the generator to the file 
     */
	@Override
	protected void runOneIteration() throws Exception {
		// TODO Auto-generated method stub
		log.debug("Timer event on {}", this.getClass().getName());

		this.sampleAndPersist();
	}
	
	private void sampleAndPersist() {
		
	float time = generator.getTime();
	float value = generator.sample(time);
	writer.write(this.sample());
	
	
	}


	/**
	 * Initiate the scheduler based on the sampling Rate
	 */
	@Override
	protected Scheduler scheduler() {
		log.debug("Creating a fixed scheduler");
		return Scheduler.newFixedRateSchedule(0, (long) (1000 / properties.getSamplingRate()), TimeUnit.MILLISECONDS);
	}

}
