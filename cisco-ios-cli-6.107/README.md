# Table of contents
-------------------

  ```
  1. General
     1.1 Extract the NED package
     1.2 Install the NED package
         1.2.1 Local install
         1.2.2 System install
     1.3 Configure the NED in NSO
  2. Optional debug and trace setup
  3. Dependencies
  4. Sample device configuration
  5. Built in live-status actions
  6. Built in live-status show
  7. Limitations
  8. How to report NED issues
  9. When connecting through a proxy using SSH or TELNET
  10. When connecting to a terminal server
  11. Example of how to configure a device with a slave device (EXEC PROXY)
  12. Fixing switchport issues depending on device and interface type
  13. Configuring ip access-lists standard|extended
  14. NED Secrets - Securing your Secrets
  ```


# 1. General
------------

  This document describes the cisco-ios NED.

  It supports all devices (Catalyst, ISR, NCS, CBR etc) as long as they are
  running IOS or IOS XE. Note: For 'IOS XR' please use the cisco-iosxr NED.

  The NED connects to the device CLI using either SSH or Telnet.
  Support for accessing device via a proxy (jumphost) is also available.

  Configuration is done by sending native CLI commands in a
  transaction to the device through the communication channel. If a
  single command fails, the whole transaction is aborted and reverted.

  If you suspect a bug in the NED, please carefully read section 8
  on how to file a detailed bug report.

  Additional README files bundled with this NED package
  ```
  +---------------------------+------------------------------------------------------------------------------+
  | Name                      | Info                                                                         |
  +---------------------------+------------------------------------------------------------------------------+
  | README-ned-settings.md    | Information about all run time settings supported by this NED.               |
  +---------------------------+------------------------------------------------------------------------------+
  ```

  Common NED Features
  ```
  +---------------------------+-----------+------------------------------------------------------------------+
  | Feature                   | Supported | Info                                                             |
  +---------------------------+-----------+------------------------------------------------------------------+
  | netsim                    | yes       | Doesn't emulate a specific device, just using the model 'best-   |
  |                           |           | effort'                                                          |
  |                           |           |                                                                  |
  | check-sync                | yes       | Six check-sync strategies accepted, see ned-setting 'read        |
  |                           |           | transaction-id-method'                                           |
  |                           |           |                                                                  |
  | partial-sync-from         | yes       | Will do a full show running-configuration towards device, and    |
  |                           |           | filter the contents before sending to NSO                        |
  |                           |           |                                                                  |
  | live-status actions       | yes       | Commands supported as live-status actions:                       |
  |                           |           | show|copy|reload|crypto|clear|ping|license|traceroute|any|any-   |
  |                           |           | hidden                                                           |
  |                           |           |                                                                  |
  | live-status show          | yes       | Check README.md section 'Built in live-status show'              |
  |                           |           |                                                                  |
  | load-native-config        | yes       | Device native 'show configuration' CLIs can be parsed and loaded |
  |                           |           | using this feature.                                              |
  +---------------------------+-----------+------------------------------------------------------------------+
  ```
  Custom NED Features
  ```
  +---------------------------+-----------+------------------------------------------------------------------+
  | Feature                   | Supported | Info                                                             |
  +---------------------------+-----------+------------------------------------------------------------------+
  | proxy-connection          | yes       | Supports 2 proxy jumps as well as direct forwarding (i.e. no     |
  |                           |           | interaction with proxy)                                          |
  |                           |           |                                                                  |
  | NSO ned secret type       | yes       | Check READM.md section 9                                         |
  +---------------------------+-----------+------------------------------------------------------------------+
  ```

  Verified target systems
  ```
  +---------------------------+-----------------+--------+---------------------------------------------------+
  | Model                     | Version         | OS     | Info                                              |
  +---------------------------+-----------------+--------+---------------------------------------------------+
  | ASR1002-X                 | 17.1.1          | ios-xe |                                                   |
  |                           |                 |        |                                                   |
  | ASR1006                   | 15.5(3)S        | ios-xe |                                                   |
  |                           |                 |        |                                                   |
  | ASR-903                   | 15.3(1)S1       | ios-xe |                                                   |
  |                           |                 |        |                                                   |
  | ASR-920-12SZ-IM           | 16.12.1         | ios-xe |                                                   |
  |                           |                 |        |                                                   |
  | WS-C3550-24               | 12.1(20)EA1a    | ios    |                                                   |
  |                           |                 |        |                                                   |
  | WS-C3650-24PD             | 16.3.6          | ios-xe |                                                   |
  |                           |                 |        |                                                   |
  | WS-C3850-24T              | 16.3.6          | ios-xe |                                                   |
  |                           |                 |        |                                                   |
  | WS-C4506                  | 12.2(25)EWA8    | ios    |                                                   |
  |                           |                 |        |                                                   |
  | WS-C4503-E                | 03.10.00.E      | ios-xe |                                                   |
  |                           |                 |        |                                                   |
  | WS-C6504-E                | 15.1(2)SY12     | ios    |                                                   |
  |                           |                 |        |                                                   |
  | C6840-X-LE-40G            | 15.4(1)SY3      | ios    |                                                   |
  |                           |                 |        |                                                   |
  | C9300-24UX                | 16.6.3          | ios-xe |                                                   |
  |                           |                 |        |                                                   |
  | C9407R                    | 16.6.3          | ios-xe |                                                   |
  |                           |                 |        |                                                   |
  | C9500-12Q                 | 16.12.4         | ios-xe |                                                   |
  |                           |                 |        |                                                   |
  | C9800-CL                  | 17.6.1          | ios-xe |                                                   |
  |                           |                 |        |                                                   |
  | cBR-8                     | 16.12.1         | ios-xe |                                                   |
  |                           |                 |        |                                                   |
  | CSR1000V                  | 15.4(2)S        | ios-xe |                                                   |
  |                           |                 |        |                                                   |
  | CSR1000V                  | 16.6.1          | ios-xe |                                                   |
  |                           |                 |        |                                                   |
  | CSR1000V                  | 17.3.5          | ios-xe |                                                   |
  |                           |                 |        |                                                   |
  | CISCO1921/K9              | 15.3(3)M2       | ios    |                                                   |
  |                           |                 |        |                                                   |
  | WS-C2960X-24TD-L          | 15.0(2a)EX5     | ios    |                                                   |
  |                           |                 |        |                                                   |
  | C891F-K9                  | 15.3(3)M2       | ios    |                                                   |
  |                           |                 |        |                                                   |
  | ISR4331/K9                | 16.6.3          | ios-xe |                                                   |
  |                           |                 |        |                                                   |
  | ISR4451-X/K9              | 16.8.1          | ios-xe |                                                   |
  |                           |                 |        |                                                   |
  | ME-3400EG-2CS-A           | 12.2(60)EZ12    | ios    |                                                   |
  |                           |                 |        |                                                   |
  | ME-3600X-24CX-M           | 15.6(2)SP5      | ios    |                                                   |
  |                           |                 |        |                                                   |
  | WS-C3750E-24TD            | 12.2(55)SE11    | ios    |                                                   |
  |                           |                 |        |                                                   |
  | ME-3800X-24FS-M           | 15.4(3)S4       | ios    |                                                   |
  |                           |                 |        |                                                   |
  | NCS4206-SA                | 16.9.3          | ios-xe |                                                   |
  +---------------------------+-----------------+--------+---------------------------------------------------+
  ```


## 1.1 Extract the NED package
------------------------------

  It is assumed the NED package `ncs-<NSO version>-cisco-ios-<NED version>.signed.bin` has already
  been downloaded from software.cisco.com.

  In this instruction the following example settings will be used:

  - NSO version: 6.0
  - NED version: 1.0.1
  - NED package downloaded to: /tmp/ned-package-store

  1. Extract the NED package and verify its signature:

      ```
      > cd /tmp/ned-package-store
      > chmod u+x ncs-6.0-cisco-ios-1.0.1.signed.bin
      > ./ncs-6.0-cisco-ios-1.0.1.signed.bin
      ```

  2. In case the signature can not be verified (for instance if no internet connection),
     do as below instead:

      ```
      > ./ncs-6.0-cisco-ios-1.0.1.signed.bin --skip-verification
      ```

  3. The result of the extraction shall be a tar.gz file with the same name as the .bin file:

      ```
      > ls *.tar.gz
      ncs-6.0-cisco-ios-1.0.1.tar.gz
      ```


## 1.2 Install the NED package
------------------------------

  There are two alternative ways to install this NED package.
  Which one to use depends on how NSO itself is setup.

  In the instructions below the following example settings will be used:

  - NSO version: 6.0
  - NED version: 1.0.1
  - NED download directory: /tmp/ned-package-store
  - NSO run time directory: ~/nso-lab-rundir

  A prerequisite is to set the environment variable NSO_RUNDIR to point at the NSO run time directory:

  ```
  > export NSO_RUNDIR=~/nso-lab-rundir
  ```


### 1.2.1 Local install
-----------------------

  This section describes how to install a NED package on a locally installed NSO
  (see "NSO Local Install" in the NSO Installation guide).

  It is assumed the NED package has been been unpacked to a tar.gz file as described in 1.1.

  1. Untar the tar.gz file. This creates a new sub-directory named:
     `cisco-ios-<NED major digit>.<NED minor digit>`:

     ```
     > tar xfz ncs-6.0-cisco-ios-1.0.1.tar.gz
     > ls -d */
     cisco-ios-cli-1.0
     ```

  2. Install the NED into NSO, using the ncs-setup tool:

     ```
     > ncs-setup --package cisco-ios-cli-1.0 --dest $NSO_RUNDIR
     ```

  3. Open a NSO CLI session and load the new NED package like below:

     ```
     > ncs_cli -C -u admin
     admin@ncs# packages reload
     reload-result {
         package cisco-ios-cli-1.0
         result true
     }
     ```

  Alternatively the tar.gz file can be installed directly into NSO. Then skip steps 1 and 2 and do like
  below instead:

  ```
    > ncs-setup --package ncs-6.0-cisco-ios-1.0.1.tar.gz --dest $NSO_RUNDIR
    > ncs_cli -C -u admin
    admin@ncs# packages reload
    reload-result {
      package cisco-ios-cli-1.0
      result true
   }
  ```


### 1.2.2 System install
------------------------

  This section describes how to install a NED package on a system installed NSO (see "NSO System
  Install" in the NSO Installation Guide).

  It is assumed the NED package has been been unpacked to a tar.gz file as described in 1.1.

  1. Do a NSO backup before installing the new NED package:

     ```
     > $NCS_DIR/bin/ncs-backup
     ```

  2. Start a NSO CLI session and fetch the NED package:

     ```
     > ncs_cli -C -u admin
     admin@ncs# software packages fetch package-from-file \
               /tmp/ned-package-store/ncs-6.0-cisco-ios-1.0.tar.gz
     admin@ncs# software packages list
     package {
      name ncs-6.0-cisco-ios-1.0.tar.gz
      installable
     }
     ```

  3. Install the NED package (add the argument replace-existing if a previous version has been loaded):

     ```
     admin@ncs# software packages install cisco-ios-1.0
     admin@ncs# software packages list
     package {
      name ncs-6.0-cisco-ios-1.0.tar.gz
      installed
     }
     ```

  4. Load the NED package

     ```
     admin@ncs# packages reload
     admin@ncs# software packages list
     package {
       name ncs-6.0-cisco-ios-cli-1.0
       loaded
     }
     ```


## 1.3 Configure the NED in NSO
-------------------------------

  This section describes the steps for configuring a device instance
  using the newly installed NED package.

  - Start a NSO CLI session:

    ```
    > ncs_cli -C -u admin
    ```

  - Enter configuration mode:

    ```
    admin@ncs# configure
    Entering configuration mode terminal
    admin@ncs(config)#
    ```

  - Configure a new authentication group (my-group) to be used for this device:

    ```
    admin@ncs(config)# devices authgroup group my-group default-map remote-name <user name on device> \
                       remote-password <password on device>
    ```

  - Configure a new device instance (example: dev-1):

    ```
    admin@ncs(config)# devices device dev-1 address <ip address to device>
    admin@ncs(config)# devices device dev-1 port <port on device>
    admin@ncs(config)# devices device dev-1 device-type cli ned-id cisco-ios-cli-1.0
    admin@ncs(config)# devices device dev-1 state admin-state unlocked
    admin@ncs(config)# devices device dev-1 authgroup my-group
    ```
    ```
    admin@ncs(config)# devices device dev-1 protocol <ssh or telnet>
    ```

  - If configured protocol is ssh, do fetch the host keys now:

     ```
     admin@ncs(config)# devices device dev-1 ssh fetch-host-keys
     ```

  - Finally commit the configuration

    ```
    admin@ncs(config)# commit
    ```

  - Verify configuration, using a sync-from.

    ```
    admin@ncs(config)# devices device dev-1 sync-from
    result true
    ```

  If the sync-from was not successful, check the NED configuration again.


# 2. Optional debug and trace setup
-----------------------------------

  It is often desirable to see details from when and how the NED interacts with the device(Example: troubleshooting)

  This can be achieved by configuring NSO to generate a trace file for the NED. A trace file
  contains information about all interactions with the device. Messages sent and received as well
  as debug printouts, depending on the log level configured.

  NSO creates one separate trace file for each device instance with tracing enabled.
  Stored in the following location:

  `$NSO_RUNDIR/logs/ned-cisco-ios-cli-1.0-<device name>.trace`

  Do as follows to enable tracing in one specific device instance in NSO:


  1. Start a NSO CLI session:

     ```
     > ncs_cli -C -u admin
     ```

  2. Enter configuration mode:

     ```
     admin@ncs# configure
     Entering configuration mode terminal
     admin@ncs(config)#
     ```

  3. Enable trace raw:

     ```
     admin@ncs(config)# devices device dev-1 trace raw
     admin@ncs(config)# commit
     ```

     Alternatively, tracing can be enabled globally affecting all configured device instances:

     ```
     admin@ncs(config)# devices global-settings trace raw
     admin@ncs(config)# commit
     ```

  4. Configure the log level for printouts to the trace file:

     ```
     admin@ncs(config)# devices device dev-1 ned-settings cisco-ios logger \
                       level [debug | verbose | info | error]
     admin@ncs(config)# commit
     ```

     Alternatively the log level can be set globally affecting all configured
     device instances using this NED package.

     ```
     admin@ncs(config)# devices device global-settings ned-settings cisco-ios logger \
                       level [debug | verbose | info | error]
     admin@ncs(config)# commit
     ```

  The log level 'info' is used by default and the 'debug' level is the most verbose.

  **IMPORTANT**:
  Tracing shall be used with caution. This feature does increase the number of IPC messages sent
  between the NED and NSO. In some cases this can affect the performance in NSO. Hence, tracing should
  normally be disabled in production systems.


  An alternative method for generating printouts from the NED is to enable the Java logging mechanism.
  This makes the NED print log messages to common NSO Java log file.

  `$NSO_RUNDIR/logs/ncs-java-vm.log`

  Do as follows to enable Java logging in the NED

  1. Start a NSO CLI session:

     ```
     > ncs_cli -C -u admin
     ```

  2. Enter configuration mode:

     ```
     admin@ncs# configure
     Entering configuration mode terminal
     admin@ncs(config)#
     ```

  3. Enable Java logging with level all from the NED package:

     ```
     admin@ncs(config)# java-vm java-logging logger com.tailf.packages.ned.ios \
                       level level-all
     admin@ncs(config)# commit
     ```

  4. Configure the NED to log to the Java logger

     ```
     admin@ncs(config)# devices device dev-1 ned-settings cisco-ios logger java true
     admin@ncs(config)# commit
     ```

     Alternatively Java logging can be enabled globally affecting all configured
     device instances using this NED package.

     ```
     admin@ncs(config)# devices global-settings ned-settings cisco-ios logger java true
     admin@ncs(config)# commit
     ```

  **IMPORTANT**:
  Java logging does not use any IPC messages sent to NSO. Consequently, NSO performance is not
  affected. However, all log printouts from all log enabled devices are saved in one single file.
  This means that the usability is limited. Typically single device use cases etc.


# 3. Dependencies
-----------------

  This NED has the following host environment dependencies:

  - Java 1.8 (NSO version < 6.2)
  - Java 17 (NSO version >= 6.2)
  - Gnu Sed

  Dependencies for NED recompile:

   - Apache Ant
   - Bash
   - Gnu Sort
   - Gnu awk
   - Grep
   - Python3 (with packages: re, sys, getopt, subprocess, argparse, os, glob)


# 4. Sample device configuration
--------------------------------

  For instance, create a second Loopback interface that is down:

     admin@ncs(config)# devices device dev-1 config
     admin@ncs(config-config)# interface Loopback 1
     admin@ncs(config-if)# ip address 128.0.0.1 255.0.0.0
     admin@ncs(config-if)# shutdown

  See what you are about to commit:

     admin@ncs(config-if)# commit dry-run outformat native
     device dev-1
       interface Loopback1
        ip address 128.0.0.1 255.0.0.0
        shutdown
       exit

  Commit new configuration in a transaction:

     admin@ncs(config-if)# commit
     Commit complete.

  Verify that NCS is in-sync with the device:

      admin@ncs(config-if)# devices device dev-1 check-sync
      result in-sync

  Compare configuration between device and NCS:

      admin@ncs(config-if)# devices device dev-1 compare-config
      admin@ncs(config-if)#

  Note: if no diff is shown, supported config is the same in NCS as on the device.


# 5. Built in live-status actions
---------------------------------

     The NED has support for all exec commands in config mode. They can
     be accessed using the 'exec' prefix. For example:

      admin@ncs(config)# devices device dev-1 config exec
                         "default interface GigabitEthernet0/0/0"
      result
      > default interface GigabitEthernet0/0/0
      Interface GigabitEthernet0/0/0 set to default configuration
      Router(config)#

     The NED also has support for all operational Cisco IOS commands
     by use of the 'devices device live-status exec any' action. Or,
     if you do not want to log|trace the command, use the any-hidden.

     For example:

      admin@ncs# devices device dev-1 live-status exec any
                 "show running-config interface Loopback0"
      result
      Building configuration...

      Current configuration : 42 bytes
      !
      interface Loopback0
       no ip address
      end

     To execute multiple commands, separate them with " ; "
     NOTE: Must be a white space on either side of the comma.
     For example:

      admin@ncs# devices device dev-1 live-status exec any
                 "show run int Gig0/0/0 ; show run int Gig0/0/1"
      result
      > show run int Gig0/0/0
      Building configuration...

      Current configuration : 71 bytes
      !
      interface GigabitEthernet0/0/0
       no ip address
       negotiation auto
      end

      Router#
      > show run int Gig0/0/1
      Building configuration...

      Current configuration : 112 bytes
      !
      interface GigabitEthernet0/0/1
       no ip address
       standby 1 priority 1
       standby 1 preempt
       negotiation auto
      end

      Router#

     NOTE: To Send CTRL-C send "CTRL-C" or "CTRL-C async" to avoid
           waiting for device output. Also note that you most likely
           will have to extend timeouts to avoid closing the current
           connection and send CTRL-C to a new connection, i.e. CTRL-C
           being ignored

     Generally the command output parsing halts when the NED detects
     an operational or config prompt, however sometimes the command
     requests additional input, 'answer(s)' to questions.

     To respond to device question(s) there are 3 different methods,
     checked in the listed order below:

     [1] the action auto-prompts list, passed in the action
     [2] the ned-settings cisco-ios live-status auto-prompts list
     [3] the command line args "| prompts" option

     IMPORTANT: [3] can be used to override an answer in auto-prompts.

     Read on for details on each method:

     [1] action auto-prompts list

     The auto-prompts list is used to pass answers to questions, to
     exit parsing, reset timeout or ignore output which triggered the
     the built-in question handling. Each list entry contains a question
     (regex format) and an optional answer (text or built-in keyword).

     The following built-in answers are supported:

     <exit>     Halt parsing and return output
     <prompt>   Retrieve the answer from "| prompts" argument(s)
     <timeout>  Reset the read timeout, useful for slow commands
     <ignore>   (or IGNORE) Ignore the output and continue parsing
     <enter>    (or ENTER) Send a newline and continue parsing

     Any other answer value is sent to the device followed by a newline,
     unless the answer is a single letter answer in case which only the
     single character is sent.

     Note: not configuring an answer is the same as setting it to <ignore>

     Here is an example of a command which needs to ignore some output
     which would normally be interpreted as a question due to the colon:

     exec auto-prompts { question "Certificate Request follows[:]" answer
           "<ignore>" } "crypto pki enroll LENNART-TP | prompts yes no"

     Also note the use of method 3, answering yes and no to the remaining
     device questions.


     [2] ned-settings cisco-ios live-status auto-prompts list

     The auto-prompts list works exactly as [1] except that it is
     configured and used for all device commands, i.e. not only for
     this specific action.

     Here are some examples of auto-prompts ned-settings:

     devices global-settings ned-settings cisco-ios live-status auto-prompts Q1 question "System configuration has been modified" answer "no"
     devices global-settings ned-settings cisco-ios live-status auto-prompts Q2 question "Do you really want to remove these keys" answer "yes"
     devices global-settings ned-settings cisco-ios live-status auto-prompts Q3 question "Press RETURN to continue" answer ENTER

     NOTE: Due to backwards compatibility, ned-setting auto-prompts
     questions get ".*" appended to their regex unless ending with
     "$". However, for option [1] the auto-prompt list passed in the
     action, you must add ".*" yourself if this matching behaviour is
     desired.


     [3] "| prompts"

     "| prompts" is passed in the command args string and is used to
     submit answer(s) to the device without a matching question pattern.
     IMPORTANT: It can also be used to override answer(s) configured in
     auto-prompts list, unless the auto-prompts contains <exit> or
     <timeout>, which are always handled first.

     One or more answers can be submitted following this syntax:

         | prompts <answer 1> .. [answer N]

     For example:

     devices device dev-1 live-status exec any "reload | prompts no yes"

     The following output of the device triggers the NED to look for the
     answer in | prompts arguments:

         ":\\s*$"
         "\\][\\?]?\\s*$"

     In other words, the above two patterns (questions) have a built-in
     <prompt> for an answer.

     Additional patterns triggering | prompts may be configured by use
     of auto-lists and setting the answer to <prompt>. This will force
     the user to specify the answer in | prompts.

     The <ignore> or IGNORE keywords can be used to ignore device output
     matching the above and continue parsing. If all output should be
     ignored, i.e. for a show command, '| noprompts' should be used.

     Some final notes on the 'answer' leaf:

     - "ENTER" or <enter> means a carriage return + line feed is sent.

     - "IGNORE", "<ignore>" or unset means the prompt was not a
        question, the device output is ignored and parsing continues.

     - A single letter answer is sent without carriage return + line,
       i.e. "N" will be sent as N only, with no return. If you want a
       return, set "NO" as the answer instead.


    There are a number of internal live-status command which may be of
    use when debugging/developing. An internal live-status command is
    executed in ncs_cli like this:

      admin@ncs# devices device dev-1 live-status exec any <internal command>

    The internal live-status commands are for development team but the
    following internal live-status commands may be of use:


    sync-from-file <file>

      Next sync-from will load the config from the file specified by
      <file> as if the config was synced from a real device. This can
      be useful to test what config is supported if you get output from
      show running-config from e.g. a raw trace.
      Example:
       admin@ncs# devices device netsim-0 live-status exec any sync-from-file /tmp/config.txt
       result
       Next sync-from will use file = /tmp/config.txt
       admin@ncs# devices device netsim-0 sync-from
      Note: Always used a NETSIM device with this setting.


    check-config-trace <trace>

      Check config from first show run in trace, listing all unknown configuration.


    check-config-dir <path>

      Check all cisco-ios trace files for unknown configuration in a directory.


    accept-eula <seconds>

      Use this command to accept all EULA agreements. The <seconds> is
      the maximum number of seconds to wait for the device to output the
      agreement.


    show outformat raw

      Will show the next 'commit dry-run outformat native' unmodified,
      i.e. with no NED transformations done.


    show ned-settings

      Will show all ned-settings for this device


# 6. Built in live-status show
------------------------------

  The cisco-ios NED supports the following live-status 'show' TTL-based commands:

  ```
  admin@ncs# show devices device csr1000v-6 live-status
  Possible completions:
    access-tunnel              show access tunnel
    arp                        show arp
    bgp                        BGP information
    cdp                        show cdp
    crypto
    device-tracking-database   show device-tracking database
    interfaces-state
    inventory                  show inventory
    ios-stats:interfaces       show interfaces
    ip
    lisp                       show lisp
    lldp                       show lldp
    running-config             Read only 'config' still shown in running-config on device
    version                    show version
    voice                      Global voice stats
    vrf                        show vrf

  ```

  Example of a live-status call:

  ```
  admin@ncs# show devices device csr1000v-6 live-status ios-stats:interfaces
                         ADMIN
  TYPE             NAME  STATUS  IP ADDRESS          MAC ADDRESS
  -------------------------------------------------------------------
  GigabitEthernet  1     up      192.168.122.100/24  5254.008e.79d2
  GigabitEthernet  2     down    -                   5254.007f.4a02
  GigabitEthernet  3     down    -                   5254.0074.ad90
  GigabitEthernet  4     down    -                   5254.0060.efe8
  GigabitEthernet  5     down    -                   5254.00bc.fcca
  admin@ncs#

  ```


# 7. Limitations
----------------

  NONE


# 8. How to report NED issues and feature requests
--------------------------------------------------

  **Issues like bugs and errors shall always be reported to the Cisco NSO NED team through
  the Cisco Support channel:**

  - <https://www.cisco.com/c/en/us/support/index.html>
  - <https://developer.cisco.com/docs/nso/#!support/network-service-orchestrator-support>

  The following information is required for the Cisco NSO NED team to be able
  to investigate an issue:

    - A detailed recipe with steps to reproduce the issue.
    - A raw trace file generated when the issue is reproduced.
    - SSH/TELNET access to a device where the issue can be reproduced by the Cisco NSO NED team.
      This typically means both read and write permissions are required.
      Pseudo access via tools like Webex, Zoom etc is not acceptable.
      However, it is ok with device access through VPNs, jump servers etc though.

  Do as follows to gather the necessary information needed for your device, here named 'dev-1':

  1. Enable full debug logging in the NED

     ```
     ncs_cli -C -u admin
     admin@ncs# configure
     admin@ncs(config)# devices device dev-1 ned-settings cisco-ios logging level debug
     admin@ncs(config)# commit
     ```

  2. Configure the NSO to generate a raw trace file from the NED

     ```
     admin@ncs(config)# devices device dev-1 trace raw
     admin@ncs(config)# commit
     ```

  3. If the NED already had trace enabled, clear it in order to submit only relevant information

     Do as follows for NSO 6.4 or newer:

     ```
     admin@ncs(config)# devices device dev-1 clear-trace
     ```

     Do as follows for older NSO versions:

     ```
     admin@ncs(config)# devices clear-trace
     ```

  4. Run a compare-config to populate the trace with initial device config

     ```
     admin@ncs(config)# devices device dev-1 compare-config
     ```

  5. Reproduce the found issue using ncs_cli or your NSO service.
     Write down each necessary step in a reproduction report.

     Please always also show the change in CLI format before commit:

      admin@ncs(config)# commit dry-run outformat native

     In addition to this, it helps if you can show how it should work
     by manually logging into the device using SSH/TELNET and type
     the relevant commands showing a successful operation.

     If the commit succeeds but the problem is a compare-config or
     out of sync issue, then end with a 2nd compare-config:

      admin@ncs(config)# devices device dev-1 compare-config

  6. Gather the reproduction report and a copy of the raw trace file
     containing data recorded when the issue happened.

  7. Contact the Cisco support and request to open a case. Provide the gathered files
     together with access details for a device that can be used by the
     Cisco NSO NED when investigating the issue.


  **Requests for new features and extensions of the NED are handled by the Cisco NSO NED team when
  applicable. Such requests shall also go through the Cisco support channel.**

  The following information is required for feature requests and extensions:

  1. Set the config on the real device including all existing dependent config
     and run sync-from to show it in the trace.

  2. Run sync-from # devices device dev-1 sync-from

  3. Attach the raw trace to the ticket

  4. List the config you want implemented in the same syntax as shown on the device

  5. SSH/TELNET access to a device that can be used by the Cisco NSO NED team for testing and verification
     of the new feature. This usually means that both read and write permissions are required.
     Pseudo access via tools like Webex, Zoom etc is not acceptable. However, it is ok with access
     through VPNs, jump servers etc as long as we can connect to the NED via SSH/TELNET.


# 9. When connecting through a proxy using SSH or TELNET
--------------------------------------------------------

  When connecting through a proxy using SSH or TELNET you must use a
  set of ned-settings, all residing under cisco-ios proxy.

  Do as follows to setup to connect to a IOS device that resides
  behind a proxy or terminal server:

   +-----+  A   +-------+   B  +-----+
   | NCS | <--> | proxy | <--> | IOS |
   +-----+      +-------+      +-----+

  Setup connection (A):

   # devices device dev-1 address <proxy address>
   # devices device dev-1 port <proxy port>
   # devices device dev-1 device-type cli protocol <proxy proto - telnet or ssh>
   # devices authgroups group ciscogroup umap admin remote-name <proxy username>
   # devices authgroups group ciscogroup umap admin remote-password <proxy password>
   # devices device dev-1 authgroup ciscogroup

  Setup connection (B):

  Define the type of connection to the device:

   # devices device dev-1 ned-settings cisco-ios proxy remote-connection <ssh|telnet>

  Define login credentials for the device:

   # devices device dev-1 ned-settings cisco-ios proxy remote-name <user name on the IOS device>
   # devices device dev-1 ned-settings cisco-ios proxy remote-password <password on the IOS device>

  (note: instead of configuring remote-name|password 'proxy authgroup' can be configured)

  [optional] Define prompt on proxy server before sending (not required for IOS(XR) proxy):

   # devices device dev-1 ned-settings cisco-ios proxy proxy-prompt <prompt pattern on proxy>

  Define pattern on proxy server after sending telnet/ssh, but before second login:

   # devices device dev-1 ned-settings cisco-ios proxy proxy-prompt2 <prompt pattern on proxy>

  Define address and port of IOS device:

   # devices device dev-1 ned-settings cisco-ios proxy remote-address <address to the IOS device>
   # devices device dev-1 ned-settings cisco-ios proxy remote-port <port used on the IOS device>

  [optional] Modify/extend the default connection command syntax from its default:

   # devices device dev-1 ned-settings cisco-ios proxy remote-command "telnet $address $port /vrf Mgmt-intf"

  Commit configuration and make sure the ned-settings are re-read:

   # commit
   # devices disconnect


# 10. When connecting to a terminal server
------------------------------------------

  Use cisco-ios proxy remote-connection serial when you are
  connecting to a terminal server. The setting triggers sending of
  extra new-lines to activate the login sequence.

  You also have the option of configuring a menu regexp and answer to
  be able to bypass menu selections.

  You may also need to specify remote-name and remote-password if the
  device has a separate set of login credentials.

  Finally, you may also need to set the cisco-ios connection
  prompt-timeout ned-setting (in milliseconds) to trigger sending of
  more newlines if the login process requires it. The NED will send
  onenewline per timeout until connect-timeout is reached and the the
  login fails.

  Example config for terminal server with 2nd login but no menu:

  devices authgroups group term-dev default-map remote-name 1st-username remote-password 1st-password remote-secondary-password cisco
  devices device term-dev address 1.2.3.4 port 1234
  devices device term-dev authgroup term-dev device-type cli ned-id cisco-ios protocol telnet
  devices device term-dev connect-timeout 30 read-timeout 600 write-timeout 600
  devices device term-dev state admin-state unlocked
  devices device term-dev ned-settings cisco-ios proxy remote-connection serial
  devices device term-dev ned-settings cisco-ios proxy remote-name 2nd-username
  devices device term-dev ned-settings cisco-ios proxy remote-password 2nd-password
  devices device term-dev ned-settings cisco-ios connection prompt-timeout 4000

  Example config for terminal server with menu but no 2nd login:

  devices authgroups group term-dev default-map remote-name 1st-username remote-password 1st-password remote-secondary-password cisco
  devices device term-dev address 1.2.3.4 port 22
  devices device term-dev authgroup term-dev device-type cli ned-id cisco-ios protocol ssh
  devices device term-dev connect-timeout 30 read-timeout 600 write-timeout 600
  devices device term-dev state admin-state unlocked
  devices device term-dev ned-settings cisco-ios proxy remote-connection serial
  devices device term-dev ned-settings cisco-ios connection prompt-timeout 4000
  devices device term-dev ned-settings cisco-ios proxy menu regexp "\\AChoose your option" answer "e\n"
    or a second example:
  devices device term-dev ned-settings cisco-ios proxy menu regexp "\\ASelection:" answer "x\n"


# 11. Example of how to configure a device with a slave device (EXEC PROXY)
---------------------------------------------------------------------------

  The NED can also support connecting to a slave device, reachable
  only by first connecting to the master device. This setting contains
  a config example of how to achieve that.

  Example config:

  Master device:

  devices device 891w address 10.67.16.59 port 23
  devices device 891w authgroup 891wauth device-type cli ned-id cisco-ios protocol telnet
  devices device 891w connect-timeout 15 read-timeout 60 write-timeout 60
  devices device 891w state admin-state unlocked

  Slave device (accessed through master and proxy, a command run in exec mode):

  devices device ap801 address 10.67.16.59 port 23
  devices device ap801 authgroup 891wauth device-type cli ned-id cisco-ios protocol telnet
  devices device ap801 connect-timeout 15 read-timeout 60 write-timeout 60
  devices device ap801 state admin-state unlocked
  devices device ap801 ned-settings cisco-ios proxy remote-connection exec
  devices device ap801 ned-settings cisco-ios proxy remote-command "service-module wlan-ap 0 session"
  devices device ap801 ned-settings cisco-ios proxy remote-prompt "Open"
  devices device ap801 ned-settings cisco-ios proxy remote-name cisco
  devices device ap801 ned-settings cisco-ios proxy remote-password cisco123
  !devices device ap801 ned-settings cisco-ios proxy remote-secondary-password cisco123


# 12. Fixing switchport issues depending on device and interface type
---------------------------------------------------------------------

  There are a two main formats used among IOS devices for switchport
  configurations. Then there is also some devices which do not
  support the switchport config on some interfaces, or all.

  By default the NED injects 'no switchport' first in all
  Port-channel and Ethernet interfaces in order to avoid a
  compare-config diff. This is same as a global config inject rule:

  !devices global-settings ned-settings cisco-ios read inject-interface-config spg interface "Ethernet|Port-channel" config "no switchport"

  For device or interface types which hide 'switchport' when enabled
  (but still no switchport setting set) a 'switchport' must be
  injected to avoid a diff. The following are some identified
  examples:

  devices device me3400 ned-settings cisco-ios read inject-interface-config spd interface "Ethernet|Port-channel" config "switchport"
  devices device cat3750 ned-settings cisco-ios read inject-interface-config spd interface "Ethernet|Port-channel" config "switchport"
  devices device me3800 ned-settings cisco-ios read inject-interface-config spd interface "Ethernet|Port-channel" config "switchport"

  Note that in order for the new switchport setting to take effect, you
  must disconnect and disconnect. A sync-from may also be needed to
  populate NCS/NSO CDB with the injected config.

  Finally, there is also a new ned-setting 'cisco-ios auto
  interface-switchport-status' which can be used instead of the
  described inject settings above. Using this ned-setting, the ned will
  check the interface type using "show <ifname> switchport" and auto
  inject 'switchport' or 'no switchport'. The only drawback to this is
  a somewhat reduced performance due to the additional show command
  each time interface is read.


# 13. Configuring ip access-lists standard|extended
---------------------------------------------------

    Before deciding how to configure 'ip access-list standard|extended'
    you must choose whether to use rule sequence numbers or not. If the
    device shows access-list sequence numbers in 'show run' then you
    must also configure them in NSO CDB or you will get a config diff.

    If you want to configure rules with sequence numbers and the device
    does not show them, you must set 'ip access-list persistent' to
    enable it, and vice versa.

    Also IMPORTANT to mention is that the NED can not auto-correct
    rules once set on the device. Hence, in order to avoid a config
    diff you must always configure the rules exactly as shown on the
    device with 'show running-config. Best and simplest way to learn
    how to configure your rules is to simply set them on the device
    first then sync-from and show them in NSO, in native or XML format.

    Finally, there are four different methods in the NED to configure
    ip access-lists. Read them carefully before choosing which to use,
    they all have different advantages and disadvantages.

    Method #1 - Default

    Use the ios:ip/access-list/standard/std-named-acl{name} or
    ios:ip/access-list/extended/ext-named-acl{name} lists to
    configure ip access-lists. Optional rule sequence number
    is embedded in the rule list multi-word-key leaf. This is the
    default setting and should work for most cases. However, there
    are some drawbacks, one being you cant insert|move rules without
    deleting all previous entries first. The NED will handle this
    automatically for you but it will be costly for large lists.


    Method #2 - New API

    Set 'api new-ip-access-list' ned-setting to true to enable.

    If you use this setting you must configure ip access-list
    standard and extended entries in ios:ip/access-list/filter-list{name}
    You must also have 'ip access-list persistent' configured
    to enable output of sequence numbers in show run on the device.
    The advantage of using this method is that you can insert|move
    individual rules by referencing the rule sequence number key 'seq'.
    However, please make sure to space your sequence numbers to make
    it possible for insertions or you may not be able to insert more.


    Method #3 - Resequence

    Set 'api access-list-resequence' ned-setting to true to enable.

    This method, which unfortunately only works with non-remark IPv4
    'ip access-list extended' rules, is the most flexible method if you
    can use it. The NED will automatically resequence the rules after each
    modification, making it possible to insert rules indefinitely.
    Configure the entries in ios:ip/access-list/resequence/extended{name}
    with this setting. Although you must have 'ip access-list persistent'
    enabled on the device you do not need to configure sequence numbers
    in NSO since they are automatically handled by the NED.


    Method #4 - Unordered

    Set 'api unordered-ip-access-list-regex' to list name regex to enable.

    This is the only method which may be used in combination with the
    other methods since it only affects list entries matching the regex
    name in the ned-setting. The method should be used for access-list
    entries where the order does not matter and/or the device reorders
    the entries after they have been set. To enable, choose a regex which
    matches the names of the unordered entries, and finally, configure
    them in ios:ios/access-list/unordered{name}, which supports both
    standard and extended access-lists, configured in 'type' leaf.


# 14. NED Secrets - Securing your Secrets
-----------------------------------------

    It is best practice to avoid storing your secrets (e.g. passwords and
    shared keys) in plain-text, either on NSO or on the device. In NSO we
    support multiple encrypted datatypes that are encrypted using a local
    key, similarly many devices such as Cisco IOS supports automatically
    encrypting all passwords stored on the device. On Cisco IOS this can
    be enabled using commands like these:

      # key config-key password-encryption SUPERSECRET
      # service password-encryption
      # password encryption aes

    which makes the system automatically encrypt all passwords using
    the key `SUPERSECRET` and show them encrypted in the output of
    `show running`.

    Naturally, for security reasons, NSO in general has no way of
    encrypting/decrypting passwords with the secret key on the
    device. This means that if nothing is done about this we will
    become out of sync once we write secrets to the device. Looking at
    the cisco-ios NED there are over 500 paths that contain such secrets.

    In order to avoid becoming out of sync the NED reads back these elements
    immediately after set and stores the encrypted value(s) in a special
    `secrets` table in oper data. Later on, when config is read from the
    device, the NED replaces all cached encrypted values with their plaintext
    values; effectively avoiding all config diffs in this area. If the values
    are changed on the device, the new encrypted value will not match the
    cached pair and no replacement will take place. This is desired, since out
    of band changes should be detected.

    This handles the device-side encryption, but passwords are still unencrypted
    in NSO. To deal with this we support using NSO-encrypted strings instead of
    plaintext passwords in the NSO data model.

    --- Handling auto-encryption

    Let us say that we have password-encryption on and we want to write a new
    user to our device:

      admin@ncs(config)# devices device dev-1 config username newuser password 0 magic
      admin@ncs(config-config)# commit

    this will be automatically encrypted by the device

      Router#show running-config | section usernaname
      username newuser password 6 xAb[PDCO[fQDJhDfMIciONMedifAAB

    But the secrets management will store this new encrypted value in our `secrets` table:

      admin@ncs# show devices device dev-1 ned-settings secrets
      ID                                      ENCRYPTED                         REGEX
      ---------------------------------------------------------------------------------
      ios:username(newuser)/password/secret   6 xAb[PDCO[fQDJhDfMIciONMedifAAB

      which means that compare-config or sync-from will not show any
      changes and will not result in any updates to CDB". In fact, we can
      still see the unencrypted value in the device tree:

      admin@ncs# show running-config devices device dev-1 config username
      devices device dev-1
       config
         username admin password 6 S]iVZERdHTgVdfxKVQYOMXQRR^QHM^
         username cisco privilege 15 password 6 "Y]i`\ZegLUiB[EMPYUAKhOBK]VRAAB"
         username newuser password 0 magic
        !
       !

    --- Increasing security with NSO-side encryption

    We have two alternatives, either we can manually encrypt our values using
    one of the NSO-encrypted types (e.g `aes-256-cfb-128-encrypted-string`) and
    set them to the tree, or we can recompile the NED to always encrypt secrets.

    --- Setting encrypted value

    Let us say we know that the NSO-encrypted string
      `$9$T963R76+wgaQuZCtcGC/Nreo75FigP+znmOln8XDFK0=` (`admin`), we
    can then set it in the device tree as normal

      admin@ncs(config)# devices device dev-1 config username newuser2 password 0 $9$T963R76+wgaQuZCtcGC/Nreo75FigP+znmOln8XDFK0=
      admin@ncs(config-config)# commit

    when commiting this value it will be decrypted and the plaintext will be written to the device.
    Unlike the previous example the plaintext is not visible in the device tree:

      admin@ncs# show running-config devices device dev-1 config username
      devices device dev-1
       config
         username admin password 6 S]iVZERdHTgVdfxKVQYOMXQRR^QHM^
         username cisco privilege 15 password 6 "Y]i`\ZegLUiB[EMPYUAKhOBK]VRAAB"
         username newuser password 0 magic
         username newuser2 password 0 $9$T963R76+wgaQuZCtcGC/Nreo75FigP+znmOln8XDFK0=
       !
     !

    On the device side this plaintext value is of course encrypted
    with the device key, and just as before we store it in our
    `secrets` table:

    admin@ncs# show devices device dev-1 ned-settings secrets
    ID                                      ENCRYPTED                         REGEX
    ---------------------------------------------------------------------------------
    ios:username(newuser)/password/secret   6 xAb[PDCO[fQDJhDfMIciONMedifAAB
    ios:username(newuser2)/password/secret  6 XTXfOVUcYDZKd`FBH\S]XEJxFcIAAB

    We can see that this corresponds to the value set on the device:

      Router#show running-config | section username
      username admin password 6 S]iVZERdHTgVdfxKVQYOMXQRR^QHM^
      username cisco privilege 15 password 6 Y]i`\ZegLUiB[EMPYUAKhOBK]VRAAB
      username newuser password 6 xAb[PDCO[fQDJhDfMIciONMedifAAB
      username newuser2 password 6 XTXfOVUcYDZKd`FBH\S]XEJxFcIAAB

      Note that we do not have entries for `admin` or `cisco`, because
      these values were set directly on NSO and not on the device, we
      do not have a corresponding plaintext value and they are handled
      entirely as encrypted values.

    --- Auto-encrypting passwords in NSO

    To avoid having to pre-encrypt your passwords you can rebuild your NED in your OS
    command shell specifying an encrypted type for secrets using a command like:

    yourhost:~/cisco-ios-cli-x.y$ NEDCOM_SECRET_TYPE="tailf:aes-cfb-128-encrypted-string" make -C src/ clean all

    Or by adding the line `NED_EXTRA_BUILDFLAGS ?= NEDCOM_SECRET_TYPE=tailf:aes-cfb-128-encrypted-string`
    in top of the `Makefile` located in <cisco-ios-cli-x.y>/src directory.

    Doing this means that even if the input to a passwordis a plaintext string, NSO will always
    encrypt it, and you will never see plain text secrets in the device tree.

    If we reload our example with the new NED all of the secrets are now encrypted:

    admin@ncs# show running-config devices device dev-1 config username
    devices device dev-1
     config
       username admin password 6 $8$LkeTpiWvR2fm0P0DK0FXPg61LAOw5TACkB1y7FYvZIYYNE2CqMyQOwZ7uDeod7oR
       username cisco privilege 15 password 6 $8$0D4JYVCvfoLqu5u77OImrdzWuP4hp9HzcAXbQPAoQUmNNWjF0VoOPVeaPRfoRqrI
       username newuser password 0 $8$IpJZaKg3HZ+7JMdhmcVlbdw2P+htNdThDuYKdldDAqM=
       username newuser2 password 0 "$8$BVzY1FLE47Wum5WXokAVZ3UqaeJQt4s7ksGyiWKOLxGZIUrhp92KqBG4R2zINyMFl+L71TOk\naT8u3/l4L/p4Xg=="
     !
    !

    and if we create yet another user we get the desired result:

    admin@ncs(config)# devices device dev-1 config username newuser3 password 0 MY-AUTO-PASSWD
    admin@ncs(config-config)# commit dry-run outformat native
    native {
        device {
           name dev-1
           data username newuser3 password 0 $8$s3EN60QdR5flGa1cv0K3/50iHX4wBcRkjrhFaok7ALg=
    }

    admin@ncs(config-config)# commit
    Commit complete.
    admin@ncs(config-config)# end
    admin@ncs# show running-config devices device dev-1 config username newuser3
    devices device dev-1
     config
       username newuser3 password 0 $8$s3EN60QdR5flGa1cv0K3/50iHX4wBcRkjrhFaok7ALg=
     !
    !

    admin@ncs# show devices device dev-1 ned-settings secrets
    ID                                      ENCRYPTED                               REGEX
    ---------------------------------------------------------------------------------------
    ios:username(newuser)/password/secret   6 xAb[PDCO[fQDJhDfMIciONMedifAAB
    ios:username(newuser2)/password/secret  6 XTXfOVUcYDZKd`FBH\S]XEJxFcIAAB
    ios:username(newuser3)/password/secret  6 QFVNW\xxPc[gDGxFixccEQQWHA[DIHJhTAAB
