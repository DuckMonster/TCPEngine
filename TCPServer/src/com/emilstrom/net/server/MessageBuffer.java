package com.emilstrom.net.server;

import java.io.*;
import java.util.*;

public class MessageBuffer {
	public static final int TYPE_INT = 0,
			TYPE_BYTE = 1,
			TYPE_BOOL = 2,
			TYPE_WORD = 3;
	
	List<Object> buffer;
	List<Integer> bufferType;
	
	public int playerID;
	
	public MessageBuffer() {
		buffer = new ArrayList<Object>();
		bufferType = new ArrayList<Integer>();
	}
	public MessageBuffer(int id) {
		playerID = id;
		
		buffer = new ArrayList<Object>();
		bufferType = new ArrayList<Integer>();
	}
	
	public int getSize() {
		int s = 0;
		for(int i=0; i<buffer.size(); i++) {
			switch(bufferType.get(i)) {
				case TYPE_INT:
					s += 4;
					break;
					
				case TYPE_WORD:
					s += 2;
					break;
					
				case TYPE_BYTE:
					s += 1;
					break;
					
				case TYPE_BOOL:
					s += 1;
					break;
			}
		}
		
		return s;
	}
	
	public void add(Object o, int t) {
		buffer.add(o);
		bufferType.add(t);
	}
	
	public void addInt(int i) {
		add(i, TYPE_INT);
	}
	
	public void addByte(byte b) {
		add(b, TYPE_BYTE);
	}
	public void addByte(int b) {
		add((byte)b, TYPE_BYTE);
	}
	
	public void addBool(boolean b) {
		add(b, TYPE_BOOL);
	}
	
	public void addWord(int w) {
		add(w, TYPE_WORD);
	}
	
	public void addString(String s) {
		addWord(s.length());

		char[] c = s.toCharArray();
		
		for(char cc : c) addByte(cc);
	}
	
	public byte readByte() {
		byte b = (byte)buffer.get(0);
		buffer.remove(0);
		
		return b;
	}
	
	public boolean readBool() {
		return (readByte() % 2) == 1;
	}
	
	public int readInt() {
		if (buffer.size() < 4) return -1;
		
		byte[] bytes = new byte[4];
		for(int i=0; i<4; i++) {
			bytes[i] = (byte)buffer.get(0);
			buffer.remove(0);
		}
		
		int i = (((0xFF & bytes[0]) << 24) | ((0xFF & bytes[1]) << 16) | ((0xFF & bytes[2]) << 8) | ((0xFF & bytes[3])));
		return i;
	}
	public int readWord() {
		if (buffer.size() < 2) return -1;
		
		byte[] bytes = new byte[2];
		for(int i=0; i<2; i++) {
			bytes[i] = (byte)buffer.get(0);
			buffer.remove(0);
		}
		
		int i = (((0xFF & bytes[0]) << 8) | (0xFF & bytes[1]));
		return i;
	}
	public String readString() {
		int n = readWord();

		String s = "";
		for(int i=0; i<n; i++) s += (char)readByte();

		return s;
	}
	
	public void copyStream(DataInputStream s, int n) {
		try {
			while(n > 0) {
				byte b = (byte)s.read();
				
				addByte(b);
				n--;
			}
		} catch(Exception e) { e.printStackTrace(); }
	}
	
	public MessageBuffer copy() {
		MessageBuffer ret = new MessageBuffer();
		
		for(int i=0; i<buffer.size(); i++) {
			ret.add(buffer.get(i), bufferType.get(i));
		}
		
		return ret;
	}
}