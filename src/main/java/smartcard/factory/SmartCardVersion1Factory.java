package smartcard.factory;

import smartcard.SmartCard;
import smartcard.SmartCardVersion1;
import javax.smartcardio.CardTerminal;
import java.util.HashMap;
import java.util.Map;

public class SmartCardVersion1Factory extends SmartCardFactory
{
    @Override
    public SmartCard create(CardTerminal reader) throws Exception
    {
        byte[] cardManager = new byte[]{/*AID hidden*/};
        byte[] mac = new byte[]{/*Key hidden*/};
        byte[] enc = new byte[]{/*Key hidden*/};
        byte[] dek = new byte[]{/*Key hidden*/};
        Map<String,byte[]> keySet = new HashMap<>();
        keySet.put("MAC", mac);
        keySet.put("ENC", enc);
        keySet.put("DEK", dek);
        return new SmartCardVersion1(reader, cardManager, keySet);
    }
}