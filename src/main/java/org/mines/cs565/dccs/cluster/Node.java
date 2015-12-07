/**
 * 
 */
package org.mines.cs565.dccs.cluster;

import org.crsh.console.jline.internal.Log;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.net.HostAndPort;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author hvandenb
 *
 */
@Slf4j
public class Node implements Comparable<Node>{

	@Getter
	private String Id;
	private HostAndPort hp;
	
	static public String generateId(HostAndPort hp) {
		HashFunction hf = Hashing.sha1();
		HashCode hc = hf.newHasher()
		       .putString(hp.getHostText(), Charsets.UTF_8)
		       .putInt(hp.getPort())
		       .hash();
		log.debug("Id generation: {} generated {}", hp, hc.toString());
		return hc.toString();
		
		//return Long.toString(hc.asInt());
		
	}
	
	static public String generateId(String host, int port) {
		return Node.generateId(HostAndPort.fromParts(host, port));
	}
	
	Node(String Id) {
		this.Id = Strings.nullToEmpty(Id);
	}

	Node(String Id, HostAndPort hp)
	{
		this(Id);
		this.hp = hp;
	}
	
	Node(HostAndPort hp)
	{
		this.Id = Strings.nullToEmpty(generateId(hp));
		this.hp = hp;
	}

	@Override
	public int compareTo(Node o) {
		// TODO Auto-generated method stub
		return 0;
	}

}
