package com._team._team.member.controller;

import com._team._team.annotation.Action;
import com._team._team.annotation.CheckPermission;
import com._team._team.annotation.Resource;
import com._team._team.dto.ApiResponse;
import com._team._team.member.dto.reqdto.ChangePasswordReqDto;
import com._team._team.member.dto.reqdto.LoginReqDto;
import com._team._team.member.dto.reqdto.MemberCreateReqDto;
import com._team._team.member.dto.resdto.LoginResDto;
import com._team._team.member.service.MemberService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/member")
public class MemberController {

    final MemberService memberService;

    @Autowired
    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }


    // 로그인
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginReqDto reqDto,
            HttpServletResponse response) {
        LoginResDto resDto = memberService.login(reqDto, response);
        return new ResponseEntity<>(
                ApiResponse.success(resDto, "로그인 성공"),
                HttpStatus.OK
        );
    }
    // 최초 로그인 비밀번호 변경
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestHeader("X-User-UUID") UUID memberId,
            @Valid @RequestBody ChangePasswordReqDto reqDto) {
        memberService.changePassword(memberId, reqDto);
        return new ResponseEntity<>(
                ApiResponse.success(null, "비밀번호 변경 성공"),
                HttpStatus.OK
        );
    }

    // 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestHeader("X-User-UUID") UUID memberId,
            HttpServletResponse response) {
        memberService.logout(memberId, response);
        return new ResponseEntity<>(
                ApiResponse.success(null, "로그아웃 성공"),
                HttpStatus.OK
        );
    }

    // AT 재발급
    @PostMapping("/generate-at")
    public ResponseEntity<?> generateAt(
            @RequestHeader("X-User-UUID") UUID memberId,
            @RequestHeader("X-User-MemberPositionId") UUID memberPositionId) {
        LoginResDto resDto = memberService.generateAt(memberId, memberPositionId);
        return new ResponseEntity<>(
                ApiResponse.success(resDto, "AT 재발급 성공"),
                HttpStatus.OK
        );
    }

    // 직원 계정 생성
    @CheckPermission(resource = Resource.MEMBER, action = Action.CREATE)
    @PostMapping("/create")
    public ResponseEntity<?> createMember(
            @RequestHeader("X-User-UUID") UUID memberId,
            @RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
            @Valid @RequestBody MemberCreateReqDto reqDto) {
        memberService.createMember(memberId, memberPositionId, reqDto);
        return new ResponseEntity<>(
                ApiResponse.success(null, "직원 계정 생성 성공"),
                HttpStatus.CREATED
        );
    }

    // 직원 목록 조회
    @CheckPermission(resource = Resource.MEMBER, action = Action.READ)
    @GetMapping("/list")
    public ResponseEntity<?> getMemberList(
            @RequestHeader("X-User-UUID") UUID memberId,
            @RequestHeader("X-User-MemberPositionId") UUID memberPositionId) {
        return new ResponseEntity<>(
                ApiResponse.success(memberService.getMemberList(memberId, memberPositionId), "직원 목록 조회 성공"),
                HttpStatus.OK
        );
    }

    // 직원 상세 조회
    @CheckPermission(resource = Resource.MEMBER, action = Action.READ)
    @GetMapping("/{targetMemberId}")
    public ResponseEntity<?> getMemberDetail(
            @RequestHeader("X-User-UUID") UUID memberId,
            @RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
            @PathVariable UUID targetMemberId) {
        return new ResponseEntity<>(
                ApiResponse.success(memberService.getMemberDetail(memberId, memberPositionId, targetMemberId), "직원 상세 조회 성공"),
                HttpStatus.OK
        );
    }

}
