package com.bzrrr.autodb.model;

import lombok.Getter;
import lombok.Setter;

/**
 * @Author: wangziheng
 * @Date: 2021/3/8
 */
@Getter
@Setter
public class FieldConfig {
    private String fieldName;
    private String fieldType;
    private Integer length;
    private Integer precision;
    private Boolean nullable;
    private String uniqueName;
    private Boolean unique;
    private String uniqueGroup;
    private String comment;
    private String indexName;
    private Boolean indexable;
    private String[] indexGroup;
}
