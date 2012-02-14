/**
 * Created on 10.02.2012
 *
 * © 2012 Daniel Thommes
 */
package org.junit4android.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Example for a JUnit3 test suite
 *
 * @author Daniel Thommes
 */
public class Junit3TestSuite {

	public static Test suite() {
		TestSuite suite = new TestSuite(Junit3TestSuite.class.getName());
		// $JUnit-BEGIN$
		suite.addTest(new Junit3TestCase());
		suite.addTest(new Junit3TestCase2());
		// $JUnit-END$
		return suite;
	}

}
