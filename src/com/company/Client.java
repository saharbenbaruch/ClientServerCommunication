package com.company;

import java.io.IOException;
import java.net.*;
import java.util.*;

import static java.lang.Thread.sleep;

public class Client {
    private DatagramSocket socket;
    private static long WaitingTime;
    private List<InetAddress> serversAddresses=new ArrayList<>();  //list of servers who respond
    private String teamName="";

    public Client(int time){
        WaitingTime =time*1000;
        /**
         * details[0]= encrypted message
         * detail[1]= hash length
         */
        String[]details= inputFromUser();
        new Thread(()-> {
            try {
                socket = new DatagramSocket();
                //address = socket.getLocalAddress();

            } catch (SocketException e) {
                e.printStackTrace();
            }

            runClient(details);
        }).start();
    }

    private void sendMessageToSpecificDest(InetAddress address, int port, String start, String end , String type, String decMessage,String hashLength){
        String discoveryMessage=teamName+type+decMessage+hashLength+start +end;
        byte[] messageInBytes = discoveryMessage.getBytes();
        DatagramPacket packet = new DatagramPacket(messageInBytes, messageInBytes.length,address , port);
        try {
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * get from user teamName, message to decrypt , length of hash
     * @return discovery message.
     */
    private String[] inputFromUser() {
        Scanner scan=new Scanner(System.in);
        System.out.println("Hi, whats you team name? (32 chars)");  //SaharSaharSaharSaharSaharSaharSa
        String inputName=scan.nextLine();
        while (inputName.length()!=32) {
            System.out.println("Please choose 32 chars team name.");
            inputName = scan.nextLine();
        }
        this.teamName= inputName;
        System.out.println("Welcome to "+teamName+". Please enter the hash:"); //13a5d51391f6a6ff5a94394b0dee6a35bf66fd73

        // compose discovery message .
        String messageToDecipher= scan.nextLine();
        System.out.println("Please enter the input string length:");
        String hashLength= scan.nextLine();

        String [] info= {messageToDecipher,hashLength};
        return info;
    }

    public static void main(String[]args)
    {
        Client client= new Client(1500);
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

    /**
     *
     *
     */
    private void runClient(String[] details) {
        /**
         *  compose discovery message to find available servers.
         */

        String start="0000";
        String end= "0000";
        String broadcastMessage=teamName+ "1"+details[0]+details[1]+start+end;

        sendDiscoverMessage(broadcastMessage);
        String [] domains = divideToDomains(Integer.parseInt(details[1]),serversAddresses.size());
        //send messages to all servers who respond to client with their domain.
        sendRequestsToAllServersParticipated(domains,details[0],details[1]);
        getAnswer(details[1]);

    }
    private void sendDiscoverMessage(String msg) {

        InetAddress broadcastAddress = null;

        try {
            byte[] buf = msg.getBytes();
            DatagramPacket packet = new DatagramPacket(buf, buf.length, broadcastAddress.getByName("255.255.255.255"), 3117);
            socket.setBroadcast(true);
            socket.send(packet);
                System.out.println("client send discovery request");

            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            while(true)
            {
                packet = new DatagramPacket(buf, buf.length);
                socket.setSoTimeout(10000);
                socket.receive(packet);
                String received = new String(packet.getData(), 0, packet.getLength());
                    System.out.println("client got " + received);
                int messageType = getMessageType(received);
                if (messageType == 2 && !serversAddresses.contains(packet.getAddress()))
                {
                    serversAddresses.add(packet.getAddress());
                }
            }
        } catch (IOException e) {
            // e.printStackTrace();
        }
    }


    /**
     * gets answer from servers (the decrypted message)
     * @param hashLength
     */
    private void getAnswer(String hashLength) {
        int numOfServers=serversAddresses.size();

        try {
            socket.getSoTimeout();
        }
        catch (SocketException e) {
            e.printStackTrace();
        }
        while (true) {
            if (numOfServers==0)
                System.out.println("No answer founded.");
            byte[] buff= new byte[585];
            DatagramPacket packet = new DatagramPacket(buff, buff.length);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
            String answer = new String(packet.getData(), 0, packet.getLength());
            int type = getMessageType(answer);
            if (type == 4) {
                extractAnswerFromMessage(answer,Integer.parseInt(hashLength));
                socket.close();
                return;
            } else if (type == 5) {
                System.out.println("One of the server didnt find the answer.");
                numOfServers--;
            }
        }
    }

    /**
     * print the decrypted answer.
     * @param answer
     * @param hashLength
     */
    private void extractAnswerFromMessage(String answer,int hashLength) {
        if(answer.length()>74)
        {
            String ans =answer.substring(74,hashLength+74);//start
            System.out.println("The answer is :"+ans);
        }
        else
        {
            System.out.println("attack! watch out!!!!!");
        }
    }


    /**
     * extract type of message.
     * 1= discover
     * 2= offer
     * 3= request
     * 4=ack
     * 5= negative ack
     * @param answer
     * @return
     */
    private int getMessageType(String answer) {
        String typeInString= answer.substring(32,33);
        return Integer.parseInt(typeInString);
    }

    /**
     * send to all servers that answer after broadcast the encrypted message with domain.
     * @param domains
     * @param messageToDecipher
     * @param hashLength
     */

    private void sendRequestsToAllServersParticipated(String[] domains, String messageToDecipher,String hashLength) {
        //compose the message without domains (every server has different domain.
        System.out.println("Client got "+serversAddresses.size()+ " offers.");
        String message = teamName + "3" + messageToDecipher + hashLength;
        for (int i = 0; i < serversAddresses.size(); i = i + 2) {
            //adding domain to message.

            sendMessageToSpecificDest(serversAddresses.get(i/2), 3117,domains[i], domains[i+1] , "3", messageToDecipher,hashLength);
            System.out.println("request message sent to:server "+(i/2));

            }
        }

    /**
     * divide string between servers equally.
     * @param stringLength
     * @param numOfServers
     * @return
     */
    public String [] divideToDomains (int stringLength, int numOfServers)
    {
        String [] domains = new String[numOfServers * 2];
        StringBuilder first = new StringBuilder(); //aaa
        StringBuilder last = new StringBuilder(); //zzz

        for(int i = 0; i < stringLength; i++){
            first.append("a"); //aaa
            last.append("z"); //zzz
        }

        int total = convertStringToInt(last.toString());
        int perServer = (int) Math.floor (((double)total) /  ((double)numOfServers));

        domains[0] = first.toString(); //aaa
        domains[domains.length -1 ] = last.toString(); //zzz
        int summer = 0;

        for(int i = 1; i <= domains.length -2; i += 2){
            summer += perServer;
            domains[i] = converxtIntToString(summer, stringLength); //end domain of server
            summer++;
            domains[i + 1] = converxtIntToString(summer, stringLength); //start domain of next server
        }

        System.out.println("domains[0]: "+domains[0]);
        System.out.println("domains[1]: "+domains[1]);
        return domains;
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

    private String converxtIntToString(int toConvert, int length) {
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
    }




