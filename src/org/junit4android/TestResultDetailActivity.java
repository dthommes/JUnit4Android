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

import java.util.List;

import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit4android.JunitTestRunnerActivity.JunitTestResult;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Activity to show the detailed test results
 *
 * @author Daniel Thommes
 */
public class TestResultDetailActivity extends Activity {

	/**
	 * Color of the text depending on the test being successful, failed or
	 * ignored
	 */
	private int textColor;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.junittestdetails);

		JunitTestResult result = JunitTestRunnerActivity.testResult4Detail;
		Description description = result.description;
		List<Failure> failures = result.failures;

		TextView classNameText = (TextView) findViewById(R.id.classNameText);
		TextView methodNameText = (TextView) findViewById(R.id.methodNameText);
		TextView shortDetailsText = (TextView) findViewById(R.id.shortDetailsText);
		ListView failureList = (ListView) findViewById(R.id.failureList);

		classNameText.setText(description.getClassName());
		methodNameText.setText(description.getMethodName());

		textColor = Color.GREEN;
		if (result.hasFailures()) {
			textColor = Color.RED;
		} else if (result.isIgnored()) {
			textColor = Color.YELLOW;
			shortDetailsText.setText("Ignored:\"" + result.ignoreReason + "\"");
		} else {
			// test passed
			shortDetailsText.setText("Passed");
		}
		classNameText.setTextColor(textColor);
		methodNameText.setTextColor(textColor);
		shortDetailsText.setTextColor(textColor);

		failureList.setAdapter(new FailureListAdapter(this,
				android.R.layout.simple_list_item_1, failures));

	}

	private class FailureListAdapter extends ArrayAdapter<Failure> {

		public FailureListAdapter(Context context, int textViewResourceId,
				List<Failure> objects) {
			super(context, textViewResourceId, objects);
		}

		/**
		 * {@inheritDoc}
		 *
		 * @see android.widget.ArrayAdapter#getView(int, android.view.View,
		 *      android.view.ViewGroup)
		 */
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Failure failure = getItem(position);
			TextView textView = new TextView(TestResultDetailActivity.this);
			textView.setText("\"" + failure.getMessage() + "\"\n\n"
					+ failure.getTrace());

			textView.setTextColor(textColor);
			return textView;
		}

	}
}
