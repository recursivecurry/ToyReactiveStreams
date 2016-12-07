package rx;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Data
public class ToySubscription implements Subscription {
    private final static int MAX_SLEEP_DURATION = 1000;
    private Subscriber<? super Integer> subscriber;
    private int remained;
    private int error;
    private int complete;
    private boolean active = false;
    private boolean cancelled = false;
    private Random random = new Random(System.currentTimeMillis());
    private long requestedCount = 0L;
    private ExecutorService executorService;

    public ToySubscription(Subscriber<? super Integer> subscriber, int remained, int error, int complete) {
        this.subscriber = subscriber;
        this.remained = remained;
        this.error = error;
        this.complete = complete;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    public void request(long n) {
        log.info("request({}) {}", n, remained);

        if (n <= 0) {
            log.info("request({}) exit0 {}", n, remained);

            subscriber.onError(new IllegalArgumentException("3.9"));
            return;
        }

        if (cancelled) {
            log.info("request({}) exit1 {}", n, remained);
            return;
        }

        requestedCount += n;

        if (active) {
            log.info("{} - request({}) exit2 {}", Thread.currentThread().getName(), n, remained);
            return;
        }

        active = true;

        executorService.submit(() -> {
            while (remained > 0) {
                if (requestedCount > 0) {
                    if (remained == error) {
                        subscriber.onError(new RuntimeException("Error!"));
                        return;
                    } else if (remained == complete) {
                        subscriber.onComplete();
                        return;
                    } else {
                        log.info("{} - subscribe.onNext BEFORE", Thread.currentThread().getName());
                        subscriber.onNext(remained);
                        log.info("subscribe.onNext AFTER");
                    }
                    requestedCount--;
                    remained--;
                }
//                int duration = random.nextInt(MAX_SLEEP_DURATION)+1;
//                try {
//                    Thread.sleep((long)duration);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
            }
            subscriber.onComplete();
        });
    }

    @Override
    public void cancel() {
        if (cancelled) {
            return;
        }
        cancelled = true;
        subscriber = null;

    }
}
