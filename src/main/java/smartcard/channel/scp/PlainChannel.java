package smartcard.channel.scp;

import smartcard.APDU;
import smartcard.channel.Channel;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

public class PlainChannel extends Channel
{
    @Override
    public CommandAPDU prepare(APDU command)
    {
        return command.create();
    }
}