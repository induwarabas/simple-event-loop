package com.sib.simple.eventloop;

/**
 * Created by Supun on 7/18/2015
 */
public class SimpleTimer {
	class Event {
		private final SimpleTimer timer;

		Event(SimpleTimer timer) {
			this.timer = timer;
		}

		public SimpleTimer getTimer() {
			return timer;
		}
	}

	private SimpleEventLoop eventLoop;
	long mills;
	private boolean isActive = false;
	private final SimpleTimerCallback callback;

	public SimpleTimer(SimpleEventLoop eventLoop, SimpleTimerCallback callback) {
		this.eventLoop = eventLoop;
		this.callback = callback;
	}

	public void stop() {
		isActive = false;
	}

	SimpleTimerCallback getCallback() {
		return callback;
	}

	public void singleShot(final long mills) {
		final SimpleTimer st = this;
		isActive = true;
		new Thread(new Runnable() {
			public void run() {
				if (isActive) {
					try {
						Thread.sleep(mills);
						isActive = false;
						eventLoop.queue(new Event(st));
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				isActive = false;
			}
		}).start();
	}

	public void start(final long mills) {
		this.mills = mills;
		isActive = true;
		final SimpleTimer st = this;
		new Thread(new Runnable() {
			public void run() {
				while (isActive) {
					try {
						eventLoop.queue(new Event(st));
						Thread.sleep(mills);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
	}

}
