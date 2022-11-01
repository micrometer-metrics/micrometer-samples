package org.springframework.cloud.bus.event;

public class MyEvent extends RemoteApplicationEvent {

	public MyEvent(Object source, String originService) {
		super(source, originService);
	}

	MyEvent(Object source, String originService, String destinationService) {
		super(source, originService, destinationService);
	}

	MyEvent() {
	}
}
