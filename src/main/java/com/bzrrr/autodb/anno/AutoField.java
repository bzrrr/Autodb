package com.bzrrr.autodb.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Author: wangziheng
 * @Date: 2021/3/8
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface AutoField {
    String name() default "";

    int length() default 255;

    boolean nullable() default true;

    int precision() default 0;

    String uniqueName() default "";

    boolean unique() default false;

    String uniqueGroup() default "";

    boolean enabled() default true;

    String columnType() default "";

    String indexName() default "";

    boolean indexable() default false;

    String[] indexGroup() default "";
}
