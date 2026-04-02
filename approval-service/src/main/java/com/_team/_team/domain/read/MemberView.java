package com._team._team.domain.read;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

@Entity
@Immutable
@Getter
@NoArgsConstructor
public class MemberView {
    @Id
    private UUID memberId;

    private String name;
}
