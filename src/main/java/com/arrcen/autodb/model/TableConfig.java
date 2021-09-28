package com.arrcen.autodb.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @Author: wangziheng
 * @Date: 2021/3/8
 */
@Getter
@Setter
public class TableConfig {
    private String tableName;
    private String primaryKey;
    private String seqName;
    private String comment;
    private List<FieldConfig> fieldConfigs;

}
