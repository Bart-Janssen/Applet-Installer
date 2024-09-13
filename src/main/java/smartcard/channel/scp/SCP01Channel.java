package smartcard.channel.scp;

import smartcard.APDU;
import smartcard.channel.Channel;
import smartcard.channel.Crypto;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import java.util.Arrays;

public class SCP01Channel extends Channel
{
    public SCP01Channel(Crypto crypto)
    {
        super.crypto = crypto;
        super.overhead = 8;
        super.macChainValue = new byte[]{(byte)0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00};
    }

    @Override
    public CommandAPDU prepare(APDU command) throws Exception
    {
        return this.mac(command);
    }

    //GP Platform 2.3.1 chapter D secure Channel Protocol '01'
    private CommandAPDU mac(APDU command) throws Exception
    {
        command.setCLA((byte) (command.getCLA() | 0b0100));
        command.setLC((byte) (command.getLC() + 8));
        byte[] data = new byte[0];
        data = super.append(data, command.getCLA());
        data = super.append(data, command.getINS());
        data = super.append(data, command.getP1());
        data = super.append(data, command.getP2());
        data = super.append(data, command.getFullLC());
        data = super.append(data, command.getData());
        byte paddingByte = (byte) 0x80;
        data = super.append(data, paddingByte);
        int paddingSize = 8 - (data.length % 8);
        if (paddingSize != 8) data = super.append(data, new byte[paddingSize]);
        byte[] encryptedData = crypto.encryptTDES_CBC(data, sessionKeys.get("session-mac"), this.getMacChainValue());
        byte[] mac = Arrays.copyOfRange(encryptedData, encryptedData.length - 8, encryptedData.length);
        this.setMacChainValue(mac);
        command.addMac(mac);
        return command.create();
    }
}