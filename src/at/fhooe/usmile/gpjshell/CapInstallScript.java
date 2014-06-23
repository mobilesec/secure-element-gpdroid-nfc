package at.fhooe.usmile.gpjshell;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;
import net.sourceforge.gpj.cardservices.AID;

// Fields in script are: Applet AID, Instance AID, privileges, params

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

		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(scriptUrl));
		} catch (FileNotFoundException e) {
			Log.d(LOG_TAG, "Couldn't open install script");
			return null;
		}

		CapInstallScript script = new CapInstallScript();
		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				String[] fields = line.split(",");

				if(fields.length < 4) {
					throw new Exception("syntax error");
				}
				for(int i = 0; i < fields.length; ++i) {
					fields[i] = fields[i].replace(" ", "");
				}
				AID appletAid = new AID(GPUtils.convertHexStringToByteArray(fields[0]));
				AID instAid = new AID(GPUtils.convertHexStringToByteArray(fields[1]));
				byte privileges = GPUtils.convertHexStringToByteArray(fields[2])[0];
				byte[] params = GPUtils.convertHexStringToByteArray(fields[3]);
				
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

		for (AppletInstallDescriptor d : script.getDescriptors()) {
			Log.d(LOG_TAG, d.toString());
		}

		return script;
	}

	public List<AppletInstallDescriptor> getDescriptors() {
		// TODO Auto-generated method stub
		return null;
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
