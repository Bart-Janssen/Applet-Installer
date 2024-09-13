package smartcard.channel.scp;

import smartcard.APDU;
import smartcard.channel.Channel;
import smartcard.channel.Crypto;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

public class SCP03Channel extends Channel
{
    public SCP03Channel(Crypto crypto)
    {
        super.crypto = crypto;
        super.overhead = 8;
        super.macChainValue = new byte[]{(byte)0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00};
    }

    @Override
    public CommandAPDU prepare(APDU command) throws Exception
    {
        return this.mac(command);
    }

    //GlobalPlatform secure Channel Protocol '03'
    private CommandAPDU mac(APDU command) throws Exception
    {
        command.setCLA((byte) (command.getCLA() | 0b0100));
        command.setLC((byte) (command.getLC() + 8));
        byte[] data = new byte[0];
        data = super.append(data, this.getMacChainValue());
        data = super.append(data, command.getCLA());
        data = super.append(data, command.getINS());
        data = super.append(data, command.getP1());
        data = super.append(data, command.getP2());
        data = super.append(data, command.getFullLC());
        data = super.append(data, command.getData());
        this.setMacChainValue(super.crypto.cmac(data, this.sessionKeys.get("session-mac")));
        byte[] mac = new byte[8];
        System.arraycopy(this.getMacChainValue(), 0, mac, 0, 8);
        command.addMac(mac);
        return command.create();
    }
}