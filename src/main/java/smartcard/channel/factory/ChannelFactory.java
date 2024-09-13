package smartcard.channel.factory;

import smartcard.channel.Channel;
import smartcard.channel.Crypto;

public abstract class ChannelFactory
{
    public abstract Channel create(Crypto crypto) throws Exception;
}