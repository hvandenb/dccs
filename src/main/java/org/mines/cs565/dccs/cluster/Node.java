/**
 * 
 */
package org.mines.cs565.dccs.cluster;

import com.google.common.base.Charsets;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.net.HostAndPort;

/**
 * @author hvandenb
 *
 */
public class Node {

	String Id;
	
	private long generateId(HostAndPort hp) {
		HashFunction hf = Hashing.sha1();
		HashCode hc = hf.newHasher()
		       .putString(hp.getHostText(), Charsets.UTF_8)
		       .putInt(hp.getPort())
		       .hash();
		
		return hc.asLong();
		
	}
	
}
