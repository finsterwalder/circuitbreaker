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

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;


/**
 * @author mfinsterwalder
 * @since 2013-02-01 08:30
 */
public class ErrorStateTest {

	private static final int ID = 4;
	private static final String DESCRIPTION = "error";
	private final ErrorStateListener listenerMock = mock(ErrorStateListener.class);
	private final RetryScheduler retrySchedulerMock = mock(RetryScheduler.class);
	private final ErrorState errorState = new ErrorState(ID, DESCRIPTION, new Retry() {

		@Override
		public boolean retrySuccessful() {
			return true;
		}
	}, listenerMock);

	@Before
	public void before() {
		errorState.setRetryScheduler(retrySchedulerMock);
	}

	@Test
	public void create() {
		assertThat(errorState.getId(), is(ID));
		assertThat(errorState.getDescription(), is(DESCRIPTION));
		assertThat(errorState.isRaised(), is(false));
		assertThat(errorState.raisedCount(), is(0L));
	}

	@Test
	public void raisingAnErrorStateIncreasesCounter() {
		errorState.raise();
		assertThat(errorState.isRaised(), is(true));
		assertThat(errorState.raisedCount(), is(1L));

		errorState.raise();
		assertThat(errorState.isRaised(), is(true));
		assertThat(errorState.raisedCount(), is(2L));
	}

	@Test
	public void raisingAnErrorNotifiesListenersAndSchedulesRetry() {
		errorState.raise();
		verify(listenerMock).raise(errorState);
		verify(retrySchedulerMock).schedule(errorState, 2, TimeUnit.SECONDS);
	}

	@Test
	public void clearingAnAlreadyClearedErrorStateDoesNothing() {
		errorState.clear();
		assertThat(errorState.isRaised(), is(false));
		assertThat(errorState.raisedCount(), is(0L));
		verifyZeroInteractions(retrySchedulerMock);
		verifyZeroInteractions(listenerMock);
	}

	@Test
	public void clearingARaisedErrorStateCallsResumes() {
		Runnable runnable = mock(Runnable.class);
		errorState.raise(runnable);
		errorState.raise(runnable);
		errorState.clear();
		verify(runnable, times(2)).run();
		verify(listenerMock).clear(errorState);
	}
}
