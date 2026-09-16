package com.example.geological.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 「同一桶同一自然日只留一条有效读数」的数据库级兜底约束。
 *
 * 规则：rain_reading 中 status=1（有效）的行，(bucket_id, read_date) 全表唯一；
 * 作废行不参与唯一（作废留痕不删，同桶同日作废后可重新记一笔）。
 *
 * 实现：生成列 valid_bucket_day 仅在读数有效时等于 "桶id:自然日"，否则为 NULL；
 * 其上建唯一索引。MySQL 唯一索引允许多个 NULL，所以作废行互不冲突，
 * 而两笔有效读数并发落同一桶同一日时，数据库只让先提交的一笔成功。
 * Service 层已在桶行悲观锁内查重并给出明确业务报错，本索引负责并发兜底。
 *
 * ddl-auto=update 不会创建/维护这种条件唯一索引，因此由本初始化器在启动时幂等补齐
 * （与操作台占站索引同一个套路）。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RainfallSchemaInitializer {

    private static final String TABLE = "rain_reading";
    private static final String GEN_COLUMN = "valid_bucket_day";
    private static final String UNIQUE_INDEX = "uk_rain_reading_valid_bucket_day";

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
        // 仅有效读数生成列值="桶id:自然日"，作废行为 NULL。
        // VIRTUAL 列不占表数据空间，唯一索引照常生效；多个 NULL 在唯一索引中互不冲突。
        jdbcTemplate.execute(
                "ALTER TABLE " + TABLE + " ADD COLUMN " + GEN_COLUMN +
                        " VARCHAR(100) GENERATED ALWAYS AS (" +
                        "CASE WHEN status = 1 THEN CONCAT(bucket_id, ':', read_date) ELSE NULL END" +
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
            log.info("已在 {} 表创建唯一索引 {}：同一桶同一自然日只留一条有效读数",
                    TABLE, UNIQUE_INDEX);
        } catch (Exception e) {
            // 存量 rec 本里若已有同桶同日的多条有效读数，唯一索引无法建立。
            // 不做任何静默改数，列出冲突明细以便人工核对（保留一条有效，其余作废后重启即可自动补建）。
            // 此期间 Service 层的桶锁内查重仍会拦截新的重复有效读数。
            log.error("唯一索引 {} 创建失败：台账中已存在同一桶同一自然日的多条有效读数，" +
                    "请先处理下列冲突（每日每桶只留一条有效，其余作废后重启服务），" +
                    "并发兜底索引才会生效。冲突明细：{}", UNIQUE_INDEX, listDuplicateValidReadings(), e);
        }
    }

    private List<Map<String, Object>> listDuplicateValidReadings() {
        try {
            return jdbcTemplate.queryForList(
                    "SELECT bucket_id AS bucketId, read_date AS readDate, COUNT(1) AS validCount, " +
                            "GROUP_CONCAT(id ORDER BY id) AS readingIds " +
                            "FROM " + TABLE + " " +
                            "WHERE status = 1 " +
                            "GROUP BY bucket_id, read_date HAVING COUNT(1) > 1");
        } catch (Exception ex) {
            log.warn("查询冲突读数失败", ex);
            return List.of();
        }
    }
}
