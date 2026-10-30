package com.example.aichat.dto.audit;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 通用分页结果包装。
 *
 * @param <T> 列表元素类型
 */
@Data
public class PageResult<T> {

    private List<T> records = new ArrayList<>();
    private long total;
    private int page;
    private int size;

    public static <T> PageResult<T> of(List<T> records, long total, int page, int size) {
        PageResult<T> r = new PageResult<>();
        r.setRecords(records == null ? new ArrayList<>() : records);
        r.setTotal(total);
        r.setPage(page);
        r.setSize(size);
        return r;
    }
}
