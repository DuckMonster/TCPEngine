package com.emilstrom.net.server;


import java.net.*;
import java.util.*;

public class TCPServer {
	public static boolean running = true,
		showMessages = true;
	public static List<Client> clientsAccepted;
	public static List<MessageBuffer> messageList;
	public static Client[] clientList;
	ServerSocket serverSocket;
	ClientAccepter clientAccepter;
	IServer host;
	
	public TCPServer(int port, IServer host) {
		this.host = host;
		clientsAccepted = new ArrayList<Client>();
		messageList = new ArrayList<MessageBuffer>();
		clientList = new Client[100];
		
		try {
			serverSocket = new ServerSocket(port);
			clientAccepter = new ClientAccepter(this);
		} catch(Exception e) {
			host.engineException(e);
		}
	}
	
	public void update() {
		while(clientsAccepted.size() > 0) {
			if (clientsAccepted.get(0) != null)
				host.clientConnected(clientsAccepted.get(0).id);
			clientsAccepted.remove(0);
		}
		
		while(messageList.size() > 0) {
			if (messageList.get(0) != null)
				host.clientMessage(messageList.get(0).playerID, messageList.get(0));
			
			messageList.remove(0);				
		}
	}
	
	public void sendMessage(int clientID, MessageBuffer mb) {
		try {clientList[clientID].sendMessage(mb);}
		catch(Exception e) {
			host.engineException(e);
		}
	}
	
	public void clientAccepted(Socket newClient) {
		for(int i=0; i<clientList.length; i++)
			if (clientList[i] == null) {
				clientList[i] = new Client(i, newClient, this);
				clientsAccepted.add(clientList[i]);
				break;
			}
	}
	
	public void clientDisconnected(int id) {
		clientList[id] = null;
		host.clientDisconnected(id);
	}
	
	public Client getClient(int id) {
		return clientList[id];
	}
}

////CLIENT ACCEPTER ///////////////
class ClientAccepter implements Runnable {
	TCPServer mainServer;
	Thread accepterThread;
	
	public ClientAccepter(TCPServer mainServer) {
		this.mainServer = mainServer;
		accepterThread = new Thread(this);
		accepterThread.start();
	}
	
	public void run() {
		while(TCPServer.running) {
			try {
				Socket newClient = mainServer.serverSocket.accept();
				newClient.setTcpNoDelay(true);
				mainServer.clientAccepted(newClient);
				
				Thread.sleep(1);
			} catch(Exception e) {
				mainServer.host.engineException(e);
			}
		}
	}
}