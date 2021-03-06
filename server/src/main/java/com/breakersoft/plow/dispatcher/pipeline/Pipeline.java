package com.breakersoft.plow.dispatcher.pipeline;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;

import com.breakersoft.plow.JobId;

public class Pipeline implements JobId, Runnable {

    private static final Logger logger =
            org.slf4j.LoggerFactory.getLogger(Pipeline.class);

    private final UUID jobId;
    private final LinkedBlockingQueue<PipelineCommand> queue;
    private final AtomicBoolean isRunning;

    public Pipeline(UUID jobId) {
        this.jobId = jobId;
        this.queue = new LinkedBlockingQueue<PipelineCommand>();
        this.isRunning = new AtomicBoolean(false);
    }

    public void add(PipelineCommand command) {
        queue.add(command);
    }

    public void addAll(Collection<PipelineCommand> command) {
        queue.addAll(command);
    }

    @Override
    public UUID getJobId() {
        return jobId;
    }

    @Override
    public void run() {
        if (queue.isEmpty()) {
            return;
        }
        if (!isRunning.compareAndSet(false, true)){
            return;
        }

        try {
            while (true) {
                PipelineCommand command = queue.poll();
                if (command == null) {
                    return;
                }
                try {
                    command.process();
                } catch (RuntimeException e) {
                    logger.warn("{} pipeline exception, unexpected exception: {}", jobId, e);
                }
            }
        }
        finally {
            isRunning.set(false);
        }
    }
}
