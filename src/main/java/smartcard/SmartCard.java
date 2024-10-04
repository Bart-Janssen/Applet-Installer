package smartcard;

import applet.Applet;
import applet.Type;
import pro.javacard.CAPFile;
import smartcard.channel.Channel;
import smartcard.channel.ChannelType;
import smartcard.channel.Crypto;
import smartcard.channel.factory.ChannelFactory;
import javax.smartcardio.*;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class SmartCard
{
    private final Card card;
    protected final byte[] cardManager;
    private final byte[] kyberKeyStoreAppletAID = new byte[]{(byte)0x50,(byte)0x51,(byte)0x43,(byte)0x20,(byte)0x4B,(byte)0x65,(byte)0x79,(byte)0x73,(byte)0x74,(byte)0x6F,(byte)0x72,(byte)0x65};
    private final byte[] kyberApplet512AID = new byte[]{(byte)0x4B,(byte)0x79,(byte)0x62,(byte)0x65,(byte)0x72};
    private final byte[] RAMAppletAID = new byte[]{(byte)0x52,(byte)0x41,(byte)0x4D,(byte)0x74,(byte)0x65,(byte)0x73,(byte)0x74};
    private final byte[] testAppletAID = new byte[]{(byte)0x74,(byte)0x65,(byte)0x73,(byte)0x74,(byte)0x00};
    protected final Map<String, byte[]> keySet;
    protected final Crypto crypto;
    protected Channel channel;
    protected final Map<ChannelType, ChannelFactory> supportedChannels = new HashMap<>();

    public abstract void openSecureChannel(ChannelType channelType) throws Exception;
    public abstract void installApplet(String applet) throws Exception;

    public SmartCard(CardTerminal reader, byte[] cardManager, Map<String, byte[]> keySet) throws Exception
    {
        this.crypto = new Crypto();
        this.keySet = keySet;
        this.cardManager = cardManager;
        this.card = reader.connect("T=1");
        System.out.println("Card: " + this.card);
    }

    protected ResponseAPDU transmit(CommandAPDU apdu) throws Exception
    {
        CardChannel channel = this.card.getBasicChannel();
        ResponseAPDU response = channel.transmit(apdu);
        byte[] responseData = response.getBytes();
        return new ResponseAPDU(responseData);
    }

    //Global Platform chapter 11.9
    public void selectCardManager() throws Exception
    {
        this.channel = this.supportedChannels.get(ChannelType.PLAIN).create(this.crypto);
        CommandAPDU command = new APDU(0x00,0xA4,0x04,0x00, this.cardManager, 0x00).create();
        ResponseAPDU response = this.transmit(command);
        System.out.print("Command:  "); this.print(command.getBytes());
        System.out.print("Response: "); this.print(response.getBytes());
    }

    //Global Platform chapter 11.9
    public void selectKyberKeyStoreApplet() throws Exception
    {
        this.channel = this.supportedChannels.get(ChannelType.PLAIN).create(this.crypto);
        CommandAPDU command = new APDU(0x00,0xA4,0x04,0x00, this.kyberKeyStoreAppletAID, 0x00).create();
        ResponseAPDU response = this.transmit(command);
        System.out.print("Command:  "); this.print(command.getBytes());
        System.out.print("Response: "); this.print(response.getBytes());
    }

    //Global Platform chapter 11.9
    public void selectKyber512Applet() throws Exception
    {
        this.channel = this.supportedChannels.get(ChannelType.PLAIN).create(this.crypto);
        CommandAPDU command = new APDU(0x00,0xA4,0x04,0x00, this.kyberApplet512AID, 0x00).create();
        ResponseAPDU response = this.transmit(command);
        System.out.print("Command:  "); this.print(command.getBytes());
        System.out.print("Response: "); this.print(response.getBytes());
    }

    //Global Platform chapter 11.9
    public void selectRAMApplet() throws Exception
    {
        this.channel = this.supportedChannels.get(ChannelType.PLAIN).create(this.crypto);
        CommandAPDU command = new APDU(0x00,0xA4,0x04,0x00, this.RAMAppletAID).create();
        ResponseAPDU response = this.transmit(command);
        System.out.print("Command:  "); this.print(command.getBytes());
        System.out.print("Response: "); this.print(response.getBytes());
    }

    public void selectTestApplet() throws Exception
    {
        this.channel = this.supportedChannels.get(ChannelType.PLAIN).create(this.crypto);
        CommandAPDU command = new APDU(0x00,0xA4,0x04,0x00, this.testAppletAID).create();
        ResponseAPDU response = this.transmit(command);
        System.out.print("Command:  "); this.print(command.getBytes());
        System.out.print("Response: "); this.print(response.getBytes());
    }

    //Global Platform chapter 11.4
    public List<Applet> getISD() throws Exception
    {
        byte[] data = new byte[]{(byte)0x4F,(byte)0x00};
        CommandAPDU command = this.channel.prepare(new APDU(0x80,0xF2,0x80,0x02,data, 0x00));
        ResponseAPDU response = this.transmit(command);
        System.out.print("Command:  "); this.print(command.getBytes());
        System.out.print("Response: "); this.print(response.getBytes());
        return this.parseApplets(response.getData(), Type.ISD);
    }

    //Global Platform chapter 11.4
    public List<Applet> getApplets() throws Exception
    {
        byte[] data = new byte[]{(byte)0x4F,(byte)0x00};
        CommandAPDU command = this.channel.prepare(new APDU(0x80,0xF2,0x40,0x02,data, 0x00));
        ResponseAPDU response = this.transmit(command);
        System.out.print("Command:  "); this.print(command.getBytes());
        System.out.print("Response: "); this.print(response.getBytes());
        return this.parseApplets(response.getData(), Type.APPLET);
    }

    //Global Platform chapter 11.4
    private List<Applet> parseApplets(byte[] data, Type type)
    {
        List<Applet> applets = new ArrayList<>();
        for (int i = 0; i < data.length;i++)
        {
            if (data[i] == (byte)0xE3)
            {
                byte[] applet = new byte[data[i+1]];
                System.arraycopy(data, i+2 , applet, 0, data[i+1]);
                applets.add(new Applet(applet, type));
                i+=data[i+1];
            }
        }
        return applets;
    }

    //Global Platform chapter 11.4
    public List<Applet> getPackages() throws Exception
    {
        byte[] data = new byte[]{(byte)0x4F,0x00};
        CommandAPDU command = this.channel.prepare(new APDU(0x80,0xF2,0x10,0x02,data, 0x00));
        ResponseAPDU response = this.transmit(command);
        System.out.print("Command:  "); this.print(command.getBytes());
        System.out.print("Response: "); this.print(response.getBytes());
        return this.parseApplets(response.getData(), Type.PACKAGE);
    }

    protected byte[] append(byte[] original, byte... toAppend)
    {
        byte[] result = new byte[original.length + toAppend.length];
        System.arraycopy(original, 0, result, 0, original.length);
        System.arraycopy(toAppend, 0, result, original.length, toAppend.length);
        return result;
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

    //Global Platform chapter 11.2.2.3.1 table 11-23
    public void uninstallApplet(String applet) throws Exception
    {
        CAPFile capFile = CAPFile.fromStream(Files.newInputStream(new File(applet).toPath()));
        byte[] aid = capFile.getPackageAID().getBytes();
        byte[] uninstallData = new byte[0];
        uninstallData = this.append(uninstallData, (byte)0x4F);//Tag: AID to delete
        uninstallData = this.append(uninstallData, (byte)aid.length);
        uninstallData = this.append(uninstallData, aid);

        CommandAPDU installAndMakeSelectableCommand = this.channel.prepare(new APDU(0x80,0xE4,0x00,0x80,uninstallData));
        ResponseAPDU installAndMakeSelectableResponse = this.transmit(installAndMakeSelectableCommand);
        System.out.print("Command:  "); this.print(installAndMakeSelectableCommand.getBytes());
        System.out.print("Response: "); this.print(installAndMakeSelectableResponse.getBytes());
    }


    public void customAPDU(String apduString) throws Exception
    {
        byte[] apdu = this.hexStringToByteArray(apduString);
        if (apdu.length < 4)
        {
            System.out.println("Need apdu header at least");
            return;
        }
        byte[] data = new byte[0];
        data = this.append(data, this.hexStringToByteArray(apduString.substring(8)));
        long startTime = System.nanoTime();

        if (data.length > 0)
        {
            CommandAPDU installAndMakeSelectableCommand = this.channel.prepare(new APDU(apdu[0],apdu[1],apdu[2],apdu[3],data,0x00));
            ResponseAPDU installAndMakeSelectableResponse = this.transmit(installAndMakeSelectableCommand);
            System.out.print("Command:  "); this.print(installAndMakeSelectableCommand.getBytes());
            System.out.print("Response: "); this.print(installAndMakeSelectableResponse.getBytes());
        }
        else
        {
            CommandAPDU installAndMakeSelectableCommand = this.channel.prepare(new APDU(apdu[0],apdu[1],apdu[2],apdu[3], 0x00));
            ResponseAPDU installAndMakeSelectableResponse = this.transmit(installAndMakeSelectableCommand);
            System.out.print("Command:  "); this.print(installAndMakeSelectableCommand.getBytes());
            System.out.print("Response: "); this.print(installAndMakeSelectableResponse.getBytes());
        }
        long duration = System.nanoTime() - startTime;
        System.out.println("Execution time (in milliseconds): " + (duration / 1_000_000));
    }

    public byte[] hexStringToByteArray(String s)
    {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2)
        {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}