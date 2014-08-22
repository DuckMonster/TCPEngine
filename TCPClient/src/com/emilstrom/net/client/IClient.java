package com.emilstrom.net.client;

import java.io.DataInputStream;

public interface IClient {
	public void serverConnected();
	public void serverMessage(MessageBuffer msg);
	public void serverDisconnected();
	public void engineException(Exception e);
}