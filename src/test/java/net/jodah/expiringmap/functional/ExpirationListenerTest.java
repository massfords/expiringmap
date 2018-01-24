package net.jodah.expiringmap.functional;

import net.jodah.concurrentunit.Waiter;
import net.jodah.expiringmap.ExpiringMap;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;

public class ExpirationListenerTest {
    private Waiter waiter;

    @BeforeMethod
    protected void beforeMethod() {
        waiter = new Waiter();
    }

    /**
     * Tests that an expiration listener is called as expected.
     */
    @Test(priority = 100)
    public void shouldCallExpirationListener() throws Throwable {
        final String key = "a";
        final String value = "v";

        Map<String, String> map = ExpiringMap.builder()
                .expiration(100, TimeUnit.MILLISECONDS)
                .expirationListener((thekey, thevalue) -> {
                    waiter.assertEquals(key, thekey);
                    waiter.assertEquals(value, thevalue);
                    waiter.resume();
                })
                .build();

        map.put(key, value);

        waiter.await(5000);
    }

    /**
     * Tests that an async expiration listener is called as expected.
     */
    @Test(priority = 100)
    public void shouldCallAsyncExpirationListener() throws Throwable {
        final String key = "a";
        final String value = "v";

        Map<String, String> map = ExpiringMap.builder()
                .expiration(100, TimeUnit.MILLISECONDS)
                .asyncExpirationListener((thekey, thevalue) -> {
                    waiter.assertEquals(key, thekey);
                    waiter.assertEquals(value, thevalue);
                    waiter.resume();
                })
                .build();

        map.put(key, value);

        waiter.await(5000);
    }

    @Test(priority = 10)
    public void asyncListenerRegisteredAfterBuilder() throws Throwable {
        final String key = "a";
        final String value = "v";

        ExpiringMap<String, String> map = ExpiringMap.builder()
                .expiration(100, TimeUnit.MILLISECONDS)
                .asyncExpirationListener((thekey, thevalue) -> {
                    waiter.assertEquals(key, thekey);
                    waiter.assertEquals(value, thevalue);
                    waiter.resume();
                })
                .build();

        Waiter postBuildWaiter = new Waiter();

        map.addAsyncExpirationListener((thekey, thevalue) -> {
            postBuildWaiter.assertEquals(key, thekey);
            postBuildWaiter.assertEquals(value, thevalue);
            postBuildWaiter.resume();
        });

        map.put(key, value);

        waiter.await(5000);
        postBuildWaiter.await(5000);
    }

    @Test(priority = 1)
    public void asyncListenerRegisteredAfterBuilder_noListenerOnBuilder() throws Throwable {
        final String key = "a";
        final String value = "v";

        ExpiringMap<String, String> map = ExpiringMap.builder()
                .expiration(100, TimeUnit.MILLISECONDS)
                .build();

        map.addAsyncExpirationListener((thekey, thevalue) -> {
            waiter.assertEquals(key, thekey);
            waiter.assertEquals(value, thevalue);
            waiter.resume();
        });

        map.put(key, value);

        waiter.await(5000);
    }

    @Test(priority = 1)
    public void asyncListenerRegisteredAfterBuilder_notExpiring() throws Throwable {

        CountDownLatch latch = new CountDownLatch(10);
        Set<Integer> expiredKeys = new HashSet<>();

        ExpiringMap<Integer, Integer> map = ExpiringMap.builder()
                .expiration(100, TimeUnit.MILLISECONDS)
                .build();

        map.addAsyncExpirationListener((thekey, thevalue) -> {
            expiredKeys.add(thekey);
            latch.countDown();
        });

        for (int i = 0; i < 10; i++) {
            map.put(i, i);
        }

        latch.await(5, TimeUnit.SECONDS);
        assertEquals(expiredKeys.size(), 10);
    }
}
