package com.jeffy;

import java.util.Map;

import org.apache.log4j.Logger;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixThreadPoolProperties;

public class DubboHystrixCommand extends HystrixCommand {

    private static Logger logger = Logger.getLogger(DubboHystrixCommand.class);
    private static final int DEFAULT_THREADPOOL_CORE_SIZE = 1;
    private Invoker<?> invoker;
    private Invocation invocation;
    private boolean hasException = false;
    private Exception exception = null;
    private Result result = null;

    public DubboHystrixCommand(Invoker<?> invoker, Invocation invocation) {
        super(Setter
                .withGroupKey(HystrixCommandGroupKey.Factory.asKey(invoker.getInterface().getName()))
                .andCommandKey(
                        HystrixCommandKey.Factory.asKey(String.format("%s_%d", invocation.getMethodName(),
                                invocation.getArguments() == null ? 0 : invocation.getArguments().length)))
                .andCommandPropertiesDefaults(
                        HystrixCommandProperties.Setter().withCircuitBreakerRequestVolumeThreshold(1)// 10秒钟内至少19此请求失败，熔断器才发挥起作用
                                .withCircuitBreakerSleepWindowInMilliseconds(300000000)// 熔断器中断请求30秒后会进入半打开状态,放部分流量过去重试
                                .withCircuitBreakerErrorThresholdPercentage(50)// 错误率达到50开启熔断保护
                                .withExecutionTimeoutEnabled(true)// 使用dubbo的超时，禁用这里的超时
                                )
                .andThreadPoolPropertiesDefaults(
                        HystrixThreadPoolProperties.Setter().withCoreSize(getThreadPoolCoreSize(invoker.getUrl()))));// 线程池为30

        this.invoker = invoker;
        this.invocation = invocation;   
//        System.out.println(invoker.getInterface().getName());
    }

    @Override
    protected Result run() throws Exception {
        try {
            result = invoker.invoke(invocation);
        } catch (Exception e) {
            System.out.println("************************have a rest*******************");
            hasException = true;
            exception = e;
        }
        return new Result() {
            
            @Override
            public Object recreate() throws Throwable {
                System.out.println("recreate():"+result.recreate());
                return result.recreate();
            }
            
            @Override
            public boolean hasException() {
                System.out.println("hasException():"+hasException);
                return hasException;
            }
            
            @Override
            public Object getValue() {
                System.out.println("getValue():"+result.getValue());
                return result.getValue();
            }
            
            @Override
            public Object getResult() {
                System.out.println("getResult():" + result.getResult());
                return result;
            }
            
            @Override
            public Throwable getException() {
                System.out.println("getException():"+exception);
                return exception;
            }
            
            @Override
            public Map<String, String> getAttachments() {
                System.out.println("getAttachments():"+result.getAttachments());
                return result.getAttachments();
            }
            
            @Override
            public String getAttachment(String key, String defaultValue) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public String getAttachment(String key) {
                // TODO Auto-generated method stub
                return null;
            }
        };
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
