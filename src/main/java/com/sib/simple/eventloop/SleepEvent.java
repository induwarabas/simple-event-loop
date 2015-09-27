package com.sib.simple.eventloop;

/**
 * Created by Supun on 7/18/2015
 */
public class SleepEvent {
	private boolean completed = false;

	public SleepEvent() {

	}

	public boolean isCompleted() {
		return completed;
	}

	public void setCompleted(boolean completed) {
		this.completed = completed;
	}
}
