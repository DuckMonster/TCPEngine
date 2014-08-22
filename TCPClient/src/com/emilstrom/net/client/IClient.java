package com.emilstrom.net.client;

public interface IClient {
	public void serverConnected();
	public void serverMessage(MessageBuffer msg);
	public void serverDisconnected();
	public void engineException(Exception e);
}