/**
 * 
 */
package org.mines.cs565.dccs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

import lombok.extern.slf4j.Slf4j;

/**
 * @author hvandenb
 *
 */
@SpringBootApplication
@Slf4j
@EnableScheduling
public class DCCSApplication {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
			
		ConfigurableApplicationContext ctx = SpringApplication.run(DCCSApplication.class, args);
		
	}

}
