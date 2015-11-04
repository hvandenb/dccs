/**
 * 
 */
package org.mines.cs565.dccs.sampler;

import java.util.BitSet;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author Henri M.B. van den Bulk
 *
 */
@Data
@AllArgsConstructor
public class Measurement{

	private long timeTick = 0;
	private float value = 0;;
	
	
}
