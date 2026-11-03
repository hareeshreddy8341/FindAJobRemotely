package com.findajobremotely.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class Paging {

    public static final int MAX_PAGE_SIZE = 50;

    private Paging() {
    }

    /** Clamps client-supplied paging values so nobody can request thousands of rows at once. */
    public static Pageable of(int page, int size, Sort sort) {
        int safePage = Math.max(0, Math.min(page, 10_000));
        int safeSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        return PageRequest.of(safePage, safeSize, sort);
    }
}
