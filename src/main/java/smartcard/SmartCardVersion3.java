package smartcard;

import pro.javacard.CAPFile;
import smartcard.channel.Channel;
import smartcard.channel.ChannelType;
import smartcard.channel.factory.scp.ChannelFactoryPlain;
import smartcard.channel.factory.scp.SCP03ChannelFactory;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import java.io.File;
import java.nio.file.Files;
import java.util.*;

public class SmartCardVersion3 extends SmartCard
{
    private final byte[] keyDiversificationData = new byte[10];

    public SmartCardVersion3(CardTerminal reader, byte[] cardManager, Map<String, byte[]> keySet) throws Exception
    {
        super(reader, cardManager, keySet);
        super.supportedChannels.put(ChannelType.PLAIN, new ChannelFactoryPlain());
        super.supportedChannels.put(ChannelType.MAC, new SCP03ChannelFactory());
        super.channel = super.supportedChannels.get(ChannelType.PLAIN).create(super.crypto);
    }

    @Override
    public void test() throws Exception
    {

    }

    @Override
    public void openSecureChannel(ChannelType channelType) throws Exception
    {
        byte[] hostChallenge = super.crypto.getRandomBytes(8);
        int keyVersion = 0x01;
        CommandAPDU initUpdateCommand = new APDU(0x80,0x50, keyVersion, 0x00, hostChallenge, 0x20).create();
        ResponseAPDU initUpdateResponse = super.transmit(initUpdateCommand);
        System.out.print("Command:  "); this.print(initUpdateCommand.getBytes());
        System.out.print("Response: "); this.print(initUpdateResponse.getBytes());
        if (initUpdateResponse.getSW() != 0x9000) throw new RuntimeException("Init update failed!");

        this.channel = super.supportedChannels.get(ChannelType.PLAIN).create(super.crypto);

        System.arraycopy(initUpdateResponse.getBytes(), 0, this.keyDiversificationData, 0, 10);

        byte[] cardChallenge = new byte[8];
        System.arraycopy(initUpdateResponse.getBytes(), 13, cardChallenge, 0, 8);
        byte[] cardCryptogram = new byte[8];
        System.arraycopy(initUpdateResponse.getBytes(), 21, cardCryptogram, 0, 8);

        Map<String, byte[]> divertedKeys = this.diverseKeys(super.keySet.get("KEY"));
        this.keySet.put("MAC",divertedKeys.get("MAC"));
        this.keySet.put("ENC",divertedKeys.get("ENC"));
        this.keySet.put("DEK",divertedKeys.get("DEK"));

        Map<String, byte[]> sessionKeys = this.calculateSessionKeys(hostChallenge, cardChallenge);
        if (!Arrays.equals(cardCryptogram, sessionKeys.get("CardCryptogram")))
        {
            System.out.println("Wrong keys");
            return;
        }
        Channel macChannel = super.supportedChannels.get(ChannelType.MAC).create(super.crypto);
        macChannel.setSessionKeys(sessionKeys);

        CommandAPDU extAuthCommand = macChannel.prepare(new APDU(0x80,0x82,0x01,0x00,sessionKeys.get("HostCryptogram")));
        ResponseAPDU extAuthResponse = super.transmit(extAuthCommand);
        System.out.print("Command:  "); this.print(extAuthCommand.getBytes());
        System.out.print("Response: "); this.print(extAuthResponse.getBytes());

        if (extAuthResponse.getSW() != 0x9000)
        {
            System.out.println("External authenticate failed.");
            return;
        }
        super.channel = macChannel;
    }

    private Map<String,byte[]> diverseKeys(byte[] key) throws Exception
    {
        Map<String, byte[]> keyset = new HashMap<>();
        byte[] enc = new byte[0];
        byte[] enc1 = new byte[]{(byte)0x01, (byte)0x00,(byte)0x00,(byte)0x00, (byte)0x01, (byte)0x00};
        enc1 = super.append(enc1, this.keyDiversificationData);
        byte[] enc2 = new byte[]{(byte)0x02, (byte)0x00,(byte)0x00,(byte)0x00, (byte)0x01, (byte)0x00};
        enc2 = super.append(enc2, this.keyDiversificationData);
        enc = super.append(enc, super.crypto.cmac(enc1, key));
        enc = super.append(enc, super.crypto.cmac(enc2, key));
        keyset.put("ENC",enc);

        byte[] mac = new byte[0];
        byte[] mac1 = new byte[]{(byte)0x01, (byte)0x00,(byte)0x00,(byte)0x00, (byte)0x02, (byte)0x00};
        mac1 = super.append(mac1, this.keyDiversificationData);
        byte[] mac2 = new byte[]{(byte)0x02, (byte)0x00,(byte)0x00,(byte)0x00, (byte)0x02, (byte)0x00};
        mac2 = super.append(mac2, this.keyDiversificationData);
        mac = super.append(mac, super.crypto.cmac(mac1, key));
        mac = super.append(mac, super.crypto.cmac(mac2, key));
        keyset.put("MAC",mac);

        byte[] dek = new byte[0];
        byte[] dek1 = new byte[]{(byte)0x01, (byte)0x00,(byte)0x00,(byte)0x00, (byte)0x03, (byte)0x00};
        dek1 = super.append(dek1, this.keyDiversificationData);
        byte[] dek2 = new byte[]{(byte)0x02, (byte)0x00,(byte)0x00,(byte)0x00, (byte)0x03, (byte)0x00};
        dek2 = super.append(dek2, this.keyDiversificationData);
        dek = super.append(dek, super.crypto.cmac(dek1, key));
        dek = super.append(dek, super.crypto.cmac(dek2, key));
        keyset.put("DEK",dek);

        return keyset;
    }

    private Map<String,byte[]> calculateSessionKeys(byte[] hostChallenge, byte[] cardChallenge) throws Exception
    {
        byte[] encSessionKey = new byte[0];
        byte[] encSessionKey1 = new byte[]{(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00 ,(byte)0x04, (byte)0x00,(byte)0x01,(byte)0x00, (byte)0x01};
        encSessionKey1 = super.append(encSessionKey1, hostChallenge);
        encSessionKey1 = super.append(encSessionKey1, cardChallenge);
        byte[] encSessionKey2 = new byte[]{(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00 ,(byte)0x04, (byte)0x00,(byte)0x01,(byte)0x00, (byte)0x02};
        encSessionKey2 = super.append(encSessionKey2, hostChallenge);
        encSessionKey2 = super.append(encSessionKey2, cardChallenge);
        encSessionKey = super.append(encSessionKey, super.crypto.cmac(encSessionKey1, super.keySet.get("ENC")));
        encSessionKey = super.append(encSessionKey, super.crypto.cmac(encSessionKey2, super.keySet.get("ENC")));

        byte[] macSessionKey = new byte[0];
        byte[] macSessionKey1 = new byte[]{(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00 ,(byte)0x06, (byte)0x00,(byte)0x01,(byte)0x00, (byte)0x01};
        macSessionKey1 = super.append(macSessionKey1, hostChallenge);
        macSessionKey1 = super.append(macSessionKey1, cardChallenge);
        byte[] macSessionKey2 = new byte[]{(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00 ,(byte)0x06, (byte)0x00,(byte)0x01,(byte)0x00, (byte)0x02};
        macSessionKey2 = super.append(macSessionKey2, hostChallenge);
        macSessionKey2 = super.append(macSessionKey2, cardChallenge);
        macSessionKey = super.append(macSessionKey, super.crypto.cmac(macSessionKey1, super.keySet.get("MAC")));
        macSessionKey = super.append(macSessionKey, super.crypto.cmac(macSessionKey2, super.keySet.get("MAC")));

        byte[] cardCryptogramHeader = new byte[]{(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00, (byte)0x00, (byte)0x00,(byte)0x00,(byte)0x40, (byte)0x01};
        cardCryptogramHeader = super.append(cardCryptogramHeader, hostChallenge);
        cardCryptogramHeader = super.append(cardCryptogramHeader, cardChallenge);
        byte[] cmacCard = super.crypto.cmac(cardCryptogramHeader, macSessionKey);
        byte[] cardCryptogram = new byte[8];
        System.arraycopy(cmacCard, 0, cardCryptogram, 0, 8);

        byte[] hostCryptogramHeader = new byte[]{(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00, (byte)0x01, (byte)0x00,(byte)0x00,(byte)0x40, (byte)0x01};
        hostCryptogramHeader = super.append(hostCryptogramHeader, hostChallenge);
        hostCryptogramHeader = super.append(hostCryptogramHeader, cardChallenge);
        byte[] cmacHost = super.crypto.cmac(hostCryptogramHeader, macSessionKey);
        byte[] hostCryptogram = new byte[8];
        System.arraycopy(cmacHost, 0, hostCryptogram, 0, 8);

        Map<String, byte[]> sessionKeys = new HashMap<>();
        sessionKeys.put("session-enc", encSessionKey);
        sessionKeys.put("session-mac", macSessionKey);
        sessionKeys.put("CardCryptogram", cardCryptogram);
        sessionKeys.put("HostCryptogram", hostCryptogram);
        return sessionKeys;
    }

    @Override
    public void installApplet(String applet) throws Exception
    {
        int maxDataWithoutMac = 0xFF-this.channel.getOverhead();
        List<byte[]> chunks = new ArrayList<>();
        CAPFile capFile = CAPFile.fromStream(Files.newInputStream(new File(applet).toPath()));
        byte[] data = new byte[0];
        byte[] code = capFile.getCode();
        byte[] header = new byte[]{(byte)0xC4, (byte)0x82, (byte)((code.length >> 8) & 0xFF), (byte)(code.length & 0xFF)};
        data = this.append(data, header);
        data = this.append(data, code);
        int fullChunks = (data.length) / maxDataWithoutMac;
        int lastChunk = (data.length) % maxDataWithoutMac;
        for (int i = 0; i < fullChunks*maxDataWithoutMac; i+=maxDataWithoutMac)
        {
            byte[] chunk = new byte[maxDataWithoutMac];
            System.arraycopy(data, i, chunk, 0, maxDataWithoutMac);
            chunks.add(chunk);
        }
        if (lastChunk > 0)
        {
            byte[] chunk = new byte[lastChunk];
            System.arraycopy(data, (data.length-lastChunk), chunk, 0, lastChunk);
            chunks.add(chunk);
        }

        //GP Platform 2.3.1 chapter 11.8.2.3.1
        byte[] installData = new byte[0];
        installData = this.append(installData, (byte)capFile.getPackageAID().getBytes().length);
        installData = this.append(installData, capFile.getPackageAID().getBytes());
        installData = this.append(installData, (byte)super.cardManager.length);
        installData = this.append(installData, super.cardManager);
        installData = this.append(installData, (byte)0x00);//Length of block hash
        installData = this.append(installData, (byte)0x00);//Length of parameters field
        installData = this.append(installData, (byte)0x00);//Length of load token

        CommandAPDU installCommand = this.channel.prepare(new APDU(0x80,0xE6,0x02,0x00,installData));
        ResponseAPDU installResponse = this.transmit(installCommand);
        System.out.print("Command:  "); this.print(installCommand.getBytes());
        System.out.print("Response: "); this.print(installResponse.getBytes());
        for (int i = 0; i < chunks.size(); i++)
        {
            boolean last = (i == (chunks.size() - 1));
            CommandAPDU loadCommand = this.channel.prepare(new APDU(0x80,0xE8,last?0x80:0x00,i,chunks.get(i)));
            ResponseAPDU loadResponse = this.transmit(loadCommand);
            System.out.print("Command:  "); this.print(loadCommand.getBytes());
            System.out.print("Response: "); this.print(loadResponse.getBytes());
        }

        //GP Platform 2.3.1 chapter 11.5.2.3.2
        byte[] installAndSelectData = new byte[0];
        installAndSelectData = this.append(installAndSelectData, (byte)capFile.getPackageAID().getBytes().length);
        installAndSelectData = this.append(installAndSelectData, capFile.getPackageAID().getBytes());//package aid
        installAndSelectData = this.append(installAndSelectData, (byte)capFile.getAppletAIDs().get(0).getBytes().length);
        installAndSelectData = this.append(installAndSelectData, capFile.getAppletAIDs().get(0).getBytes());//module aid
        installAndSelectData = this.append(installAndSelectData, (byte)capFile.getAppletAIDs().get(0).getBytes().length);
        installAndSelectData = this.append(installAndSelectData, capFile.getAppletAIDs().get(0).getBytes());//application aid
        installAndSelectData = this.append(installAndSelectData, (byte)0x01);//Length of privileges field
        installAndSelectData = this.append(installAndSelectData, (byte)0x00);//Privileges
        installAndSelectData = this.append(installAndSelectData, (byte)0x02);//Length of install parameters field
        installAndSelectData = this.append(installAndSelectData, (byte)0xC9);//Application specific: chapter 11.5.2.3.7 table 11-49
        installAndSelectData = this.append(installAndSelectData, (byte)0x00);//Application specific parameters
        installAndSelectData = this.append(installAndSelectData, (byte)0x00);//Length of install token

        CommandAPDU installAndMakeSelectableCommand = this.channel.prepare(new APDU(0x80,0xE6,0x0C,0x00,installAndSelectData));
        ResponseAPDU installAndMakeSelectableResponse = this.transmit(installAndMakeSelectableCommand);
        System.out.print("Command:  "); this.print(installAndMakeSelectableCommand.getBytes());
        System.out.print("Response: "); this.print(installAndMakeSelectableResponse.getBytes());
    }
}