package at.fhooe.usmile.gpjshell;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import javax.smartcardio.CardException;

import net.sourceforge.gpj.cardservices.AID;
import net.sourceforge.gpj.cardservices.AIDRegistryEntry;
import net.sourceforge.gpj.cardservices.interfaces.NfcTerminal;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.widget.ListView;
import android.widget.ProgressBar;
import at.fhooe.usmile.gpjshell.MainActivity.APDU_COMMAND;
import at.fhooe.usmile.gpjshell.TimerLog.LogEvent;
import at.fhooe.usmile.gpjshell.objects.GPChannelSet;
import at.fhooe.usmile.gpjshell.objects.GPKeyset;

public class AppletInstallTest extends Activity {

	public static final String EXTRA_RUNS = "at.fhooe.usmile.gpjshell.AppletInstallTest.runs";
	public static final String EXTRA_APPLET_URI = "at.fhooe.usmile.gpjshell.AppletInstallTest.applet_uri";
	private static final String LOG_TAG = "Applet Test";
	public static final String EXTRA_KEYSET = "at.fhooe.usmile.gpjshell.AppletInstallTest.keyset";
	public static final String EXTRA_CHANNELSET = "at.fhooe.usmile.gpjshell.AppletInstallTest.channelset";
	private static final String LOG_FILE = "/storage/sdcard0/applet_install_test.log";

	private boolean mCancelled = false;
	private int mRuns;
	private String mAppletUri = null;
	private TestRunner mTestRunner = null;

	private ProgressBar mProgress = null;
	private GPKeyset mKeySet;
	private GPChannelSet mChannelSet;
	private Integer mSeekReader;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_applet_install_test);

		mRuns = (Integer) getIntent().getSerializableExtra(EXTRA_RUNS);
		mAppletUri = (String) getIntent()
				.getSerializableExtra(EXTRA_APPLET_URI);

		mKeySet = (GPKeyset) getIntent().getSerializableExtra(EXTRA_KEYSET);
		mChannelSet = (GPChannelSet) getIntent().getSerializableExtra(
				EXTRA_CHANNELSET);

		mProgress = (ProgressBar) findViewById(R.id.applet_install_test_progress);
		mProgress.setMax(mRuns);
		mTestRunner = new TestRunner();
		mTestRunner.execute();
	}

	protected void onPause() {
		super.onPause();
		Log.d(LOG_TAG, "onpause");
		mCancelled = true;
	}

	private void close() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				finish();
			}
		});

	}

	public void runTest(Integer nRuns) {
		mCancelled = false;

		GPConnection conn = GPConnection.getInstance(getApplicationContext());

		AID capAid = null;
		try {
			capAid = CAPFile.readAID(mAppletUri);
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		d("AID: " + capAid.toString());
		try {
			conn.loadAppletsfromCard();
		} catch (CardException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		for (AIDRegistryEntry a : conn.getRegistry()) {
			d("found aid " + a.getAID().toString());
			if (a.getAID().equals(capAid)) {
				d("Found applet " + capAid.toString() + ", deleting");
				conn.deleteApplet(capAid);
				break;
			}
		}

		TimerLog tsLog = new TimerLog();

		conn.setTimestampLog(tsLog);
		GPCommand c = new GPCommand(APDU_COMMAND.APDU_INSTALL, 0, null,
				(byte) 0, mAppletUri);

		// performCommand(APDU_COMMAND.APDU_DELETE_BY_AID,
		// mReaderSpinner.getSelectedItemPosition(),
		// params1, privileges1, capAid);
		//
		// performCommand(APDU_COMMAND.APDU_INSTALL,
		// mReaderSpinner.getSelectedItemPosition(),
		// params1, privileges1, mAppletUrl);
		//
		// performCommand(APDU_COMMAND.REPORT_END_OF_QUEUE, 0,
		// null, (byte) 0, this);

		d("Starting test: " + mAppletUri);
		mProgress.setProgress(0);
		for (int i = 0; i < mRuns; ++i) {
			if (mCancelled) {
				break;
			}
			long start = SystemClock.elapsedRealtime();

			tsLog.beginTest();

			// load + install applet
			conn.performCommand(
					NfcTerminal.getInstance(getApplicationContext()), mKeySet,
					mChannelSet, c);

			// delete applet
			conn.deleteApplet(capAid);

			long stop = SystemClock.elapsedRealtime();

			mProgress.setProgress(i + 1);
		}

		tsLog.writeToFile(LOG_FILE, new LogEvent[] {
				LogEvent.CAP_LOAD_FINISHED,
				LogEvent.APPLET_INSTALL_FINISHED,
				LogEvent.APPLET_DELETE_FINISHED });

		d("Log written to " + LOG_FILE);

		close();
	}

	private void d(final String msg) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				MainActivity.LogMe log = MainActivity.log();
				log.d(LOG_TAG, msg);
			}
		});
	}

	private class TestRunner extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... _cmd) {
			runTest(mRuns);
			return null;
		}

	}

}
