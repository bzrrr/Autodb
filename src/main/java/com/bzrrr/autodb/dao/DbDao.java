package com.bzrrr.autodb.dao;

import com.bzrrr.autodb.model.FieldConfig;
import com.bzrrr.autodb.model.TableConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @Author: wangziheng
 * @Date: 2021/3/8
 */
@Mapper
@Repository
public interface DbDao {
    @UpdateProvider(type = TableProvider.class, method = "createTable")
    void createTable(@Param("tableConfig") TableConfig tableConfig);

    class TableProvider {
        public String createTable(TableConfig tableConfig) {
            List<FieldConfig> configs = tableConfig.getFieldConfigs();
            StringBuilder sb = new StringBuilder();
            for (FieldConfig config : configs) {
                String type = config.getFieldType();
                sb.append("\"").append(config.getFieldName()).append("\"").append(" ").append(type);
                if ("NUMBER".equalsIgnoreCase(type)) {
                    sb.append("(").append(Math.min(38, config.getLength())).append(",").append(config.getPrecision()).append(")").append(" ");
                } else if (!StringUtils.equalsAnyIgnoreCase(type, "DATE", "NCLOB", "CLOB")) {
                    sb.append("(").append(config.getLength()).append(")").append(" ");
                }
                if (!config.getNullable()) {
                    sb.append(" not null");
                }
                sb.append(",");

            }

            if (StringUtils.isNotBlank(tableConfig.getPrimaryKey())) {
                return "CREATE TABLE \"${tableConfig.tableName}\" (" +
                        sb.toString() +
                        "primary key (\"" + tableConfig.getPrimaryKey() + "\"))";
            } else {
                return "CREATE TABLE \"${tableConfig.tableName}\" (" +
                        sb.toString().substring(0, sb.length() - 1) + ")";
            }
        }
    }

    @Select("SELECT count(*) FROM user_tables t WHERE t.TABLE_NAME=#{tableName}")
    Boolean checkTableExist(String tableName);

    /**
     * 获取表所有用户表
     *
     * @return
     */
    @Select("SELECT TABLE_NAME FROM user_tables")
    List<String> selectTable();

    /**
     * 获取用户表所有字段
     *
     * @param tableName
     * @return
     */
    @Select("select t.COLUMN_NAME from user_tab_columns t where table_name=#{tableName}")
    List<String> selectColumn(@Param("tableName") String tableName);

    @Select("select count(*) from user_sequences where sequence_name = #{seqName}")
    Boolean checkSeqExists(@Param("seqName") String seqName);

    @Update("CREATE SEQUENCE \"${seqName}\" MINVALUE 1 MAXVALUE 999999999 INCREMENT BY 1 START WITH 1 CACHE 20 NOORDER  NOCYCLE")
    void createTableSeq(String seqName);

    @Update("ALTER TABLE \"${tableName}\" ADD CONSTRAINT ${uniqueName} UNIQUE (\"${uniqueName}\")")
    void addTableUnique(@Param("tableName") String tableName, @Param("uniqueName") String uniqueName);

    @Update({"<script>",
            "ALTER TABLE \"${tableName}\" ADD CONSTRAINT ${uniqueGroup} UNIQUE ",
            "<foreach collection='uniqueNames' item='uniqueName' open='(' separator=',' close=')'>",
            "${uniqueName}",
            "</foreach>",
            "</script>"})
    void addTableUniqueGroup(@Param("tableName") String tableName, @Param("uniqueGroup") String uniqueGroup, @Param("uniqueNames") List<String> uniqueNames);

    @Update("COMMENT ON TABLE \"${tableName}\" IS '${comment}'")
    void addTableComment(@Param("tableName") String tableName, @Param("comment") String comment);

    @Update("COMMENT ON COLUMN \"${tableName}\".\"${columnName}\" IS '${comment}'")
    void addColumnComment(@Param("tableName") String tableName, @Param("columnName") String columnName, @Param("comment") String comment);

    @Update("CREATE INDEX \"${indexName}\" ON \"${tableName}\" (\"${columnName}\")")
    void addTableIndex(@Param("tableName") String tableName, @Param("columnName") String columnName, @Param("indexName") String indexName);

    @Update({"<script>",
            "CREATE INDEX \"${indexGroupName}\" ON \"${tableName}\" ",
            "<foreach collection='columnNames' item='columnName' open='(' separator=',' close=')'>",
            "\"${columnName}\"",
            "</foreach>",
            "</script>"})
    void addTableIndexGroup(@Param("tableName") String tableName, @Param("indexGroupName") String indexGroupName, @Param("columnNames") List<String> columnNames);

}
