package com.sib.simple.eventloop;

import com.sib.simple.network.Client;
import com.sib.simple.network.ClientCallback;
import com.sib.simple.network.Server;
import com.sib.simple.network.ServerCallback;

import java.util.*;

/**
 * Created by Supun on 7/17/2015
 */
public class SimpleEventLoop implements Runnable {
	private static class Data {
		public final Queue<Object> tqueue = new LinkedList<>();
		Set<SimpleEventHandler> callback = new HashSet<>();
		private boolean isActive = false;
	}

	final Data data;

	String name = UUID.randomUUID().toString();

	public SimpleEventLoop() {
		this.data = new Data();
	}

	private SimpleEventLoop(Data data) {
		this.data = data;
	}

	public void addEventHandler(SimpleEventHandler handler) {
		this.data.callback.add(handler);
	}

	public SimpleTimer createTimer(SimpleTimerCallback timerCallback) {
		return new SimpleTimer(this, timerCallback);
	}

	public Server createServer(ServerCallback callback) {
		return new Server(this, callback);
	}

	public Client createClient(ClientCallback callback) {
		return new Client(this, callback);
	}

	public void start() {
		if (!data.isActive) {
			new Thread(this).start();
		}
	}

	public void stop() {
		data.isActive = false;
		synchronized (data.tqueue) {
			data.tqueue.notify();
		}
	}

	public void run() {
		data.isActive = true;
		boolean breakBySleep = false;
		while (data.isActive) {
			while (true) {
				Object obj;
				synchronized (data.tqueue) {
					obj = data.tqueue.poll();
				}
				if (obj == null) {
					break;
				}
				if (obj instanceof SimpleTimer.Event) {
					SimpleTimer.Event event = (SimpleTimer.Event)obj;
					event.getTimer().getCallback().onTimer(event.getTimer());
				} else if (obj instanceof Server.Event) {
					Server.Event serverEvent = (Server.Event)obj;
					serverEvent.getServer().onEvent(serverEvent);
				}else if (obj instanceof Client.Event) {
					Client.Event clientEvent = (Client.Event)obj;
					clientEvent.getClient().onEvent(clientEvent);
				} else if(obj instanceof SleepEvent) {
					SleepEvent event = (SleepEvent)obj;
					event.setCompleted(true);
					synchronized (event) {
						event.notify();
					}
					breakBySleep = true;
					break;
				} else if (data.callback != null){
					for (SimpleEventHandler handler : data.callback) {
						handler.onEvent(this, obj);
					}
				}
				synchronized (data.tqueue) {
					if (data.tqueue.isEmpty()) {
						break;
					}
				}
			}
			if (breakBySleep) {
				break;
			}
			try {
				synchronized (data.tqueue) {
					data.tqueue.wait();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void queue(Object object) {
		if (!data.isActive) {
			return;
		}
		if (object == null) {
			throw new NullPointerException("Cannot queue a null object");
		}
		synchronized (data.tqueue) {
			data.tqueue.add(object);
			data.tqueue.notify();
		}
	}

	public void sleep(long mills) {
		SleepEvent event = new SleepEvent();
		createTimer(new SimpleTimerCallback() {
			@Override
			public void onTimer(SimpleTimer timer) {
				queue(event);
			}
		}).singleShot(mills);
		new Thread(new SimpleEventLoop(data)).start();
		while (!event.isCompleted()) {
			try {
				synchronized (event) {
					event.wait();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
