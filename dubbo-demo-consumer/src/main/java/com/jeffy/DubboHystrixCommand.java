package com.jeffy;

import org.apache.log4j.Logger;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.jeffy.service.LocalServiceImpl;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixThreadPoolProperties;

public class DubboHystrixCommand extends HystrixCommand {

    private static Logger logger = Logger.getLogger(DubboHystrixCommand.class);
    private static final int DEFAULT_THREADPOOL_CORE_SIZE = 30;
    private Invoker<?> invoker;
    private Invocation invocation;

    public DubboHystrixCommand(Invoker<?> invoker, Invocation invocation) {
        super(Setter
                .withGroupKey(HystrixCommandGroupKey.Factory.asKey(invoker.getInterface().getName()))
                .andCommandKey(
                        HystrixCommandKey.Factory.asKey(String.format("%s_%d", invocation.getMethodName(),
                                invocation.getArguments() == null ? 0 : invocation.getArguments().length)))
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                        // 10秒钟内至少19此请求失败，熔断器才发挥起作用
                        .withCircuitBreakerRequestVolumeThreshold(20)//00000000
                        // 熔断器中断请求30秒后会进入半打开状态,放部分流量过去重试
                        .withCircuitBreakerSleepWindowInMilliseconds(30000)
                        // 错误率达到50开启熔断保护
                        .withCircuitBreakerErrorThresholdPercentage(50)
                        // 使用dubbo的超时，禁用这里的超时
                        .withExecutionTimeoutEnabled(true))
                .andThreadPoolPropertiesDefaults(
                        // 线程池大小为DEFAULT_THREADPOOL_CORE_SIZE
                        HystrixThreadPoolProperties.Setter().withCoreSize(getThreadPoolCoreSize(invoker.getUrl()))));

        this.invoker = invoker;
        this.invocation = invocation;
        // System.out.println(invoker.getInterface().getName());
    }

    @Override
    protected Result run() throws Exception {
        return invoker.invoke(invocation);
    }

    @Override
    protected String getFallback() {
        System.out.println("isCircuitBreakerOpen:" + this.isCircuitBreakerOpen());
        LocalServiceImpl localService = new LocalServiceImpl();
        localService.getMongo();
        return "服务降级";
    }

    /**
     * 获取线程池大小
     * 
     * @param url
     * @return
     */
    private static int getThreadPoolCoreSize(URL url) {
        if (url != null) {
            int size = url.getParameter("ThreadPoolCoreSize", DEFAULT_THREADPOOL_CORE_SIZE);
            if (logger.isDebugEnabled()) {
                logger.debug("ThreadPoolCoreSize:" + size);
            }
            return size;
        }
        return DEFAULT_THREADPOOL_CORE_SIZE;
    }
}
