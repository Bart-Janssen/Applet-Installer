package smartcard.factory;

import smartcard.SmartCard;
import javax.smartcardio.CardTerminal;

public abstract class SmartCardFactory
{
    abstract SmartCard create(CardTerminal reader) throws Exception;
}
