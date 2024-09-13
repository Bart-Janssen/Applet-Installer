package smartcard.channel.factory.scp;

import smartcard.channel.Channel;
import smartcard.channel.Crypto;
import smartcard.channel.factory.ChannelFactory;
import smartcard.channel.scp.SCP03Channel;

public class SCP03ChannelFactory extends ChannelFactory
{
    @Override
    public Channel create(Crypto crypto) throws Exception
    {
        return new SCP03Channel(crypto);
    }
}