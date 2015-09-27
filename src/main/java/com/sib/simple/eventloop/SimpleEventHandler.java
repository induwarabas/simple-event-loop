package com.sib.simple.eventloop;

/**
 * Created by Supun on 7/18/2015
 */
public interface SimpleEventHandler {
	void onEvent(SimpleEventLoop eventLoop, Object data);
}
