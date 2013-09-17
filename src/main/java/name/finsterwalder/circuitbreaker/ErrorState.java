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

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;


/**
 * @author mfinsterwalder
 * @since 2013-01-23 10:56
 */
public class ErrorState {

	private final int id;
	private final String description;
	private final Retry retry;
	private final Collection<Runnable> resumes = new ConcurrentLinkedQueue<>();
	private final Collection<ErrorStateListener> listeners = new ConcurrentLinkedQueue<>();
	private AtomicLong raised = new AtomicLong();
	private RetryScheduler retryScheduler = new RetryScheduler();
	private final long delay;
	private final TimeUnit timeUnit;

	public ErrorState(int id, String description, Retry retry, long delay, TimeUnit timeUnit, ErrorStateListener... errorStateListeners) {
		Ensure.notEmpty(description, "description");
		Ensure.notNull(retry, "retry");
		this.id = id;
		this.description = description;
		this.retry = retry;
		for (ErrorStateListener listener : errorStateListeners) {
			this.listeners.add(listener);
		}
		this.delay = delay;
		this.timeUnit = timeUnit;
	}

	public ErrorState(int id, String description, Retry retry, ErrorStateListener... errorStateListeners) {
		this(id, description, retry, 2, TimeUnit.SECONDS, errorStateListeners);
	}

	public int getId() {
		return id;
	}

	public String getDescription() {
		return description;
	}

	@Override
	public String toString() {
		return description;
	}

	public boolean isRaised() {
		return raised.get() > 0;
	}

	public long raisedCount() {
		return raised.get();
	}

	public void raise() {
		long value = raised.incrementAndGet();
		if (value == 1) {
			retryScheduler.schedule(this, delay, timeUnit);
			notifyListenersOfRaise();
		}
	}

	public void raise(final Runnable resume) {
		resumes.add(resume);
		raise();
	}

	private void notifyListenersOfRaise() {
		for (ErrorStateListener listener : listeners) {
			listener.raise(this);
		}
	}

	private void notifyListenersOfClear() {
		for (ErrorStateListener listener : listeners) {
			listener.clear(this);
		}
	}

	public boolean retrySuccessful() {
		boolean successful = retry.retrySuccessful();
		if (successful) {
			clear();
		}
		return successful;
	}

	public void clear() {
		long oldValue = raised.getAndSet(0);
		if (oldValue > 0) {
			for (Runnable resume : resumes) {
				resume.run();
			}
			notifyListenersOfClear();
		}
	}

	public void shutdown() {
		retryScheduler.shutdown();
	}

	public void setRetryScheduler(final RetryScheduler retryScheduler) {
		this.retryScheduler = retryScheduler;
	}
}
