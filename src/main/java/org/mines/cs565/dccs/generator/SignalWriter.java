/**
 * 
 */
package org.mines.cs565.dccs.generator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.mines.cs565.dccs.sampler.Measurement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.google.common.base.Optional;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Henri M.B. van den Bulk
 *
 */
@Slf4j
@Component
public class SignalWriter {

	//Delimiter used in CSV file
		private static final String NEW_LINE_SEPARATOR = "\n";
		//CSV file header
		private static final Object [] FILE_HEADER = {"timestamp","value"};
		
		FileWriter fileWriter = null;
		Optional<CSVPrinter> csvFilePrinter = null;
		//Create the CSVFormat object with "\n" as a record delimiter
        CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator(NEW_LINE_SEPARATOR);
        
        @Value("${generator.file")
    	private String fileName = "wave.csv";

		@PostConstruct
		void init() {
			log.info("Initialize the SignalWriter");
			
			//initialize FileWriter object
			try {
				fileWriter = new FileWriter(fileName, true);
				
				//initialize CSVPrinter object 
		        csvFilePrinter = Optional.of(new CSVPrinter(fileWriter, csvFileFormat));
		        
		        //Create CSV file header
		        csvFilePrinter.get().printRecord(FILE_HEADER);
		        
			} catch (IOException e) {
				log.error("Unable to create file [{}], due to {}", fileName, e.getCause().getMessage());
			}

		}
		
		@Async
		public void write(Measurement m) {
			if (csvFilePrinter.isPresent()) {
				List r = new ArrayList();
	            
				r.add(String.valueOf(m.getTimeTick()));
	            r.add(String.valueOf(m.getValue()));
	            
	            try {
					csvFilePrinter.get().printRecord(r);
				} catch (IOException e) {
					log.warn("Unable to write to csv file");
				}
			}
		}
		
		@PreDestroy
		void done() {
//			if (csvFilePrinter.isPresent()) {
//				csvFilePrinter.
			if (fileWriter != null) {
				try {
					fileWriter.close();
				} catch (IOException e) {
					log.error("Unable to close writer");
				}
			}
			
		}
	
}
