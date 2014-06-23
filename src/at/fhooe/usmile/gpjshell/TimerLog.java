package at.fhooe.usmile.gpjshell;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.os.SystemClock;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.util.Log;

public class TimerLog {
	private static final String LOG_TAG = "TsLog";

	public enum LogEvent {
		CAP_LOAD_FINISHED, APPLET_DELETE_FINISHED, APPLET_INSTALL_FINISHED, ECHO_TEST_FINISHED
	};

	private List<Map<LogEvent, Integer>> mDurations = null;

	private long currentStart;

	public TimerLog() {
		mDurations = new ArrayList<Map<LogEvent, Integer>>();
	}

	public void beginTest() {
		mDurations.add(new HashMap<TimerLog.LogEvent, Integer>());
		start();
	}

	private void start() {
		currentStart = SystemClock.elapsedRealtime();
	}

	void log(LogEvent event) {
		if (mDurations.size() > 0) {
			Integer duration = (int) (SystemClock.elapsedRealtime() - currentStart);
			Log.d(LOG_TAG, "duration: " + event.toString() + ": " + duration);

			Map<LogEvent, Integer> currentRun = mDurations.get(mDurations
					.size() - 1);
			currentRun.put(event, duration);

			start();
		}
	}

	public void writeToFile(String fileName, LogEvent[] eventsToWrite) {
		BufferedWriter writer;
		try {
			writer = new BufferedWriter(new FileWriter(fileName));

			// write header
			for (LogEvent event : eventsToWrite) {
				writer.write(", " + event.toString());
			}
			writer.write('\n');

			// write one run per line
			int i = 0;
			for (Map<LogEvent, Integer> run : mDurations) {
				writer.write(String.valueOf(i));
				for (LogEvent event : eventsToWrite) {
					Integer duration = run.get(event);
					if (duration == null) {
						writer.write(", ");
					} else {
						writer.write(", " + duration);
					}
				}
				writer.write('\n');
				++i;
			}
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
