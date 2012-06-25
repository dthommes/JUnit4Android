/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.junit4android;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.junit.Ignore;
import org.junit.runner.Description;
import org.junit.runner.Request;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Activity that runs JUnit tests.
 *
 * @author Daniel Thommes
 */
public class JunitTestRunnerActivity extends Activity {

	private static final String LOGTAG = "JUnit4Android";

	/**
	 * TestResult for display after the test is finished
	 *
	 * @author Daniel Thommes
	 */
	protected class JunitTestResult {

		Description description;
		List<Failure> failures = new LinkedList<Failure>();

		int testCount;
		boolean finished = false;
		String ignoreReason = null;
		// duration in ms
		long duration;

		/**
		 * @return the duration
		 */
		public long getDuration() {
			return duration;
		}

		/**
		 * @param description
		 * @param testCount
		 */
		public JunitTestResult(Description description, int testCount) {
			this.description = description;
			this.testCount = testCount;
		}

		public JunitTestResult(Description description, int testCount,
				String ignoreReason) {
			this.description = description;
			this.testCount = testCount;
			this.ignoreReason = ignoreReason;
		}

		/**
		 * @return the description
		 */
		public Description getDescription() {
			return description;
		}

		/**
		 * @return the failures
		 */
		public List<Failure> getFailures() {
			return failures;
		}

		public boolean isIgnored() {
			return ignoreReason != null;
		}

		public boolean addFailure(Failure object) {
			return failures.add(object);
		}

		@Override
		public String toString() {
			return description.getClassName();
		}

		public boolean hasFailures() {
			return !failures.isEmpty();
		}

	}

	/**
	 * {@link AsyncTask} that runs all tests in the background
	 *
	 * @author Daniel Thommes
	 */
	private class TestRunTask extends
			AsyncTask<Class<?>, JunitTestResult, Void> {

		boolean runHasFailures = false;
		int progress = 0;
		boolean rootTest = true;
		private int testCount = 0;

		/**
		 * {@inheritDoc}
		 *
		 * @see android.os.AsyncTask#doInBackground(Params[])
		 */
		protected Void doInBackground(Class<?>... testClasses) {
			runTests(testClasses);
			return null;
		}

		/**
		 * {@inheritDoc}
		 *
		 * @see android.os.AsyncTask#onProgressUpdate(Progress[])
		 */
		protected void onProgressUpdate(JunitTestResult... results) {
			JunitTestResult result = results[0];
			progressBar.setMax(result.testCount);
			++progress;
			Log.d(LOGTAG, "Test progress: " + progress + "/" + testCount);
			runHasFailures |= result.hasFailures();
			if (runHasFailures) {
				progressBar.setSecondaryProgress(progress);
			} else {
				progressBar.setProgress(progress);
			}
			addTestResult(result);
		}

		/**
		 * {@inheritDoc}
		 *
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		@Override
		protected void onPostExecute(Void result) {
			synchronized (JunitTestRunnerActivity.this) {
				startButton.setEnabled(true);
				setProgressBarIndeterminateVisibility(false);
				testListView.collapseGroup(testResultMap.size() - 1);
			}
			setCurrentTestTextView(JunitTestRunnerActivity.this.testClass
					.getName());
			Set<Entry<String, List<JunitTestResult>>> entrySet = testResultMap
					.entrySet();
			// TEST-org.apache.commons.beanutils.expression.DefaultResolverTestCase
			String directoryName = "junit4android/reports/";
			String fileName = "TEST-" + testClass.getName() + ".xml";
			try {
				JunitXmlWriter.writeXml(directoryName, fileName, testResultMap);
				Toast.makeText(
						JunitTestRunnerActivity.this,
						"Test Report has been saved as \n" + directoryName
								+ fileName, Toast.LENGTH_LONG).show();
			} catch (RuntimeException e) {
				e.printStackTrace();
				Toast.makeText(
						JunitTestRunnerActivity.this,
						e.getCause().getClass().getSimpleName() + ": "
								+ e.getCause().getMessage(), Toast.LENGTH_LONG)
						.show();
			}

		}

		/**
		 * {@inheritDoc}
		 *
		 * @see android.os.AsyncTask#onCancelled()
		 */
		@Override
		protected void onCancelled() {
			Toast.makeText(JunitTestRunnerActivity.this,
					"JUnit4Android: The test run has been cancelled.",
					Toast.LENGTH_LONG).show();
		}

		/**
		 * Helper to get an Junit3 test suite's static suite method
		 *
		 * @param clazz
		 * @return the suite method
		 */
		private Method getSuiteMethod(Class<?> clazz) {
			Method method;
			try {
				method = clazz.getMethod("suite");
			} catch (Exception e) {
				return null;
			}
			if (Modifier.isStatic(method.getModifiers())) {
				return method;
			}
			return null;
		}

		/**
		 * @param testClasses
		 */
		@SuppressWarnings("unchecked")
		private void runTests(Class<?>... testClasses) {
			for (Class<?> testClass : testClasses) {
				if (!TestCase.class.isAssignableFrom(testClass)) {
					/*************************************************************
					 * JUnit3 TestSuite handling because the below runner
					 * couldn't do it
					 *************************************************************/
					Method suiteMethod = getSuiteMethod(testClass);
					if (suiteMethod != null) {
						try {
							TestSuite suite = (TestSuite) suiteMethod.invoke(
									null, (Object[]) null);
							List<?> tests = Collections.list(suite.tests());
							// Find out the number of tests
							if (rootTest) {
								for (Object test : tests) {
									Request runnerRequest = Request
											.classWithoutSuiteMethod(test
													.getClass());
									Runner runner = runnerRequest.getRunner();
									testCount += runner.testCount();
								}
								rootTest = false;
							}
							for (Object test : tests) {
								Class<? extends Object> testCaseClass = test
										.getClass();
								runTests(testCaseClass);
							}
							// Test methods will not be considered in a suite
							continue;
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					}
				}

				/*************************************************************
				 * JUnit3 TestCases, JUnit4 Tests and Suites are run this way
				 *************************************************************/
				Request runnerRequest = Request
						.classWithoutSuiteMethod(testClass);
				Runner runner = runnerRequest.getRunner();
				if (rootTest) {
					testCount = runner.testCount();
					rootTest = false;
				}
				final RunNotifier notifier = new RunNotifier();

				notifier.addListener(new RunListener() {

					/**
					 * This result will be filled during the run and then added
					 * to the result list of this activity
					 */
					JunitTestResult result;

					long startTime;

					@Override
					public void testStarted(Description description)
							throws Exception {
						startTime = System.nanoTime();
						String displayName = description.getDisplayName();
						setCurrentTestTextView(description.getMethodName());
						Log.d(LOGTAG, "Test started: " + displayName);
						stopIfCancelled();
						result = new JunitTestResult(description, testCount);
					}

					@Override
					public void testIgnored(Description description)
							throws Exception {
						Log.d(LOGTAG,
								"Test ignored: " + description.getMethodName());
						stopIfCancelled();
						String ignoreReason = "";
						// Getting the reason for the ignore from the Ignore
						// annotation
						// http://tech.groups.yahoo.com/group/junit/messages/20125?threaded=1&m=e&var=1&tidx=1
						Collection<Annotation> annotations = description
								.getAnnotations();
						for (Iterator<Annotation> iterator = annotations
								.iterator(); iterator.hasNext();) {
							Annotation annotation = iterator.next();
							if (annotation.annotationType()
									.equals(Ignore.class)) {
								Ignore ignore = (Ignore) annotation;
								ignoreReason = ignore.value();
							}
						}
						JunitTestResult junitTestResult = new JunitTestResult(
								description, testCount, ignoreReason);
						publishProgress(junitTestResult);
					}

					@Override
					public void testFailure(Failure failure) throws Exception {
						stopIfCancelled();
						Log.e(LOGTAG,
								"Test Failure message: " + failure.getMessage());
						Log.e(LOGTAG, "Test Failure stacktrace:\n",
								failure.getException());
						result.addFailure(failure);
					}

					@Override
					public void testFinished(Description description)
							throws Exception {
						Log.d(LOGTAG,
								"Test finished: "
										+ description.getDisplayName());
						stopIfCancelled();
						result.finished = true;
						result.duration = (System.nanoTime() - startTime) / 1000000;
						publishProgress(result);
					}

					/**
					 * If the runTask is cancelled, stop the test run
					 */
					private void stopIfCancelled() {
						if (isCancelled()) {
							Log.d(LOGTAG,
									"The TestRunTask has been finished, asking the testRunner to stop.");
							notifier.pleaseStop();
						}
					}

				});
				runner.run(notifier);
			}
		}
	}

	private void setCurrentTestTextView(final String displayName) {
		runOnUiThread(new Runnable() {
			public void run() {
				testNameTextView.setText(displayName);
			}
		});
	}

	/**
	 * Adapter for the JunitTestResults
	 */
	public class ExpandableTestListAdapter extends BaseExpandableListAdapter {

		private final DataSetObservable mDataSetObservable = new DataSetObservable();

		public void registerDataSetObserver(DataSetObserver observer) {
			mDataSetObservable.registerObserver(observer);
		}

		public void unregisterDataSetObserver(DataSetObserver observer) {
			mDataSetObservable.unregisterObserver(observer);
		}

		/**
		 * Notifies the attached View that the underlying data has been changed
		 * and it should refresh itself.
		 */
		public void notifyDataSetChanged() {
			mDataSetObservable.notifyChanged();
		}

		public void notifyDataSetInvalidated() {
			mDataSetObservable.notifyInvalidated();
		}

		@Override
		@SuppressWarnings("unchecked")
		public List<JunitTestResult> getGroup(int groupPosition) {
			Object[] groups = testResultMap.values().toArray();
			Object group = groups[groupPosition];
			return (List<JunitTestResult>) group;
		}

		@Override
		public int getGroupCount() {
			return testResultMap.size();
		}

		@Override
		public JunitTestResult getChild(int groupPosition, int childPosition) {
			List<JunitTestResult> group = getGroup(groupPosition);
			return group.get(childPosition);
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			return getGroup(groupPosition).size();
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}

		/**
		 * Creates the view for a test method
		 *
		 * @see android.widget.ExpandableListAdapter#getChildView(int, int,
		 *      boolean, android.view.View, android.view.ViewGroup)
		 */
		@Override
		public View getChildView(int groupPosition, int childPosition,
				boolean isLastChild, View convertView, ViewGroup parent) {
			TextView textView = getGenericView();
			JunitTestResult testResult = getChild(groupPosition, childPosition);
			String text = testResult.description.getMethodName();
			if (testResult.hasFailures()) {
				textView.setTextColor(Color.RED);
				Failure failure = testResult.failures.get(0);
				text += "\n\"" + failure.getMessage() + "\"";
			} else if (testResult.isIgnored()) {
				textView.setTextColor(Color.YELLOW);
				text += "\nignored:\"" + testResult.ignoreReason + "\"";
			} else {
				textView.setTextColor(Color.GREEN);
			}
			textView.setText(text);
			return textView;
		}

		/**
		 * Creates the view for the test node
		 *
		 * @see android.widget.ExpandableListAdapter#getGroupView(int, boolean,
		 *      android.view.View, android.view.ViewGroup)
		 */
		@Override
		public View getGroupView(int groupPosition, boolean isExpanded,
				View convertView, ViewGroup parent) {
			TextView textView = getGenericView();
			List<JunitTestResult> group = getGroup(groupPosition);
			boolean hasFailures = false;
			for (JunitTestResult myTestResult : group) {
				hasFailures |= myTestResult.hasFailures();
			}
			textView.setText(group.get(0).description.getClassName());
			if (hasFailures) {
				textView.setTextColor(Color.RED);
			} else {
				textView.setTextColor(Color.GREEN);
			}
			return textView;
		}

		/**
		 * Helper
		 *
		 * @return
		 */
		private TextView getGenericView() {
			// Layout parameters for the ExpandableListView
			AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT);

			TextView textView = new TextView(JunitTestRunnerActivity.this);
			textView.setLayoutParams(lp);
			textView.setMinHeight(64);
			// Center the text vertically
			textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
			// Set the text starting position
			textView.setPadding(60, 3, 3, 0);
			return textView;
		}

	}

	/**
	 * Key for the extra to enable Autorun
	 */
	private static final String AUTORUN_EXTRA = "autorun";
	/**
	 * Key for the extra naming the test class
	 */
	private static final String TEST_CLASS_EXTRA = "testClass";
	/**
	 * ID for the context menu item to rerun a selected test
	 */
	private static final int RERUN_MENU_ITEM_ID = 0;
	/**
	 * Map with test results being displayed in the listView
	 */
	private LinkedHashMap<String, List<JunitTestResult>> testResultMap = new LinkedHashMap<String, List<JunitTestRunnerActivity.JunitTestResult>>();
	/**
	 * List adapter for the test results
	 */
	private ExpandableTestListAdapter testListAdapter;
	/**
	 * List view to display the test results
	 */
	private ExpandableListView testListView;
	/**
	 * Static variable used to display a detailed view of a selected test result
	 */
	public static JunitTestResult testResult4Detail;
	/**
	 * The test class to be run by this activity - can also be a test suite
	 */
	private Class<?> testClass;
	/**
	 * Start button reference
	 */
	private Button startButton;
	/**
	 * Text view displaying the name of the test suite
	 */
	private TextView testNameTextView;
	/**
	 * Progressbar displaying the test progress
	 */
	private ProgressBar progressBar;
	/**
	 * Name of the test the user has selected in the list
	 */
	private String selectedTestName;
	/**
	 * Flag indicating whether this activity start the test run after its start
	 * directly
	 */
	private boolean autorun;
	private TestRunTask testRunTask;

	/**
	 * {@inheritDoc}
	 *
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Progressbar in the status bar
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.junittestrunner);

		// wiring the listview with its adapter
		testListView = (ExpandableListView) findViewById(R.id.expandableListView);
		testListAdapter = new ExpandableTestListAdapter();
		testListView.setAdapter(testListAdapter);

		startButton = (Button) findViewById(R.id.startButton);
		testNameTextView = (TextView) findViewById(R.id.suiteNameTextView);
		progressBar = (ProgressBar) findViewById(R.id.progressBar);

		testListView
				.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
					@Override
					public boolean onChildClick(ExpandableListView parent,
							View v, int groupPosition, int childPosition,
							long id) {
						JunitTestResult result = testListAdapter.getChild(
								groupPosition, childPosition);
						testResult4Detail = result;
						startActivity(new Intent(JunitTestRunnerActivity.this,
								TestResultDetailActivity.class));

						return true;
					}
				});

		registerForContextMenu(testListView);

		loadAutorunFlag();

		try {
			testClass = loadTestClass();
			testNameTextView.setText(testClass.getName());
		} catch (Exception e) {
			startButton.setEnabled(false);
			Toast.makeText(
					JunitTestRunnerActivity.this,
					e.getClass().getSimpleName()
							+ "\nwhen loading the test (suite):\n\""
							+ e.getMessage() + "\"", Toast.LENGTH_LONG).show();
			testNameTextView.setSingleLine(false);
			testNameTextView.setText(e.getClass().getSimpleName() + ": "
					+ e.getMessage());
			testNameTextView.setTextColor(Color.RED);
			Log.e(LOGTAG, "Error loading the test class: ", e);
		}

	}

	public void loadAutorunFlag() {
		if (getIntent().hasExtra(AUTORUN_EXTRA)) {
			autorun = getIntent().getBooleanExtra(AUTORUN_EXTRA, false);
		} else {
			/*************************************************************
			 * Try to load the autoRun info from the activity's metadata
			 *************************************************************/
			Bundle bundle = getActivityMetadata();
			if (bundle != null) {
				autorun = bundle.getBoolean(AUTORUN_EXTRA);
			}
		}
	}

	/**
	 * @return
	 */
	private Bundle getActivityMetadata() {
		try {
			ActivityInfo activityInfo = getPackageManager().getActivityInfo(
					getComponentName(), PackageManager.GET_META_DATA);
			return activityInfo.metaData;
		} catch (NameNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see android.app.Activity#onStart()
	 */
	@Override
	protected void onStart() {
		super.onStart();
		if (autorun) {
			runTests();
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see android.app.Activity#onStop()
	 */
	@Override
	protected void onStop() {
		super.onStop();
		if (testRunTask != null) {
			testRunTask.cancel(true);
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu,
	 *      android.view.View, android.view.ContextMenu.ContextMenuInfo)
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		ExpandableListView.ExpandableListContextMenuInfo info = (ExpandableListView.ExpandableListContextMenuInfo) menuInfo;
		int type = ExpandableListView
				.getPackedPositionType(info.packedPosition);

		// Only create a context menu for group items
		if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
			selectedTestName = ((TextView) info.targetView).getText()
					.toString();
			menu.setHeaderTitle(selectedTestName);
			menu.add(0, RERUN_MENU_ITEM_ID, 0, "Rerun this test...");
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (item.getItemId() == RERUN_MENU_ITEM_ID) {
			Intent intent = new Intent(this, JunitTestRunnerActivity.class);
			intent.putExtra(TEST_CLASS_EXTRA, selectedTestName);
			intent.putExtra(AUTORUN_EXTRA, true);
			startActivity(intent);
		}
		return true;
	}

	/**
	 * @param view
	 */
	public void onStartTestClicked(View view) {
		runTests();
	}

	/**
	 * Runs the tests given in the test class
	 */
	private void runTests() {
		// Tests run automatically only once
		autorun = false;
		progressBar.setMax(0);
		progressBar.setProgress(0);
		progressBar.setSecondaryProgress(0);
		testResultMap.clear();
		startButton.setEnabled(false);
		setProgressBarIndeterminateVisibility(true);
		testRunTask = new TestRunTask();
		testRunTask.execute(testClass);
	}

	/**
	 * Helper to load the test class. First tries to load the class from a
	 * String Extra in the calling intent, then from the class name as given in
	 * the meta-data tag of this activity (key 'testClass'). If there is no
	 * class given via intent or metadata falls back to
	 * <appPackageName>.AllTests class.
	 *
	 * @return The test class to be run by this activity
	 * @throws NameNotFoundException
	 * @throws ClassNotFoundException
	 */
	private Class<?> loadTestClass() throws NameNotFoundException,
			ClassNotFoundException {
		/*************************************************************
		 * Try to get the testClass name from an extra
		 *************************************************************/
		String testSuiteClassName = getIntent()
				.getStringExtra(TEST_CLASS_EXTRA);
		if (testSuiteClassName == null) {
			/*************************************************************
			 * Try to load the testClass name from the activity's metadata
			 *************************************************************/
			Bundle bundle = getActivityMetadata();
			if (bundle != null) {
				testSuiteClassName = bundle.getString(TEST_CLASS_EXTRA);
			}
		}
		if (testSuiteClassName == null) {
			/*************************************************************
			 * Fall back to <packagename>.AllTests
			 *************************************************************/
			testSuiteClassName = getPackageName() + ".AllTests";
		}
		Class<?> suiteClass = Class.forName(testSuiteClassName);
		return suiteClass;
	}

	/**
	 * Adds a result of a test to the list view
	 *
	 * @param result
	 */
	private void addTestResult(JunitTestResult result) {
		String className = result.description.getClassName();
		List<JunitTestRunnerActivity.JunitTestResult> results = testResultMap
				.get(className);
		if (results == null) {
			results = new LinkedList<JunitTestRunnerActivity.JunitTestResult>();
			testResultMap.put(className, results);
		}
		testListView.expandGroup(testResultMap.size() - 1);
		if (testResultMap.size() > 1) {
			testListView.collapseGroup(testResultMap.size() - 2);
		}
		results.add(result);
		testListAdapter.notifyDataSetChanged();
		int groupCount = testListAdapter.getGroupCount();
		int childrenCount = testListAdapter.getChildrenCount(groupCount - 1);
		int scrollPosition = groupCount + childrenCount - 1;
		testListView.smoothScrollToPosition(scrollPosition);
	}
}
