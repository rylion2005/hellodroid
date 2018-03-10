package com.hellodroid.lan;

/*
** REVISED HISTORY
**   yl7 | 18-2-9: Created
**     
*/

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.hellodroid.identity.Utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
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
public class LanScanner {
    private static final String TAG = "LanScanner";

    private static final int SCAN_PERIOD = 10 * 1000;
    private static LanScanner mInstance;
    private String myLocalAddress;


/* ********************************************************************************************** */

    private LanScanner(){
        Timer timer = new Timer();
        timer.schedule(new Scanning(), 100, SCAN_PERIOD);
    }

    public static LanScanner newInstance(){
        if (mInstance == null){
            mInstance = new LanScanner();
        }
        return mInstance;
    }

    public List<String> getNeighbours(){
        return getIpsFromArp();
    }

    public static List<String> getIpsFromArp(){
        List<String> ipList = new ArrayList<>();

        try {
            BufferedReader br = new BufferedReader(new FileReader("/proc/net/arp"));
            String line = "";
            String ip = "";
            String flag = "";
            String mac = "";
            while ((line = br.readLine()) != null) {
                if ( (line.length() >= 63) && (!line.toUpperCase(Locale.US).contains("IP"))  ){
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

        @Override
        public void run() {
            Log.v(TAG, ":Scanner: timer running");
            discovery(myLocalAddress);
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
                    } catch (IOException e) {
                        // TODO
                    }
                }
            }
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
}
