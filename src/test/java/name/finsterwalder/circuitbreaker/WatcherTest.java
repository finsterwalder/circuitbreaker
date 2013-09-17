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

import org.junit.After;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


/**
 * @author mfinsterwalder
 * @since 2013-01-23 12:51
 */
public class WatcherTest {

	Watcher watcher = new Watcher();

	@After
	public void after() {
		watcher.shutdown();
	}

	@Test
	public void successfullRetryCancelsRetryAndCallsResume() throws InterruptedException {
		CountingRetry retry = new CountingRetry(1);
		MemorizingResume resume = new MemorizingResume();
		watcher.register(new ErrorState(1, "error", retry, 10, TimeUnit.MILLISECONDS));
		watcher.signal(1, resume);
		assertThat(resume.resumed, is(false));
		int count = 0;
		while (!resume.resumed && count < 50) {
			count++;
			Thread.sleep(3);
		}
		assertThat(resume.resumed, is(true));
		assertThat(retry.count, is(1));
	}

	@Test
	public void unsuccessfullRetryschedulesAnotherRetry() throws InterruptedException {
		CountingRetry retry = new CountingRetry(3);
		MemorizingResume resume = new MemorizingResume();
		watcher.register(new ErrorState(2, "error", retry, 10, TimeUnit.MILLISECONDS));
		watcher.signal(2, resume);
		assertThat(resume.resumed, is(false));
		int count = 0;
		while (!resume.resumed && count < 500) {
			count++;
			Thread.sleep(3);
		}
		assertThat(resume.resumed, is(true));
		assertThat(retry.count, is(3));
	}

	private class CountingRetry implements Retry {
		private int count = 0;
		private final int maxCount;

		public CountingRetry(int maxCount) {
			this.maxCount = maxCount;
		}

		@Override
		public boolean retrySuccessful() {
			return ++count >= maxCount;
		}
	}

	private class MemorizingResume implements Runnable {
		private boolean resumed;

		@Override
		public void run() {
			resumed = true;
		}
	}
}
