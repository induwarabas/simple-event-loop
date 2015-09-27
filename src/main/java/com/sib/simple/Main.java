package com.sib.simple;


import com.sib.simple.eventloop.SimpleEventLoop;
import com.sib.simple.eventloop.SimpleTimer;
import com.sib.simple.eventloop.SimpleTimerCallback;
import com.sib.simple.network.Client;
import com.sib.simple.network.ClientCallback;
import com.sib.simple.network.Server;
import com.sib.simple.network.ServerCallback;

import java.io.IOException;

/**
 * Created by Supun on 7/18/2015
 */
public class Main {
	public static void main(String[] args) throws Exception {
		SimpleEventLoop l1 = new SimpleEventLoop();
		l1.start();

		SimpleEventLoop l2 = new SimpleEventLoop();
		l2.start();

		Server server = l1.createServer(new ServerCallback() {
			public void onData(Server server, Client client, Object message) {
				System.out.println("onData(Server server, Client client, Object message)");
			}

			public void onDisconnected(Server server, Client client) {
				System.out.println("onDisconnected(Server server, Client client) ");
			}

			public void onConnect(Server server, Client client) {
				System.out.println("onConnect(Server server, Client client)");
			}
		});
		server.start(5000);

		final Client client = l2.createClient(new ClientCallback() {
			public void onData(Client client, Object message) {
				System.out.println("onData(Client client, Object message)");
			}

			public void onDisconnected(Client client) {
				System.out.println("onDisconnected(Client client)");
			}

			public void onConnect(Client client) {
				System.out.println("onConnect(Client client)");
			}
		});

		client.connect("localhost",5000);
		client.waitForConnect();
		l2.createTimer(new SimpleTimerCallback() {
			public void onTimer(SimpleTimer timer) {
				try {
					client.send("GRRR");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}).start(1000);

	}
}
