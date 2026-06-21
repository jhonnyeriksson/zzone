package com.tailf.packages.ned.extremexos;

import java.util.HashMap;

import com.tailf.conf.ConfBuf;
import com.tailf.conf.ConfXMLParam;
import com.tailf.conf.ConfXMLParamValue;
import com.tailf.maapi.MaapiCrypto;
import com.tailf.maapi.MaapiException;
import com.tailf.ned.CliSession;
import com.tailf.ned.NedExpectResult;
import com.tailf.ned.NedWorker;
import com.tailf.packages.ned.nedcom.interactor.Console;

public class ExtremeXosConsole extends Console {
	protected HashMap<String, String> oldsecrets;

	//
	// Setup
	//

	public ExtremeXosConsole(NedWorker worker, ExtremeXosNedCli ned) throws Exception {
		super(ned);
		this.oldsecrets = new HashMap<>();

	}

	//
	// Interactor callback methods
	//

	@Override
	public void sendRetry(Object args) throws Exception {

		// Skeletor says: Add ned specific handling here...

		super.sendRetry(args);
	}

	@Override
	public void recoverError(Object args) throws Exception {

        String rec = cmd.trim();

        if (rec.startsWith("configure netlogin ports") && match.contains("Invalid input detected")) {
            log.info("ignoring error for:"+ rec);
        }
        else {
        	super.recoverError(args);
        }
	}

	// Skeletor says: Extend by adding ned specific callback methods here

	public void setOldSecret(String cmd, String secret) {

		if (cmd == null) {
			oldsecrets.clear();
		} else if (secret == null) {
			oldsecrets.remove(cmd);
		} else {
			oldsecrets.put(cmd, secret);
		}
	}

	public void sendOldSecret(Object arg) throws Error {

		String key = (String) arg;
		String val;
		String msg;

		if (commands.containsKey(key)) {
			val = commands.get(key);
			msg = log.isDebug() ? val : key;
		} else {
			key = cmd;
			val = oldsecrets.get(cmd);
			msg = log.isDebug() ? val : "*";
		}

		if (val == null) {
			throw new Error("Missing password for '" + key + "'");
		}
		if (val.equals("\\n")) {// special handling for empty string pwd
			val = "";
		}

		log.verbose(String.format("Sending secret: %s", msg));

		setTracer(null);

		expector.send(val, true, false);

		setTracer(ned.getWorker());
	}

	public ConfXMLParam[] execScp2(ConfXMLParam[] p) throws Exception {
		CliSession session = ned.getSession();

		StringBuilder cmd = new StringBuilder("scp2 vr");

		for (int i = 0; i < p.length - 1; i++) {
			cmd.append(" ").append(p[i].getValue().toString());
		}

		session.println(cmd.toString());
		session.expect(cmd.toString(),ned.getWorker());

		NedExpectResult res = session.expect(new String[] {".*password:", "[E]error","[I]invalid" },ned.getWorker());

		if (res.getHit() == 0) {
			session.setTracer(null);
			String encrypted = p[p.length-1].getValue().toString();
			String decrypted = null;
			String realvalue = null;
			try {
				MaapiCrypto mCrypto = new MaapiCrypto(ned.getMaapi());
				decrypted = mCrypto.decrypt(encrypted);
			}
			catch (MaapiException e) {
				// Ignore, assume cleartext/device-native(encrypted/obfuscated) value
			}
			realvalue = decrypted == null?encrypted:decrypted;
			session.println(realvalue);
			session.setTracer(ned.getWorker());
		}

		String reply = session.expect(new String[] { ".*[^\\#\\(\\) ]+\\s*#[ ]?$"},ned.getWorker()).getText();


		int fetchIdx = reply.indexOf("Fetching");
		if (fetchIdx < 0) {
			fetchIdx = reply.indexOf("\n");
		}
		int midIdx = reply.indexOf("\n", fetchIdx);
		int dldIdx = reply.indexOf("Downloading");
		if (dldIdx >= 0) {
			dldIdx -= 4; // including 2x \n
		} else {
			dldIdx = reply.indexOf("Please wait");
			if (dldIdx < 0) {
				dldIdx = midIdx;
			}
			else {
				dldIdx -= 2;
			}
		}
		int endIdx = reply.lastIndexOf("\n", dldIdx);

		reply = reply.substring(0, midIdx) + reply.substring(endIdx);

		return new ConfXMLParam[] { new ConfXMLParamValue(ned.getPrefix() + "-stats", "result", new ConfBuf(reply)) };
	}

}
