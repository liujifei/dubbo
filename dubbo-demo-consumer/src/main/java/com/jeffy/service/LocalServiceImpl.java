package com.jeffy.service;

import com.alibaba.dubbo.demo.DemoService;


public class LocalServiceImpl implements DemoService {

    @Override
    public String sayHello(String name) {
        return null;
    }

    @Override
    public String getMongo() {
        System.out.println("服务降级了");
        return "new Mongo";
    }

}
