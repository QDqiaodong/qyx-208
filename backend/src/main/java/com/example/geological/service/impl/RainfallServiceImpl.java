package com.example.geological.service.impl;

import com.example.geological.dto.RainBucketDTO;
import com.example.geological.dto.RainDailyTotalDTO;
import com.example.geological.dto.RainReadingRecordDTO;
import com.example.geological.dto.RainReadingUpdateDTO;
import com.example.geological.dto.RainReadingVoidDTO;
import com.example.geological.entity.RainBucket;
import com.example.geological.entity.RainDailyTotal;
import com.example.geological.entity.RainReading;
import com.example.geological.exception.RainReadingConflictException;
import com.example.geological.repository.RainBucketRepository;
import com.example.geological.repository.RainDailyTotalRepository;
import com.example.geological.repository.RainReadingRepository;
import com.example.geological.service.RainfallService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RainfallServiceImpl implements RainfallService {

    private final RainBucketRepository bucketRepository;
    private final RainReadingRepository readingRepository;
    private final RainDailyTotalRepository totalRepository;
    private final JdbcTemplate jdbcTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    /** 毫米数统一保留 0.1 毫米精度 */
    private double normalizeMm(double v) {
        return BigDecimal.valueOf(v).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private double requireNonNegativeMm(Double mm, String fieldLabel) {
        if (mm == null) {
            throw new IllegalArgumentException(fieldLabel + "不能为空");
        }
        if (mm < 0) {
            throw new IllegalArgumentException(fieldLabel + "不能为负数: " + mm);
        }
        return normalizeMm(mm);
    }

    private LocalDate parseDate(String date, String fieldLabel) {
        if (date == null || date.trim().isEmpty()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(date.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(fieldLabel + "格式不正确，应为 yyyy-MM-dd: " + date);
        }
    }

    private RainBucket lockBucket(Long bucketId) {
        return bucketRepository.findByIdForUpdate(bucketId)
                .orElseThrow(() -> new IllegalArgumentException("收集桶不存在: " + bucketId));
    }

    /**
     * 改数/作废的公共前置：先按 id 找到读数属于哪只桶，锁住桶行，
     * 再在锁内重读这条读数——拿到的一定是其他值班已提交后的最新状态，
     * 版本核对、状态判定都基于这个不会再被别人改动的快照。
     */
    private RainReading lockAndRefresh(Long readingId) {
        RainReading reading = readingRepository.findById(readingId)
                .orElseThrow(() -> new IllegalArgumentException("雨量读数不存在: " + readingId));
        lockBucket(reading.getBucket().getId());
        entityManager.refresh(reading);
        return reading;
    }

    /**
     * 版本核对：两名值班抢着动同一桶同一日时，先提交的已把版本 +1，
     * 后提交的 expectedVersion 对不上即 409——同一条读数的并发改动只成一次。
     */
    private void requireVersion(RainReading reading, Integer expectedVersion) {
        if (expectedVersion == null) {
            throw new IllegalArgumentException("缺少读数版本号：请刷新列表后再操作");
        }
        if (!Objects.equals(reading.getVersion(), expectedVersion)) {
            throw new RainReadingConflictException(String.format(
                    "桶[%s] %s 的读数刚被其他值班改动过（当前版本 %d，你手里的是版本 %d），本次操作未生效；请刷新列表后重试",
                    reading.getBucket().getBucketNo(), reading.getReadDate(),
                    reading.getVersion(), expectedVersion));
        }
    }

    /**
     * 重算某自然日的气象卡片合计并落库：合计 = 该日各桶有效读数之和（作废不计）。
     *
     * 本方法只在写事务内、持有桶行锁之后调用；先 INSERT IGNORE 确保卡片行存在，
     * 再对该行加悲观写锁——改不同桶但同一自然日的两笔写在此串行，
     * 后到者重算时看得到先到者已提交的读数，合计不会丢笔。
     * 读数改动与卡片重算在同一事务：任一失败整体回滚，卡片仍是动手前的数。
     */
    private void refreshDailyTotal(LocalDate date) {
        jdbcTemplate.update(
                "INSERT IGNORE INTO rain_daily_total (read_date, total_mm, update_time) VALUES (?, 0, NOW())",
                Date.valueOf(date));
        RainDailyTotal total = totalRepository.findByReadDateForUpdate(date)
                .orElseThrow(() -> new IllegalStateException("气象卡片行缺失: " + date));
        Double sum = readingRepository.sumValidByReadDate(date);
        total.setTotalMm(normalizeMm(sum == null ? 0.0 : sum));
        totalRepository.save(total);
    }

    @Override
    @Transactional
    public RainBucket createBucket(RainBucketDTO dto) {
        String bucketNo = dto.getBucketNo() == null ? "" : dto.getBucketNo().trim();
        if (bucketNo.isEmpty()) {
            throw new IllegalArgumentException("桶号不能为空");
        }
        if (bucketRepository.findByBucketNo(bucketNo).isPresent()) {
            throw new IllegalArgumentException("桶号已存在，不能重复建档: " + bucketNo);
        }
        RainBucket bucket = new RainBucket();
        bucket.setBucketNo(bucketNo);
        bucket.setRemark(dto.getRemark());
        return bucketRepository.save(bucket);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RainBucket> findAllBuckets() {
        return bucketRepository.findAllByOrderByIdAsc();
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public RainReading record(RainReadingRecordDTO dto) {
        double mm = requireNonNegativeMm(dto.getMillimeters(), "毫米数");
        LocalDate date = parseDate(dto.getReadDate(), "自然日");

        // 先锁桶行：同一桶的记/改/作废在此串行，锁内查重才是最终口径
        RainBucket bucket = lockBucket(dto.getBucketId());

        List<RainReading> valid = readingRepository.findByBucket_IdAndReadDateAndStatus(
                bucket.getId(), date, RainReading.STATUS_VALID);
        if (!valid.isEmpty()) {
            RainReading existing = valid.get(0);
            throw new IllegalArgumentException(String.format(
                    "桶[%s]在 %s 已有一条有效读数 %.1f 毫米（台账#%d），同一桶同一自然日只留一条有效读数；" +
                            "请对那条读数改数或作废，本次登记未落账",
                    bucket.getBucketNo(), date, existing.getMillimeters(), existing.getId()));
        }

        RainReading reading = new RainReading();
        reading.setBucket(bucket);
        reading.setReadDate(date);
        reading.setMillimeters(mm);
        reading.setStatus(RainReading.STATUS_VALID);
        reading.setVersion(1);
        reading.setRemark(dto.getRemark());
        RainReading saved = readingRepository.save(reading);

        // 同一事务内重算气象卡片当日合计：只落读数、不动卡片不算完成
        refreshDailyTotal(date);
        return saved;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public RainReading updateMillimeters(Long id, RainReadingUpdateDTO dto) {
        double mm = requireNonNegativeMm(dto.getMillimeters(), "毫米数");

        RainReading reading = lockAndRefresh(id);
        if (!Objects.equals(reading.getStatus(), RainReading.STATUS_VALID)) {
            throw new IllegalArgumentException(String.format(
                    "该读数已作废（作废原因：%s），作废读数不能再改毫米数；该桶该日如需读数请重新记一笔",
                    reading.getVoidReason()));
        }
        requireVersion(reading, dto.getExpectedVersion());

        reading.setMillimeters(mm);
        reading.setVersion(reading.getVersion() + 1);
        RainReading saved = readingRepository.save(reading);

        // 改数与卡片合计重算同一事务：改完卡片立即按各桶有效读数相加
        refreshDailyTotal(reading.getReadDate());
        return saved;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public RainReading voidReading(Long id, RainReadingVoidDTO dto) {
        String reason = dto.getVoidReason() == null ? "" : dto.getVoidReason().trim();
        if (reason.isEmpty()) {
            throw new IllegalArgumentException("作废原因不能为空：作废必须留下原因");
        }

        RainReading reading = lockAndRefresh(id);
        if (!Objects.equals(reading.getStatus(), RainReading.STATUS_VALID)) {
            throw new IllegalArgumentException("该读数已是作废状态，不能重复作废");
        }
        requireVersion(reading, dto.getExpectedVersion());

        reading.setStatus(RainReading.STATUS_VOID);
        reading.setVoidReason(reason);
        reading.setVoidTime(LocalDateTime.now());
        reading.setVersion(reading.getVersion() + 1);
        RainReading saved = readingRepository.save(reading);

        // 作废与卡片合计重算同一事务：合计里立即不再算这一笔
        refreshDailyTotal(reading.getReadDate());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RainReading> findReadings(Long bucketId, String readDate, Integer status) {
        LocalDate date = (readDate == null || readDate.trim().isEmpty()) ? null : parseDate(readDate, "自然日");
        return readingRepository.findAllWithBucket().stream()
                .filter(r -> bucketId == null || Objects.equals(r.getBucket().getId(), bucketId))
                .filter(r -> date == null || Objects.equals(r.getReadDate(), date))
                .filter(r -> status == null || Objects.equals(r.getStatus(), status))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RainDailyTotalDTO> findCards(String from, String to) {
        LocalDate fromDate = (from == null || from.trim().isEmpty()) ? null : parseDate(from, "起始日期");
        LocalDate toDate = (to == null || to.trim().isEmpty()) ? null : parseDate(to, "截止日期");
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("起始日期不能晚于截止日期");
        }

        List<RainDailyTotal> totals;
        if (fromDate == null && toDate == null) {
            totals = totalRepository.findAllByOrderByReadDateDesc();
        } else {
            totals = totalRepository.findByReadDateBetweenOrderByReadDateDesc(
                    fromDate != null ? fromDate : LocalDate.of(1970, 1, 1),
                    toDate != null ? toDate : LocalDate.of(9999, 12, 31));
        }

        Map<LocalDate, Long> validCounts = readingRepository.countValidGroupByReadDate().stream()
                .collect(Collectors.toMap(row -> (LocalDate) row[0], row -> (Long) row[1]));

        return totals.stream().map(t -> {
            RainDailyTotalDTO dto = new RainDailyTotalDTO();
            dto.setReadDate(t.getReadDate().toString());
            dto.setTotalMm(t.getTotalMm());
            dto.setValidCount(validCounts.getOrDefault(t.getReadDate(), 0L).intValue());
            dto.setUpdateTime(t.getUpdateTime() != null ? t.getUpdateTime().toString() : null);
            return dto;
        }).toList();
    }
}
