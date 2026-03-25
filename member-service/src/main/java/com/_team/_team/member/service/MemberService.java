package com._team._team.member.service;

import com._team._team.member.repository.MemberRepository;
import jakarta.persistence.EntityNotFoundException;
import org.apache.kafka.common.protocol.types.Field;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
public class MemberService {

    final MemberRepository memberRepository;

    @Autowired
    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public String getMember(Long id) {

        // id 별로 각각 다른 예외 발생
        if (id == 1) {
            throw new IllegalArgumentException("잘못된 요청입니다.");   // 400
        }
        if (id == 2) {
            throw new NoSuchElementException("회원을 찾을 수 없습니다."); // 404
        }
        if (id == 3) {
            throw new EntityNotFoundException("엔티티를 찾을 수 없습니다."); // 404
        }
        if (id == 4) {
            throw new IllegalStateException("잘못된 상태입니다.");       // 400
        }
        if (id == 5) {
            throw new RuntimeException("런타임 에러입니다.");            // 500
        }

        return "정상 응답입니다. id: " + id;
    }
}

