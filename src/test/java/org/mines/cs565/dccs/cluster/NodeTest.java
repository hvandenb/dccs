/**
 * 
 */
package org.mines.cs565.dccs.cluster;

import static org.junit.Assert.*;

import org.junit.Test;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.net.HostAndPort;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Henri van den Bulk
 *
 */
@Slf4j
public class NodeTest {

	@Test
	public void testId() {
		Node n = new Node("123");
		
		assertTrue(n.getId().equals("123"));
	}
	
	@Test
	public void generateId() {
		HostAndPort hp = HostAndPort.fromString("127.0.0.1:8000").withDefaultPort(8000);
		
		Node n = new Node(hp);
		String id = Node.generateId(hp);
		log.info("Generated id {} from {} is  {}", n.getId(), hp.toString(), id);
		assertTrue(String.format("Generated id {} from {} is not equal to {}", n.getId(), hp.toString(), id), n.getId().equals(id));
	}

	@Test
	public void generateNonMatchingId() {
		HostAndPort hp = HostAndPort.fromString("127.0.0.1:8000").withDefaultPort(8000);
		HostAndPort hp2 = HostAndPort.fromString("127.0.0.1:8001").withDefaultPort(8000);
		
		Node n = new Node(hp);
		String id = Node.generateId(hp2);
		log.info("Generated id {} from {} should not be the same as {}", n.getId(), hp.toString(), id);
		assertFalse(String.format("Generated id %s from %s is should not equal to %s", n.getId(), hp.toString(), id), n.getId().equals(id));
	}

	
}
