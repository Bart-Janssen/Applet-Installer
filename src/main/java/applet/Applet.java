package applet;

public class Applet
{
    private byte[] aid = new byte[0];
    private LifeCycle lifeCycle;
    private byte[] privileges = new byte[0];
    private byte[] version = new byte[0];
    private Type type;

    public Applet(byte[] applet, Type type)
    {
        this.type = type;
        byte lifeCycle = 0x00;
        for (int i = 0; i < applet.length; i++)
        {
            if (applet[i] == (byte)0x4F)
            {
                this.aid = new byte[(byte)applet[i+1]];
                System.arraycopy(applet, i+2 , this.aid, 0, applet[i+1]);
                i+=applet[i+1]+1;
            }
            if (applet[i] == (byte)0x9F && applet[i+1] == (byte)0x70)
            {
                lifeCycle = applet[i+3];
                i+=4;
            }
            if (applet[i] == (byte)0xCE)
            {
                this.version = new byte[(byte)applet[i+1]];
                System.arraycopy(applet, i+2 , this.version, 0, applet[i+1]);
                i+=4;
            }
            if (applet[i] == (byte)0xC5)
            {
                this.privileges = new byte[(byte)applet[i+1]];
                if (type == Type.APPLET && (applet[i+2] & 0b10000000) == 0b10000000)
                {
                    this.type = Type.SSD;
                }
                for (byte b = 0; b < applet[i+1]; b++)
                {
                    this.privileges[b] = applet[i+2+b];
                }
            }
        }
        this.setLifeCycle(lifeCycle);
    }

    private void setLifeCycle(byte lifeCycle)
    {
        //GP Platform 2.3.1 chapter 11.1.1 table 11-6
        if (this.type == Type.ISD)
        {
            if (lifeCycle == (byte)0b00000001) this.lifeCycle = LifeCycle.OP_READY;
            else if (lifeCycle == (byte)0b00000111) this.lifeCycle = LifeCycle.INITIALIZED;
            else if (lifeCycle == (byte)0b00001111) this.lifeCycle = LifeCycle.SECURED;
            else if (lifeCycle == (byte)0b01111111) this.lifeCycle = LifeCycle.LOCKED;
            else if (lifeCycle == (byte)0b11111111) this.lifeCycle = LifeCycle.TERMINATED;
            else this.lifeCycle = LifeCycle.UNKNOWN;
        }
        //GP Platform 2.3.1 chapter 11.1.1 table 11-5
        if (this.type == Type.SSD)
        {
            if (lifeCycle == (byte)0b00000011) this.lifeCycle = LifeCycle.INSTALLED;
            else if (lifeCycle == (byte)0b00000111) this.lifeCycle = LifeCycle.SELECTABLE;
            else if (lifeCycle == (byte)0b00001111) this.lifeCycle = LifeCycle.PERSONALIZED;
            else if (lifeCycle == (byte)0b10000011) this.lifeCycle = LifeCycle.LOCKED;
            else this.lifeCycle = LifeCycle.UNKNOWN;
        }
        //GP Platform 2.3.1 chapter 11.1.1 table 11-4
        if (this.type == Type.APPLET)
        {
            if (lifeCycle == (byte)0b00000011) this.lifeCycle = LifeCycle.INSTALLED;
            else if (lifeCycle == (byte)0b00000111) this.lifeCycle = LifeCycle.SELECTABLE;
            else if (lifeCycle == (byte)0b10000011) this.lifeCycle = LifeCycle.LOCKED;
            else this.lifeCycle = LifeCycle.APPLICATION_SPECIFIC;
        }
        //GP Platform 2.3.1 chapter 11.1.1 table 11-3
        if (this.type == Type.PACKAGE)
        {
            if (lifeCycle == (byte)0b00000001) this.lifeCycle = LifeCycle.LOADED;
            else this.lifeCycle = LifeCycle.UNKNOWN;
        }
    }

    public LifeCycle getLifeCycle()
    {
        return this.lifeCycle;
    }

    public byte[] getVersion()
    {
        return this.version;
    }

    public byte[] getAID()
    {
        return this.aid;
    }

    public byte[] getPrivileges()
    {
        return this.privileges;
    }

    public Type getType()
    {
        return this.type;
    }

    public String getPrivilegesString()
    {
        StringBuilder privileges = new StringBuilder();
        for (int i = 0; i < this.getPrivileges().length; i++)
        {
            privileges.append(this.getPrivilegesString(i));
        }
        return privileges.toString().isEmpty() ? "" : privileges.substring(0, privileges.toString().lastIndexOf(','));
    }

    //GP Platform 2.3.1 chapter 11.1.2
    private String getPrivilegesString(int index)
    {
        String privileges = "";
        //GP Platform 2.3.1 chapter 11.1.2 table 11-7
        if (index == 0) privileges =
                (((this.privileges[index] & 0b10000000) == 0b10000000) ? "Security Domain, " : "") +
                (((this.privileges[index] & 0b11000000) == 0b11000000) ? "DAP Verification, " : "") +
                (((this.privileges[index] & 0b10100000) == 0b10100000) ? "Delegated Management, " : "") +
                (((this.privileges[index] & 0b00010000) == 0b00010000) ? "Card Lock, " : "") +
                (((this.privileges[index] & 0b00001000) == 0b00001000) ? "Card Terminate, " : "") +
                (((this.privileges[index] & 0b00000100) == 0b00000100) ? "Card Reset, " : "") +
                (((this.privileges[index] & 0b00000010) == 0b00000010) ? "CVM Management, " : "") +
                (((this.privileges[index] & 0b11000001) == 0b11000001) ? "Mandated DAP Verification, " : "");

        //GP Platform 2.3.1 chapter 11.1.2 table 11-8
        if (index == 1) privileges =
                (((this.privileges[index] & 0b10000000) == 0b10000000) ? "Trusted Path, " : "") +
                (((this.privileges[index] & 0b01000000) == 0b01000000) ? "Authorized Management, " : "") +
                (((this.privileges[index] & 0b00100000) == 0b00100000) ? "Token Management, " : "") +
                (((this.privileges[index] & 0b00010000) == 0b00010000) ? "Global Delete, " : "") +
                (((this.privileges[index] & 0b00001000) == 0b00001000) ? "Global Lock, " : "") +
                (((this.privileges[index] & 0b00000100) == 0b00000100) ? "Global Registry, " : "") +
                (((this.privileges[index] & 0b00000010) == 0b00000010) ? "Final Application, " : "") +
                (((this.privileges[index] & 0b00000001) == 0b00000001) ? "Global Service, " : "");

        //GP Platform 2.3.1 chapter 11.1.2 table 11-9
        if (index == 2) privileges =
                (((this.privileges[index] & 0b10000000) == 0b10000000) ? "Receipt Generation, " : "") +
                (((this.privileges[index] & 0b01000000) == 0b01000000) ? "Ciphered Load File Data Block, " : "") +
                (((this.privileges[index] & 0b00100000) == 0b00100000) ? "Contactless Activation, " : "") +
                (((this.privileges[index] & 0b00010000) == 0b00010000) ? "Contactless Self-Activation, " : "") +
                (((this.privileges[index] & 0b00001111) == 0b00001111) ? "RFU " : "");

        return privileges;
    }
}