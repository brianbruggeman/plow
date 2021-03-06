package com.breakersoft.plow.thrift.dao;

import java.util.List;
import java.util.UUID;

import com.breakersoft.plow.thrift.TaskFilterT;
import com.breakersoft.plow.thrift.TaskStatsT;
import com.breakersoft.plow.thrift.TaskT;

public interface ThriftTaskDao {

    TaskT getTask(UUID id);

    List<TaskT> getTasks(TaskFilterT filter);

    String getLogPath(UUID id);

    List<TaskStatsT> getTaskStats(UUID taskId);

}
