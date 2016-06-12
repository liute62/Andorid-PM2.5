package app.utils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * Created by jiangph on 16-5-21.
 */
public class SDMRequest implements Runnable {

    DatagramSocket socket=null;

    public void run()
    {
        // 向局域网UDP广播信息：Hello, World!
        try {
            InetAddress serverAddress = InetAddress.getByName(HttpUtil.SERVERIP);
            System.out.println("Client: Start connecting\n");
            socket = new DatagramSocket(HttpUtil.SERVERPORT);
            byte[] buf = "?ip".getBytes();
            DatagramPacket packet = new DatagramPacket(buf, buf.length,
                    serverAddress, HttpUtil.SERVERPORT);
            System.out.println("Client: Sending " + new String(buf)
                    + "\n");
            socket.send(packet);
            System.out.println("Client: Message sent\n");
            System.out.println("Client: Succeed!\n");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 接收UDP广播，有的手机不支持
        while (true) {
            byte[] recbuf = new byte[255];
            DatagramPacket recpacket = new DatagramPacket(recbuf,
                    recbuf.length);
            try {
                socket.receive(recpacket);

                String deviceResult=new String(recpacket.getData(),recpacket.getOffset(),recpacket.getLength());
                String[] sn=deviceResult.split(",");
                if (sn.length>5)
                {
                    HttpUtil.deviceNumber=sn[10].split(":")[1];

                    System.out.println("deviceNumber"+HttpUtil.deviceNumber);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

}




