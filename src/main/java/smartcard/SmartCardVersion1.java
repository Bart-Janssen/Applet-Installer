package smartcard;

import pro.javacard.CAPFile;
import smartcard.channel.Channel;
import smartcard.channel.ChannelType;
import smartcard.channel.factory.scp.ChannelFactoryPlain;
import smartcard.channel.factory.scp.SCP01ChannelFactory;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import java.io.File;
import java.nio.file.Files;
import java.util.*;

public class SmartCardVersion1 extends SmartCard
{
    public SmartCardVersion1(CardTerminal reader, byte[] cardManager, Map<String, byte[]> keySet) throws Exception
    {
        super(reader, cardManager, keySet);
        super.supportedChannels.put(ChannelType.PLAIN, new ChannelFactoryPlain());
        super.supportedChannels.put(ChannelType.MAC, new SCP01ChannelFactory());
        super.channel = super.supportedChannels.get(ChannelType.PLAIN).create(super.crypto);
    }

    @Override
    public void test() throws Exception
    {
        CommandAPDU initUpdateCommand = new APDU(0x80,0xB4,0x00,0x10,0x00).create();
        ResponseAPDU initUpdateResponse = super.transmit(initUpdateCommand);
        System.out.print("Command:  "); this.print(initUpdateCommand.getBytes());
        System.out.print("Response: "); this.print(initUpdateResponse.getBytes());
    }

    @Override
    public void installApplet(String applet) throws Exception
    {
        int maxDataWithoutMac = 100-this.channel.getOverhead();
        List<byte[]> chunks = new ArrayList<>();
        CAPFile capFile = CAPFile.fromStream(Files.newInputStream(new File(applet).toPath()));
        byte[] code = capFile.getCode();
        byte[] header = new byte[]{(byte)0xC4, (byte)0x82, (byte)((code.length >> 8) & 0xFF), (byte)(code.length & 0xFF)};
        code = this.append(header, code);
        int fullChunks = (code.length) / maxDataWithoutMac;
        int lastChunk = (code.length) % maxDataWithoutMac;
        for (int i = 0; i < fullChunks*maxDataWithoutMac; i+=maxDataWithoutMac)
        {
            byte[] chunk = new byte[maxDataWithoutMac];
            System.arraycopy(code, i, chunk, 0, maxDataWithoutMac);
            chunks.add(chunk);
        }
        if (lastChunk > 0)
        {
            byte[] chunk = new byte[lastChunk];
            System.arraycopy(code, (code.length-lastChunk), chunk, 0, lastChunk);
            chunks.add(chunk);
        }

        //GP Platform 2.3.1 chapter 9.5.2.3.1
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

        //GP Platform 2.3.1 chapter 9.5.2.3.2
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

    //GP Platform 2.3.1 chapter D secure Channel Protocol '01'
    @Override
    public void openSecureChannel(ChannelType channelType) throws Exception
    {
        byte[] hostChallenge = super.crypto.getRandomBytes(8);
        int keyVersion = 0x00;
        CommandAPDU initUpdateCommand = new APDU(0x80,0x50, keyVersion, 0x01, hostChallenge).create();
        ResponseAPDU initUpdateResponse = super.transmit(initUpdateCommand);
        System.out.print("Command:  "); this.print(initUpdateCommand.getBytes());
        System.out.print("Response: "); this.print(initUpdateResponse.getBytes());
        if (initUpdateResponse.getSW() != 0x9000) throw new RuntimeException("Init update failed!");

        this.channel = super.supportedChannels.get(ChannelType.PLAIN).create(super.crypto);

        byte[] cardChallenge = new byte[8];
        System.arraycopy(initUpdateResponse.getBytes(), 12, cardChallenge, 0, 8);
        byte[] cardResponseCryptogram = new byte[8];
        System.arraycopy(initUpdateResponse.getBytes(), 20, cardResponseCryptogram, 0, 8);
        byte[] derivationData = new byte[16];
        System.arraycopy(cardChallenge, 4, derivationData, 0, 4);
        System.arraycopy(hostChallenge, 0, derivationData, 4, 4);
        System.arraycopy(cardChallenge, 0, derivationData, 8, 4);
        System.arraycopy(hostChallenge, 4, derivationData, 12, 4);

        Map<String, byte[]> sessionKeys = new HashMap<>();
        sessionKeys.put("session-enc", this.calculateSessionKey(derivationData, super.keySet.get("ENC")));
        sessionKeys.put("session-mac", this.calculateSessionKey(derivationData, super.keySet.get("MAC")));

        byte[] cardCryptogram = this.calculateCryptogram(hostChallenge, cardChallenge, sessionKeys.get("session-enc"));
        byte[] hostCryptogram = this.calculateCryptogram(cardChallenge, hostChallenge, sessionKeys.get("session-enc"));

        if (!Arrays.equals(cardCryptogram, cardResponseCryptogram))
        {
            System.out.println("Wrong keys");
            return;
        }

        Channel macChannel = super.supportedChannels.get(ChannelType.MAC).create(super.crypto);
        macChannel.setSessionKeys(sessionKeys);

        CommandAPDU extAuthCommand = macChannel.prepare(new APDU(0x80,0x82,0x01,0x00,hostCryptogram));
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

    //GP Platform 2.3.1 chapter D secure Channel Protocol '01'
    private byte[] calculateCryptogram(byte[] challenge1, byte[] challenge2, byte[] key) throws Exception
    {
        byte[] data = new byte[24];
        byte[] padding = new byte[]{(byte)0x80,0x00,0x00,0x00,0x00,0x00,0x00,0x00};
        byte[] iv = new byte[]{(byte)0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00};
        System.arraycopy(challenge1, 0, data, 0, 8);
        System.arraycopy(challenge2, 0, data, 8, 8);
        System.arraycopy(padding, 0, data, 16, 8);

        byte[] encrypted = super.crypto.encryptTDES_CBC(data, key, iv);
        byte[] cryptogram = new byte[8];
        System.arraycopy(encrypted, 16, cryptogram, 0, 8);
        return cryptogram;
    }

    //GP Platform 2.3.1 chapter D secure Channel Protocol '01'
    private byte[] calculateSessionKey(byte[] derivationData, byte[] key) throws Exception
    {
        byte[] derivationData1 = new byte[8];
        System.arraycopy(derivationData, 0, derivationData1, 0, 8);
        byte[] derivationData2 = new byte[8];
        System.arraycopy(derivationData, 8, derivationData2, 0, 8);

        byte[] sessionKey1 = super.crypto.encryptTDES_ECB(derivationData1, key);
        byte[] sessionKey2 = super.crypto.encryptTDES_ECB(derivationData2, key);

        byte[] sessionKey = new byte[16];
        System.arraycopy(sessionKey1, 0, sessionKey, 0, 8);
        System.arraycopy(sessionKey2, 0, sessionKey, 8, 8);

        return sessionKey;
    }
}