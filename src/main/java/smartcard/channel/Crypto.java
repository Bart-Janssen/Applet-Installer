package smartcard.channel;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;

public class Crypto
{
    public byte[] getRandomBytes(int n)
    {
        byte[] randomBytes = new byte[n];
        SecureRandom random = new SecureRandom();
        random.nextBytes(randomBytes);
        return randomBytes;
    }

    public byte[] encryptTDES_ECB(byte[] plainText, byte[] key) throws Exception
    {
        byte[] keyBytes;
        if (key.length == 16)
        {
            keyBytes = new byte[24];
            System.arraycopy(key, 0, keyBytes, 0, 16);
            System.arraycopy(key, 0, keyBytes, 16, 8);
        }
        else
        {
            keyBytes = key;
        }
        SecretKey secretKey = new SecretKeySpec(keyBytes, "DESede");
        Cipher cipher = Cipher.getInstance("DESede/ECB/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return cipher.doFinal(plainText);
    }

    public byte[] encryptTDES_CBC(byte[] data, byte[] key, byte[] iv) throws Exception
    {
        byte[] keyBytes;
        if (key.length == 16)
        {
            keyBytes = new byte[24];
            System.arraycopy(key, 0, keyBytes, 0, 16);
            System.arraycopy(key, 0, keyBytes, 16, 8);
        }
        else
        {
            keyBytes = key;
        }
        SecretKey secretKey = new SecretKeySpec(keyBytes, "DESede");
        Cipher cipher = Cipher.getInstance("DESede/CBC/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));
        return cipher.doFinal(data);
    }

    public byte[] cmac(byte[] data, byte[] key) throws Exception
    {
        return new Cmac().cmac(data,key);
    }

    public byte[] encryptDES_ECB(byte[] key, byte[] data) throws Exception
    {
        Cipher cipher = Cipher.getInstance("DES/ECB/NoPadding");
        byte[] key8 = new byte[8];
        System.arraycopy(key, 0, key8, 0, 8);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key8, "DES"));
        return cipher.doFinal(data);
    }

    //GP Platform 2.3.1 chapter E.3 secure Channel Protocol '02'
    public byte[] encryptDES_TDES(byte[] key, byte[] data, byte[] iv) throws Exception
    {
        Cipher TDES_CBC = Cipher.getInstance("DESede/CBC/NoPadding");
        byte[] key24 = new byte[24];
        System.arraycopy(key, 0, key24, 0, 16);
        System.arraycopy(key, 0, key24, 16, 8);
        TDES_CBC.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key24, "DESede"), new IvParameterSpec(iv));
        byte[] mac = new byte[8];
        byte[] macData;
        if (data.length > 8)
        {
            //GP Platform 2.3.1 chapter E.4.1 secure Channel Protocol '02'
            Cipher DES_CBC = Cipher.getInstance("DES/CBC/NoPadding");
            byte[] key8 = new byte[8];
            System.arraycopy(key, 0, key8, 0, 8);
            DES_CBC.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key8, "DES"), new IvParameterSpec(iv));
            macData = DES_CBC.doFinal(data, 0, data.length - 8);
            System.arraycopy(macData, macData.length - 8, mac, 0, 8);
            TDES_CBC.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key24, "DESede"), new IvParameterSpec(mac));
        }
        macData = TDES_CBC.doFinal(data, data.length - 8, 8);
        System.arraycopy(macData, macData.length - 8, mac, 0, 8);
        return mac;
    }
}