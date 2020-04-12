package com.company;

import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Server {
    private DatagramSocket socket;
    private static long TIMESERVER;

    public Server(int timeToDie) {
        TIMESERVER = timeToDie;
        new Thread(()-> {
            try {
                socket = new DatagramSocket(3117);
                runServer();
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void runServer() {
        byte[] received = new byte[585];
        while (true){

        DatagramPacket packet = new DatagramPacket(received, received.length);
        try {
            socket.receive(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String receivedM = new String(packet.getData(), 0, packet.getLength());
        String[] splitMessage = splitMessage(receivedM);
            InetAddress clientAddress = packet.getAddress();
            int port = packet.getPort();

        //discover message arrived
        if (splitMessage[1].equals("1")) {
            System.out.println("Server received discover message.");

            // Server offer himself to the client.
            String offerMessage = "SaharSaharSaharSaharSaharSaharSa" + "2" + "0000000000000000000000000000000000000000" + "0000";
            packet = new DatagramPacket(offerMessage.getBytes(), offerMessage.getBytes().length, clientAddress, port);
            try {
                socket.send(packet);
                System.out.println("Server send offer.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //request message arrived
        if (splitMessage[1].equals("3")) {
            System.out.println("Server got request message: "+splitMessage[2]);
            ExecutorService exec = Executors.newFixedThreadPool(10);
            exec.submit(new Thread(() -> {

            String ans = tryDeHash(splitMessage[4], splitMessage[5], splitMessage[2]);

            if (ans == null)
                sendAnsToClient("5", splitMessage, ans, port, clientAddress);
            else
                sendAnsToClient("4", splitMessage, ans, port, clientAddress);
            }));

            try {
                exec.awaitTermination(TIMESERVER, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            exec.shutdownNow();
            socket.close();
        }
    }
    }

    /**
     * after decription send back answer to client.
     * @param ans
     * @param port
     * @param clientAddress
     */
    private void sendAnsToClient(String type,String[] splitMessage,String ans, int port, InetAddress clientAddress) {
        if (splitMessage==null)
            return;
        String message="";
        if (type.equals("5")) {
             message = splitMessage[0] + type + splitMessage[2] + splitMessage[3] + splitMessage[4] + splitMessage[5];

        }
        else if (type.equals("4")){
             message = splitMessage[0] + type + splitMessage[2] + splitMessage[3] + ans + splitMessage[5];
        }

        DatagramPacket packet= new DatagramPacket(message.getBytes(),message.getBytes().length,clientAddress,port);

        try {
            socket.send(packet);
            System.out.println("Server send a message from type 4 or 5.");

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private String tryDeHash (String startRange, String endRange, String originalHash){
                int start = convertStringToInt(startRange);
                int end = convertStringToInt(endRange);
                int length = startRange.length();
                for (int i = start; i <= end; i++) {
                    String currentString = converxtIntToString(i, length);
                    String hash = hash(currentString);
                    if (originalHash.equals(hash)) {
                        return currentString;
                    }
                }
                return null;
            }

            private int convertStringToInt(String toConvert) {
                char[] charArray = toConvert.toCharArray();
                int num = 0;
                for(char c : charArray){
                    if(c < 'a' || c > 'z'){
                        throw new RuntimeException();
                    }
                    num *= 26;
                    num += c - 'a';
                }
                return num;
            }


            private String converxtIntToString (int toConvert, int length) {
                StringBuilder s = new StringBuilder(length);
                while (toConvert > 0 ){
                    int c = toConvert % 26;
                    s.insert(0, (char) (c + 'a'));
                    toConvert /= 26;
                    length --;
                }
                while (length > 0){
                    s.insert(0, 'a');
                    length--;
                }
                return s.toString();
            }

    public static void main(String[]args)
    {
        System.out.println("Welcome it's server ...");
        Server server = new Server(10);
    }

    private String hash(String toHash) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] messageDigest = md.digest(toHash.getBytes());
            BigInteger no = new BigInteger(1, messageDigest);
            StringBuilder hashText = new StringBuilder(no.toString(16));
            while (hashText.length() < 32){
                hashText.insert(0, "0");
            }
            return hashText.toString();
        }
        catch (NoSuchAlgorithmException e){
            throw new RuntimeException(e);
        }
    }


    private String[] splitMessage(String message)
    {
        if(message.length()>74)
        {
            String [] splitMessage= new String[6];
            splitMessage[0] =message.substring(0,32); // team name
            splitMessage[1]=message.substring(32,33); // // message type
            splitMessage[2]=message.substring(33,73); // hash to find
            splitMessage[3]=message.substring(73,74); // original length
            int size=message.length()-74;
            size=size/2;
            splitMessage[4]=message.substring(74,size+74);//start
            splitMessage[5]=message.substring(size+74); // end
            return splitMessage;
        }
        return null;
    }


}
