package net.sourceforge.gpj.cardservices.interfaces;

import javax.smartcardio.Card;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;

import android.content.Context;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import at.fhooe.usmile.gpjshell.MainActivity;

public class NfcTerminal extends GPTerminal {

	private Callback mCallback = null;
	private IsoDep mAvailableTag = null;
	
	public NfcTerminal(Callback cb) {
		mCallback = cb;

		mCallback.terminalReady();
		mCallback.requestTag();
	}
	
	public Card connect(String string) throws CardException {
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
		// TODO Auto-generated method stub
		return false;
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

	public interface Callback {
		
		public void requestTag();
		public void terminalReady();
	}

	public boolean passTag(Tag tag) {
		mAvailableTag = IsoDep.get(tag);
		return mAvailableTag != null;
	}

}
