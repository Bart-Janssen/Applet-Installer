package smartcard.factory;

import smartcard.SmartCard;
import smartcard.SmartCardVersion3;
import javax.smartcardio.CardTerminal;
import java.util.HashMap;
import java.util.Map;

public class SmartCardVersion3Factory extends SmartCardFactory
{
    @Override
    public SmartCard create(CardTerminal reader) throws Exception
    {
        byte[] cardManager = new byte[]{/*AID hidden*/};
        byte[] key = new byte[]{/*Key hidden*/};
        Map<String,byte[]> keySet = new HashMap<>();
        keySet.put("KEY", key);
        return new SmartCardVersion3(reader, cardManager, keySet);
    }
}