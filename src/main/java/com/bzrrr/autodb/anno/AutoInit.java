package com.bzrrr.autodb.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Author: wangziheng
 * @Date: 2021/8/18
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface AutoInit {

    /**
     * 数据模型类
     * @return
     */
    Class<?> value();
}
