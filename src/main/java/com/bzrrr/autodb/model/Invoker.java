package com.bzrrr.autodb.model;

import lombok.Data;

import java.lang.reflect.Method;

/**
 * @Author: wangziheng
 * @Date: 2021/8/18
 */
@Data
public class Invoker {
    private Method method;
    private Object bean;

    public Invoker(Method method, Object bean) {
        this.method = method;
        this.bean = bean;
    }
}
