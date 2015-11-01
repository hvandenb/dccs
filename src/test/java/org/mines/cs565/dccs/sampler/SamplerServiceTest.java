/**
 * 
 */
package org.mines.cs565.dccs.sampler;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.mines.cs565.dccs.DCCSApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;

/**
 * @author henrivandenbulk
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = DCCSApplication.class)
@WebIntegrationTest(randomPort = true)
public class SamplerServiceTest {

	@Autowired
	private SamplerService sampler;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void basicTest() {
		assertNotNull("Sampler can not be null", sampler);
	}
	@Test
	public void testCalculateSampleFrequency() {

		long interval = this.sampler.calculateSamplingInterval(500); 
		assertTrue("Not 2ms", interval == 2);

	}

}
