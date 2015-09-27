package com.sib.simple.network;

/**
 * Created by Supun on 7/15/2015
 */
public interface ClientCallback {
	void onData(Client client, Object message);
	void onDisconnected(Client client);
	void onConnect(Client client);
}
