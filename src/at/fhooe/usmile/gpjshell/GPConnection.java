/*******************************************************************************
 * Copyright (c) 2014 Michael Hölzl <mihoelzl@gmail.com>.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Michael Hölzl <mihoelzl@gmail.com> - initial implementation
 *     Thomas Sigmund - data base, key set, channel set selection and GET DATA integration
 ******************************************************************************/
package at.fhooe.usmile.gpjshell;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

import net.sourceforge.gpj.cardservices.AID;
import net.sourceforge.gpj.cardservices.AIDRegistryEntry;
import net.sourceforge.gpj.cardservices.AIDRegistryEntry.Kind;
import net.sourceforge.gpj.cardservices.CapFile;
import net.sourceforge.gpj.cardservices.GPUtil;
import net.sourceforge.gpj.cardservices.GlobalPlatformService;
import net.sourceforge.gpj.cardservices.exceptions.GPDeleteException;
import net.sourceforge.gpj.cardservices.exceptions.GPInstallForLoadException;
import net.sourceforge.gpj.cardservices.exceptions.GPLoadException;
import net.sourceforge.gpj.cardservices.exceptions.GPSecurityDomainSelectionException;
import net.sourceforge.gpj.cardservices.interfaces.NfcTerminal;
import net.sourceforge.gpj.cardservices.interfaces.OpenMobileAPITerminal;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.provider.OpenableColumns;
import android.util.Log;
import at.fhooe.usmile.gpjshell.CapInstallScript.AppletInstallDescriptor;
import at.fhooe.usmile.gpjshell.TimerLog.LogEvent;
import at.fhooe.usmile.gpjshell.objects.GPAppletData;
import at.fhooe.usmile.gpjshell.objects.GPChannelSet;
import at.fhooe.usmile.gpjshell.objects.GPKeyset;

public class GPConnection {
	
	private static final String LOG_TAG = "GPConnection";
	
	private static GPConnection _INSTANCE = null;
	private GPAppletData data = null;
	private GlobalPlatformService mGPService;

	private Context mContext;

	private TimerLog mTsLog = null;

	public static GPConnection getInstance(Context _con) {
		synchronized (GPConnection.class) {
			if (_INSTANCE == null) {
				_INSTANCE = new GPConnection(_con);
			}
			return _INSTANCE;
		}
	}

	private GPConnection(Context _con) {
		mContext = _con;
		data = new GPAppletData(null, -1);
	}

	public List<AIDRegistryEntry> getRegistry() {
		return data.getRegistry();
	}

	public void setSelectedApplet(int position) {
		data.setSelectedApplet(position);
	}

	public AIDRegistryEntry getSelectedApplet() {
		return data.getSelectedApplet();
	}

	private void deleteSelectedApplet() throws GPDeleteException, CardException {
		if (data.getSelectedApplet().getKind() == Kind.IssuerSecurityDomain
				|| data.getSelectedApplet().getKind() == Kind.SecurityDomain) {
			throw new CardException(
					"Deleting Security domain currently not supported");
		}
		mGPService.deleteAID(data.getSelectedApplet().getAID(), true);
		data.removeSelectedAppletFromList();
	}

	private void deleteAID(AID deleteAID) throws GPDeleteException, CardException {

		mGPService.deleteAID(deleteAID, true);
	}
	
	/**
	 * initializes the keys for the smartcard to be used later. it uses a predefined keyset
	 * @param channel 
	 * @param keyset predefined keyset
	 */
	private void initializeKeys(CardChannel channel, GPKeyset keyset) {
		mGPService = new GlobalPlatformService(channel);
		mGPService.setKeys(keyset.getID(), keyset.getENCByte(),
				keyset.getMACByte(), keyset.getKEKByte());
	}

	private void open() throws GPSecurityDomainSelectionException, CardException {

		mGPService.addAPDUListener(mGPService);
		mGPService.open();
	}

	/**
	 * opens a secure channel with id from keyset and channel-settings
	 * @param uniqueIndex - unique ID of keyset
	 * @param keyId - keyID of keyset
	 * @param keyVersion - version of keyset
	 * @param scpVersion
	 * @param securityLevel
	 * @param gemalto
	 * @throws IllegalArgumentException
	 * @throws CardException
	 */
	private void openSecureChannel(int uniqueIndex, int keyId, int keyVersion,
			int scpVersion, int securityLevel, boolean gemalto)
			throws IllegalArgumentException, CardException {
		mGPService.openSecureChannel(uniqueIndex, keyId, keyVersion,
				scpVersion, securityLevel, gemalto);
	}

	
	private ResponseAPDU getData(int p1, int p2) throws IllegalStateException, CardException {
		CommandAPDU getData = new CommandAPDU(
				GlobalPlatformService.CLA_GP,
				GlobalPlatformService.GET_DATA, p1, p2);

		return mGPService.transmit(getData);
	}

	/**
	 * Installs a selected cap-File (applet) to the smartcard. This method used
	 * predefined parameters and privileges for installation
	 * 
	 * @param _appletUrl
	 *            - url of the applet
	 * @param params
	 *            - install parameters
	 * @param privileges
	 *            - privileges used for installation
	 * @throws IOException
	 * @throws MalformedURLException
	 * @throws GPInstallForLoadException
	 * @throws GPLoadException
	 * @throws CardException
	 */
	private void installCapFile(String _appletUrl, byte[] params, byte privileges)
			throws IOException, MalformedURLException,
			GPInstallForLoadException, GPLoadException, CardException {
		CapFile cpFile = new CapFile(new URL(_appletUrl).openStream(), null);

		mGPService.loadCapFile(cpFile, false, false, 255 - 16, true, false);
		
		logTimestamp(LogEvent.CAP_LOAD_FINISHED);

		AID p = cpFile.getPackageAID();
		Log.d(LOG_TAG, "Installing Applet with package AID " + p.toString());

		CapInstallScript inst = CapInstallScript.getFromCapFile(_appletUrl);

		if (inst == null) {
			Log.d(LOG_TAG, "Warning: no install script for CAP file found");
			for (AID a : cpFile.getAppletAIDs()) {
				Log.d(LOG_TAG, "params" + GPUtils.byteArrayToString(params));
				mGPService.installAndMakeSelecatable(p, a, null, privileges,
						params, null);
				logTimestamp(LogEvent.APPLET_INSTALL_FINISHED);
				Log.d(LOG_TAG,
						"Finished installing applet. AID: " + a.toString());
			}
		} else {
			for(AppletInstallDescriptor d : inst.getDescriptors()) {
				Log.d(LOG_TAG, d.toString());
				mGPService.installAndMakeSelecatable(p, d.getAppletAid(), d.getInstAid(), 
						d.getPrivileges(), d.getParams(), null);
				logTimestamp(LogEvent.APPLET_INSTALL_FINISHED);
				Log.d(LOG_TAG,
						"Finished installing applet. AID: " + d.getAppletAid().toString() + " to " + d.getInstAid().toString());			
			}
		}
	}

	private void logTimestamp(LogEvent event) {
		if (mTsLog != null) {
			mTsLog.log(event);
		}
	}

	public void setTimestampLog(TimerLog log) {
		mTsLog = log;
	}

	public GPAppletData loadAppletsfromCard() throws CardException {
		data.setRegistry(mGPService.getStatus().allPackages());
		return data;
	}

	public void deleteApplet(AID aid) {
		try {
			deleteAID(aid);
			logTimestamp(LogEvent.APPLET_DELETE_FINISHED);
		} catch (GPDeleteException e) {
			e.printStackTrace();
		} catch (CardException e) {
			e.printStackTrace();
		}
	}

	/**
	 * installs an applet from preset url
	 * 
	 * @param _url
	 *            where the applet is located
	 * @throws IOException
	 * @throws MalformedURLException
	 * @throws GPInstallForLoadException
	 * @throws GPLoadException
	 * @throws CardException
	 */
	private String installApplet(String _url) throws IOException,
			MalformedURLException, GPInstallForLoadException, GPLoadException,
			CardException {
		return installApplet(_url, null, (byte) 0);
	}

	private String installApplet(String _url, byte[] params, byte privileges)
			throws IOException, MalformedURLException,
			GPInstallForLoadException, GPLoadException, CardException {

		if (_url == null) {
			return "no Applet selected";
		}
		if (!(_url).endsWith(".cap")) {
			throw new IOException("Not a valid path or not a cap file");
		}

		String ret = "Loading Applet from " + _url + "<br/>";

		installCapFile(_url, params, privileges);

		return ret + "Installation successful";
	}

	/**
	 * lists all applets installed on the currently selected smartcard
	 * 
	 * @throws CardException
	 */
	private String listApplets(String _reader) throws CardException {
		GPAppletData mApplets = loadAppletsfromCard();

		return "Read all applets from reader "
						+ _reader + ". <br>"
						+ mApplets.getRegistry().size()
						+ " Applets.";
	}

	/**
	 * Method that acquires a CardChannel from the given terminal and performs
	 * the given GPCommand on it
	 * 
	 * @param keyset
	 * @param channelSet
	 * @param _cmd
	 * @return
	 */
	public String performCommand(CardTerminal _term, GPKeyset keyset, GPChannelSet channelSet,
			GPCommand _cmd) {

		try {
			Card c = null;

			if (_term instanceof OpenMobileAPITerminal) {
				((OpenMobileAPITerminal) _term).setReader(_cmd.getSeekReader());
			}
			
			c = _term.connect("*");
			System.out.println("Found card in terminal: " + _term.getName());
			if (c.getATR() != null) {
				System.out.println("ATR: "
						+ GPUtil.byteArrayToString(c.getATR().getBytes()));
			}
			CardChannel channel = c.openLogicalChannel();
			return performCommand(channel, keyset, channelSet, _cmd);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Method that performs the given GPCommand on an channel
	 * 
	 * @param keyset
	 * @param channelSet
	 * @param _cmd
	 * @return
	 */
	public String performCommand(CardChannel channel, GPKeyset keyset,
			GPChannelSet channelSet, GPCommand _cmd) {
		String ret = "";
		try {

			boolean closeConn = true;
			if (keyset != null && channelSet != null) {
				initializeKeys(channel, keyset);
			}

			open();

			// opening channel with index of keyset - is unique
			openSecureChannel(keyset.getID(),
					keyset.getID(), keyset.getVersion(),
					channelSet.getScpVersion(), channelSet.getSecurityLevel(),
					channelSet.isGemalto());

			Log.d(LOG_TAG, "Secure channel opened");

			switch (_cmd.getCmd()) {
			case APDU_INSTALL:
				if (_cmd.getInstallParams() != null) {
					ret = installApplet((String)_cmd.getCommandParameter(), _cmd.getInstallParams(), _cmd.getPrivileges());
				} else {
					ret = installApplet((String)_cmd.getCommandParameter());
				}
				break;

			case APDU_DELETE_SENT_APPLET:
				AID aid;
				aid = CAPFile.readAID((String)_cmd.getCommandParameter());
				ret = "TCPConn" + GPUtils.byteArrayToString(aid.getBytes());
				deleteApplet(aid);
				break;

			case APDU_DELETE_SELECTED_APPLET:
				deleteSelectedApplet();
				ret = "Applet deleted";
				break;

			case APDU_DISPLAYAPPLETS_ONCARD:
				ret = listApplets(_cmd.getSeekReaderName());
				channel.close();

				Intent intent = new Intent(mContext, AppletListActivity.class);
				intent.putExtra(AppletListActivity.EXTRA_CHANNELSET, channelSet);
				intent.putExtra(AppletListActivity.EXTRA_KEYSET, keyset);
				intent.putExtra(AppletListActivity.EXTRA_SEEKREADER, _cmd.getSeekReader());
				
				mContext.startActivity(intent);
				closeConn = false;
				break;

			case APDU_CMD_OPEN:
				closeConn = false;
				return "GPConnection initialized";

			default:
				break;
			}

			if (closeConn) {
				channel.close();
				// c.disconnect(true);
			}
		} catch (GPSecurityDomainSelectionException e) {
			ret = "GPSecurityDomainSelectionException " + e.getLocalizedMessage();
			e.printStackTrace();
		} catch (GPInstallForLoadException e) {
			ret = "GPInstallForLoadException - Applet already installed? " + e.getLocalizedMessage();
			e.printStackTrace();
		} catch (CardException e) {
			ret = "CardException " + e.getLocalizedMessage();
			e.printStackTrace();
		} catch (MalformedURLException e) {
			ret = "MalformedURLException " + e.getLocalizedMessage();
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
			ret = "IOException " + e.getLocalizedMessage();
		}
		return ret;
	}
	
	public ResponseAPDU transmit(CommandAPDU cmd) {
		try {
			return mGPService.transmit(cmd);
		} catch (IllegalStateException e) {
			Log.d(LOG_TAG, "Error transmitting APDU:" + e.getMessage());
			e.printStackTrace();
		} catch (CardException e) {
			Log.d(LOG_TAG, "Error transmitting APDU:" + e.getMessage());
			e.printStackTrace();
		}
		return null;
	}
	
}
