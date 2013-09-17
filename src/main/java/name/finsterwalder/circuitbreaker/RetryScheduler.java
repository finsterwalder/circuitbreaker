/*
 * CircuitBreaker - A tool to manage interuptions in software services
 * Copyright (C) 2013 Malte Finsterwalder
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package name.finsterwalder.circuitbreaker;

import name.finsterwalder.utils.Ensure;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


/**
 * @author mfinsterwalder
 * @since 2013-01-30 21:06
 */
public class RetryScheduler {

	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

	public void schedule(final ErrorState errorState, final long delay, final TimeUnit timeUnit) {
		CancelRetryOnSuccessRunnable runnable = new CancelRetryOnSuccessRunnable(errorState);
		final ScheduledFuture<?> scheduledFuture = scheduler.scheduleWithFixedDelay(runnable, 0, delay, timeUnit);
		runnable.setScheduledFuture(scheduledFuture);
	}

	private static class CancelRetryOnSuccessRunnable implements Runnable {

		private volatile ScheduledFuture<?> scheduledFuture;
		private final ErrorState errorState;

		private CancelRetryOnSuccessRunnable(final ErrorState errorState) {
			Ensure.notNull(errorState, "errorState");
			this.errorState = errorState;
		}

		@Override
		public void run() {
			if (errorState.retrySuccessful()) {
				scheduledFuture.cancel(false);
			}
		}

		public void setScheduledFuture(final ScheduledFuture<?> scheduledFuture) {
			this.scheduledFuture = scheduledFuture;
		}
	}

	public void shutdown() {
		scheduler.shutdown();
	}
}
