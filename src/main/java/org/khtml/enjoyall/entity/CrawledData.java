package org.khtml.enjoyall.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class CrawledData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String url;

    private String title;
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String content;
}
