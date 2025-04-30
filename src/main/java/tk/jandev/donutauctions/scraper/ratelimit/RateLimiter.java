package tk.jandev.donutauctions.scraper.ratelimit;

import java.util.concurrent.ConcurrentLinkedQueue;

public class RateLimiter {
    private final int maxRequests;
    private final long periodMillis;
    private final ConcurrentLinkedQueue<Long> timestamps = new ConcurrentLinkedQueue<>(); // represents a list of the timestamps of all requests made in the last periodSeconds

    public RateLimiter(int maxRequests, int periodSeconds) {
        this.maxRequests = maxRequests;
        this.periodMillis = periodSeconds * 1000L;
    }

    public void acquire() { // blocks the current thread until the global ratelimit allows for more requests
        while (true) {
            long now = System.currentTimeMillis();
            cleanRequestsOlderThanPeriod(now);

            if (timestamps.size() < maxRequests) { // remove all requests that were not made within the last periodSeconds
                timestamps.add(now);
                return;
            }

            try {
                Thread.sleep(10); // arbitrary sleep time, 10 ms because why not
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void cleanRequestsOlderThanPeriod(long now) {
        while (!timestamps.isEmpty() && (now - timestamps.peek()) > periodMillis) {
            timestamps.poll();
        }
    }
}
