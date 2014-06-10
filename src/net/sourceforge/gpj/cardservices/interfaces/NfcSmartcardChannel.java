package net.sourceforge.gpj.cardservices.interfaces;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

import android.util.Log;

public class NfcSmartcardChannel extends CardChannel {
	private static final String LOG_TAG = "NFC Channel";
	private NfcSmartcard mCard = null;

	public NfcSmartcardChannel(NfcSmartcard c) {
		mCard = c;
		// TODO Auto-generated constructor stub
	}

	@Override
	public Card getCard() {
		return mCard;
	}

	@Override
	public int getChannelNumber() {
		return 0;
	}

	@Override
	public ResponseAPDU transmit(CommandAPDU command) throws CardException {
	//	Log.d(LOG_TAG, "Transmitting apdu" + command);
		try {
			ResponseAPDU resp = mCard.transmit(command);
			return resp;
		} catch (Exception e) {
			throw new CardException("Error transmitting APDU: " + e.getMessage());
		}
		
	}

	@Override
	public int transmit(ByteBuffer command, ByteBuffer response)
			throws CardException {
		Log.d(LOG_TAG, "Transmitting command" + command);
		throw new CardException("Not supported yet");
	}

	@Override
	public void close() throws CardException {
		// do nothing
	}

	public boolean supportsExtendedLengthApdus() {
		return mCard.supportsExtendedLengthApdus();
	}

}
