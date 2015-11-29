package org.mines.cs565.dccs.sampler;

import javax.annotation.PostConstruct;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.mines.cs565.dccs.generator.GeneratorConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.stereotype.Component;

/**
* @author Henri van den Bulk
*
*/
@Aspect
@Component
public class SamplerServiceAspect {
    private final CounterService counterService;

    @Autowired
    public SamplerServiceAspect(CounterService counterService) {
        this.counterService = counterService;
    }

	@PostConstruct
	void init() {
		counterService.reset(SamplerConstants.INVOCATIONS_COUNTER);
		counterService.reset(SamplerConstants.SAMPLES_COUNTER);
	}
    
    @AfterReturning(pointcut="execution(* org.mines.cs565.dccs.sampler.SamplerService.sample(..))",
    		returning="retVal")
    public void afterCallingSample(JoinPoint joinPoint, Object retVal) {
    	
    	counterService.increment(SamplerConstants.INVOCATIONS_COUNTER);

    	if (retVal != null) {
    		if ((boolean)retVal == true)
    			counterService.increment(SamplerConstants.SAMPLES_COUNTER);
    	}
 		
    }
}
