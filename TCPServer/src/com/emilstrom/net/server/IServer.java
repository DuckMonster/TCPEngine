package com.emilstrom.net.server;

public interface IServer {
	public void clientConnected(int id);
	public void clientMessage(int id, MessageBuffer stream);
	public void clientDisconnected(int id);
	public void engineException(Exception e);
}