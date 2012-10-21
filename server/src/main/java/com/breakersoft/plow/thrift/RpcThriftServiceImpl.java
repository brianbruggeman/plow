package com.breakersoft.plow.thrift;

import java.util.List;
import java.util.UUID;

import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.breakersoft.plow.event.JobLaunchEvent;
import com.breakersoft.plow.service.JobService;
import com.breakersoft.plow.thrift.dao.ThriftJobDao;
import com.breakersoft.plow.thrift.dao.ThriftLayerDao;
import com.breakersoft.plow.thrift.dao.ThriftTaskDao;

@ThriftService
public class RpcThriftServiceImpl implements RpcServiceApi.Iface {

    private Logger logger = org.slf4j.LoggerFactory.getLogger(RpcThriftServiceImpl.class);

    @Autowired
    JobService jobService;

    @Autowired
    ThriftJobDao thriftJobDao;

    @Autowired
    ThriftLayerDao thriftLayerDao;

    @Autowired
    ThriftTaskDao thriftTaskDao;

    @Override
    public JobT launch(Blueprint bp) throws PlowException, TException {

        logger.info("launchung job: {} ", bp);

        JobLaunchEvent event =  jobService.launch(bp);

        JobT result = new JobT();
        result.id = event.getJob().getJobId().toString();
        result.name = event.getBlueprint().job.getName();
        return result;
    }

    @Override
    public JobT getRunningJob(String name) throws PlowException, TException {
        return thriftJobDao.getRunningJob(name);
    }

    @Override
    public JobT getJob(String jobId) throws PlowException, TException {
        return thriftJobDao.getJob(jobId);
    }

    @Override
    public LayerT getLayer(String id) throws PlowException, TException {
        return thriftLayerDao.getLayer(UUID.fromString(id));
    }

    @Override
    public List<LayerT> getLayers(String jobId) throws PlowException, TException {
        return thriftLayerDao.getLayers(UUID.fromString(jobId));
    }

    @Override
    public TaskT getTask(String id) throws PlowException, TException {
        return thriftTaskDao.getTask(UUID.fromString(id));
    }

    @Override
    public List<TaskT> getTasks(String layerId) throws PlowException, TException {
        return thriftTaskDao.getTasks(UUID.fromString(layerId));
    }

    @Override
    public List<JobT> getJobs(JobFilter filter) throws PlowException, TException {
        return thriftJobDao.getJobs(filter);
    }

}
