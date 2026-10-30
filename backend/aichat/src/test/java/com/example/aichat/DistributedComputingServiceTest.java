package com.example.aichat;

import com.example.aichat.service.DistributedComputingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * B3: DistributedComputingService 线程安全测试。
 *
 * 验证：
 *   1. 100 线程并发入队出队无任务丢失
 *   2. 队列容量上限 + 拒绝策略生效
 *   3. getNextTask 按优先级取最高
 */
class DistributedComputingServiceTest {

    private DistributedComputingService service;

    @BeforeEach
    void setUp() {
        service = new DistributedComputingService();
    }

    @Test
    void 并发入队出队_100线程无丢失() throws InterruptedException {
        int producerCount = 50;
        int tasksPerProducer = 20; // 50*20=1000，刚好等于队列容量上限
        AtomicInteger submitted = new AtomicInteger(0);
        AtomicInteger consumed = new AtomicInteger(0);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch producersDone = new CountDownLatch(producerCount);

        // 消费者线程：持续出队
        Thread consumer = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                DistributedComputingService.ComputeTask task = service.getNextTask();
                if (task != null) {
                    consumed.incrementAndGet();
                } else {
                    try {
                        Thread.sleep(2);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        });
        consumer.start();

        // 生产者线程
        List<Thread> producers = new ArrayList<>();
        for (int t = 0; t < producerCount; t++) {
            Thread p = new Thread(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < tasksPerProducer; i++) {
                        try {
                            service.submitTask("compute", null,
                                    DistributedComputingService.ComputeTask.TaskPriority.NORMAL);
                            submitted.incrementAndGet();
                        } catch (IllegalStateException e) {
                            // 队列满被拒绝，不计入成功提交
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    producersDone.countDown();
                }
            });
            producers.add(p);
            p.start();
        }

        startLatch.countDown();
        assertTrue(producersDone.await(30, TimeUnit.SECONDS), "生产者应在 30s 内完成");

        // 等待队列被消费干净
        long deadline = System.currentTimeMillis() + 15000;
        while ((Integer) service.getSystemStats().get("queuedTasks") > 0
                && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
        // 再给消费者一点时间收尾
        Thread.sleep(300);
        consumer.interrupt();

        int s = submitted.get();
        int c = consumed.get();
        // 核心断言：成功入队的任务全部被消费，无丢失
        assertEquals(s, c, "成功提交的任务数(" + s + ")应等于消费数(" + c + ")，无丢失");
        assertTrue(s > 0, "应有成功提交的任务");
    }

    @Test
    void 队列容量上限_拒绝策略生效() {
        int maxCapacity = 1000;
        for (int i = 0; i < maxCapacity; i++) {
            service.submitTask("test", null,
                    DistributedComputingService.ComputeTask.TaskPriority.LOW);
        }
        // 队列已满，再提交应抛 IllegalStateException
        assertThrows(IllegalStateException.class, () ->
                service.submitTask("overflow", null,
                        DistributedComputingService.ComputeTask.TaskPriority.HIGH));
    }

    @Test
    void getNextTask_按优先级取最高() {
        service.submitTask("low", null, DistributedComputingService.ComputeTask.TaskPriority.LOW);
        service.submitTask("urgent", null, DistributedComputingService.ComputeTask.TaskPriority.URGENT);
        service.submitTask("normal", null, DistributedComputingService.ComputeTask.TaskPriority.NORMAL);

        DistributedComputingService.ComputeTask first = service.getNextTask();
        assertEquals(DistributedComputingService.ComputeTask.TaskPriority.URGENT, first.getPriority());

        DistributedComputingService.ComputeTask second = service.getNextTask();
        assertEquals(DistributedComputingService.ComputeTask.TaskPriority.NORMAL, second.getPriority());

        DistributedComputingService.ComputeTask third = service.getNextTask();
        assertEquals(DistributedComputingService.ComputeTask.TaskPriority.LOW, third.getPriority());

        assertNull(service.getNextTask());
    }
}
