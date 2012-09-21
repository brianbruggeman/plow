package com.breakersoft.plow.dao;

import java.util.List;

import com.breakersoft.plow.Folder;
import com.breakersoft.plow.Job;
import com.breakersoft.plow.Task;
import com.breakersoft.plow.dispatcher.DispatchFolder;
import com.breakersoft.plow.dispatcher.DispatchTask;
import com.breakersoft.plow.dispatcher.DispatchJob;
import com.breakersoft.plow.dispatcher.DispatchNode;

public interface DispatchDao {

    boolean reserveFrame(Task frame);

    boolean unReserveFrame(Task frame);

    List<DispatchTask> getFrames(DispatchJob job, DispatchNode node);

    DispatchJob getDispatchJob(Job job);

    DispatchFolder getDispatchFolder(Folder folder);
}