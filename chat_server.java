package com.example.androidchatserver;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import android.support.v7.app.ActionBarActivity;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Bundle;

public class MainActivity extends ActionBarActivity {

 static final int SocketServerPORT = 8080;

 TextView infoIp, infoPort, chatMsg;

 String msgLog = "";
 
 List<ChatClient> userList;

 ServerSocket serverSocket;

 @Override
 protected void onCreate(Bundle savedInstanceState) {
  super.onCreate(savedInstanceState);
  setContentView(R.layout.activity_main);
  infoIp = (TextView) findViewById(R.id.infoip);
  infoPort = (TextView) findViewById(R.id.infoport);
  chatMsg = (TextView) findViewById(R.id.chatmsg);

  infoIp.setText(getIpAddress());

  userList = new ArrayList<ChatClient>();
  
  ChatServerThread chatServerThread = new ChatServerThread();
  chatServerThread.start();
 }

 @Override
 protected void onDestroy() {
  super.onDestroy();

  if (serverSocket != null) {
   try {
    serverSocket.close();
   } catch (IOException e) {
    // TODO Auto-generated catch block
    e.printStackTrace();
   }
  }
 }

/////////////////////////////////////////////////////////////////////////////////////////////////////////

 private class ChatServerThread extends Thread {

  @Override
  public void run() {
   Socket socket = null;

   try {
    serverSocket = new ServerSocket(SocketServerPORT);
    MainActivity.this.runOnUiThread(new Runnable() {

     @Override
     public void run() {
      infoPort.setText("I'm waiting here: "
        + serverSocket.getLocalPort());
     }
    });
    
    while (true) {
     socket = serverSocket.accept();
     ChatClient client = new ChatClient();
     userList.add(client);
     ConnectThread connectThread = new ConnectThread(client, socket);
     connectThread.start();
    }

   } catch (IOException e) {
    e.printStackTrace();
   } finally {
    if (socket != null) {
     try {
      socket.close();
     } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
     }
    }
   }

  }

 }
 
///////////////////////////////////////////////////////////////////////////////////

 private class ConnectThread extends Thread {
  
  Socket socket;
  ChatClient connectClient;
  String msgToSend = "";

  ConnectThread(ChatClient client, Socket socket){
   connectClient = client;
   this.socket= socket;
   client.socket = socket;
   client.chatThread = this;
  }

  @Override
  public void run() {
   DataInputStream dataInputStream = null;
   DataOutputStream dataOutputStream = null;
   
   try {
    dataInputStream = new DataInputStream(socket.getInputStream());
    dataOutputStream = new DataOutputStream(socket.getOutputStream());
    
    String n = dataInputStream.readUTF();
    
    connectClient.name = n;
    
    msgLog += connectClient.name + " connected@" + 
      connectClient.socket.getInetAddress() + 
      ":" + connectClient.socket.getPort() + "\n";
    MainActivity.this.runOnUiThread(new Runnable() {

     @Override
     public void run() {
      chatMsg.setText(msgLog);
     }
    });
    
    dataOutputStream.writeUTF("Welcome " + n + "\n");
    dataOutputStream.flush();
    
    broadcastMsg(n + " join our chat.\n");
    
    while (true) {
     if (dataInputStream.available() > 0) {
      String newMsg = dataInputStream.readUTF();
      
      
      msgLog += n + ": " + newMsg;
      MainActivity.this.runOnUiThread(new Runnable() {

       @Override
       public void run() {
        chatMsg.setText(msgLog);
       }
      });
      
      broadcastMsg(n + ": " + newMsg);
     }
     
     if(!msgToSend.equals("")){
      dataOutputStream.writeUTF(msgToSend);
      dataOutputStream.flush();
      msgToSend = "";
     }
     
    }
    
   } catch (IOException e) {
    e.printStackTrace();
   } finally {
    if (dataInputStream != null) {
     try {
      dataInputStream.close();
     } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
     }
    }

    if (dataOutputStream != null) {
     try {
      dataOutputStream.close();
     } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
     }
    }
    
    userList.remove(connectClient);
    MainActivity.this.runOnUiThread(new Runnable() {

     @Override
     public void run() {
      Toast.makeText(MainActivity.this, 
       connectClient.name + " removed.", Toast.LENGTH_LONG).show();
      
      msgLog += "-- " + connectClient.name + " leaved\n";
      MainActivity.this.runOnUiThread(new Runnable() {

       @Override
       public void run() {
        chatMsg.setText(msgLog);
       }
      });
      
      broadcastMsg("-- " + connectClient.name + " leaved\n");
     }
    });
   }
   
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////
  
  private void sendMsg(String msg){
   msgToSend = msg;
  }
  
 }
 
///////////////////////////////////////////////////////////////////////////////////////////////

 private void broadcastMsg(String msg){
  for(int i=0; i<userList.size(); i++){
   userList.get(i).chatThread.sendMsg(msg);
   msgLog += "- send to " + userList.get(i).name + "\n";
  }
  
  MainActivity.this.runOnUiThread(new Runnable() {

   @Override
   public void run() {
    chatMsg.setText(msgLog);
   }
  });
 }

////////////////////////////////////////////////////////////////////////////////////////////////////

 private String getIpAddress() {
  String ip = "";
  try {
   Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
     .getNetworkInterfaces();
   while (enumNetworkInterfaces.hasMoreElements()) {
    NetworkInterface networkInterface = enumNetworkInterfaces
      .nextElement();
    Enumeration<InetAddress> enumInetAddress = networkInterface
      .getInetAddresses();
    while (enumInetAddress.hasMoreElements()) {
     InetAddress inetAddress = enumInetAddress.nextElement();

     if (inetAddress.isSiteLocalAddress()) {
      ip += "SiteLocalAddress: "
        + inetAddress.getHostAddress() + "\n";
     }

    }

   }

  } catch (SocketException e) {
   // TODO Auto-generated catch block
   e.printStackTrace();
   ip += "Something Wrong! " + e.toString() + "\n";
  }

  return ip;
 }

/////////////////////////////////////////////////////////////////////////////////////////////////////////

 class ChatClient {
  String name;
  Socket socket;
  ConnectThread chatThread;
  
 }

}