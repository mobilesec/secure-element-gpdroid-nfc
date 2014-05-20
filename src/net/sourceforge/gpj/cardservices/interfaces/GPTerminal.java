package net.sourceforge.gpj.cardservices.interfaces;

import javax.smartcardio.CardTerminal;

public abstract class GPTerminal extends CardTerminal {
	
	

	abstract public int getReader();

	abstract public void setReader(int mReader);

	abstract public void shutdown();

	abstract public boolean isConnected();


}
