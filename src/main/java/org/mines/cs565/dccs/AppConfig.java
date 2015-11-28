package org.mines.cs565.dccs;

import org.mines.cs565.dccs.generator.GeneratorService;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;


@Configuration
@ComponentScan
@EnableAutoConfiguration
public class AppConfig {

	
	
	@Bean
	public GeneratorService generatorService() {
	    return new GeneratorService();
	}
}
