package at.fhooe.usmile.gpjshell;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;
import net.sourceforge.gpj.cardservices.AID;

// Each line of the script contains one install descriptor.
// Therefore, fields of a line are: 
// <Applet AID>, <Instance AID>, <privileges>, <params>  # <comment>
// Fields are separated by commas, spaces are ignored and can be used for better readability.
// Everything after a # is a comment and ignored by the application

public class CapInstallScript {
	private static final String LOG_TAG = "CAP install script";

	private List<AppletInstallDescriptor> mDescriptors = null;

	private CapInstallScript() {
		mDescriptors = new ArrayList<AppletInstallDescriptor>();
	}

	private void addDescriptor(AppletInstallDescriptor d) {
		mDescriptors.add(d);
	}

	public static CapInstallScript getFromCapFile(String capUrl) {
		if (!capUrl.endsWith(".cap"))
			return null;
		String scriptUrl = capUrl.substring(0, capUrl.lastIndexOf(".cap"))
				+ ".inst";
		scriptUrl = scriptUrl.substring(scriptUrl.lastIndexOf("file://")+"file://".length());

		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(scriptUrl));
		} catch (FileNotFoundException e) {
			Log.d(LOG_TAG, "Couldn't open install script " + scriptUrl);
			return null;
		}

		CapInstallScript script = new CapInstallScript();
		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				if(line.contains("#")) {
					line = line.substring(0, line.indexOf('#'));
				}
				
				String[] fields = line.split(",");

				if(fields.length < 4) {
					throw new Exception("syntax error");
				}
				for(int i = 0; i < fields.length; ++i) {
					fields[i] = fields[i].replace(" ", "");
				}
				AID appletAid = new AID(GPUtils.convertHexStringToByteArray(fields[0]));
				AID instAid = new AID(GPUtils.convertHexStringToByteArray(fields[1]));
				String privStr = (fields[2].length() % 2 == 0) ? fields[2] : ("0" + fields[2]);
				byte privileges = GPUtils.convertHexStringToByteArray(privStr)[0];
				String paramStr = (fields[3].length() % 2 == 0) ? fields[3] : ("0" + fields[3]);
				byte[] params = GPUtils.convertHexStringToByteArray(paramStr);
				params = new byte[] {(byte) 0xC9, 0x00};
				script.addDescriptor(new AppletInstallDescriptor(appletAid, instAid, privileges, params));
			}


		} catch (Exception e) {
			Log.d(LOG_TAG, "Error parsing install script: " + e.getMessage());
			return null;
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return script;
	}

	public List<AppletInstallDescriptor> getDescriptors() {
		return mDescriptors;
	}

	public static class AppletInstallDescriptor {
		private final AID mAppletAid;
		private final AID mInstAid;
		private final byte mPrivileges;
		private final byte[] mParams;

		public AppletInstallDescriptor(AID appletAid, AID instAid,
				byte privileges, byte[] params) {
			mAppletAid = appletAid;
			mInstAid = instAid;
			mPrivileges = privileges;
			mParams = params;
		}

		public AID getAppletAid() {
			return mAppletAid;
		}

		public AID getInstAid() {
			return mInstAid;
		}

		public byte getPrivileges() {
			return mPrivileges;
		}

		public byte[] getParams() {
			return mParams;
		}

		public String toString() {
			return mAppletAid.toString() + " --> " + mInstAid.toString() + ", "
					+ String.valueOf(mPrivileges) + ", "
					+ GPUtils.byteArrayToString(mParams);

		}
	}

}
