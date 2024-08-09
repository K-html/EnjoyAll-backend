package org.khtml.enjoyall.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class Board {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String img_url;

    private String title;

    private CATEGORY category;

    private String eligibility;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    private String contact;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "apply_method")
    private String applyMethod;

    @Column(name = "view_count")
    private Long viewCount;

}
