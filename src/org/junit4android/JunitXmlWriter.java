/**
 * Created on 23.05.2012
 *
 * © 2012 Daniel Thommes
 */
package org.junit4android;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.runner.notification.Failure;
import org.junit4android.JunitTestRunnerActivity.JunitTestResult;
import org.xmlpull.v1.XmlSerializer;

import android.os.Environment;
import android.util.Log;
import android.util.Xml;

/**
 *
 *
 * @author Daniel Thommes
 */
public class JunitXmlWriter {

	public static void writeXml(String directoryName, String fileName,
			HashMap<String, List<JunitTestResult>> testResultMap) {

		XmlSerializer serializer = Xml.newSerializer();

		File storageDir = Environment.getExternalStorageDirectory();
		File junitDir = new File(storageDir, directoryName);
		junitDir.mkdirs();
		File reportFile = new File(junitDir, fileName);

		Writer writer = null;
		try {
			writer = new PrintWriter(reportFile, "UTF-8");
			serializer.setOutput(writer);
			serializer.startDocument("UTF-8", true);
			serializer.startTag("", "testsuites");

			// TODO: Properties from
			// https://developer.android.com/reference/android/os/Build.html

			Set<Entry<String, List<JunitTestResult>>> testSuites = testResultMap
					.entrySet();
			for (Entry<String, List<JunitTestResult>> testSuite : testSuites) {
				writeTestSuite(serializer, testSuite);
			}

			serializer.endTag("", "testsuites");
			serializer.endDocument();
			serializer.flush();
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void writeTestSuite(XmlSerializer serializer,
			Entry<String, List<JunitTestResult>> testSuite) throws IOException {
		Log.d("Junit4Android-XMLWriter",
				"Writing test suite: " + testSuite.getKey());
		int tests = 0;
		int errors = 0;
		int skipped = 0;
		long duration = 0;

		List<JunitTestResult> testCases = testSuite.getValue();

		for (JunitTestResult testCase : testCases) {
			++tests;
			duration += testCase.getDuration();
			if (testCase.isIgnored()) {
				++skipped;
			}
			if (testCase.hasFailures()) {
				++errors;
			}
		}

		serializer.startTag("", "testsuite");
		// <testsuite failures="0" time="0.098" errors="0" skipped="0"
		// tests="8"
		// name="org.apache.commons.beanutils.BeanComparatorTestCase">
		serializer.attribute("", "failures", "" + 0);
		serializer.attribute("", "time", "" + ((double) duration) / 1000);
		serializer.attribute("", "errors", "" + errors);
		serializer.attribute("", "skipped", "" + skipped);
		serializer.attribute("", "tests", "" + tests);
		serializer.attribute("", "name", testSuite.getKey());
		for (JunitTestResult testCase : testCases) {
			writeTestCase(serializer, testCase);
		}
		serializer.endTag("", "testsuite");
	}

	public static void writeTestCase(XmlSerializer serializer,
			JunitTestResult result) throws IOException {
		// <testcase time="0.071"
		// classname="org.apache.commons.beanutils.BeanComparatorTestCase"
		// name="testSimpleCompare"/>
		serializer.startTag("", "testcase");
		serializer.attribute("", "time", "" + ((double) result.getDuration())
				/ 1000.0);
		serializer.attribute("", "classname", result.getDescription()
				.getClassName());
		serializer.attribute("", "name", result.getDescription()
				.getMethodName());
		if (result.isIgnored()) {
			// <skipped/>
			serializer.startTag("", "skipped").endTag("", "skipped");
		}
		if (result.hasFailures()) {
			for (Failure failure : result.getFailures()) {
				Throwable throwable = failure.getException();
				if (throwable != null && throwable instanceof Exception) {
					serializer.startTag("", "error");
					serializer.attribute("", "message",
							"" + throwable.getMessage());
					serializer.attribute("", "type", throwable.getClass()
							.getName());
					serializer.text(failure.getTrace());
					serializer.endTag("", "error");
				} else if (throwable != null) {
					serializer.startTag("", "failure");
					serializer.text(failure.getTrace());
					serializer.endTag("", "failure");
				}
			}
		}
		serializer.endTag("", "testcase");
	}
}
