package smartcard;

import javax.smartcardio.CommandAPDU;

public class APDU
{
    private byte cla;
    private byte ins;
    private byte p1;
    private byte p2;
    private byte lc;
    private byte le;
    private boolean leAbsent;
    private boolean lcAbsent;

    private byte[] data;

    public APDU(int cla, int ins, int p1, int p2, byte[] data)
    {
        this.cla = (byte)cla;
        this.ins = (byte)ins;
        this.p1 = (byte)p1;
        this.p2 = (byte)p2;
        this.lc = (byte)data.length;
        lcAbsent = false;
        this.data = data;
        this.leAbsent = true;
    }

    public APDU(int cla, int ins, int p1, int p2, int le)
    {
        this.cla = (byte)cla;
        this.ins = (byte)ins;
        this.p1 = (byte)p1;
        this.p2 = (byte)p2;
        this.le = (byte)le;
        this.data = new byte[0];
        this.leAbsent = false;
        this.lcAbsent = true;
    }

    public APDU(int cla, int ins, int p1, int p2, byte[] data, int le)
    {
        this.cla = (byte)cla;
        this.ins = (byte)ins;
        this.p1 = (byte)p1;
        this.p2 = (byte)p2;
        this.lc = (byte)data.length;
        this.data = data;
        this.le = (byte)le;
        this.leAbsent = false;
    }

    public CommandAPDU create()
    {
        byte[] apdu = new byte[]
        {
            this.cla, this.ins, this.p1, this.p2,
        };
        if (!this.lcAbsent) apdu = this.appendByteToArray(apdu, this.lc);
        apdu = this.appendByteArrays(apdu, this.data);
        if (!this.leAbsent) apdu = this.appendByteToArray(apdu, this.le);
        return new CommandAPDU(apdu);
    }

    private byte[] appendByteArrays(byte[] array1, byte[] array2)
    {
        byte[] result = new byte[array1.length + array2.length];
        System.arraycopy(array1, 0, result, 0, array1.length);
        System.arraycopy(array2, 0, result, array1.length, array2.length);
        return result;
    }

    private byte[] appendByteToArray(byte[] array1, byte b)
    {
        byte[] array2 = new byte[] {b};
        byte[] result = new byte[array1.length + 1];
        System.arraycopy(array1, 0, result, 0, array1.length);
        System.arraycopy(array2, 0, result, array1.length, array2.length);
        return result;
    }

    public void setCLA(byte cla)
    {
        this.cla = cla;
    }

    public byte getCLA()
    {
        return this.cla;
    }

    public void setLC(byte lc)
    {
        this.lc = lc;
        this.lcAbsent = false;
    }

    public byte getLC()
    {
        return this.lc;
    }

    public byte getINS()
    {
        return this.ins;
    }

    public byte getP1()
    {
        return this.p1;
    }

    public byte getP2()
    {
        return this.p2;
    }

    public byte getFullLC()
    {
        return this.lc;
    }

    public byte[] getData()
    {
        return this.data;
    }

    public void addMac(byte[] mac)
    {
        byte[] data = new byte[this.data.length + mac.length];
        System.arraycopy(this.data, 0, data, 0, this.data.length);
        System.arraycopy(mac, 0, data, this.data.length, mac.length);
        this.data = data;
    }
}