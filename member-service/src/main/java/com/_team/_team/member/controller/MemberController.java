package com._team._team.member.controller;

import com._team._team.member.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/member")
public class MemberController {

    final MemberService memberService;

    @Autowired
    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/health")
    public String healthCheck(){
        return "OK2";
    }
    @GetMapping("/{id}")
    public ResponseEntity<?> getMember(@PathVariable Long id) {
        return ResponseEntity.ok(memberService.getMember(id));
    }
}
