package smartcard.channel.factory.scp;

import smartcard.channel.Channel;
import smartcard.channel.Crypto;
import smartcard.channel.factory.ChannelFactory;
import smartcard.channel.scp.PlainChannel;

public class ChannelFactoryPlain extends ChannelFactory
{
    @Override
    public Channel create(Crypto crypto) throws Exception
    {
        return new PlainChannel();
    }
}