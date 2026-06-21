package com.tailf.packages.ned.extremexos;


import static com.tailf.packages.ned.nedcom.NedString.stringDequote;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.tailf.conf.ConfException;
import com.tailf.maapi.MaapiConfigFlag;
import com.tailf.maapi.MaapiCrypto;
import com.tailf.maapi.MaapiException;
import com.tailf.ned.NedWorker;
import com.tailf.packages.ned.nedcom.MaapiUtils;
import com.tailf.packages.ned.nedcom.NedComCliBase;
import com.tailf.packages.ned.nedcom.NedComCliExtensions;
import com.tailf.packages.ned.nedcom.NedCommonLib;
import com.tailf.packages.ned.nedcom.Schema;
import com.tailf.packages.ned.nedcom.Schema.ParserContext;

public class ExtremeXosCliExtensions implements NedComCliBase.ExtensionsHandler {

  private static final String PREFIX = "extreme-xos:";
  private static final String NO_CONFIGURE = "no configure";
  private static final String NO_CREATE = "no create";
  private static final String UNCONFIGURE = "unconfigure";
  private static final String DELETE = "delete";
  protected ExtremeXosNedCli ned;
  protected Schema schema;
  protected Set<String> deleteCmds;
  
  private Set<String> visited;
  private HashMap<String, ArrayList<String>> redeploySnmpAccessProfile;



  public ExtremeXosCliExtensions(ExtremeXosNedCli ned) {
    this.ned = ned;
    this.schema = ned.getCurrentSchema();
  }

  @Override
  public void initialize() {
    this.deleteCmds = new HashSet<>();

    this.visited = new HashSet<>();
    this.redeploySnmpAccessProfile = new HashMap<>();

  }


  // no-to-disable
  public void noToDisable(NedWorker worker, Schema.CallbackMetaData metaData, Schema.ParserContext parserContext) {
    if (ned.isNetsim()) {
      return;
    }
    String line = parserContext.getCurrentLine();
    if (parserContext.isDelete()) {
      StringBuilder indent = parserContext.currentIndent();
      if (line.trim().startsWith("no enable")) {
        indent.append(line.replace("no enable", "disable").trim());
      }else if (line.trim().startsWith("no disable")) {
        indent.append(line.replace("no disable", "enable").trim());
      }
      String disableLine = indent.toString();
      ned.log.debug( String.format(" enable to disable %s (%s)", parserContext.getCurrentKeyPath(), disableLine));
      parserContext.replaceCurrentLine(disableLine);
    }
  }

  // "delete-with"
  public void deleteWith(NedWorker worker, Schema.CallbackMetaData metaData, Schema.ParserContext parserContext) {
    boolean lineskip = false;
    String line = parserContext.getCurrentLine();

    if (ned.isNetsim() ||
        !(line.trim().startsWith("no ") ||
        line.trim().startsWith("unconfigure ")||
        line.trim().startsWith("delete "))) {
      return;
    }
    String arg = metaData.argument;
    if (arg.equals("none")) {
      parserContext.skipCurrentLine();
      ned.log.debug("no args, skip line: "+line);
      return;
    }

    if (metaData.name.contains("delete-with")) {
      String newline = null;
      if (line.indexOf(NO_CONFIGURE) >= 0) {
        if (line.indexOf("add ") >= 0) {
          newline = line.replace("no ","").replace("add ","delete "); //configure * delete
        }
        else {
          newline = line.replace(NO_CONFIGURE,UNCONFIGURE);
        }
      } else if (line.indexOf(NO_CREATE) >= 0) {
        newline = line.replace(NO_CREATE,DELETE);
      }
      else {
        ned.log.debug("Warning: Line not handled: "+line);
        return;
      }


      if (!deleteCmds.contains(line)) {
        deleteCmds.add(line);
        ned.log.debug(String.format("replace %s with '%s'", line, newline));

        parserContext.replaceCurrentLine(newline);
      } else {
        parserContext.skipCurrentLine();
        ned.log.debug("already handled, skip line: "+line);
      }

    } else if (metaData.name.contains("trim-delete-if-match")) {
      Pattern p = Pattern.compile(arg);
      Matcher m = p.matcher(line);
      if (m.find()) {
        parserContext.skipCurrentLine();
        ned.log.debug("skip line: "+line);
      }
    } else if (metaData.name.contains("replace-delete-output")) {

      String[] args = metaData.argument.split(" :: ");
      if (args.length>2) {
        String[] skipArgs = args[2].split(",");
        for (String skip : skipArgs) {
          if (line.indexOf(skip) >= 0) {
            lineskip = true;
          }
        }
      }

      if (!lineskip) {
        Pattern p = Pattern.compile(args[0]);
        Matcher m = p.matcher(line);
        if (m.find()) {
          String newline = line.replaceAll(args[0],args[1]);
          if (!deleteCmds.contains(line)) {
            deleteCmds.add(line);

            ned.log.debug(String.format("replace %s with '%s'", line, newline));
            parserContext.replaceCurrentLine(newline);
          } else {
            parserContext.skipCurrentLine();
            ned.log.debug("already handled,skip line: "+line);
          }
        }
        else {
          ned.log.debug("line not matched: "+line + " with "+args[0]);

        }
      }
    }
    else {
      ned.log.error("Error: Metadata name not handled: "+metaData.name);
    }
  }

  // "quoted-string"
  public void quotedString(NedWorker worker, Schema.CallbackMetaData metaData, Schema.ParserContext parserContext) {
    if (ned.isNetsim()) {
      return;
    }
      if (parserContext.currentDataContext instanceof Schema.TreeLeaf) {
          String line = parserContext.getCurrentLine();
          String strToQuote = parserContext.currentDataContext.value(null);
          String prefix = parserContext.currentDataContext.node.cliToken();
          if (prefix.length() > 0) {
              prefix = prefix + " ";
          }
          ned.log.debug( String.format("Do quoted-string in %s, string to quote: '%s'", parserContext.currentDataContext.getKeyPath(), strToQuote));
          line = line.replace(String.format("%s%s", prefix, strToQuote), String.format("%s\"%s\"", prefix, strToQuote));
          parserContext.replaceCurrentLine(line);
      }
  }

  public void sendSecret(final NedWorker worker, Schema.CallbackMetaData metaData,
      Schema.ParserContext parserContext, final int fromT, final int toT)  {

    if (ned.isNetsim()) {
      return;
    }
    String line = parserContext.getCurrentLine();
    if (parserContext.isDelete() || line.startsWith("no") || line.contains(DELETE) || line.startsWith(UNCONFIGURE)) {
      return;
    }
    String encrypted = ((Schema.TreeLeaf)parserContext.currentDataContext).getValue();
    String decrypted = null;
    String realvalue = null;
    ned.log.debug(String.format("sendSecret:: line = %s", line));


    try {
        MaapiCrypto mCrypto = new MaapiCrypto(ned.maapi);
        decrypted = decryptPwd(encrypted, mCrypto);
    } catch (MaapiException e) {
      // Ignore, assume cleartext/device-native (encrypted/obfuscated) value
    }
    realvalue = decrypted == null?encrypted:decrypted;



    if (ned.isClearText(realvalue)) {

      if (line.contains(" encrypted ")) {
        line = line.replace(encrypted, realvalue);

        line = line.replace("encrypted ","");

        if (line.trim().startsWith("create account")) {
          int len = line.lastIndexOf(realvalue)+ realvalue.length();

          if (len !=0 && line.length() != len) {
            //<new_pwd> <old_pwd> format -> may occur at rollback - strip the old_pwd form the line
            line = line.substring(0,len);
          }
        }
      }
      else if (line.indexOf("auth-encrypted localized-key "+encrypted) >=0) {
        line = line.replace("auth-encrypted localized-key "+encrypted,realvalue);
      }
      else if (line.indexOf("privacy-encrypted localized-key "+encrypted)>=0) {
        line = line.replace("privacy-encrypted localized-key "+encrypted,realvalue);
      }
      else if (line.indexOf("localized-key "+encrypted)>=0) {
        line = line.replace("localized-key "+encrypted,realvalue);
      }
    }

    ned.log.debug(String.format("sendSecret:: replace line with '%s'", line));

    parserContext.replaceCurrentLine(line);


  }

  public void multiLineBanner(NedWorker worker, Schema.CallbackMetaData metaData, Schema.ParserContext parserContext) {
    if (ned.isNetsim()) {
      return;
    }
    if (parserContext.currentDataContext instanceof Schema.TreeLeaf) {
      String line = parserContext.getCurrentLine();
      Pattern p = Pattern.compile("(configure banner (before-login save-to-configuration|after-login)) (.*)");
      Matcher m = p.matcher(line);
      if (m.find()) {
        String strToDequote = parserContext.currentDataContext.value(null);
        String tokens = m.group(1);
        String delimiter = "\n";
        String value = stringDequote(strToDequote).replace("\r","");
        value = String.format("%s%s%s", delimiter, value, delimiter);

        String metaTag = NedComCliExtensions.NO_PROMPT_AFTER_SEND + "\n";
        String newLine = (tokens+value).replaceAll(".+\n", metaTag + "$0")+"\r\n";

        ned.log.debug(String.format("replace %s with '%s'", line, newLine));
        parserContext.replaceCurrentLine(newLine);

      }
    }
  }

  // "replace-input"
  public void replaceInput(NedWorker worker, Schema.CallbackMetaData metaData, Schema.ParserContext parserContext) {
    if (parserContext.parserDirection == Schema.ParserDirection.FROM_DEVICE &&
        parserContext.getState() == Schema.ParserState.PRE_MATCH) {

      String line = parserContext.getCurrentLine();
      String[] args = metaData.argument.split(" :: ");
      String replacer = args.length>1?args[1]:"";

      Pattern fromPat = Pattern.compile(args[0]);
      Matcher m = fromPat.matcher(line);
      if (m.find()) {
        String newline = line.replaceAll(m.group(1), replacer);
        parserContext.replaceCurrentLine(newline);
        ned.log.debug(String.format("replace %s with '%s'", line, newline));
      }
    }
  }

  public void accountsPwd(NedWorker worker, Schema.CallbackMetaData metaData,
      Schema.ParserContext parserContext, final int fromT, final int toT) throws IOException, ConfException  {

    if (ned.isNetsim()) {
      return;
    }
    String line = parserContext.getCurrentLine();
    ned.log.debug(String.format("line = %s", line));

    if (parserContext.isDelete() ||
        line.startsWith("no") ||
        line.contains(DELETE) ||
        line.startsWith(UNCONFIGURE)) {
      return;
    }
    String maapiPath = parserContext.getNCSCurrentKP(ned.device_id);
    ned.log.debug(String.format("maapiPath = %s", maapiPath));

    if (!ned.maapi.exists(fromT, maapiPath)) {
      //leaf doesn't exist in from
      //this is a create operation -> call sendSecret
      sendSecret(worker, metaData, parserContext, fromT, toT);
      return;

    }
    //maapi exists -> change pwd || change pwd for hidden accounts that don't have a pwd

    if (parserContext.parserDirection == Schema.ParserDirection.TO_DEVICE &&
        parserContext.getState() == Schema.ParserState.POST_MATCH) {

      String regex = "create account \\w+ (\\w+) encrypted (.*)";
      Pattern p = Pattern.compile(regex);
      Matcher m = p.matcher(line);
      if (m.find()) {
        String newline;
        String pwdvalue = m.group(2);
        if(ned.isClearText(pwdvalue)) {
          newline = "configure account "+m.group(1)+" password";
        }
        else {
          //send device encrypted password -> no need to handle secrets
          newline = "configure account "+m.group(1)+" password encrypted "+pwdvalue;
        }

        ned.log.debug(String.format("replace line with '%s'", newline));
        parserContext.replaceCurrentLine(newline);
        handleSecrets(pwdvalue, newline, fromT, parserContext);

      }
    }
  }

  /**
   * @param worker
   * @param pwdvalue
   * @param line
   * @param parserContext
   * @throws ConfException
   * @throws IOException
   */
  private void handleSecrets(String pwdvalue, String line,int fromT, ParserContext parserContext)
      throws IOException, ConfException {
    String[] passwords;
    String oldpwd = null;
    String newpwd = null;
    String maapiPath = parserContext.getNCSCurrentKP(ned.device_id);


    try {
      //try to decrypt
      MaapiCrypto mCrypto = new MaapiCrypto(ned.maapi);

      //get old password from cdb
      if (ned.maapi.exists(fromT, maapiPath)) {
        //get value
        oldpwd = ned.maapi.getElem(fromT, maapiPath).toString();

        if (oldpwd != null) {
          //try to decrypt
          oldpwd = decryptPwd(oldpwd, mCrypto);
          if (oldpwd.contains(" ")) {
            //case where the old password was set as "new old" format
            oldpwd = oldpwd.split(" ")[0]; //set old password to previous newpwd
          }
        }
      }
      if (pwdvalue.contains(" ")) {
        //<new_pwd> <old_pwd> format
        passwords = pwdvalue.split(" ");

        newpwd = decryptPwd(passwords[0], mCrypto);

        if (oldpwd == null || !ned.isClearText(oldpwd)) {
          //use input old password only if the cdb oldpassword is not clear-text (rollback situations)
          oldpwd = decryptPwd(passwords[1], mCrypto);
        }
      }
      else {
        newpwd = decryptPwd(pwdvalue, mCrypto);
      }

    } catch (MaapiException e) {
      // Ignore, assume cleartext/device-native (encrypted/obfuscated) value
      ned.log.error(e.getMessage(),e);
    }

    setSecrets(line, oldpwd, newpwd);
  }

  /**
   * @param worker
   * @param line
   * @param oldpwd
   * @param newpwd
   */
  private void setSecrets(String line, String oldpwd, String newpwd) {
    if (newpwd != null && ned.device != null) {

      ned.device.setSecret(line, newpwd);
      if (oldpwd != null) {

        ned.device.setOldSecret(line, oldpwd);
      }
      else {
        ned.log.error( "Invalid old-password or password format!");
      }
    }
  }

  /**
   * @param pwdvalue
   * @param newpwd
   * @param mCrypto
   * @return
   * @throws MaapiException
   */
  private String decryptPwd(String pwdvalue, MaapiCrypto mCrypto)
      throws MaapiException {
    String decrypted = pwdvalue;
    if (pwdvalue.length() > 3 &&
        pwdvalue.charAt(0) == '$' &&
        pwdvalue.charAt(2) == '$' &&
        (pwdvalue.charAt(1) == '4' || pwdvalue.charAt(1) == '8'|| pwdvalue.charAt(1) == '9')) {
      if (!NedCommonLib.inPrepareDryPhase(ned.getWorker())) { //in dry-run do not decrypt- security issue
        String value = mCrypto.decrypt(pwdvalue);
        decrypted = value!=null?value:decrypted;
      } else {
        ned.log.debug( "In Dry-Run: secrets are not to be decrypted");
      }
    }
    return decrypted;
  }

  // upm-list-out
  public void upmListOut(NedWorker worker, Schema.CallbackMetaData metaData, Schema.ParserContext parserContext) {

	    if (parserContext.parserDirection == Schema.ParserDirection.TO_DEVICE && !parserContext.isDelete()) {

	    	//need to signal the interactor that this is a multiline input
	    	String line = parserContext.getCurrentLine().trim();
	    	String metaTag = NedComCliExtensions.NO_PROMPT_AFTER_SEND + "\n";

	    	if (!line.startsWith(metaTag)) {
		    	String newLine = metaTag+line;
		    	parserContext.replaceCurrentLine(newLine);
	    	}
	    }
	    else {
	        if (parserContext.getState() == Schema.ParserState.PRE_MATCH) {
	            parserContext.startMultiLine(metaData);
	        } else if (parserContext.getCurrentLine().trim().equals(".")) {
	            List<String> multiLines = parserContext.endMultiLine();
	            StringBuilder commands = new StringBuilder();
	            multiLines.remove(multiLines.size()-1); // . is not part of commands
	            for (String l : multiLines) {
	                l = l.trim();
	                if (l.length() > 0) {
	                    if (commands.length() > 0) {
	                        commands.append("\n");
	                    }
	                    commands.append(l);
	                }
	            }
	            schema.addData(parserContext.currentDataContext,
	                               (Schema.Leaf)metaData.node, commands.toString());
	            parserContext.injectImmediate("."); // to exit context
	        }
	    }


  }
  
  //redeploy-access-profile-at-acl-modify
  public void redeployAccesProfileAtAclModify(final NedWorker worker, Schema.CallbackMetaData metaData,
		  Schema.ParserContext parserContext, final int fromT, final int toT) throws Exception {


	  String line = parserContext.getCurrentLine();
	  String aclPath = parserContext.getNCSCurrentKP(ned.device_id);
	  String aclName = aclPath.replaceFirst(".+\\{(\\S+)(?: \\d+)?\\}$", "$1");
	  String sapPath = aclPath + "/../../configure/snmp/access-profile/add";


	  if (line.startsWith(DELETE)) {

		  String aclFromData = readConfig(ned, fromT, aclPath, 0);
		  if (aclFromData == null) {
			  ned.log.debug("no rule present in acl on device");
			  return;
		  }
		  String aclToData = readConfig(ned, toT, aclPath, 0);
		  if (aclToData == null || aclFromData.equals(aclToData)) {
			  ned.log.debug("no rule updated in acl");
			  return;
		  }
		  
		  String sapFrom = readConfig(ned, fromT, sapPath, 0);

		  if (sapFrom == null) {
			  ned.log.debug("no snmp access-profile data present on device");
			  return;
		  }

		  String sapCmd =  "configure snmp access-profile add " + aclName + " ";
		  
		  String avoid = "remove-snmp-access-profile : " + sapCmd;
		  if (visited.contains(avoid)) {
			  ned.log.debug("Already handled in previous call");
			  return;
		  }

		  ArrayList<String> removeSap = new ArrayList<>();
          ArrayList<String> redeploySap = new ArrayList<>();

		  for (String entry : sapFrom.split("\n")) {
			  if (entry.contains(sapCmd)) {
				  removeSap.add(sapCmd.replace("add", DELETE));
				  redeploySap.add(entry);
				  break;
			  }
		  }
		  if (removeSap.isEmpty()) {
			  ned.log.debug("no acces-profile reference to DELETE");
			  return;
		  }

		  ned.log.debug("removing acces-profiles updating acl rule: " + sapCmd);

		  visited.add(avoid);
		  parserContext.injectBefore(removeSap);
          redeploySnmpAccessProfile.put(aclPath, redeploySap);
		  
		  
	  } else { //2nd iteration, at create ...
          ArrayList<String> redeploy = redeploySnmpAccessProfile.get(aclPath);
          if (redeploy != null) {
              ned.log.debug("redeploying snmp access-profile after updating acl rule");
              parserContext.injectAfter(redeploy);
          }
	  }
      
  }
  
  public static String readConfig(ExtremeXosNedCli ned, int th, String path, int trim) {

      try {
          path = MaapiUtils.normalizePath(path);

          if (!ned.maapi.exists(th, path)) {
              return null;
          }

          EnumSet<MaapiConfigFlag> flags = EnumSet.of(MaapiConfigFlag.MAAPI_CONFIG_C_IOS,
                                                      MaapiConfigFlag.CISCO_IOS_FORMAT);

          String data = MaapiUtils.readConfig(ned.maapi, th, flags, path);

          String spaces = new String(new char[2+trim]).replace('\0', ' ');
          String prefix = PREFIX;
          String[] lines = data.split("\n");
          StringBuilder sb = new StringBuilder();
          for (int n = 0; n < lines.length; n++) {
              String line = lines[n];
              if (line.startsWith(spaces)) {
                  line = line.substring(2);
                  if (line.trim().startsWith(prefix) || line.trim().startsWith("no "+prefix)) {
                      line = line.replaceFirst(prefix, "");
                  }
                  sb.append(line+"\n");
              }
          }

          data = sb.toString().trim();
          if (data.isEmpty()) {
              return null;
          }

          return data;
      }
      catch (Exception e) {
          ned.log.debug("ExtremeXosCliExtensions.readConfig() failed: %s", e.getMessage());
          return null;
      }
  }
  
  //remove-before-change
  public void removeBeforeChange(final NedWorker worker, Schema.CallbackMetaData metaData,
		  Schema.ParserContext parserContext, final int fromT, final int toT) throws Exception {
	  if (!parserContext.isDelete()) {
		  String line = parserContext.getCurrentLine();
		  String aclPath = parserContext.getNCSCurrentKP(ned.device_id);
		  String aclFromData = readConfig(ned, fromT, aclPath, 0);
	      String metaTag = NedComCliExtensions.NO_PROMPT_AFTER_SEND + "\n";

		  if (aclFromData == null) {
			  ned.log.debug("no from present on device");
			  return;
		  }
		  String aclToData = readConfig(ned, toT, aclPath, 0);
		  if (aclToData == null || aclFromData.equals(aclToData)) {
			  ned.log.debug("no update found");
			  return;
		  }
		  ned.log.debug("inject a delete command"); 
		  parserContext.injectBefore(line.replace("create", DELETE));

	      if (!line.startsWith(metaTag)) {
	    	  String newLine = metaTag+line;
	    	  parserContext.replaceCurrentLine(newLine);
	      }

	  }
		  
  }
  
  
}