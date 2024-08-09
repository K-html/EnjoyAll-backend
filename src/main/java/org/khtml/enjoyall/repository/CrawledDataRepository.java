package org.khtml.enjoyall.repository;

import org.khtml.enjoyall.entity.CrawledData;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrawledDataRepository extends JpaRepository<CrawledData, Long>{
    boolean existsByUrl(String url);
}