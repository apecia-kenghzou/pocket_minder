package com.pocketminder;

import com.pocketminder.util.ErrorHandler;

import org.junit.Test;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import static org.junit.Assert.*;

/**
 * Unit tests for ErrorHandler
 */
public class ErrorHandlerTest {

    @Test
    public void testRetryOperationSuccess() throws Exception {
        int[] attempt = {0};

        String result = ErrorHandler.retryOperation(() -> {
            attempt[0]++;
            if (attempt[0] < 2) {
                throw new IOException("Test error");
            }
            return "Success";
        }, 3);

        assertEquals("Should succeed on second attempt", "Success", result);
        assertEquals("Should have 2 attempts", 2, attempt[0]);
    }

    @Test(expected = IOException.class)
    public void testRetryOperationFailure() throws Exception {
        ErrorHandler.retryOperation(() -> {
            throw new IOException("Test error");
        }, 3);
    }

    @Test
    public void testIsRecoverable() {
        assertTrue("IOException should be recoverable",
                ErrorHandler.isRecoverable(new IOException()));
        assertTrue("SocketTimeoutException should be recoverable",
                ErrorHandler.isRecoverable(new SocketTimeoutException()));
        assertTrue("UnknownHostException should be recoverable",
                ErrorHandler.isRecoverable(new UnknownHostException()));
        assertFalse("NullPointerException should not be recoverable",
                ErrorHandler.isRecoverable(new NullPointerException()));
    }

    @Test
    public void testRetryWithExponentialBackoff() throws Exception {
        int[] attempt = {0};
        long[] times = new long[3];

        try {
            ErrorHandler.retryOperation(() -> {
                times[attempt[0]] = System.currentTimeMillis();
                attempt[0]++;
                throw new IOException("Test error");
            }, 3);
        } catch (IOException e) {
            // Expected
        }

        assertEquals("Should have 3 attempts", 3, attempt[0]);

        // Check exponential backoff (allowing some tolerance)
        if (times.length >= 2) {
            long delay1 = times[1] - times[0];
            assertTrue("First retry should wait ~1 second", delay1 >= 900);
        }
    }
}
