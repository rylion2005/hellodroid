package com.hellodroid.socket;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class MyMulticastSocket {
    public static final String TAG = "MyMulticastSocket";

    private static final String MULTICAST_ADDRESS = "230.230.200.100";
    private static final int SOCKET_PORT = 32324;
    private static final int SOCKET_BUFFER_BYTES = 2048;

    private byte[] myself;
    private int    mByteLength;

    public void broadcastMyself(byte[] myself, int length){
        this.myself = myself.clone();
        mByteLength = length;
    }


/* ============================================================================================== */

    class Listener implements Runnable{

        public Listener(){ }

        @Override
        public void run(){
            try {
                byte[] data = new byte[SOCKET_BUFFER_BYTES];
                InetAddress addr = InetAddress.getByName(MULTICAST_ADDRESS);
                MulticastSocket ms = new MulticastSocket(SOCKET_PORT);
                ms.joinGroup(addr);
                ms.setTimeToLive(32);
                DatagramPacket packet = new DatagramPacket(data, data.length);
                ms.receive(packet);
                if (packet.getData().length > 0){
                    handleMessage(packet.getData(), packet.getData().length);
                }
                ms.close();
            } catch (IOException e) {
                //
            }
        }

        private void handleMessage(byte[] data, int length){

        }
    }


    class Sender implements Runnable {

        @Override
        public void run() {
            try {
                InetAddress ip = InetAddress.getByName(MULTICAST_ADDRESS);
                DatagramPacket packet = new DatagramPacket(myself, mByteLength, SOCKET_PORT);
                MulticastSocket ms = new MulticastSocket();
                ms.send(packet);
                ms.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
