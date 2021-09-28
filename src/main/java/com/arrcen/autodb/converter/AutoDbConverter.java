package com.arrcen.autodb.converter;

import com.arrcen.autodb.anno.AutoField;
import com.arrcen.autodb.anno.AutoInit;
import com.arrcen.autodb.config.AutoDbProperties;
import com.arrcen.autodb.dao.DbDao;
import com.arrcen.autodb.model.FieldConfig;
import com.arrcen.autodb.model.Invoker;
import com.arrcen.autodb.model.TableConfig;
import com.arrcen.autodb.utils.ClassScaner;
import com.arrcen.autodb.utils.ColumnUtil;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.google.common.collect.Lists;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * @Author: wangziheng
 * @Date: 2021/3/8
 */
public class AutoDbConverter implements ApplicationContextAware {
    private static final Logger log = LoggerFactory.getLogger(AutoDbConverter.class);
    @Resource
    private AutoDbProperties properties;
    @Resource
    private DbDao dao;
    private Map<String, AutoDbPostProcessor> initMap;
    private Map<Class<?>, Invoker> invokerMap = new HashMap<>();

    @PostConstruct
    public void init() {
        if (properties.isEnabled()) {
            String[] modelPath = properties.getModelPath();
            if (modelPath == null) {
                return;
            }
            List<String> tableNames = new ArrayList<String>();
            Set<Class> classes = ClassScaner.scan(modelPath, TableName.class);
            for (Class<?> clz : classes) {

                try {
                    // 没有打注解不需要创建表
                    if (!ColumnUtil.hasTableAnnotation(clz) || !ColumnUtil.getTableEnabled(clz)) {
                        continue;
                    }
                    // 禁止出现重名表
                    checkTableName(tableNames, clz);
                    createTable(clz);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    public void createTable(Class<?> clz) {
        TableConfig tableConfig = new TableConfig();
        String tableName = ColumnUtil.getTableName(clz);
        List<FieldConfig> allFields = getAllFields(clz, tableConfig);
        tableConfig.setTableName(tableName);
        tableConfig.setFieldConfigs(allFields);
        ApiModel apiModel = clz.getAnnotation(ApiModel.class);
        if (apiModel != null) {
            tableConfig.setComment(apiModel.value());
        }
        createByDs(clz, tableConfig, tableName, allFields);
    }


    public void createByDs(Class<?> clz, TableConfig tableConfig, String tableName, List<FieldConfig> allFields) {
        if (!dao.checkTableExist(tableName)) {
            dao.createTable(tableConfig);
            KeySequence keySeq = clz.getAnnotation(KeySequence.class);
            if (keySeq != null && com.baomidou.mybatisplus.core.toolkit.StringUtils.isNotBlank(keySeq.value())) {
                if (!dao.checkSeqExists(keySeq.value())) {
                    dao.createTableSeq(keySeq.value());
                }
            }
            if (StringUtils.isNotBlank(tableConfig.getComment())) {
                dao.addTableComment(tableName, tableConfig.getComment());
            }
            createComment(tableName, allFields);
            createUnique(tableName, allFields);
            createIndex(tableName, allFields);
            log.info("成功创建表: " + tableName);
            doInitData(clz, tableName);
        }
    }

    private void createComment(String tableName, List<FieldConfig> allFields) {
        allFields.forEach(fieldConfig -> {
            if (StringUtils.isNotBlank(fieldConfig.getComment())) {
                dao.addColumnComment(tableName, fieldConfig.getFieldName(), fieldConfig.getComment());
            }
        });
    }

    private void createUnique(String tableName, List<FieldConfig> allFields) {
        Map<String, List<String>> uniqueGroup = new HashMap<>();
        allFields.forEach(fieldConfig -> {
            if (fieldConfig.getUnique()) {
                dao.addTableUnique(tableName, fieldConfig.getFieldName());
            }
            String fieldUniqueGroup = fieldConfig.getUniqueGroup();
            if (com.baomidou.mybatisplus.core.toolkit.StringUtils.isNotBlank(fieldUniqueGroup)) {
                if (uniqueGroup.containsKey(fieldUniqueGroup)) {
                    List<String> list = uniqueGroup.get(fieldUniqueGroup);
                    list.add(fieldConfig.getFieldName());
                    uniqueGroup.replace(fieldUniqueGroup, list);
                } else {
                    uniqueGroup.put(fieldUniqueGroup, Lists.newArrayList(fieldConfig.getFieldName()));
                }
            }
        });
        for (Map.Entry<String, List<String>> entry : uniqueGroup.entrySet()) {
            String groupName = entry.getKey();
            List<String> fields = entry.getValue();
            dao.addTableUniqueGroup(tableName, groupName, fields);
        }
    }

    private void createIndex(String tableName, List<FieldConfig> allFields) {
        Map<String, List<String>> indexGroup = new HashMap<>();
        allFields.forEach(fieldConfig -> {
            if (fieldConfig.getIndexable()) {
                dao.addTableIndex(tableName, fieldConfig.getFieldName(), fieldConfig.getFieldName());
            }
            String[] fieldIndexGroups = fieldConfig.getIndexGroup();
            if (fieldIndexGroups != null && fieldIndexGroups.length > 0) {
                for (String fieldIndexGroup : fieldIndexGroups) {
                    if (com.baomidou.mybatisplus.core.toolkit.StringUtils.isNotBlank(fieldIndexGroup)) {
                        if (indexGroup.containsKey(fieldIndexGroup)) {
                            List<String> list = indexGroup.get(fieldIndexGroup);
                            list.add(fieldConfig.getFieldName());
                            indexGroup.replace(fieldIndexGroup, list);
                        } else {
                            indexGroup.put(fieldIndexGroup, Lists.newArrayList(fieldConfig.getFieldName()));
                        }
                    }
                }
            }
        });
        for (Map.Entry<String, List<String>> entry : indexGroup.entrySet()) {
            String groupName = entry.getKey();
            List<String> fields = entry.getValue();
            dao.addTableIndexGroup(tableName, groupName, fields);
        }
    }

    private void doInitData(Class<?> clz, String tableName) {
        if (invokerMap.containsKey(clz)) {
            Invoker invoker = invokerMap.get(clz);
            invoke(invoker);
            log.info("成功初始化数据: " + tableName);
        } else {
            for (AutoDbPostProcessor bean : initMap.values()) {
                Class<?> aClass = GenericTypeResolver.resolveTypeArgument(bean.getClass(), AutoDbPostProcessor.class);
                if (aClass == clz) {
                    bean.doInitDatabase();
                    log.info("成功初始化数据: " + tableName);
                }
            }
        }
    }

    private void checkTableName(List<String> tableNames, Class<?> clz) {
        String tableName = ColumnUtil.getTableName(clz);
        if (tableNames.contains(tableName)) {
            log.warn(tableName + "表名出现重复，禁止创建！");
        }
        tableNames.add(tableName);
    }

    private List<FieldConfig> getAllFields(Class<?> clz, TableConfig tableConfig) {
        FieldConfig primary = new FieldConfig();
        List<FieldConfig> fieldConfigs = new ArrayList<>();

        for (; clz != Object.class; clz = clz.getSuperclass()) {
            Field[] fields = clz.getDeclaredFields();
            for (Field field : fields) {
                if (!ColumnUtil.getColumnEnabled(field)) {
                    continue;
                }
                AutoField autoField = field.getAnnotation(AutoField.class);
                String columnType = null;
                if (autoField != null && StringUtils.isNotBlank(autoField.columnType())) {
                    columnType = autoField.columnType();
                } else {
                    columnType = ColumnUtil.getColumnType(field);
                }
                TableField tableField = field.getAnnotation(TableField.class);
                if (com.baomidou.mybatisplus.core.toolkit.StringUtils.isBlank(columnType) || (tableField != null && !tableField.exist())) {
                    continue;
                }
                FieldConfig fieldConfig = new FieldConfig();
                TableId tableId = field.getAnnotation(TableId.class);
                String columnName = ColumnUtil.getColumnName(field);
                fieldConfig.setFieldName(columnName);
                fieldConfig.setFieldType(columnType);
                if (autoField != null) {
                    if ("NUMBER".equalsIgnoreCase(columnType)) {
                        fieldConfig.setLength(autoField.length() + autoField.precision());
                        fieldConfig.setPrecision(autoField.precision());
                    } else {
                        fieldConfig.setLength(autoField.length());
                    }
                    fieldConfig.setNullable(autoField.nullable());
                    fieldConfig.setUnique(autoField.unique());
                    fieldConfig.setUniqueGroup(autoField.uniqueGroup());
                    fieldConfig.setIndexable(autoField.indexable());
                    fieldConfig.setIndexGroup(autoField.indexGroup());
                } else {
                    if (StringUtils.equalsAnyIgnoreCase(field.getType().getName(), "java.lang.Double", "java.lang.Float")) {
                        fieldConfig.setLength(38);
                        fieldConfig.setPrecision(2);
                    } else {
                        fieldConfig.setPrecision(0);
                        fieldConfig.setLength(255);
                    }
                    fieldConfig.setNullable(true);
                    fieldConfig.setUnique(false);
                    fieldConfig.setIndexable(false);
                }
                ApiModelProperty modelProperty = field.getAnnotation(ApiModelProperty.class);
                if (modelProperty != null) {
                    fieldConfig.setComment(modelProperty.value());
                }
                if (tableId != null) {
                    tableConfig.setPrimaryKey(tableId.value());
                    fieldConfig.setNullable(false);
                    primary = fieldConfig;
                } else {
                    fieldConfigs.add(fieldConfig);
                }
            }
        }

        List<FieldConfig> list = new ArrayList<>(fieldConfigs.size() + 1);
        if (StringUtils.isNotBlank(tableConfig.getPrimaryKey())) {
            list.add(primary);
        }
        list.addAll(fieldConfigs);
        return list;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        initMap = applicationContext.getBeansOfType(AutoDbPostProcessor.class);
        Map<String, Object> serviceMap = applicationContext.getBeansWithAnnotation(Service.class);
        Map<String, Object> componentMap = applicationContext.getBeansWithAnnotation(Component.class);
        List<Object> list = new ArrayList<>();
        list.addAll(serviceMap.values());
        list.addAll(componentMap.values());
        for (Object bean : list) {
            Method[] methods = bean.getClass().getMethods();
            for (Method declaredMethod : methods) {
                AutoInit ma = AnnotationUtils.findAnnotation(declaredMethod, AutoInit.class);
                if (ma != null) {
                    invokerMap.put(ma.value(), new Invoker(declaredMethod, bean));
                }
            }
        }
    }

    private void invoke(Invoker invoker) {
        try {
            invoker.getMethod().invoke(invoker.getBean());
        } catch (IllegalAccessException | InvocationTargetException e) {
            log.error(e.getMessage(), e);
        }
    }
}
