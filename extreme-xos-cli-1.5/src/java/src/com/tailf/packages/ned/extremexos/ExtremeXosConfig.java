package com.tailf.packages.ned.extremexos;

import static com.tailf.packages.ned.nedcom.NedString.fillGroups;
import static com.tailf.packages.ned.nedcom.NedString.getMatch;
import static com.tailf.packages.ned.nedcom.NedString.stringQuote;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.tailf.ned.NedErrorCode;
import com.tailf.ned.NedException;
import com.tailf.packages.ned.nedcom.interactor.Console;

public class ExtremeXosConfig {


    private ExtremeXosNedCli ned;



    public ExtremeXosConfig(ExtremeXosNedCli ned) throws Exception {
        this.ned = ned;
   }

    public String modifyInput(String data) throws Exception {

        //banner quote
        data = modifyBanner(data);
        data += getShowAccounts();

        ned.log.debug("Data after transforms:\n" + data);


        return data.trim();
    }

    private String getShowAccounts() throws Exception {
      if (ned.isNetsim()) {
        return "";
      }
      String output = ned.device.execute(Console.ACT_READ_CONFIG, "show configuration detail | inc \"configure account\"");
      return output.trim().replaceAll("configure account (\\w+) encrypted","create account $1 $1 encrypted");
    }

    /**
     * @param worker
     * @param data
     * @return
     */
    private String modifyBanner(String data) {
      String[] regex = {"\n(.*save-to-configuration)\r\n([\\s\\S]*?(?=\r\n\r\n.*?(disable|enable|\r\n#|configure)))",
          "\n(.*after-login)\r\n([\\s\\S]*?(?=\r\n\r\n.*?(disable|enable|\r\n#|configure)))"
      };
      for (String reg : regex) {
        Pattern p = Pattern.compile(reg);
        Matcher m = p.matcher(data);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
          String line = m.group(1);
          String quoted = stringQuote(m.group(2).replace("\r",""));
          ned.log.debug("transformed <= quoted '"+line+" "+ quoted);
          m.appendReplacement(sb, Matcher.quoteReplacement("\n"+line+" "+quoted));
        }
        m.appendTail(sb);
        data = sb.toString();
      }

      return data;
    }

    public String modifyOutput(String data) throws NedException {
    	
        // write/inject-command ned-setting - inject command(s) 
        if (!ned.injectCommand.isEmpty()) {
            ned.log.debug("applying write/inject-command ned-setting");
            for (int n = 0; n < ned.injectCommand.size(); n++) {
                String[] entry = ned.injectCommand.get(n);
                data = injectData("\n" + data.trim() + "\n", entry, "=>");
            }
        }

        return data;
    }
    
    /**
     * Inject line(s) with read/inject-config or write/inject-command ned-settings
     * @param 
     * @return
     * @throws NedException
     */
    private String injectData(String data, String[] entry, String dir) throws NedException {

        String regex = entry[1];
        final String line = entry[2];
        final String where = entry[3]; 
        if (where == null) {
            throw new NedException(NedErrorCode.NED_EXTERNAL_ERROR,"ned-settings: inject missing 'where' value");
        }

        // where == first | where == last
        if ("first".equals(where) || (regex == null && where.startsWith("before"))) {
            if (regex != null && getMatch(data, "("+regex+"(?:[\r])?[\n])") == null) {
                return data;
            }
            ned.log.debug( "transformed "+dir+" injected: "+stringQuote(line)+" first in config");
            return line + "\n" + data;
        } else if ("last".equals(where) || (regex == null && where.startsWith("after"))) {
            if (regex != null && getMatch(data, "("+regex+"(?:[\r])?[\n])") == null) {
                return data;
            }
            ned.log.debug( "transformed "+dir+" injected: "+stringQuote(line)+" last in config");
            return data + line + "\n";
        }

        if (where.contains("-topmode")) {
            // create topmode regex
            regex = "(?<=[\n])" + entry[1].trim() + "(?:[\r])?\n.+?\n(!|exit)";
        }

        // append end of line to regex
        regex += "(?:[\r])?[\n]";

        ned.log.debug( "injectdata regex = "+stringQuote(regex));

        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(data);

        // Special (slow) case for after-last and after-topmode
        String insert;
        if ("after-last".equals(where) || "after-topmode".equals(where)) {
            int end = -1;
            String[] groups = null;
            while (m.find()) {
                end = m.end(0);
                groups = fillGroups(m);
            }
            if (end != -1) {
                try {
                    insert = fillInjectLine(line + "\n", where, groups, dir);
                } catch (Exception e) {
                    throw new NedException(NedErrorCode.NED_EXTERNAL_ERROR,"ned-settings: malformed inject regexp '"+entry[1]+"' : "+e.getMessage());
                }
                data = data.substring(0, end) + insert + "\n" + data.substring(end);
            }
        }

        else {
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                String replacement = m.group(0);
                try {
                    insert = fillInjectLine(line + "\n", where, fillGroups(m), dir);
                } catch (Exception e) {
                    throw new NedException(NedErrorCode.NED_EXTERNAL_ERROR,"ned-settings: malformed inject regexp '"+entry[1]+"' : "+e.getMessage());
                }
                if ("before-first".equals(where) || "before-topmode".equals(where)) {
                    m.appendReplacement(sb, Matcher.quoteReplacement(insert + replacement));
                    break;
                } else if ("before-each".equals(where)) {
                    m.appendReplacement(sb, Matcher.quoteReplacement(insert + replacement));
                } else if ("after-each".equals(where)) {
                    m.appendReplacement(sb, Matcher.quoteReplacement(replacement + insert));
                }
            }
            m.appendTail(sb);
            data = sb.toString();
        }

        return data;
    }
    
    private String fillInjectLine(String insert, String where, String[] groups, String dir) {
        int offset = 0;

        // Replace $i with group value from match.
        // Note: hard coded to only support up to $9
        for (int i = insert.indexOf('$'); i >= 0; i = insert.indexOf('$', i+offset)) {
            int num = (insert.charAt(i+1) - '0');
            insert = insert.substring(0,i) + groups[num] + insert.substring(i+2);
            offset = offset + groups[num].length() - 2;
        }

        ned.log.debug("transformed "+dir+" injected "+stringQuote(insert)+" "+where+" "+stringQuote(groups[0]));

        return insert;
    }
}
