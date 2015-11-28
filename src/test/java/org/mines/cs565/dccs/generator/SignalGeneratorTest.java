/**
 * 
 */
package org.mines.cs565.dccs.generator;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mines.cs565.dccs.DCCSApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Henri M.B. van den Bulk
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = DCCSApplication.class)
@WebIntegrationTest(randomPort = true)
@Slf4j
public class SignalGeneratorTest {

	@Autowired
    SignalGenerator generator;
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		generator.reset();
		generator.run();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {

		
		fail("Not yet implemented---> " + generator.getValue());
	}

}
