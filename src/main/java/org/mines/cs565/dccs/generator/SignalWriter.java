/**
 * 
 */
package org.mines.cs565.dccs.generator;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.mines.cs565.dccs.sampler.Measurement;
import org.springframework.beans.factory.annotation.Autowired;
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

	// Delimiter used in CSV file
	private static final String NEW_LINE_SEPARATOR = "\n";
	// CSV file header
	private static final Object[] FILE_HEADER = { "timestamp", "value" };

	Optional<FileWriter> fileWriter = null;
	Optional<CSVPrinter> csvFilePrinter = null;
	// Create the CSVFormat object with "\n" as a record delimiter
	CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator(NEW_LINE_SEPARATOR);


	@Autowired
	private SignalProperties properties;

	@PostConstruct
	void init() {
		log.info("Initialize the SignalWriter");

		// initialize FileWriter object
		try {
			fileWriter = Optional.of(new FileWriter(properties.getOutputFileName(), true));

			// initialize CSVPrinter object
			csvFilePrinter = Optional.of(new CSVPrinter(fileWriter.get(), csvFileFormat));

			// Create CSV file header
			csvFilePrinter.get().printRecord(FILE_HEADER);

		} catch (IOException e) {
			log.error("Unable to create file [{}], due to {}", properties.getOutputFileName(), e.getCause().getMessage());
		}

	}

	/**
	 * Write out a single measurement to the output file
	 * @param m
	 */
	@Async
	public void write(Measurement<?> m) {
		if (csvFilePrinter.isPresent()) {
			List<String> r = new ArrayList<String>();

			r.add(String.valueOf(m.getTimeTick()));
			r.add(String.valueOf(m.getValue()));

			try {
				csvFilePrinter.get().printRecord(r);
			} catch (IOException e) {
				log.warn("Unable to write to csv file");
			}
		}
	}

	/**
	 * Close out anything that might be considered open
	 */
	@PreDestroy
	void done() {
		// if (csvFilePrinter.isPresent()) {
		// csvFilePrinter.
		if (fileWriter.isPresent()) {
			try {
				fileWriter.get().close();
			} catch (IOException e) {
				log.error("Unable to close writer");
			}
		}

	}

}
