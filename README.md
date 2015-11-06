This is a simple event loop for multi-threaded java application. 

An event loop has its own java Thread and all callbacks are triggered in that thread context. It also contains a transfer queue implementation.
# simple-event-loop

###Communicating between two threads.
Communicating between two threads is simple. No locks required.
Here is a sample code to send an object to the event loop.
```java
// Creating new event loop
SimpleEventLoop s1 = new SimpleEventLoop();

// Starting. This will create a new thread.
s1.start();

// Add event handler for the event loop. This will receive Objects queue from anywhere.
s1.addEventHandler(new SimpleEventHandler() {
	@Override
	public void onEvent(SimpleEventLoop eventLoop, Object data) {
		if (data instanceof String) {
			// The String sent from main thread will receive here
			System.out.println("Received: " + data.toString());
		}
	}
});

// Sending String from main thread.
s1.queue("Hello");
```

###Creating timers
You can create timers in both singleShot and repeatable ways.

#####Single Shot timers.
These timers will call only once and these are not repeatable.
```java
// Creating new event loop
SimpleEventLoop s1 = new SimpleEventLoop();

// Starting. This will create a new thread.
s1.start();

// Creating a timer
SimpleTimer timer = s1.createTimer(new SimpleTimerCallback() {
	@Override
	public void onTimer(SimpleTimer timer) {
		System.out.println("I am in the timer");
	}
});

// Specify time interval in milliseconds and call singleShot
timer.singleShot(1000);
```

#####Repeating timers
These timers will repeat until stop it.
```java
// Creating new event loop
SimpleEventLoop loop = new SimpleEventLoop();

// Starting. This will create a new thread.
loop.start();

// Creating a timer
SimpleTimer timer = loop.createTimer(new SimpleTimerCallback() {
	@Override
	public void onTimer(SimpleTimer timer) {
		System.out.println("I am in the timer");
	}
});

// Specify time interval in milliseconds call start
timer.start(1000);
```

###Sleeping in events
If you call `Thread.sleep` in event callbacks, the event loop will get blocked. All other events will not trigger until you wake. But sometimes you may need to give a change to process another event while sleeping. You can do it by calling the `sleep` function of the event loop.
See the difference of two scenarios.

#####Scenario 1. Sleeping with Thread.sleep
```java
// Creating new event loop
SimpleEventLoop loop = new SimpleEventLoop();

// Starting. This will create a new thread.
loop.start();

// Creating a timer
SimpleTimer timer1 = loop.createTimer(new SimpleTimerCallback() {
	@Override
	public void onTimer(SimpleTimer timer) {
		System.out.println("I am going to sleep in timer 1");
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("I waked in timer 1");
	}
});
timer1.start(10000);

// Creating another timer
SimpleTimer timer2 = loop.createTimer(new SimpleTimerCallback() {
	@Override
	public void onTimer(SimpleTimer timer) {
		System.out.println("Grrrrr");
	}
});
timer2.start(1000);
```
Output
```
I am going to sleep in timer 1
I waked in timer 1
Grrrrr
Grrrrr
Grrrrr
Grrrrr
Grrrrr
Grrrrr
Grrrrr
Grrrrr
Grrrrr
Grrrrr
I am going to sleep in timer 1
I waked in timer 1
Grrrrr
Grrrrr
Grrrrr
Grrrrr
Grrrrr
Grrrrr
Grrrrr
Grrrrr
```
Here timer2 events queued until timer1 wake.

#####Scenario 2. Sleeping with loop.sleep
```java
// Creating new event loop
SimpleEventLoop loop = new SimpleEventLoop();

// Starting. This will create a new thread.
loop.start();

// Creating a timer
SimpleTimer timer1 = loop.createTimer(new SimpleTimerCallback() {
	@Override
	public void onTimer(SimpleTimer timer) {
		System.out.println("I am going to sleep in timer 1");
		loop.sleep(5000);
		System.out.println("I waked in timer 1");
	}
});
timer1.start(10000);

// Creating another timer
SimpleTimer timer2 = loop.createTimer(new SimpleTimerCallback() {
	@Override
	public void onTimer(SimpleTimer timer) {
		System.out.println("Grrrrr");
	}
});
timer2.start(1000);
```
Output
```
I am going to sleep in timer 1
Grrrrr
Grrrrr
Grrrrr
Grrrrr
Grrrrr
I waked in timer 1
Grrrrr
Grrrrr
Grrrrr
Grrrrr
Grrrrr
I am going to sleep in timer 1
Grrrrr
Grrrrr
Grrrrr
Grrrrr
Grrrrr
I waked in timer 1
```
Here the timer2 events triggered while timer1 is sleeping.

###Network communication
You can create a server client application easy using the event loop.
#####Creating a server
```java
SimpleEventLoop loop = new SimpleEventLoop();
loop.start();

Server server = loop.createServer(new ServerCallback() {
	@Override
	public void onData(Server server, Client client, Object message) {
		System.out.println("Data from client: " + client.getName() + " : " + message.toString());
		try {
			client.send("Hello Client");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onDisconnected(Server server, Client client) {
		System.out.println("Client disconnected. " + client.getName());
	}

	@Override
	public void onConnect(Server server, Client client) {
		System.out.println("Connected new client. ");
		client.setName("Client1");
	}
});

try {
	server.start(3000);
} catch (IOException e) {
	e.printStackTrace();
}
```

#####Creating a client
```java
SimpleEventLoop loop2 = new SimpleEventLoop();
loop2.start();

Client client = loop2.createClient(new ClientCallback() {
	@Override
	public void onData(Client client, Object message) {
		System.out.println("Data from server: " + message);
	}

	@Override
	public void onDisconnected(Client client) {
		System.out.println("Client disconnected");
	}

	@Override
	public void onConnect(Client client) {
		System.out.println("Client connected");
	}
});
try {
	client.connect("localhost", 3000);
	client.waitForConnect();
	client.send("Hello Server");
} catch (Exception e) {
	e.printStackTrace();
}
```
