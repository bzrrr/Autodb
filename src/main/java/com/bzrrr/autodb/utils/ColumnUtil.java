package com.bzrrr.autodb.utils;

import com.bzrrr.autodb.anno.AutoField;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.google.common.base.CaseFormat;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;

/**
 * @Author: wangziheng
 * @Date: 2021/3/8
 */
public class ColumnUtil {

    public static boolean hasTableAnnotation(Class<?> clasz) {
        TableName tableNamePlus = clasz.getAnnotation(TableName.class);
        return tableNamePlus != null;
    }

    public static String getTableName(Class<?> clasz) {
        TableName tableNamePlus = clasz.getAnnotation(TableName.class);
        if (!hasTableAnnotation(clasz)) {
            return null;
        }
        if (tableNamePlus != null && !StringUtils.isEmpty(tableNamePlus.value())) {
            return tableNamePlus.value();
        }
        return getBuildUpperName(clasz.getSimpleName());
    }

    public static boolean getTableEnabled(Class<?> clasz) {
        AutoField autoField = clasz.getAnnotation(AutoField.class);
        if (autoField == null) {
            return true;
        }
        return autoField.enabled();
    }

    private static String getBuildUpperName(String name) {
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE,
                name).toUpperCase();
    }

    private static String getBuildLowerName(String name) {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE,
                name).toUpperCase();
    }

    public static boolean getColumnEnabled(Field field) {
        AutoField autoField = field.getAnnotation(AutoField.class);
        if (autoField == null) {
            return true;
        }
        return autoField.enabled();
    }

    public static String getColumnName(Field field) {
        TableField tableField = field.getAnnotation(TableField.class);
        TableId tableId = field.getAnnotation(TableId.class);
        if (tableField != null && !StringUtils.isEmpty(tableField.value()) && tableField.exist()) {
            return tableField.value();
        }
        if (tableId != null && !StringUtils.isEmpty(tableId.value())) {
            return tableId.value();
        }
        return getBuildLowerName(field.getName());
    }

    public static String getColumnType(Field field) {
        String type = field.getType().getName();
        if ("java.lang.String".equalsIgnoreCase(type)) {
            return "VARCHAR2";
        } else if ("java.lang.Integer".equalsIgnoreCase(type) || "java.lang.Boolean".equalsIgnoreCase(type) || "java.lang.Long".equalsIgnoreCase(type) || "java.lang.Double".equalsIgnoreCase(type) || "java.lang.Float".equalsIgnoreCase(type)) {
            return "NUMBER";
        } else if ("java.util.Date".equalsIgnoreCase(type)) {
            return "DATE";
        } else if (field.getType().isEnum()) {
            return "VARCHAR2";
        }
        return null;
    }
}
