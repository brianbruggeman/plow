package com.breakersoft.plow.test.dispatcher.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import javax.annotation.Resource;

import org.junit.Before;
import org.junit.Test;

import com.breakersoft.plow.dispatcher.DispatchService;
import com.breakersoft.plow.dispatcher.dao.StatsDao;
import com.breakersoft.plow.dispatcher.domain.DispatchJob;
import com.breakersoft.plow.dispatcher.domain.DispatchNode;
import com.breakersoft.plow.dispatcher.domain.DispatchProc;
import com.breakersoft.plow.dispatcher.domain.DispatchTask;
import com.breakersoft.plow.event.JobLaunchEvent;
import com.breakersoft.plow.rnd.thrift.DiskIO;
import com.breakersoft.plow.rnd.thrift.RunningTask;
import com.breakersoft.plow.service.JobService;
import com.breakersoft.plow.test.AbstractTest;
import com.breakersoft.plow.thrift.JobSpecT;
import com.google.common.collect.Lists;

public class StatsDaoTests extends AbstractTest {

    @Resource
    JobService jobService;

    @Resource
    DispatchService dispatchService;

    @Resource
    StatsDao statsDao;

    private DispatchNode node;
    private DispatchJob job;
    private DispatchProc proc;
    private DispatchTask task;
    private JobLaunchEvent event;

    @Before
    public void init() {
        JobSpecT spec = getTestJobSpec();
        event = jobService.launch(spec);
        node = dispatchService.getDispatchNode(
                nodeService.createNode(getTestNodePing()).getName());

        job = new DispatchJob(event.getJob());
        task = dispatchService.getDispatchableTasks(job, node).get(0);

        assertTrue(dispatchService.reserveTask(task));
        proc = dispatchService.allocateProc(node, task);
    }

    @Test
    public void testUpdateProcRuntimeStats() {

        RunningTask r_task = new RunningTask();
        r_task.taskId = task.getTaskId().toString();
        r_task.procId = proc.getProcId().toString();
        r_task.jobId = job.getJobId().toString();
        r_task.layerId = task.getLayerId().toString();
        r_task.rssMb = 1024;
        r_task.pid = 101;
        r_task.lastLog = "foo bar";
        // This is converted to a float in DB.
        r_task.cpuPercent = 50;
        r_task.diskIO = new DiskIO(10l, 10l, 100l, 200l);

        statsDao.batchUpdateProcRuntimeStats(Lists.newArrayList(r_task));

        Map<String, Object> record = jdbc().queryForMap(
                "SELECT * FROM plow.proc WHERE pk_proc=?", proc.getProcId());

        assertEquals(r_task.lastLog, record.get("str_last_log_line"));
        assertEquals(r_task.rssMb, record.get("int_ram_used"));
        assertEquals(r_task.rssMb, record.get("int_ram_high"));
        assertEquals(0.5f, record.get("flt_cores_used"));
        assertEquals(0.5f, record.get("flt_cores_high"));
    }

    @Test
    public void testUpdateTaskRuntimeStats() {

        RunningTask r_task = new RunningTask();
        r_task.taskId = task.getTaskId().toString();
        r_task.procId = proc.getProcId().toString();
        r_task.jobId = job.getJobId().toString();
        r_task.layerId = task.getLayerId().toString();
        r_task.rssMb = 1024;
        r_task.pid = 101;
        r_task.lastLog = "foo bar";
        // This is converted to a float in DB.
        r_task.cpuPercent = 50;
        r_task.diskIO = new DiskIO(10l, 10l, 100l, 200l);

        statsDao.batchUpdateTaskRuntimeStats(Lists.newArrayList(r_task));

        Map<String, Object> record = jdbc().queryForMap(
                "SELECT * FROM plow.task WHERE pk_task=?", proc.getTaskId());
    }

    @Test
    public void testUpdateJobRuntimeStats() {

        RunningTask r_task = new RunningTask();
        r_task.taskId = task.getTaskId().toString();
        r_task.procId = proc.getProcId().toString();
        r_task.jobId = job.getJobId().toString();
        r_task.layerId = task.getLayerId().toString();
        r_task.rssMb = 4096;
        r_task.pid = 101;
        r_task.lastLog = "foo bar";
        // This is converted to a float in DB.
        r_task.cpuPercent = 50;
        r_task.diskIO = new DiskIO(10l, 10l, 100l, 200l);

        statsDao.batchUpdateJobRuntimeStats(Lists.newArrayList(r_task));


        long memory = jdbc().queryForObject("SELECT int_ram_high FROM plow.job_stat WHERE pk_job=?", Integer.class, task.getJobId());
        assertEquals(4096, memory);

        float cpu = jdbc().queryForObject("SELECT flt_cores_high FROM plow.job_stat WHERE pk_job=?", Float.class, task.getJobId());
        assertEquals(0.5f, cpu, 0.01);
    }

    @Test
    public void testUpdateLayerRuntimeStats() {

        RunningTask r_task = new RunningTask();
        r_task.taskId = task.getTaskId().toString();
        r_task.procId = proc.getProcId().toString();
        r_task.jobId = job.getJobId().toString();
        r_task.layerId = task.getLayerId().toString();
        r_task.rssMb = 2048;
        r_task.pid = 101;
        r_task.lastLog = "foo bar";
        r_task.cpuPercent = 50;
        r_task.diskIO = new DiskIO(10l, 10l, 100l, 200l);

        statsDao.batchUpdateLayerRuntimeStats(Lists.newArrayList(r_task));

        int result;

        r_task.rssMb = 4096;
        statsDao.batchUpdateLayerRuntimeStats(Lists.newArrayList(r_task));
        result = jdbc().queryForObject("SELECT int_ram_high FROM plow.layer_stat WHERE pk_layer=?", Integer.class, task.getLayerId());
        assertEquals(4096, result);

        float memory = jdbc().queryForObject("SELECT flt_cores_high FROM plow.layer_stat WHERE pk_layer=?", Float.class, task.getLayerId());
        assertEquals(.5, memory, 0.01);
    }

    @Test
    public void testUpdateLayerMinMemory() {

        RunningTask r_task = new RunningTask();
        r_task.taskId = task.getTaskId().toString();
        r_task.procId = proc.getProcId().toString();
        r_task.jobId = job.getJobId().toString();
        r_task.layerId = task.getLayerId().toString();
        r_task.rssMb = 2048;
        r_task.pid = 101;
        r_task.lastLog = "foo bar";
        r_task.cpuPercent = 50;
        r_task.diskIO = new DiskIO(10l, 10l, 100l, 200l);

        int result;

        statsDao.batchUpdateLayerMinimumMemory(Lists.newArrayList(r_task));
        result = jdbc().queryForObject("SELECT int_ram_min FROM plow.layer WHERE pk_layer=?", Integer.class, task.getLayerId());
        assertEquals(2048, result);

        r_task.rssMb = 4096;
        statsDao.batchUpdateLayerMinimumMemory(Lists.newArrayList(r_task));
        result = jdbc().queryForObject("SELECT int_ram_min FROM plow.layer WHERE pk_layer=?", Integer.class, task.getLayerId());
        assertEquals(4096, result);

        r_task.rssMb = 1024;
        statsDao.batchUpdateLayerMinimumMemory(Lists.newArrayList(r_task));
        result = jdbc().queryForObject("SELECT int_ram_min FROM plow.layer WHERE pk_layer=?", Integer.class, task.getLayerId());
        assertEquals(4096, result);
    }
}
