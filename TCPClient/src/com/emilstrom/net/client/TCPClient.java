package com.emilstrom.net.client;

import java.net.*;
import java.io.*;
import java.util.*;

public class TCPClient implements Runnable {
	public static final boolean WRITE_MSG = true;
	public static int myID,
		msgRecieved = 0,
		msgSent = 0;
	
	public static String    serverIP;
	public static int       serverPort;
	
	public boolean connected = false, tryConnect = false;
	Socket serverSocket;
	IClient host;

	Sender sender;
	Listener listener;
	
	public TCPClient(IClient host) {
		this.host = host;
	}
	public TCPClient(String ip, int port, IClient host) {
		this.host = host;
		connect(ip, port);
	}
	
	public void connect(String ip, int port) {		
		serverIP = ip;
		serverPort = port;
		
		new Thread(this).start();
	}
	
	public void run() {
		tryConnect = true;
		
		try {
			serverSocket = new Socket(serverIP, serverPort);
			serverSocket.setTcpNoDelay(true);
			connected = true;
		} catch(Exception e) {
			System.out.println("Can't connect to " + serverIP + ":" + Integer.toString(serverPort));
			System.out.println(e);
			disconnect();
		}
		
		tryConnect = false;
		
		if (connected) {
			sender = new Sender(this, serverSocket);
			listener = new Listener(this, serverSocket);
			host.serverConnected();
		}
	}
	
	public void sendMessage(MessageBuffer msg) {
		sender.msgBuffer.add(msg);
	}
	
	public void update() {
		if (listener == null) return;
		
		while(listener.messageList.size() > 0) {
			if (listener.messageList.get(0) != null)
				host.serverMessage(listener.messageList.get(0));
			
			listener.messageList.remove(0);				
		}
	}

	public void disconnect() {
		close();
		connected = false;
		
		host.serverDisconnected();
	}
	
	public void close() {	
		if (!connected) return;
		
		try {
			sender.close();
			serverSocket.close();
		} catch(Exception e) {
			host.engineException(e);
		}
	}
}

class Listener implements Runnable {
	TCPClient hostEngine;
	DataInputStream in;
	Thread listenerThread;
	boolean running = true;
	
	List<MessageBuffer> messageList;
	
	public Listener(TCPClient hostEngine, Socket serverSocket) {
		this.hostEngine = hostEngine;
		messageList = new ArrayList<MessageBuffer>();
		
		try {
			in = new DataInputStream(serverSocket.getInputStream());
		} catch(Exception e) {
			System.out.println("Can't create DataInputStream");
			System.out.println(e);
		}
		
		listenerThread = new Thread(this);
		listenerThread.start();
	}
	
	public void run() {
		while(hostEngine.connected && running) {
			try {
				int size = in.readInt();
				
				if (size == -1) {
					break;
				}
				
				MessageBuffer msg = new MessageBuffer();
				msg.copyStream(in, size);
				
				messageList.add(msg);
			}
			catch(IOException e) {
				hostEngine.disconnect();
				break;
			}
			catch(Exception e) {
				hostEngine.host.engineException(e);
			}
		}
	}
	
	public void close() {
		try {
			in.close();
			running = false;

			listenerThread.wait();
		} catch(Exception e) {
			hostEngine.host.engineException(e);
		}
	}
}

class Sender implements Runnable {
	TCPClient hostEngine;
	DataOutputStream out;
	Thread senderThread;
	boolean running = true;
	
	List<MessageBuffer> msgBuffer;
	
	public Sender(TCPClient hostEngine, Socket serverSocket) {
		this.hostEngine = hostEngine;
		msgBuffer = new ArrayList<MessageBuffer>();
		
		try {
			out = new DataOutputStream(serverSocket.getOutputStream());
		} catch(Exception e) {
			hostEngine.host.engineException(e);
		}
		
		senderThread = new Thread(this);
		senderThread.start();
	}
	
	public void sendMessage(MessageBuffer msg) {
		try {
			out.writeInt(msg.getSize());
			
			for(int i=0; i<msg.buffer.size(); i++) {
				switch(msg.bufferType.get(i)) {
					case MessageBuffer.TYPE_INT:
						out.writeInt((Integer)msg.buffer.get(i));
						break;
						
					case MessageBuffer.TYPE_BYTE:
						out.writeByte((Byte)msg.buffer.get(i));
						break;
						
					case MessageBuffer.TYPE_BOOL:
						out.writeBoolean((Boolean)msg.buffer.get(i));
						break;
						
					case MessageBuffer.TYPE_WORD:
						int val = (Integer)msg.buffer.get(i);
						
						byte[] byteList = new byte[2];
						byteList[0] = (byte)((val >> 8) & 0xFF);
						byteList[1] = (byte)(val & 0xFF);
						
						out.writeByte(byteList[0]);
						out.writeByte(byteList[1]);
						break;
				}
			}
			
			out.flush();
		} 
		catch(IOException e) {
			hostEngine.disconnect();
		}
		catch(Exception e) {
			hostEngine.host.engineException(e);
		}
	}
	
	
	public void run() {
		while(hostEngine.connected && running) {
			while(msgBuffer.size() > 0) {
				sendMessage(msgBuffer.get(0));
				msgBuffer.remove(0);
			}

			try{ Thread.sleep(1); } catch(Exception e) {}
		}
	}
	
	public void close() {
		try {
			out.close();
			running = false;

			senderThread.wait();
		} catch(Exception e) {
			hostEngine.host.engineException(e);
		}
	}
}