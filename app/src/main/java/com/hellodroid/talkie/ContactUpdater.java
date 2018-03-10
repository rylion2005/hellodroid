package com.hellodroid.talkie;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class ContactUpdater {
    public static final String TAG = "ContactUpdater";

    private static final String MULTICAST_ADDRESS = "230.230.222.222";
    private static final int SOCKET_PORT = 32346;
    private static final int SOCKET_BUFFER_BYTES = 2048;

    private final Listener mListener = new Listener();
    private Callback mCallback;

    private byte[] myself;
    private int mByteLength;

/* ********************************************************************************************** */
    public ContactUpdater(){
        (new Thread(mListener)).start();
    }

    public void register(Callback cb){
        if (cb != null){
            mCallback = cb;
        }
    }

    public void broadcastMyself(byte[] myself, int length){
        this.myself = myself.clone();
        mByteLength = length;
        (new Thread(new Sender())).start();
    }

/* ********************************************************************************************** */

    class Listener implements Runnable{
        private volatile boolean mRunning = false;

        public Listener(){ }

        private void setRunnable(boolean start){
            mRunning = start;
        }

        @Override
        public void run(){
            mRunning = true;
            while (mRunning) {
                try {
                    byte[] data = new byte[SOCKET_BUFFER_BYTES];
                    InetAddress addr = InetAddress.getByName(MULTICAST_ADDRESS);
                    MulticastSocket ms = new MulticastSocket(SOCKET_PORT);
                    ms.joinGroup(addr);
                    ms.setTimeToLive(32);
                    DatagramPacket packet = new DatagramPacket(data, data.length);
                    ms.receive(packet);
                    if (packet.getData().length > 0) {
                        if (mCallback != null) {
                            mCallback.onBytesReceived(packet.getData(), packet.getData().length);
                        }
                    }
                    ms.close();
                } catch (IOException e) {
                    //
                }
            }
        }
    }


    class Sender implements Runnable {

        @Override
        public void run() {
            try {
                InetAddress ip = InetAddress.getByName(MULTICAST_ADDRESS);
                DatagramPacket packet = new DatagramPacket(myself, mByteLength, ip, SOCKET_PORT);
                MulticastSocket ms = new MulticastSocket();
                ms.send(packet);
                ms.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    interface Callback {
        void onBytesReceived(byte[] bytes, int length);
    }
}
