package com.sib.simple.eventloop;

/**
 * Created by Supun on 7/18/2015
 */
public class Main {
	public static void main(String[] args) {
		final SimpleEventLoop s1 = new SimpleEventLoop();
		s1.start();
		s1.createTimer(new SimpleTimerCallback() {
			@Override
			public void onTimer(SimpleTimer timer) {
				System.out.println("Hoooo");
				s1.sleep(5000);
				System.out.println("Booo");
			}
		}).start(3000);
		s1.createTimer(new SimpleTimerCallback() {
			@Override
			public void onTimer(SimpleTimer timer) {
				System.out.println("Sleeping1");
				s1.sleep(5000);
				System.out.println("Waked");

				System.out.println("Sleeping2");
				s1.sleep(5000);
				System.out.println("Waked2");
			}
		}).singleShot(1000);
	}
}
