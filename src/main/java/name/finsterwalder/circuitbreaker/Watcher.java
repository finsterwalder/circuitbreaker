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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


/**
 * @author mfinsterwalder
 * @since 2013-01-23 11:23
 */
public class Watcher {

	/*
	 * Things to do
	 *
	 * ErrorStateListener-Interface signalNth()?
	 * Consider thread safety!
	 * Resume with timeout?
	 */

	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(0);
	private final Map<Integer, ErrorState> errorStateMap = new HashMap<>();

	public <T extends Throwable> void signalAndThrow(final int errorStateId, final T exception) throws T {
		signal(errorStateId);
		throw exception;
	}

	public <T extends Throwable> void signalAndThrow(final int errorStateId, final Runnable resume, final T exception) throws T {
		signal(errorStateId, resume);
		throw exception;
	}

	public void signal(final int errorStateId) {
		ErrorState errorState = errorStateMap.get(errorStateId);
		errorState.raise();
	}

	public void signal(final int errorStateId, final Runnable resume) {
		ErrorState errorState = errorStateMap.get(errorStateId);
		errorState.raise(resume);
	}

	public void register(final ErrorState errorState) {
		Ensure.that(!errorStateMap.containsKey(errorState.getId()), "ErrorState with ID " + errorState.getId() + " not yet registered");
		errorStateMap.put(errorState.getId(), errorState);
	}

	public void clear(final int errorStateId) {
		// TODO implement
	}

	public void shutdown() {
		for (ErrorState errorState : errorStateMap.values()) {
			errorState.shutdown();
		}
	}
}
