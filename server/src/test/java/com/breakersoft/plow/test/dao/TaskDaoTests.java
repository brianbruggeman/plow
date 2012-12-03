package com.breakersoft.plow.test.dao;

import static org.junit.Assert.*;

import java.util.Map;

import javax.annotation.Resource;

import org.junit.Test;

import com.breakersoft.plow.Task;
import com.breakersoft.plow.Job;
import com.breakersoft.plow.Layer;
import com.breakersoft.plow.dao.TaskDao;
import com.breakersoft.plow.dao.JobDao;
import com.breakersoft.plow.dao.LayerDao;
import com.breakersoft.plow.rnd.thrift.RunningTask;
import com.breakersoft.plow.test.AbstractTest;
import com.breakersoft.plow.thrift.JobSpecT;
import com.breakersoft.plow.thrift.LayerSpecT;
import com.breakersoft.plow.thrift.TaskState;

public class TaskDaoTests extends AbstractTest {

    @Resource
    TaskDao taskDao;

    @Resource
    LayerDao layerDao;

    @Resource
    JobDao jobDao;

    private Layer layer;

    private Task task;

    private Job job;

    @Test
    public void testCreate() {
        JobSpecT spec = getTestJobSpec();
        job = jobDao.create(TEST_PROJECT, spec);
        LayerSpecT lspec = spec.getLayers().get(0);
        layer = layerDao.create(job, lspec, 0);
        task = taskDao.create(layer, "0001-test", 1, 0, 0);
    }

    @Test
    public void testGet() {
        testCreate();
        Task f1 = taskDao.get(task.getTaskId());
        assertEquals(task, f1);

        Task f2 = taskDao.get(layer, 1);
        assertEquals(task, f1);
        assertEquals(f2, f1);
    }

    @Test
    public void testGetByNameOrId() {
        testCreate();
        Task f1 = taskDao.get(task.getTaskId());
        assertEquals(task, f1);

        Task f2 = taskDao.getByNameOrId(job, f1.getTaskId().toString());
        assertEquals(task, f1);
        assertEquals(f2, f1);

        Task f3 = taskDao.getByNameOrId(job, "0001-test");
        assertEquals(task, f3);
        assertEquals(f2, f3);
    }

    @Test
    public void testUpdateState() {
        testCreate();
        assertTrue(taskDao.updateState(task,
                TaskState.INITIALIZE, TaskState.WAITING));
        assertFalse(taskDao.updateState(task,
                TaskState.DEAD, TaskState.RUNNING));
    }

    @Test
    public void testReserve() {
        testCreate();
        taskDao.start(task);
    }

    @Test
    public void testStop() {
        testCreate();
        taskDao.stop(task, TaskState.SUCCEEDED);
    }

    @Test
    public void testStart() {
        testCreate();
        taskDao.reserve(task);
    }

    @Test
    public void testUnreserve() {
        testCreate();
        taskDao.unreserve(task);
    }

    @Test
    public void testResetTaskDispatchData() {
        testCreate();
        taskDao.resetTaskDispatchData(task, "foo", 1, 1024);
    }

    @Test
    public void testUpdateTaskDispatchData() {

        testCreate();

        simpleJdbcTemplate.update("UPDATE task SET int_state=? WHERE pk_task=?",
                TaskState.RUNNING.ordinal(), task.getTaskId());

        RunningTask runtask = new RunningTask();
        runtask.taskId = task.getTaskId().toString();
        runtask.lastLog = "foo";
        runtask.maxRss = 999;
        runtask.progress = 50;
        taskDao.updateTaskDispatchData(runtask);

        Map<String,Object> data = simpleJdbcTemplate.queryForMap(
                "SELECT * FROM plow.task_dsp WHERE pk_task=?",
                task.getTaskId());

        assertEquals(runtask.lastLog, (String) data.get("str_last_log_line"));
        assertEquals((Long)runtask.maxRss, Long.valueOf((int) data.get("int_used_ram_max")));
        assertEquals(50, data.get("int_progress"));
    }

}
