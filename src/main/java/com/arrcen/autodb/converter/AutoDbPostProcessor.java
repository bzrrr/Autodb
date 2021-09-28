package com.arrcen.autodb.converter;

/**
 * @Author: wangziheng
 * @Date: 2021/8/18
 */
public interface AutoDbPostProcessor<T> {

    /**
     * 向数据库表添加初始化数据
     */
    void doInitDatabase();
}
