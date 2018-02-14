package com.hellodroid.lan;

/*
** REVISED HISTORY
**   yl7 | 18-2-9: Created
**     
*/

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/*
** ********************************************************************************
**
** Scanner
**   scan ip in the same local access network
**
** USAGE:
**   ......
**
** ********************************************************************************
*/
public class Scanner extends Handler {
    private static final String TAG = "Scanner";

    private static final int MESSAGE_UPDATE_ADDRESSES = 0xAA;
    private static final int SCAN_PERIOD = 1000; //60 * 1000; // default 1 minutes

    private static Scanner mInstance;
    private static final List<Callback> mCallbackList = new ArrayList<>();

    private String myLocalAddress;
    private String myNetworkAddress;


/* ********************************************************************************************** */


    public static Scanner newInstance(Callback cb){
        if (mInstance == null){
            mInstance = new Scanner(cb);
        } else {
            mCallbackList.add(cb);
        }
        return mInstance;
    }

    @Override
    public void handleMessage(Message msg) {
        Log.v(TAG, "Message: " + msg.what);
        switch (msg.what) {
            case MESSAGE_UPDATE_ADDRESSES:
                for (Callback cb : mCallbackList){
                    cb.onUpdateNeighbors(getNeighbours());
                    cb.onUpdateLocalAddress(getMyLocalAddress());
                    cb.onUpdateInternetAddress(getMyInternetAddress());
                }
                break;

            default:break;
        }
    }

    public ArrayList<String> getNeighbours(){
        return readArp();
    }

    public String getMyLocalAddress(){
        return myLocalAddress;
    }

    public String getMyInternetAddress(){
        return myNetworkAddress;
    }


/* ********************************************************************************************** */

    private Scanner(Callback cb){
        if ( cb != null ){
            mCallbackList.add(cb);
        }

        Timer timer = new Timer();
        timer.schedule(new Scanning(), 100, SCAN_PERIOD);
    }

    private ArrayList<String> readArp() {
        ArrayList<String> ipList = new ArrayList<>();

        try {
            BufferedReader br = new BufferedReader(new FileReader("/proc/net/arp"));
            String line = "";
            String ip = "";
            String flag = "";
            String mac = "";
            while ((line = br.readLine()) != null) {
                if ( (line.length() < 63) || (line.toUpperCase(Locale.US).contains("IP"))  ){
                    continue;
                } else {
                    mac = line.substring(41, 63).trim();
                    if ( ! mac.contains("00:00:00:00:00:00")) {
                        //flag = line.substring(29, 32).trim();
                        ipList.add(line.substring(0, 17).trim());
                    }
                }
            }
            br.close();
        } catch(Exception e) {
            //TODO:
        }
        return ipList;
    }


/* ********************************************************************************************** */

    class Scanning extends TimerTask{
        boolean ifnotify = false;

        @Override
        public void run() {
            Log.v(TAG, ":Scanner: timer task running");

            myLocalAddress = getLocalAddress();
            myNetworkAddress = getInternetAddress();

            if (!ifnotify) {
                discovery(myLocalAddress);
            } else {
                sendEmptyMessage(MESSAGE_UPDATE_ADDRESSES);
            }
            ifnotify = !ifnotify;
        }

        private void discovery(String ip){

            if ((ip == null) || (ip.length() == 0)){
                return;
            }

            String ipHead = ip.substring(0, getWhenCount(ip, "\\.", 2));
            for (int i = 0; i < 255; i++){
                String head3 = ipHead + "." + Integer.toString(i);
                for (int j = 0; j < 255; j++){
                    try {
                        String address4 = head3 + "." + Integer.toString(j);
                        DatagramPacket dp = new DatagramPacket(new byte[0], 0, 0);
                        DatagramSocket socket = new DatagramSocket();
                        dp.setAddress(InetAddress.getByName(address4));
                        socket.send(dp);
                        socket.close();
                    } catch (SocketException e) {
                        // TODO
                    } catch (UnknownHostException e) {
                        // TODO
                    } catch (IOException e) {
                        // TODO
                    }
                }
            }
        }


        private String getLocalAddress() {

            String hostIp = null;
            try {
                Enumeration nis = NetworkInterface.getNetworkInterfaces();
                InetAddress ia = null;
                while (nis.hasMoreElements()) {
                    NetworkInterface ni = (NetworkInterface) nis.nextElement();
                    Enumeration<InetAddress> ias = ni.getInetAddresses();
                    while (ias.hasMoreElements()) {
                        ia = ias.nextElement();
                        if (ia instanceof Inet6Address) {
                            continue;// skip ipv6
                        }
                        String ip = ia.getHostAddress();
                        if (!"127.0.0.1".equals(ip)) {
                            hostIp = ia.getHostAddress();
                            break;
                        }
                    }
                }
            } catch (SocketException e) {
                //TODO
                //e.printStackTrace();
            }
            return hostIp;
        }

        private String getInternetAddress() {
            URL infoUrl = null;
            InputStream inStream = null;
            String ipLine = "";
            HttpURLConnection httpConnection = null;
            try {
                // infoUrl = new URL("http://ip168.com/");
                infoUrl = new URL("http://pv.sohu.com/cityjson?ie=utf-8");
                URLConnection connection = infoUrl.openConnection();
                httpConnection = (HttpURLConnection) connection;
                int responseCode = httpConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    inStream = httpConnection.getInputStream();
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(inStream, "utf-8"));
                    StringBuilder strber = new StringBuilder();
                    String line = null;
                    while ((line = reader.readLine()) != null){
                        strber.append(line + "\n");
                    }
                    Pattern pattern = Pattern.compile("((?:(?:25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d)))\\.){3}(?:25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d))))");
                    Matcher matcher = pattern.matcher(strber.toString());
                    if (matcher.find()) {
                        ipLine = matcher.group();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    inStream.close();
                    httpConnection.disconnect();
                } catch (IOException|NullPointerException e) {
                    e.printStackTrace();
                }
            }
            return ipLine;
        }

        private int getWhenCount(final String line, final String mode, final int count) {
            Matcher matcher = Pattern.compile(mode).matcher(line);
            int index = 0;
            while(matcher.find()) {
                index++;
                if(index == count){
                    break;
                }
            }
            return matcher.start();
        }

    }

    public interface Callback {
        void onUpdateNeighbors(ArrayList<String> neighbors);
        void onUpdateLocalAddress(String address);
        void onUpdateInternetAddress(String address);
    }
}
