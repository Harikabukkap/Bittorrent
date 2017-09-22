import java.io.IOException;
import java.util.Arrays;
import java.io.*;

/*
 * Class to perform various operations on Byte arrays.
 */
public class ByteArrayManipulation {
    public synchronized static byte[] readBytes(InputStream input, byte[] byteArr, int length) throws IOException {
        int index = 0;
        for (;length != 0;) {
            int inputAvail = input.available();
            byte[] data = new byte[Math.min(length, inputAvail)];
            if (Math.min(length, inputAvail) != 0) {
                input.read(data);
                byteArr = ByteArrayManipulation.mergeByteArrays(byteArr, index, data, Math.min(length, inputAvail));
                index = index + Math.min(length, inputAvail);
                length = length - Math.min(length, inputAvail);
            }
        }
        return byteArr;
    }
    

    public static int byteArrayToInt(byte[] b) {
        int result = 0;
        int i = 0;
        int hex = 0x000000FF;
        while(i < 4){
            int shift = (4 - 1 - i) * 8;
            result = result+(b[i] & hex) << shift;
            i++;
        }
        return result;
    }

    public static byte[] intToByteArray(int integer) {
        byte[] result = new byte[4];
        int[] hex = {0xFF000000,0x00FF0000,0x0000FF00,0x000000FF};
        int cnt = 24;
        for(int i = 0;i<4;i++){
            if(i!=3)
                result[i] = (byte) ((integer & hex[i]) >> cnt);
            else
                result[i] = (byte) (integer & hex[i]);
            cnt =cnt-8;
        }
        return result;
        }
    
    public static byte[] mergeByteArrays(byte[] a, byte[] b) throws IOException {
        int aLength = a.length;
        int bLength = b.length;

        byte[] mergedByte = new byte[aLength + bLength];
        for(int i = 0;i<aLength; i++){
            mergedByte[i] = a[i];
        }
        for(int i = aLength;i<aLength+bLength; i++){
            mergedByte[i] = b[i-aLength];
        }
        return mergedByte;

     }
         
    public static byte[] mergeByteArrays(byte[] a, int aLength, byte[] b, int bLength) throws IOException {
        byte[] mergedByte = new byte[aLength + bLength];
        for(int i = 0;i<aLength; i++){
            mergedByte[i] = a[i];
        }
        for(int i = aLength;i<aLength+bLength; i++){
            mergedByte[i] = b[i-aLength];
        }
        return mergedByte;
    }
    
    public static byte[] mergeByteArray(byte b, byte[] a) throws IOException {
        byte[] mergedByte = new byte[a.length + 1];
        for(int i = 0;i<=a.length; i++){
            mergedByte[i] = a[i];
        }
        mergedByte[a.length] = b;
        return mergedByte;
    }
    public static byte[] mergeByte(byte[] a, byte b) throws IOException {
        byte[] mergedByte = new byte[a.length + 1];
        for(int i = 0;i<a.length; i++){
            mergedByte[i] = a[i];
        }
        mergedByte[a.length] = b;
        return mergedByte;
    }
    
}