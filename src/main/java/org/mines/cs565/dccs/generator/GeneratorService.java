package org.mines.cs565.dccs.generator;

import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.mines.cs565.dccs.sampler.Measurement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.util.concurrent.AbstractScheduledService;

import lombok.extern.slf4j.Slf4j;

/**
 * The Generator Service is a wrapper to the actual {@link SignalGenerator}
 * @author Henri van den Bulk
 *
 */
@Service
@Slf4j
public class GeneratorService extends AbstractScheduledService{

	@Autowired
	private SignalProperties properties;
	@Autowired
	private SignalGenerator generator;
	
	
	@Autowired
	private SignalWriter writer;
	
	@PostConstruct
	void init() {
		log.info("Initialize the Signal Generator Service");

		generator.run();
		try {
//			this.startUp();
			// Only start up the output when it's enable
			if (properties.isEnableOutput()) {
				startAsync();			
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.error(e.getLocalizedMessage());
		} //
	}
	
	/**
	 * Close things down before the object gets destroyed.
	 */
	@PreDestroy
	public void done() {
		log.info("Stopping the Generator Service");
	}	
		
    /**
     * This method writes out a single sample of the generator to the file 
     */
	@Override
	protected void runOneIteration() throws Exception {
		// TODO Auto-generated method stub
		log.debug("Timer event on {}", this.getClass().getName());
		
		float time = generator.getTime();
		float value = generator.getSample(time);
		writer.write(new Measurement((long) time, value));
		
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
