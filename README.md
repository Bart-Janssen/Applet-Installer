# Applet installer
This repository is part of the 
'Post-Quantum Cryptography on smart cards' by Bart Janssen at the 
Open University, faculty of Management, Science and Technology. 
Master Software Engineering.

The applet installer is created as a tool to install applets 
with custom functionality onto smart cards.

### Support
A secure chanel can be opened to execute commands where a secure
channel is required. SCP01, SCP02 and SCP03 with MAC are supported.

### Functions
The custom functionality of this tool are:
- "cap" - This reads out a .cap file with a hard coded .cap file locations, that uses [CapFile](https://github.com/martinpaljak/capfile/blob/master/src/main/java/pro/javacard/CAPFile.java).
- "cm" - Selects the card manager
- "keystore" - Selects the [keystore applet](https://github.com/Bart-Janssen/Applets/tree/main/applet/src/keystore).
- "kyber" - Selects the [Kyber applet](https://github.com/Bart-Janssen/Applets/tree/main/applet/src/kyber).
- "ram" - Selects the [ram applet](https://github.com/Bart-Janssen/Applets/tree/main/applet/src/ram).
- "select \<applet>" - Selects the applet provided. Possible values are: cm, keystore, kyber and ram
- "apdu \<apdu>" - Allows custom APDU commands
- "secure" - Opens a secure channel based on SCP MAC
- "applets" - Shows applets on the smart card; (requires secure channel)
- "install \<applet>" - Installs an applet onto the smart card. Possible values are: keystore, kyber and ram; (requires secure channel)
- "uninstall \<applet>" - Uninstalls an applet from the smart card. Possible values are: keystore, kyber and ram; (requires secure channel)
- "update \<applet>" - Updates an applet by uninstalling and installing it. Possible values are: keystore, kyber and ram; (requires secure channel)

# Disclaimer
This tool and any of its functionality are written to only support 
smart cards dedicated to this project and no guarantee is given 
that it works on any smart card. The smart cards for this project
are private and details of the smart cards are kept hidden.

If someone wishes to use this code, the files 
'SmartCardVersion1Factory', 'SmartCardVersion2Factory' or 
'SmartCardVersion3Factory' should be edited for the smart card 
SCP version used, to match its card manager AID and SCP keys.