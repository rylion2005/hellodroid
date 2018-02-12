package com.hellodroid.lan;

/*
** REVISED HISTORY
**   yl7 | 18-2-9: Created
**     
*/

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
public class Scanner {
    private static final String TAG = "Scanner";

    private static int SCAN_PERIOD = 60 * 1000; // default 1 minutes
    private String myNetworkAddress;


/* ********************************************************************************************** */

    public Scanner (){
        Thread t = new Thread(new ScanningRunnable());
        t.start();
    }

    public ArrayList<String> getNeighbours(){
        return readArp();
    }

    public String getMyNetworkAddress(){
        return myNetworkAddress;
    }


/* ********************************************************************************************** */

    private String getMyLocalAddress() {

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


    private String getNetworkAddress() {
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
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                inStream.close();
                httpConnection.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return ipLine;
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


/* ********************************************************************************************** */

    class ScanningRunnable implements Runnable{
        @Override
        public void run() {
            myNetworkAddress = getMyNetworkAddress();
            while (true) {
                discovery(getMyLocalAddress());
                try {
                    Thread.sleep(SCAN_PERIOD);
                } catch (InterruptedException e) {
                    //TODO:
                }
            }
        }
    }
}
