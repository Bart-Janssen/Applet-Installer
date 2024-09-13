package smartcard.channel;

import smartcard.APDU;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import java.util.Map;

public abstract class Channel
{
    protected Crypto crypto;
    protected int overhead;

    protected Map<String, byte[]> sessionKeys;

    protected byte[] macChainValue;

    public abstract CommandAPDU prepare(APDU command) throws Exception;

    public void setSessionKeys(Map<String,byte[]> sessionKeys)
    {
        this.sessionKeys = sessionKeys;
    }

    public void setMacChainValue(byte[] macChainValue)
    {
        this.macChainValue = macChainValue;
    }

    public byte[] getMacChainValue()
    {
        return this.macChainValue;
    }

    protected void print(byte[] data)
    {
        StringBuilder sb = new StringBuilder();
        for (byte b : data)
        {
            sb.append(String.format("%02X ", b));
        }
        System.out.print(sb);
        System.out.println();
    }

    protected byte[] append(byte[] original, byte... toAppend)
    {
        byte[] result = new byte[original.length + toAppend.length];
        System.arraycopy(original, 0, result, 0, original.length);
        System.arraycopy(toAppend, 0, result, original.length, toAppend.length);
        return result;
    }

    public int getOverhead()
    {
        return this.overhead;
    }
}