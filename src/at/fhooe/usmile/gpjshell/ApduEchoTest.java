package at.fhooe.usmile.gpjshell;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

import net.sourceforge.gpj.cardservices.AID;
import net.sourceforge.gpj.cardservices.AIDRegistryEntry;
import net.sourceforge.gpj.cardservices.ISO7816;
import net.sourceforge.gpj.cardservices.interfaces.GPTerminal;
import net.sourceforge.gpj.cardservices.interfaces.NfcSmartcardChannel;
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
import android.text.Html;
import android.util.Log;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import at.fhooe.usmile.gpjshell.MainActivity.APDU_COMMAND;
import at.fhooe.usmile.gpjshell.TimerLog.LogEvent;
import at.fhooe.usmile.gpjshell.objects.GPChannelSet;
import at.fhooe.usmile.gpjshell.objects.GPKeyset;

public class ApduEchoTest extends Activity {

	public static final String EXTRA_RUNS = "at.fhooe.usmile.gpjshell.ApduEchoTest.runs";
	public static final String EXTRA_STEP_SIZE = "at.fhooe.usmile.gpjshell.ApduEchoTest.step_size";
	public static final String EXTRA_STEPS = "at.fhooe.usmile.gpjshell.ApduEchoTest.steps";
	private static final String LOG_TAG = "Echo Test";
	public static final String EXTRA_KEYSET = "at.fhooe.usmile.gpjshell.AppletInstallTest.keyset";
	public static final String EXTRA_CHANNELSET = "at.fhooe.usmile.gpjshell.AppletInstallTest.channelset";
	private static final String LOG_FILE = "/storage/sdcard0/apdu_echo_test.log";

	private static final String LOG_INFO = "Info";
	private static final String LOG_RECEIVED = "Received";
	private static final String LOG_SENT = "Sent";

	private static final String APPLET_AID = "F04D524F4C4543484F";

	private boolean mCancelled = false;
	private int mRuns;
	private int mStepSize;
	private int mSteps;

	private TestRunner mTestRunner = null;

	private ProgressBar mProgress = null;
	private GPKeyset mKeySet;
	private GPChannelSet mChannelSet;
	private Integer mSeekReader;
	private GPTerminal mTerm = null;
	private CardChannel mChannel = null;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_echo_test);

		mRuns = (Integer) getIntent().getSerializableExtra(EXTRA_RUNS);
		mStepSize = (Integer) getIntent().getSerializableExtra(EXTRA_STEP_SIZE);
		mSteps = (Integer) getIntent().getSerializableExtra(EXTRA_STEPS);

		mProgress = (ProgressBar) findViewById(R.id.echo_test_progress);
		mProgress.setMax(mSteps);

		mTestRunner = new TestRunner();
		mTestRunner.execute();
	}

	protected void onPause() {
		super.onPause();
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

		TimerLog tsLog = new TimerLog();

		mTerm = NfcTerminal.getInstance(getApplicationContext());
		try {
			mChannel = mTerm.connect("*").getBasicChannel();
		} catch (CardException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		CommandAPDU cmd = new CommandAPDU(ISO7816.CLA_ISO7816,
				ISO7816.INS_SELECT, 0x04, 0x00,
				GPUtils.convertHexStringToByteArray(APPLET_AID));

		ResponseAPDU resp = transmit(cmd);
		if (resp == null) {
			log(LOG_INFO, "Error selecting applet");

		}

		log(LOG_INFO, GPUtils.byteArrayToString(resp.getBytes()));
		if (resp.getSW() != 0x9000) {
			log(LOG_INFO, "Error selecting applet");
			return;
		}

		log(LOG_INFO, "Applet selected");

		Random rnd = new Random(0);

		try {

			for (int size = mStepSize; size <= mSteps * mStepSize; size += mStepSize) {

//				if (size > 255 && mChannel instanceof NfcSmartcardChannel
//						&& !((NfcSmartcardChannel)mChannel).supportsExtendedLengthApdus()) {
//					throw new Exception("Reader does not support extended length APDUs");
//					
//				}

				ByteArrayOutputStream bo = new ByteArrayOutputStream();

				// le
				if (size > 255) {
					bo.write(0);
					bo.write(size & 0xFF);
					bo.write((size >> 8) & 0xFF);
				} else {
					bo.write(size);
				}

				byte[] payload = new byte[size];
				rnd.nextBytes(payload);
				try {
					bo.write(payload);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				// lc
				if (size > 255) {
					bo.write(0);
					bo.write(size & 0xFF);
					bo.write((size >> 8) & 0xFF);
				} else {
					bo.write(size);
				}

				tsLog.beginTest();

				cmd = new CommandAPDU(0x80, 0x01, 0x00, 0x00, bo.toByteArray());
				for (int i = 0; i < mRuns; ++i) {
					if (mCancelled) {
						break;
					}

					resp = transmit(cmd);

					if (resp == null) {
						throw new Exception("Error in echo test");

					}
					// log(LOG_SENT, GPUtils.byteArrayToString(cmd.getBytes()));
					// log(LOG_RECEIVED,
					// GPUtils.byteArrayToString(resp.getBytes()));

				}

				tsLog.log(LogEvent.ECHO_TEST_FINISHED);
				final int progress = size / mStepSize;

				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						mProgress.setProgress(progress);
					}
				});

			}
		} catch (Exception e) {
			log(LOG_INFO, e.getMessage());
		}
		log(LOG_INFO, "Test finished, writing log to " + LOG_FILE);

		tsLog.writeToFile(LOG_FILE,
				new LogEvent[] { LogEvent.ECHO_TEST_FINISHED });

		close();
	}

	private ResponseAPDU transmit(CommandAPDU cmd) {
		if (mChannel == null) {
			log(LOG_INFO, "Error, channel not opened");
		}
		try {
			return mChannel.transmit(cmd);
		} catch (CardException e) {
			log(LOG_INFO, "Error transmitting APDU");
			e.printStackTrace();
		}
		return null;
	}

	private void log(final String tag, final String msg) {

		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				MainActivity.log().d(LOG_TAG, tag + ": " + msg);
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
