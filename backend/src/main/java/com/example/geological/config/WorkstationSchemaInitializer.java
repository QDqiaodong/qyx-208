package com.example.geological.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 操作台「适配工作站」占用关系的数据库级约束。
 *
 * 规则：同一时刻同一适配工作站名字只允许一台在用(status=1)操作台占用；
 * 停用台不占站，空站名（含空白串）不占站。
 *
 * 实现：生成列 active_adapt_station 仅在操作台在用且填了站名时等于该站名，否则为 NULL；
 * 其上建唯一索引。MySQL 唯一索引允许同一列存在多个 NULL，所以停用台、清空站名后互不冲突，
 * 而两台在用台并发写成同一站名时，数据库只让先提交的一台成功，后到的一台唯一索引冲突、
 * 整笔保存回滚。Service 层的占用预检负责给出明确业务报错，本索引负责并发兜底。
 *
 * ddl-auto=update 不会创建/维护这种条件唯一索引，因此由本初始化器在启动时幂等补齐。
 * 用 @PostConstruct 在容器初始化阶段执行（晚于 Hibernate 建表、早于对外提供服务），
 * 保证第一条请求到达前索引已就位。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WorkstationSchemaInitializer {

    private static final String TABLE = "workstation";
    private static final String GEN_COLUMN = "active_adapt_station";
    private static final String UNIQUE_INDEX = "uk_workstation_active_adapt_station";

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void initialize() {
        ensureGeneratedColumn();
        ensureUniqueIndex();
    }

    private boolean columnExists(String column) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class, TABLE, column);
        return count != null && count > 0;
    }

    private boolean indexExists(String index) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM INFORMATION_SCHEMA.STATISTICS " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?",
                Integer.class, TABLE, index);
        return count != null && count > 0;
    }

    private void ensureGeneratedColumn() {
        if (columnExists(GEN_COLUMN)) {
            return;
        }
        // 仅在用且站名非空白时生成列值=TRIM(站名)，其余为 NULL。
        // VIRTUAL 列不占表数据空间，唯一索引照常生效；多个 NULL 在唯一索引中互不冲突。
        jdbcTemplate.execute(
                "ALTER TABLE " + TABLE + " ADD COLUMN " + GEN_COLUMN +
                        " VARCHAR(200) GENERATED ALWAYS AS (" +
                        "CASE WHEN status = 1 AND adapt_station IS NOT NULL " +
                        "AND TRIM(adapt_station) <> '' THEN TRIM(adapt_station) ELSE NULL END" +
                        ") VIRTUAL");
        log.info("已在 {} 表创建生成列 {}", TABLE, GEN_COLUMN);
    }

    private void ensureUniqueIndex() {
        if (indexExists(UNIQUE_INDEX)) {
            return;
        }
        try {
            jdbcTemplate.execute(
                    "CREATE UNIQUE INDEX " + UNIQUE_INDEX + " ON " + TABLE + " (" + GEN_COLUMN + ")");
            log.info("已在 {} 表创建唯一索引 {}：同一适配工作站名字同时只允许一台在用操作台占用",
                    TABLE, UNIQUE_INDEX);
        } catch (Exception e) {
            // 存量台账里若已有两台在用台写着同一站名，唯一索引无法建立。
            // 不做任何静默改数，列出冲突站名以便人工核对处理；处理后重启即可自动补建索引。
            // 此期间 Service 层的占用预检仍会拦截新的重复占用。
            log.error("唯一索引 {} 创建失败：台账中已存在两台在用操作台占用同一适配工作站名字，" +
                    "请先在操作台台账中处理下列冲突（保留一台占用，其余台释放或停用后重启服务），" +
                    "并发兜底索引才会生效。冲突明细：{}", UNIQUE_INDEX, listDuplicateStations(), e);
        }
    }

    private List<Map<String, Object>> listDuplicateStations() {
        try {
            return jdbcTemplate.queryForList(
                    "SELECT TRIM(adapt_station) AS adaptStation, COUNT(1) AS activeCount, " +
                            "GROUP_CONCAT(id ORDER BY id) AS workstationIds " +
                            "FROM " + TABLE + " " +
                            "WHERE status = 1 AND adapt_station IS NOT NULL AND TRIM(adapt_station) <> '' " +
                            "GROUP BY TRIM(adapt_station) HAVING COUNT(1) > 1");
        } catch (Exception ex) {
            log.warn("查询冲突站名失败", ex);
            return List.of();
        }
    }
}
