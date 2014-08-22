package com.emilstrom.net.server;

import java.net.*;
import java.io.*;
import java.util.*;

public class Client {
	public int id;
	public boolean active = true;
	public Socket mySocket;
	
	TCPServer engine;

	ClientSender sender;
	ClientListener listener;
	
	public Client(int id, Socket mySocket, TCPServer engine) {
		this.id = id;
		this.mySocket = mySocket;
		this.engine = engine;

		sender = new ClientSender(this, mySocket);
		listener = new ClientListener(this, mySocket);
	}

	public boolean isConnected() {
		return mySocket != null && mySocket.isConnected() && !mySocket.isClosed();
	}
	
	public void sendMessage(MessageBuffer msg) {
		sender.outBuffer.add(msg);
	}
	
	public void disconnect() {
		close();
	}
	
	public void close() {
		if (!isConnected()) return;

		try {
			active = false;

			mySocket.close();
			sender.close();
			listener.close();
		} catch(Exception e) {
			engine.host.engineException(e);
		}
		
		engine.clientDisconnected(id);
	}
	
	public String getIP() {
		return mySocket.getInetAddress().toString();
	}
}

class ClientListener implements Runnable {
	Client hostClient;
	DataInputStream in;
	boolean active = true,
			readable = false;
	Thread listenThread;
	
	public ClientListener(Client hostClient, Socket clientSocket) {
		this.hostClient = hostClient;
		
		try {
			in = new DataInputStream(clientSocket.getInputStream());
		} catch(Exception e) {
			hostClient.engine.host.engineException(e);
		}
		
		listenThread = new Thread(this);
		listenThread.start();
	}
	
	public void run() {
		while(TCPServer.running && active) {
			try {
				int size = in.readInt();
				
				if (size == -1) {
					if (hostClient.active) hostClient.disconnect();
					break;
				}
				
				MessageBuffer msg = new MessageBuffer(hostClient.id);
				msg.copyStream(in, size);
				
				TCPServer.messageList.add(msg);
			}
			catch(IOException e) {
				hostClient.disconnect();
				break;
			}
			catch(Exception e) {
				hostClient.engine.host.engineException(e);
			}
		}
	}
	
	public void close() {
		active = false;
		
		try {
			in.close();
		} catch(Exception e) {
			hostClient.engine.host.engineException(e);
		}
	}
}

class ClientSender implements Runnable {
	public List<MessageBuffer> outBuffer;
	Client hostClient;
	DataOutputStream out;
	boolean active = true;
	Thread senderThread;
	
	public ClientSender(Client hostClient, Socket clientSocket) {
		this.hostClient = hostClient;
		outBuffer = new ArrayList<MessageBuffer>();
		
		try {
			out = new DataOutputStream(clientSocket.getOutputStream());
		} catch(Exception e) {
			hostClient.engine.host.engineException(e);
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
						out.writeInt((int)msg.buffer.get(i));
						break;
						
					case MessageBuffer.TYPE_BYTE:
						out.writeByte((byte)msg.buffer.get(i));
						break;
						
					case MessageBuffer.TYPE_BOOL:
						out.writeBoolean((boolean)msg.buffer.get(i));
						break;
						
					case MessageBuffer.TYPE_WORD:
						int val = (int)msg.buffer.get(i);
						
						byte[] byteList = new byte[2];
						byteList[0] = (byte)((val >> 8) & 0xFF);
						byteList[1] = (byte)(val & 0xFF);
						
						out.writeByte(byteList[0]);
						out.writeByte(byteList[1]);
						break;
				}
				
				if (TCPServer.showMessages) System.out.println(msg.buffer.get(i));
			}

			if (TCPServer.showMessages) System.out.println("---");
			out.flush();
		} catch(Exception e) {
			hostClient.engine.host.engineException(e);
		}
	}
	
	public void run() {
		while(TCPServer.running && active) {
			while(outBuffer.size() > 0) {
				sendMessage(outBuffer.get(0));
				outBuffer.remove(0);
			}
			
			try{ Thread.sleep(1); }
			catch(Exception e) {
				hostClient.engine.host.engineException(e);
			}
		}
	}
	
	public void close() {
		active = false;
		
		try {
			out.close();
		} catch(Exception e) {
			hostClient.engine.host.engineException(e);
		}
	}
}