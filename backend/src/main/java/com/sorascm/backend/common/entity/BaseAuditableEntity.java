package com.sorascm.backend.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class BaseAuditableEntity {
    @CreatedDate
    @Column(name = "created_at ", nullable = false )
    private Instant created_at;

    @LastModifiedDate
    @Column(name= "updated_at", nullable = false)
    private Instant updated_at;

    private Instant getCreated_at(){
        return created_at;

    }

    private Instant getUpdated_at (){
        return updated_at;

    }
}
