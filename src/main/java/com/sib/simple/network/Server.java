package com.sib.simple.network;

import com.sib.simple.eventloop.SimpleEventLoop;
import com.sib.simple.network.msg.HBMsg;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.serialization.ObjectSerializationCodecFactory;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Created by Supun on 7/18/2015
 */
public class Server implements IoHandler {
	ServerCallback callback;
	private final SimpleEventLoop loop;
	NioSocketAcceptor acceptor;



	public static class Event {

		private final Client client;
		private final Server server;
		private final ServerCallback callback;

		public Event(Server server, Client client, ServerCallback callback) {
			this.client = client;
			this.server = server;
			this.callback = callback;
		}

		public Client getClient() {
			return client;
		}

		public Server getServer() {
			return server;
		}

		public ServerCallback getCallback() {
			return callback;
		}
	}

	public static class EventOnDisconnect extends Event{

		public EventOnDisconnect(Server server, Client client, ServerCallback callback) {
			super(server, client, callback);
		}
	}

	public static class EventOnConnect extends Event{

		public EventOnConnect(Server server, Client client, ServerCallback callback) {
			super(server, client, callback);
		}
	}

	public static class EventOnData extends Event{
		private final Object data;
		public EventOnData(Server server, Client client, ServerCallback callback, Object data) {
			super(server, client, callback);
			this.data = data;
		}
		public Object getData() {
			return data;
		}
	}

	public Server(SimpleEventLoop loop, ServerCallback callback) {
		this.callback = callback;
		this.loop = loop;
	}

	public void start(int port) throws IOException {
		acceptor = new NioSocketAcceptor();
		acceptor.getFilterChain().addLast( "codec", new ProtocolCodecFilter(new ObjectSerializationCodecFactory()));
		acceptor.setHandler(this);
		acceptor.getSessionConfig().setReadBufferSize( 2048 );
		acceptor.getSessionConfig().setIdleTime( IdleStatus.WRITER_IDLE, 10);
		acceptor.getSessionConfig().setIdleTime( IdleStatus.READER_IDLE, 10);
		acceptor.setReuseAddress(true);
		acceptor.bind(new InetSocketAddress(port));
	}

	public void onEvent(Event event) {
		if (event instanceof EventOnConnect) {
			event.getCallback().onConnect(event.getServer(), event.getClient());
		} else if (event instanceof EventOnDisconnect) {
			event.getCallback().onDisconnected(event.getServer(), event.getClient());
		} else if (event instanceof EventOnData) {
			EventOnData data = (EventOnData) event;
			event.getCallback().onData(event.getServer(), event.getClient(), data.getData());
		}
	}

	public void stop() throws IOException {
		if (acceptor != null) {
			acceptor.dispose();
			acceptor = null;
		}
	}

	@Override
	public void sessionCreated(IoSession session) throws Exception {
		Client c = new Client(loop, this, callback, session);
		session.setAttribute("client", c);
	}

	@Override
	public void sessionOpened(IoSession session) throws Exception {

	}

	@Override
	public void sessionClosed(IoSession session) throws Exception {
		loop.queue(new Server.EventOnDisconnect(this, (Client)session.getAttribute("client"), callback));
	}

	@Override
	public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
		//System.out.println("SERVER: IDLE " + status + " " + session.getAttribute("idle"));
		//System.out.println("Server: " + status + " " + session.getAttribute("idle") + " IIDDDEEEELLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLL");
		if (status == IdleStatus.WRITER_IDLE || status == IdleStatus.BOTH_IDLE) {
			session.write(new HBMsg());
		}
		if (status == IdleStatus.READER_IDLE || status == IdleStatus.BOTH_IDLE) {
			Integer idle = (Integer)session.getAttribute("idle");
			if (idle == null) {
				idle = 0;
			}
			++idle;
			session.setAttribute("idle", idle);
			if (idle == 5) {
				//System.out.println("Server: CCCLLLOOOSSSINNNGGGG Session due to inactivity.");
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
		loop.queue(new Server.EventOnData(this, (Client)session.getAttribute("client"), callback, message));
	}

	@Override
	public void messageSent(IoSession session, Object message) throws Exception {

	}

	@Override
	public void inputClosed(IoSession session) throws Exception {
		session.close(true);
	}
}
