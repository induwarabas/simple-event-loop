package com.sib.simple.network;

/**
 * Created by Supun on 7/15/2015
 */
public interface ServerCallback {
	void onData(Server server, Client client, Object message);
	void onDisconnected(Server server, Client client);
	void onConnect(Server server, Client client);
}
