package kr.neolab.sdk.util;

import kr.neolab.sdk.pen.bluetooth.lib.ByteConverter;

/**
 * Created by LMS on 2019-09-06.
 */
public class UuidUtil
{
    public static  String changeAddressFromLeToSpp(byte[] data)
    {
        int index = 0;
        int size = 0;
        byte flag = 0;
        while(data.length > index)
        {
            size = data[index++];
            if ( data.length <= index )
                return null;
            flag = data[index];
            if ( (flag & 0xFF) == 0xFF )
            {
                ++index;
                byte[] mac = new byte[6];
                System.arraycopy(data, index, mac, 0, 6);
                StringBuilder sb = new StringBuilder(18);
                for (byte b : mac) {
                    if (sb.length() > 0)
                        sb.append(':');
                    sb.append(String.format("%02x", b));
                }
                String strMac = sb.toString().toUpperCase();
                return strMac;
            }
            else
            {
                index += size;
            }
        }
        return null;
    }

    public static int getCompanyCodeFromUUID(byte[] data)
    {
        int index = 0;
        while (data.length > index)
        {
            if(data.length < index + 2)
                return -1;
            int size = data[index];
            index++;
            byte type = data[index];
            int length = index + size;
            if(data.length < length)
                return -1;
            if((type & 0xFF) == 0xFF && size == 12)
            {
                index += 8;
                byte[] dev = new byte[2];
                System.arraycopy(data, index, dev, 0, 2);

                int companyCode = ByteConverter.byteArrayToInt(dev);
                return companyCode;
            }
            else
            {
                index += size;
            }
        }
        return -1;
//        int index = 0;
//        int size = data[index];
//        index++;
//        index += size;
//        if(data.length < index)
//            return -1;
//        size = data[index];
//        if(data.length < (index + 1 + size))
//            return -1;
//        index += 9;
//
//        if(data.length < 13)
//            return -1;
//
//        byte[] dev = new byte[2];
//        System.arraycopy(data, index, dev, 0, 2);
//
//        int companyCode = ByteConverter.byteArrayToInt(dev);
//        return companyCode;
    }

    public static int getProductCodeFromUUID(byte[] data)
    {
        int index = 0;
        while (data.length > index)
        {
            if(data.length < index + 2)
                return -1;
            int size = data[index];
            index++;
            byte type = data[index];
            int length = index + size;
            if(data.length < length)
                return -1;
            if((type & 0xFF) == 0xFF && size == 12)
            {
                index += 8;
                index += 2;
                byte[] dev = new byte[1];
                System.arraycopy(data, index, dev, 0, 1);

                int productCode = ByteConverter.byteArrayToInt(dev);
                return productCode;
            }
            else
            {
                index += size;
            }
        }
        return -1;
//        int index = 0;
//        int size = data[index];
//        index++;
//        index += size;
//        if(data.length < index)
//            return -1;
//        size = data[index];
//        if(data.length < (index + 1 + size))
//            return -1;
//        index += 9;
//        index += 2;
//        if(data.length < 13)
//            return -1;
//
//        byte[] dev = new byte[1];
//        System.arraycopy(data, index, dev, 0, 1);
//
//        int productCode = ByteConverter.byteArrayToInt(dev);
//        return productCode;
    }

//    public static int getColorCodeFromUUIDVer5(byte[] data)
//    {
//
//        int index = 0;
//        int size = data[index];
//        index++;
//        index += size;
//        if(data.length < index)
//            return -1;
//        size = data[index];
//        if(data.length < (index + 1 + size))
//            return -1;
//        index += 9;
//        index += 2;
//        index += 1;
//        if(data.length < 13)
//            return -1;
//
//        byte[] dev = new byte[1];
//        System.arraycopy(data, index, dev, 0, 1);
//
//        int colorCode = ByteConverter.byteArrayToInt(dev);
//        return colorCode;
//    }

    public static int getColorCodeFromUUID(byte[] data)
    {
        int index = 0;
        while (data.length > index)
        {
            if(data.length < index + 2)
                return -1;
            int size = data[index];
            index++;
            byte type = data[index];
            int length = index + size;
            if(data.length < length)
                return -1;
            if((type & 0xFF) == 0xFF && size == 12)
            {
                index += 8;
                index += 2;
                index += 1;
                byte[] dev = new byte[1];
                System.arraycopy(data, index, dev, 0, 1);

                int colorCode = ByteConverter.byteArrayToInt(dev);
                return colorCode;
            }
            else
            {
                index += size;
            }
        }
        return -1;
    }

}
