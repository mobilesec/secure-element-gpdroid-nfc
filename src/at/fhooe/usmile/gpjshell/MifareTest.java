package at.fhooe.usmile.gpjshell;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import android.R.bool;
import android.content.Context;
import android.nfc.tech.MifareClassic;
import android.os.AsyncTask;
import android.os.SystemClock;
import at.fhooe.usmile.gpjshell.MainActivity.LogMe;

public class MifareTest extends AsyncTask<Void, String, Void> {

	private final static String LOG_TAG = "MF Test";
	private MifareClassic mTag = null;
	private MainActivity mContext = null;
	private MainActivity.LogMe mLog = null;
	private static final int FIRST_SECTOR = 1;
	private Log mTestLog;
	private static final int nRuns = 1000;
	private boolean mRunning = false;
	private static final byte[][] sectorKeys = new byte[][] {
			{ (byte) 0xA5, (byte) 0xA5, (byte) 0xA5, (byte) 0xA5, (byte) 0xA5,
					(byte) 0xA5 }, // "key0"
			{ (byte) 0x5A, (byte) 0x5A, (byte) 0x5A, (byte) 0x5A, (byte) 0x5A,
					(byte) 0x5A } }; // "key1"

	public MifareTest(MifareClassic tag, Context context, LogMe log) {
		mTag = tag;
		mContext = (MainActivity) context;
		mLog = log;
		mTestLog = new Log();
	}

	protected Void doInBackground(Void... v) {

		if (mTag.getType() != MifareClassic.TYPE_CLASSIC
				|| mTag.getSize() != MifareClassic.SIZE_1K) {
			log("Invalid card type, only supporting Mifare Classic 1k");
			return null;
		}

		log("Preparing tag...");
		resetKeys();
		
		mRunning = true;

		for (int i = 0; i < nRuns; ++i) {
			if (isCancelled())
				break;
			runSingle(i);
		}

		try {
			mTestLog.writeToFile("/storage/sdcard0/mifare_test.log");
		} catch (IOException e) {
			log("Error writing to log file");
		}
		
		log("Restoring default keys");
		resetKeys();

		mRunning = false;
		return null;
	}

	private void resetKeys() {
		int nSectors = mTag.getSectorCount();

		if (!mTag.isConnected()) {
			try {
				mTag.connect();
			} catch (IOException e1) {
				mLog.d(LOG_TAG, "Error connecting to tag");
				return;
			}
		}

		byte[] trailer = buildTrailer(MifareClassic.KEY_DEFAULT,
				MifareClassic.KEY_DEFAULT);

		for (int s = FIRST_SECTOR; s < nSectors; ++s) {
			// get last block of sector
			int blockIdx = mTag.sectorToBlock(s)
					+ mTag.getBlockCountInSector(s) - 1;
			try {
				authenticateA(s, sectorKeys[0]);
				mTag.writeBlock(blockIdx, trailer);
				continue;
			} catch (Exception e) {
				log("Error writing to sector, trying keyB");
			}
			try {
				authenticateB(s, sectorKeys[0]);
				mTag.writeBlock(blockIdx, trailer);
			} catch (Exception e) {
				log("Error writing to sector, trying keyB");
			}
			
		}
	}

	public boolean isRunning() {
		return mRunning;
	}

	protected void onPostExecute(Void result) {
		mLog.d(LOG_TAG, "Test finished");
		mContext.mifareTestFinished();
	}

	protected void onCancelled() {
		mLog.d(LOG_TAG, "Test cancelled");

		mContext.mifareTestFinished();
	}

	protected void onProgressUpdate(String... status) {
		mLog.d(LOG_TAG, status[0]);

	}

	private void runSingle(int index) {
		Random rnd = new Random(index);

		int nSectors = mTag.getSectorCount();
		int nBlocksWritten = 0;
		if (!mTag.isConnected()) {
			try {
				mTag.connect();
			} catch (IOException e1) {
				log("Error connecting to tag");
				return;
			}
		}

		long start = SystemClock.elapsedRealtime();

		for (int s = FIRST_SECTOR; s < nSectors; ++s) {
			try {
				nBlocksWritten += fillSector(index, s, rnd);
			} catch (Exception e) {
				log("Error writing to sector " + s + ": " + e.getMessage());
				return;
			}
		}

		rnd.setSeed(index);

		for (int s = FIRST_SECTOR; s < nSectors; ++s) {
			try {
				verifySector(s, index, rnd);
			} catch (Exception e) {
				log("Error verifying sector " + s + ": " + e.getMessage());
				return;
			}
		}

		long stop = SystemClock.elapsedRealtime();

		mTestLog.addEntry(nBlocksWritten, (int) (stop - start));
		String status = "Duration of run " + index + ": "
				+ (int) (stop - start) + "ms";
		log(status);
	}

	private byte[] buildTrailer(byte[] keyA, byte[] keyB) {
		byte[] trailer = new byte[16];

		for (int i = 0; i < keyA.length; ++i) {
			trailer[i] = keyA[i];
		}
		trailer[6] = (byte) 0xFF;
		trailer[7] = (byte) 0x07;
		trailer[8] = (byte) 0x80;

		// use key0 or key1 as new keyB, depending on test index
		for (int i = 0; i < keyB.length; ++i) {
			trailer[10 + i] = keyB[i];
		}
		return trailer;
	}

	private int fillSector(int index, int sector, Random rnd) throws Exception {

		int blocksToWrite = mTag.getBlockCountInSector(sector);
		byte[] key = sectorKeys[index & 0x01];

		authenticateA(sector, key);

		key = sectorKeys[(index ^ 0x01) & 0x01]; // new key is inverted old key

		byte[] data;
		for (int b = 0; b < blocksToWrite; ++b) {

			int blockIndex = mTag.sectorToBlock(sector) + b;

			if (b == blocksToWrite - 1) {
				data = buildTrailer(key, key);
			} else {
				data = new byte[16];
				rnd.nextBytes(data);
			}
			mTag.writeBlock(blockIndex, data);

		}
		return blocksToWrite;

	}

	private void verifySector(int sector, int index, Random rnd)
			throws Exception {

		int blocksToWrite = mTag.getBlockCountInSector(sector) - 1;

		byte[] key = sectorKeys[(index ^ 0x01) & 0x01]; // new key is inverted
														// old key

		authenticateA(sector, key);

		for (int b = 0; b < blocksToWrite; ++b) {

			byte ref[] = new byte[16];
			rnd.nextBytes(ref);

			int blockIndex = mTag.sectorToBlock(sector) + b;

			byte data[] = mTag.readBlock(blockIndex);

			if (!Arrays.equals(data, ref)) {
				throw new Exception("R/W data mismatch");
			}

		}

	}

	private void authenticateA(int sector, byte[] key) throws Exception {

		if (mTag.authenticateSectorWithKeyA(sector, key)) {
			return;
		}
		// if (mTag.authenticateSectorWithKeyB(sector,
		// MifareClassic.KEY_DEFAULT)) {
		// publishProgress("authenticated with default key");
		// return;
		// }
		if (mTag.authenticateSectorWithKeyA(sector, MifareClassic.KEY_DEFAULT)) {
			return;
		}
		byte[] inverted = new byte[key.length];
		for (int i = 0; i < key.length; ++i)
			inverted[i] = (byte) ~key[i];
		if (mTag.authenticateSectorWithKeyA(sector, inverted)) {
			log("authenticated with inverted sector key");
			return;
		}

		throw new Exception("Authentication error");

	}
	
	private void authenticateB(int sector, byte[] key) throws Exception {

		if (mTag.authenticateSectorWithKeyB(sector, key)) {
			return;
		}
		
		if (mTag.authenticateSectorWithKeyB(sector, MifareClassic.KEY_DEFAULT)) {
			log("authenticated with default key");
			return;
		}
		byte[] inverted = new byte[key.length];
		for (int i = 0; i < key.length; ++i)
			inverted[i] = (byte) ~key[i];
		if (mTag.authenticateSectorWithKeyB(sector, inverted)) {
			log("authenticated with inverted sector key");
			return;
		}

		throw new Exception("Authentication error");

	}

	private class Log {
		private List<Entry> mEntries = new ArrayList<Entry>();

		Log() {
		}

		public void addEntry(int blocks, int ms) {
			mEntries.add(new Entry(blocks, ms));
		}

		public void writeToFile(String fileName) throws IOException {
			BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
			int i = 0;
			for (Entry entry : mEntries) {

				writer.write(i + ", " + entry.blocks + ", " + entry.durationMs
						+ '\n');
				++i;
			}

			// Close writer
			writer.close();

		}

		private class Entry {
			public final int blocks;
			public final int durationMs;

			Entry(int b, int d) {
				blocks = b;
				durationMs = d;
			}
		}
	}

	private void log(String msg) {
		publishProgress(new String[] { msg });
	}
}
