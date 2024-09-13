package smartcard.channel;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;

/*
* This code has been copied from an online source, and therefore not written by me.
* */

public class Cmac
{
    private static final int BLOCK_SIZE = 16;

    private byte[] xor(byte[] a, byte[] b)
    {
        byte[] result = new byte[a.length];
        for (int i = 0; i < a.length; i++)
        {
            result[i] = (byte)(a[i] ^ b[i]);
        }
        return result;
    }

    private byte[] leftShift(byte[] block)
    {
        byte[] result = new byte[block.length];
        int overflow = 0;
        for (int i = block.length - 1; i >= 0; i--)
        {
            int value = (block[i] & 0xFF) << 1;
            result[i] = (byte) ((value & 0xFF) + overflow);
            overflow = (value & 0x100) >> 8;
        }
        return result;
    }

    private byte[] padBlock(byte[] block)
    {
        byte[] padded = Arrays.copyOf(block, BLOCK_SIZE);
        padded[block.length] = (byte) 0x80;
        return padded;
    }

    public byte[] cmac(byte[] data, byte[] key) throws Exception
    {
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
        SecretKey secretKey = new SecretKeySpec(key, "AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] zeroBlock = new byte[BLOCK_SIZE];
        byte[] l = cipher.doFinal(zeroBlock);
        byte[] k1 = leftShift(l);
        if ((l[0] & 0x80) != 0) k1[BLOCK_SIZE - 1] ^= (byte)0x87;
        byte[] k2 = leftShift(k1);
        if ((k1[0] & 0x80) != 0) k2[BLOCK_SIZE - 1] ^= (byte)0x87;
        int n = (data.length + BLOCK_SIZE - 1) / BLOCK_SIZE;
        boolean isCompleteBlock = data.length % BLOCK_SIZE == 0;
        byte[] lastBlock;
        if (n == 0)
        {
            n = 1;
            lastBlock = padBlock(data);
        }
        else
        {
            byte[] lastPart = Arrays.copyOfRange(data, (n - 1) * BLOCK_SIZE, data.length);
            if (isCompleteBlock) lastBlock = this.xor(lastPart, k1);
            else lastBlock = this.xor(padBlock(lastPart), k2);
        }
        byte[] x = new byte[BLOCK_SIZE];
        byte[] y;
        for (int i = 0; i < n - 1; i++)
        {
            byte[] m = Arrays.copyOfRange(data, i * BLOCK_SIZE, (i + 1) * BLOCK_SIZE);
            y = this.xor(m, x);
            x = cipher.doFinal(y);
        }
        y = this.xor(lastBlock, x);
        return cipher.doFinal(y);
    }
}