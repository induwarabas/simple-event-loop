package com.sib.simple.network;

import com.sib.simple.eventloop.SimpleEventLoop;
import com.sib.simple.network.msg.HBMsg;
import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.serialization.ObjectSerializationCodecFactory;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.joda.time.DateTime;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;

/**
 * Created by Supun on 7/14/2015
 */
public class Client implements IoHandler {

	public static class Event {

		private final Client client;
		private final ClientCallback callback;

		public Event(Client client, ClientCallback callback) {
			this.client = client;
			this.callback = callback;
		}

		public Client getClient() {
			return client;
		}

		public ClientCallback getCallback() {
			return callback;
		}
	}

	public static class EventOnDisconnect extends Event {

		public EventOnDisconnect(Client client, ClientCallback callback) {
			super(client, callback);
		}
	}

	public static class EventOnConnect extends Event {

		public EventOnConnect(Client client, ClientCallback callback) {
			super(client, callback);
		}
	}

	public static class EventOnData extends Event {
		private final Object data;

		public EventOnData(Client client, ClientCallback callback, Object data) {
			super(client, callback);
			this.data = data;
		}

		public Object getData() {
			return data;
		}
	}

	private String name = "";
	private String host;
	private int port;
	NioSocketConnector connector;
	IoSession session;
	ClientCallback callback;
	ServerCallback serverCallback;
	Server server;
	private final SimpleEventLoop loop;
	ConnectFuture future;

	public Client(SimpleEventLoop loop, ClientCallback callback) {
		this.callback = callback;
		this.loop = loop;
	}

	public Client(SimpleEventLoop loop, Server server, ServerCallback callback, IoSession session) throws IOException {
		this.loop = loop;
		this.server = server;
		this.serverCallback = callback;
		this.session = session;
		System.out.println(host + ":" + port);
		loop.queue(new Server.EventOnConnect(server, this, serverCallback));
	}

	public void connect(String host, int port) throws IOException, ExecutionException, InterruptedException {
		this.host = host;
		this.port = port;
		connector = new NioSocketConnector();
		connector.setConnectTimeoutMillis(4000);
		connector.getFilterChain().addLast("codec",
				new ProtocolCodecFilter(new ObjectSerializationCodecFactory()));
		connector.getSessionConfig().setIdleTime(IdleStatus.WRITER_IDLE, 10);
		connector.getSessionConfig().setIdleTime(IdleStatus.READER_IDLE, 10);
		connector.getSessionConfig().setReceiveBufferSize(2048);
		connector.setHandler(this);
		System.out.println("Connecting.. " + host + ":" + port + DateTime.now().toString());
		future = connector.connect(new InetSocketAddress(host, port));
	}

	public void waitForConnect() throws Exception {
		future.awaitUninterruptibly();
		session = future.getSession();
	}

	public void onEvent(Event event) {
		if (event instanceof EventOnConnect) {
			event.getCallback().onConnect(event.getClient());
		} else if (event instanceof EventOnDisconnect) {
			event.getCallback().onDisconnected(event.getClient());
		} else if (event instanceof EventOnData) {
			EventOnData data = (EventOnData) event;
			event.getCallback().onData(event.getClient(), data.getData());
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public synchronized void send(Object object) throws IOException {
		session.write(object);
	}

	public synchronized void disconnect() throws IOException {
		session.close(true);
	}

	@Override
	public void sessionCreated(IoSession session) throws Exception {

	}

	@Override
	public void sessionOpened(IoSession session) throws Exception {
		this.session = session;
		loop.queue(new Client.EventOnConnect(this, callback));
	}

	@Override
	public void sessionClosed(IoSession session) throws Exception {
		loop.queue(new Client.EventOnDisconnect(this, callback));
		connector.dispose();
	}

	@Override
	public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
		//System.out.println("CLIENT: IDLE " + status + " " + session.getAttribute("idle"));
		//System.out.println("Client: " + status + " " + session.getAttribute("idle") + " IIDDDEEEELLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLL");
		if (status == IdleStatus.WRITER_IDLE) {
			session.write(new HBMsg());
		}
		if (status == IdleStatus.READER_IDLE) {
			Integer idle = (Integer)session.getAttribute("idle");
			if (idle == null) {
				idle = 0;
			}
			++idle;
			session.setAttribute("idle", idle);
			if (idle == 5) {
				//System.out.println("Client: CCCLLLOOOSSSINNNGGGG Session due to inactivity.");
				session.close(true);
			}
		}
	}

	@Override
	public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
		session.close(true);
	}

	@Override
	public void messageReceived(IoSession session, Object message) throws Exception {
		session.setAttribute("idle", 0);
		if (message instanceof HBMsg) {
			return;
		}
		loop.queue(new Client.EventOnData(this, callback, message));
	}

	@Override
	public void messageSent(IoSession session, Object message) throws Exception {

	}

	@Override
	public void inputClosed(IoSession session) throws Exception {
		session.close(true);
	}
}
