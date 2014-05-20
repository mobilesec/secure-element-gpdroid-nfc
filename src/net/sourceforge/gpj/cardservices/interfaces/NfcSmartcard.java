package net.sourceforge.gpj.cardservices.interfaces;

import java.io.IOException;

import javax.smartcardio.ATR;
import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

import android.nfc.tech.IsoDep;

public class NfcSmartcard extends Card {

	private IsoDep mIsoDep = null;
	
	public NfcSmartcard(IsoDep isoDep) {
		mIsoDep = isoDep;
	}
	
	@Override
	public ATR getATR() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getProtocol() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CardChannel getBasicChannel() {
		return new NfcSmartcardChannel(this);
	}

	@Override
	public CardChannel openLogicalChannel() throws CardException {
		return new NfcSmartcardChannel(this);
	}

	@Override
	public void beginExclusive() throws CardException {
		// TODO Auto-generated method stub		
	}

	@Override
	public void endExclusive() throws CardException {
		// TODO Auto-generated method stub
	}

	@Override
	public byte[] transmitControlCommand(int controlCode, byte[] command)
			throws CardException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void disconnect(boolean reset) throws CardException {
		// TODO Auto-generated method stub
		
	}

	public ResponseAPDU transmit(CommandAPDU cmd) throws IOException {
		if(!mIsoDep.isConnected()) mIsoDep.connect();
		return new ResponseAPDU(mIsoDep.transceive(cmd.getBytes()));
		
	}
	
	
}
