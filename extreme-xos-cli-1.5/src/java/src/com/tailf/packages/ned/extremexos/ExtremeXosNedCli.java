package com.tailf.packages.ned.extremexos;

import static com.tailf.packages.ned.nedcom.NedProgress.reportProgressStart;
import static com.tailf.packages.ned.nedcom.NedProgress.reportProgressStop;
import static com.tailf.packages.ned.nedcom.NedString.stringQuote;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.tailf.conf.ConfXMLParam;
import com.tailf.ned.NedCmd;
import com.tailf.ned.NedErrorCode;
import com.tailf.ned.NedException;
import com.tailf.ned.NedMux;
import com.tailf.ned.NedWorker;
import com.tailf.ned.SSHSessionException;
import com.tailf.packages.ned.nedcom.NedComCliBase;
import com.tailf.packages.ned.nedcom.NedLogger;
import com.tailf.packages.ned.nedcom.NedCommonLib.NedState;
import com.tailf.packages.ned.nedcom.NedCommonLib.PlatformInfo;
import com.tailf.packages.ned.nedcom.NedProgress;
import com.tailf.packages.ned.nedcom.NedSecretCliExt;
import com.tailf.packages.ned.nedcom.NedString;
import com.tailf.packages.ned.nedcom.interactor.Console;
import com.tailf.packages.ned.nedcom.interactor.Interactor;

public class ExtremeXosNedCli extends NedComCliBase {

	protected static final NedErrorCode ERREXT = NedErrorCode.NED_EXTERNAL_ERROR;
	protected static final NedErrorCode ERRINT = NedErrorCode.NED_INTERNAL_ERROR;
	private static final String NSKEY = "__key__";
	protected ExtremeXosConsole device;
	protected ExtremeXosConfig config;

	protected boolean isCommitCapable = false;
	protected NedLogger log;
	protected ArrayList<String[]> injectCommand;

	public ExtremeXosNedCli() {
		super();
		log = getLogger();

	}

	public ExtremeXosNedCli(String deviceId, NedMux mux, boolean trace, NedWorker worker) throws Exception {
		super(deviceId, mux, trace, worker);
		log = getLogger();
	}

	//
	// Setup
	//

	@Override
	public Console createConsole(NedWorker worker) throws Exception {
		device = new ExtremeXosConsole(worker, this);
		return device;
	}

	@Override
	public void nedSettingsDidChange(NedWorker worker, Set<String> changedKeys, boolean isconnected) throws Exception {


		
        /*
         * write/inject-command
         */
        injectCommand = new ArrayList<>();
        List<Map<String,String>> entries = nedSettings.getListEntries("write/inject-command");
        for (Map<String,String> entry : entries) {
            String[] newEntry = new String[4];
            newEntry[0] = entry.get(NSKEY); // "id"
            newEntry[1] = entry.get("config-line");
            if (newEntry[1] == null) {
                newEntry[1] = "";
            }
            newEntry[2] = entry.get("command");
            newEntry[3] = entry.get("where");
            if (newEntry[3] == null) {
                throw new NedException(NedErrorCode.NED_EXTERNAL_ERROR,
                		"ned-settings: write/inject-command "+newEntry[0]+" missing 'where'");
            }
            String buf = "write/inject-command "+newEntry[0]+" cfg "+stringQuote(newEntry[1]);
            if (newEntry[2] != null) {
                buf += " cmd "+stringQuote(newEntry[2]);
            } else {
                newEntry[2] = "";
                buf += " filtered";
            }
            buf += " "+newEntry[3];
            log.debug(buf);
            injectCommand.add(newEntry);
        }
        
		if (config != null) {
			config = new ExtremeXosConfig(this);
		}
	}

	@Override
	protected void setupInstance(NedWorker worker, PlatformInfo platformInfo) throws Exception {

		config = new ExtremeXosConfig(this);
		secrets = new NedSecretCliExt(this);
		secrets.setDebug(log.isDebug());
	}

	//
	// Internal helpers
	//

	private String prepareConfig(NedWorker worker, String data) throws NedException {

		try {
			device.setSecret(null, null);
			device.setOldSecret(null, null);

			data = parseCLIDiff(worker, data);
			data = config.modifyOutput(data);
		}
		catch (Exception e) {
			throw new NedException(ERRINT, "Failed to prepare config", e);
		}

		return data;
	}

	private void setConfig(NedWorker worker, int cmd, String data)
			throws NedException, IOException,SSHSessionException,ApplyException {

		try {

			data = prepareConfig(worker, data);

			NedProgress.Progress progress = reportProgressStart(this, NedProgress.SEND_CONFIG);

			try {
				if (isNetsim()) {
					device.enterConfig();
				}
				boolean ignoreErrors = cmd == NedCmd.REVERT_CLI || cmd == NedCmd.ABORT_CLI;
				device.setIgnore(ignoreErrors, false, !ignoreErrors);
				device.applyConfig(splitCLIDiffIntoChunks(worker, data));
				reportProgressStop(progress);
			}
			catch (Interactor.Error e) {
				reportProgressStop(progress, "error");
				log.error(e.getMessage(), e);
				device.abortConfig();
				throw new ApplyException(e.getMessage(), e.top, e.cfg);
			}
			catch (Exception e) {
				reportProgressStop(progress, "error");
				log.error(e.getMessage(), e);
				throw e;
			}

			if (isNetsim()) {
				device.exitConfig();
			}
		}
		catch (NedException | IOException | SSHSessionException | ApplyException e) {
			throw e;
		}
		catch (Exception e) {
			log.error(e.getMessage(), e);
			throw new NedException(ERREXT, "Failed to apply config", e);
		}
	}

	private void commitConfig() throws Exception {

		if (isCommitCapable) {
			device.applyConfig(device.getCommand(Console.CMD_COMMIT));
			device.exitConfig();
		}
	}

	//
	// NSO NED API
	//

	@Override
	public void show(NedWorker worker, String toptag) throws Exception {
		
		parseAndLoadXMLConfigStream(maapi, worker, schema, getConfig(), NedState.SHOW);     
		worker.showCliResponse("");
	}

	/**
	 * @param worker
	 * @return
	 * @throws Exception
	 * @throws NedException
	 */
	private String getConfig() throws Exception {
		String data = null;
		NedProgress.Progress progress = reportProgressStart(this, "reading config");
		try {
			data = device.readConfig();
		}
		catch (Exception e) {
			reportProgressStop(progress, "error");
			throw e;
		}

		reportProgressStop(progress);

		data = config.modifyInput(data);
		return data;

	}

	@Override
	public void prepareDry(NedWorker worker, String data) throws Exception {

		try {
			data = parseCLIDiff(worker, data);
			data = config.modifyOutput(data);
			
			if (!log.isDebug()) { //remove meta-data from dry-run when not debug
				data = data.replaceAll("!.*\\n","");
			}
		}
		catch (Exception e) {
			throw new NedException(ERRINT, "Failed to prepare config", e);
		}
		worker.prepareDryResponse(data);
	}

	@Override
	public void applyConfig(NedWorker worker, int cmd, String data) throws NedException, IOException, SSHSessionException, ApplyException {

		setConfig(worker, cmd, data);
	}

	@Override
	public void commit(NedWorker worker, int timeout) throws Exception {
		commitConfig();
		worker.commitResponse();
	}

	@Override
	public void persist(NedWorker worker) throws Exception {

		device.saveConfig();
		if (secrets.needUpdate()) {
			secrets.cache(worker, getDeviceConfiguration(worker));
		}

		worker.persistResponse();
	}

	@Override
	public void abort(NedWorker worker, String data) throws Exception {



		if (!isCommitCapable) {
			setConfig(worker, NedCmd.ABORT_CLI, data);
		}
		else if (device.isInConfigMode()){
			device.abortConfig();
		}



		worker.abortResponse();
	}

	@Override
	public void revert(NedWorker worker, String data) throws Exception {

		if (!isCommitCapable) {
			setConfig(worker, NedCmd.REVERT_CLI, data);
			commitConfig();
		}
		else if (device.isInConfigMode()) {
			device.exitConfig();
		}
		worker.revertResponse();
	}

	@Override
	public void getTransId(NedWorker worker) throws Exception {

		String data = device.readConfig();
		data = config.modifyInput(data);
		worker.getTransIdResponse(NedString.calculateMd5Sum(data));
	}

	@Override
	public void command(NedWorker worker, String cmdName, ConfXMLParam[] p) throws Exception {
		ConfXMLParam[] response;
		if (cmdName.equals("scp2")) {
			response = device.execScp2(p);
		}
		else {
			response = device.execAny(cmdName, p);
		}

		worker.commandResponse(response);

		if (device.isRebooting()) {
			log.info("Device is rebooting...");
			device.sleep(30 * (long)1000);
		}
	}

	@Override
	protected String getDeviceConfiguration(NedWorker worker) throws Exception {
		return getConfig();
	}

	@Override
	public boolean isClearText(String secret) {
		if (secret.isEmpty()) {
			return true;
		}
		if (secret.startsWith("#$") || secret.charAt(0) == '$' &&
				secret.charAt(2) == '$' &&
				secret.charAt(1) == '5') {
			return false;   // secret is encrypted
		}
		Pattern p = Pattern.compile("([0-9a-fA-F]{2}(:[0-9a-fA-F]{2})+)");
		Matcher m = p.matcher(secret);
		return !m.find();
	}

	@Override
	public boolean isNetsim() {
		return this.platformInfo.version.indexOf("NETSIM")>=0 ||
				this.platformInfo.version.trim().isEmpty();
	}
}
