/**
 * 
 */
package org.mines.cs565.dccs;

import org.mines.cs565.dccs.generator.GeneratorService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import lombok.extern.slf4j.Slf4j;

/**
 * @author hvandenb
 *
 */
@SpringBootApplication
@Slf4j
@EnableScheduling
@EnableAsync
@Import(AppConfig.class)
public class DCCSApplication {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
			
		ConfigurableApplicationContext ctx = SpringApplication.run(DCCSApplication.class, args);
		
	}
	

}
