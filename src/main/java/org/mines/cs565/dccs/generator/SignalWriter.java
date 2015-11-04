/**
 * 
 */
package org.mines.cs565.dccs.generator;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Component;

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

	
}
