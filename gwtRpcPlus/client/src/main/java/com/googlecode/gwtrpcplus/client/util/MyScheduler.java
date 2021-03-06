package com.googlecode.gwtrpcplus.client.util;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;

public interface MyScheduler {

	class DefaultScheduler implements MyScheduler {
		public DefaultScheduler() {
		}

		@Override
		public void scheduleFinaly(ScheduledCommand cmd) {
			Scheduler.get().scheduleFinally(cmd);
		}
	}

	void scheduleFinaly(ScheduledCommand cmd);
}
