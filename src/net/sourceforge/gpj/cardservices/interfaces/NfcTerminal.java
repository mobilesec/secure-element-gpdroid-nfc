package net.sourceforge.gpj.cardservices.interfaces;

import javax.smartcardio.Card;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;

import android.content.Context;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import at.fhooe.usmile.gpjshell.GPConnection;
import at.fhooe.usmile.gpjshell.MainActivity;
import at.fhooe.usmile.gpjshell.objects.GPAppletData;

public class NfcTerminal extends GPTerminal {

	private IsoDep mAvailableTag = null;
	private Context mContext = null;
	private static NfcTerminal _INSTANCE = null;
	
	
	public static NfcTerminal getInstance(Context con) {
		synchronized (NfcTerminal.class) {
			if (_INSTANCE == null) {
				_INSTANCE = new NfcTerminal(con);
			}
			return _INSTANCE;
		}
	}

	private NfcTerminal(Context con) {
		mContext = con;
	}
	
	public Card connect(String unused) throws CardException {
		if(mAvailableTag != null) {
			return new NfcSmartcard(mAvailableTag);
		}
		throw new CardException("NFC card not present");
	}

	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isCardPresent() throws CardException {
		return mAvailableTag != null;
	}

	@Override
	public boolean waitForCardPresent(long timeout) throws CardException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean waitForCardAbsent(long timeout) throws CardException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int getReader() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setReader(int mReader) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void shutdown() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isConnected() {
		// TODO Auto-generated method stub
		return mAvailableTag != null;
	}


	public boolean passTag(Tag tag) {
		mAvailableTag = IsoDep.get(tag);
		return mAvailableTag != null;
	}

}
