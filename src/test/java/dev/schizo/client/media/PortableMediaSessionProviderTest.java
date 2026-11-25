package dev.schizo.client.media;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

final class PortableMediaSessionProviderTest {
    @Test
    void closeOwnsActiveSourceExactlyOnce() throws Exception {
        BlockingSource source = new BlockingSource();
        PortableMediaSessionProvider provider = new PortableMediaSessionProvider(
            snapshot -> { },
            "Linux",
            () -> source,
            sink -> new PortableMediaSessionProvider.WindowsBackend() {
                @Override public void start() { }
                @Override public String lastFailure() { return null; }
                @Override public void close() { }
            }
        );

        provider.start();
        assertTrue(source.readStarted.await(1, TimeUnit.SECONDS));

        provider.close();

        assertEquals(1, source.closeCount.get());
        assertEquals(PortableMediaSessionProvider.Availability.CLOSED, provider.availability());

        provider.close();
        assertEquals(1, source.closeCount.get());
    }

    private static final class BlockingSource implements MediaSessionSource {
        private final CountDownLatch readStarted = new CountDownLatch(1);
        private final CountDownLatch releaseRead = new CountDownLatch(1);
        private final AtomicInteger closeCount = new AtomicInteger();

        @Override
        public MediaSnapshot read() throws IOException {
            this.readStarted.countDown();
            try {
                this.releaseRead.await();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
            return MediaSnapshot.empty();
        }

        @Override
        public void close() {
            this.closeCount.incrementAndGet();
            this.releaseRead.countDown();
        }
    }
}
