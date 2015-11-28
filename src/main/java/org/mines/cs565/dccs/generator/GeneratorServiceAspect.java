/**
 * 
 */
package org.mines.cs565.dccs.generator;

import javax.annotation.PostConstruct;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.stereotype.Component;

/**
 * @author Henri van den Bulk
 *
 */
@Aspect
@Component
public class GeneratorServiceAspect {

    private final CounterService counterService;

    @Autowired
    public GeneratorServiceAspect(CounterService counterService) {
        this.counterService = counterService;
    }

	@PostConstruct
	void init() {
		counterService.reset(GeneratorConstants.PERSISTED_SAMPLE_COUNTER_NAME);
		counterService.reset(GeneratorConstants.SAMPLE_REQUEST_COUNTER_NAME);
	}
    
    @AfterReturning("execution(* org.mines.cs565.dccs.generator.GeneratorService.sampleAndPersist(..))")
    public void afterCallingSampleAndPersist(JoinPoint joinPoint) {
    	
    	counterService.increment(GeneratorConstants.PERSISTED_SAMPLE_COUNTER_NAME);
		
    }

    @AfterReturning("execution(* org.mines.cs565.dccs.generator.GeneratorService.sample(..))")
    public void afterCallingSample(JoinPoint joinPoint) {
    	
    	counterService.increment(GeneratorConstants.SAMPLE_REQUEST_COUNTER_NAME);
		
    }
	
}
