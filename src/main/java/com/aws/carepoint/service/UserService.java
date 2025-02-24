package com.aws.carepoint.service;

import com.aws.carepoint.dto.UsersDto;
import com.aws.carepoint.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static com.aws.carepoint.util.RandomPassword.generateRandomPassword;

@Slf4j
@Service
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final SmsService smsService;

    @Autowired
    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder, SmsService smsService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.smsService = smsService;
    }



    public void userSignUp(UsersDto usersDto) {     // 일반회원가입
        // 아이디 중복 검사
        if (userMapper.countByUserId(usersDto.getUserId()) > 0) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다. 다른 아이디를 입력해주세요.");
        }
        // 닉네임 중복 검사
        if (userMapper.countByUserNick(usersDto.getUserNick()) > 0) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다. 다른 닉네임을 입력해주세요.");
        }
        // 비밀번호 암호화
        String encodedPwd = passwordEncoder.encode(usersDto.getUserPwd());
        usersDto.setUserPwd(encodedPwd);
        userMapper.insertUser(usersDto);
    }


    // 로그인
    public UsersDto checkId(String userId) {
        UsersDto usersDto = userMapper.findByUserId(userId);
        if (usersDto != null) {
        } else {
            System.out.println("DB에서 해당 userId를 찾을 수 없음");
        }
        return usersDto;
    }

    public boolean checkPwd(String rawPwd, String encodedPwd) { //(사용자가 입력한 비번,암호화된 비번 대조하기)
        return passwordEncoder.matches(rawPwd, encodedPwd);
    }


    public UsersDto processKakaoLogin(Map<String, Object> kakaoUser, HttpSession session) {
        try {
            //  1. JSON 형식으로 변환 후 콘솔 출력
            ObjectMapper objectMapper = new ObjectMapper();
            String kakaoUserJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(kakaoUser);
        } catch (Exception e) {     // 오류 출력
        }

        // 2. 카카오에서 받은 사용자 정보 파싱
        String kakaoId = kakaoUser.get("id").toString();
        Map<String, Object> properties = (Map<String, Object>) kakaoUser.get("properties");
        Map<String, Object> kakaoAccount = (Map<String, Object>) kakaoUser.get("kakao_account");

        // 3. 필요한 데이터 추출 삼항연산
        String nickname = properties.getOrDefault("nickname", "No Nickname").toString();  // 닉네임
        String email = (kakaoAccount != null && kakaoAccount.containsKey("email"))
                ? kakaoAccount.get("email").toString()
                : "no-email";  // 이메일
        String phone = (kakaoAccount != null && kakaoAccount.containsKey("phone_number"))
                ? kakaoAccount.get("phone_number").toString()
                : "no-phone";  // 전화번호

        String randomPwd = generateRandomPassword();  // 랜덤 비밀번호 생성 후 암호화

        // 4. DB에서 기존 회원 조회
        UsersDto existingUser = userMapper.findByEmail(email);

        if (existingUser == null) { // 회원 이메일 없으면
            // 5. 자동 회원가입
            UsersDto newUser = new UsersDto();
            newUser.setUserId(kakaoId); // 카카오 ID를 userId로 저장
            newUser.setUserNick(nickname);
            newUser.setUserPwd(passwordEncoder.encode(randomPwd)); // 비밀번호 암호화 후 저장
            newUser.setEmail(email);
            newUser.setPhone(phone);
            newUser.setSocialLoginStatus(1); // 소셜 로그인 유저

            userMapper.insertUser(newUser);  // DB에 신규 회원 저장
            existingUser = newUser; // 신규 회원 정보 저장
            //System.out.println("새로운 카카오 사용자 회원가입 완료! (ID: " + existingUser + ")");
        } else {
            //System.out.println("기존 카카오 사용자 로그인 성공! (ID: " + existingUser + ")");
        }


        return existingUser;
    }

    @Transactional
    public boolean resetPasswordAndSendSMS(String userName, String userId, String phone) {
        UsersDto usersDto = userMapper.findUserByNameAndIdAndPhone(userName, userId, phone);

        if (usersDto == null) {
            System.out.println("일치하는 회원 정보 없음: userName=" + userName + ", userId=" + userId + ", phone=" + phone);
            return false;  // 회원 정보 없음
        }

// 조회된 정보 출력 (usersDto가 null이 아닐 때만)
        System.out.println("DB 조회 성공: userPk=" + usersDto.getUserPk());

        // 2. 임시 비밀번호 생성
        String tempPassword = generateRandomPassword();
        String encodedPassword = passwordEncoder.encode(tempPassword);

        // 3. 데이터베이스에서 비밀번호 업데이트

        int updatedRows = userMapper.updateUserPassword(usersDto.getUserPk(), encodedPassword);
        if (updatedRows == 0) {
            System.out.println("비밀번호 업데이트 실패: userPk=" + usersDto.getUserPk());
            return false;
        }

        // 4. 문자 발송
        String message = "임시 비밀번호: " + tempPassword + " (로그인 후 변경해주세요)";
        System.out.println("비밀번호 변경시 전송되는 메시지 ==================== > " + message);
        smsService.sendSms(phone, message);

        return true;
    }

    public UsersDto checkUserByPk(int userPk) {
        return userMapper.findByUserPk(userPk);
    }

    @Transactional
    public boolean markUserAsDeleted(int userPk) {
        return userMapper.updateDelStatus(userPk) > 0;
    }

    public void modifyUserPwd(int userPk, String newPassword) {
        log.info("비밀번호 변경 요청 - UserPK: {}", userPk);
        userMapper.modifyUserPwd(userPk, newPassword);
        log.info("비밀번호 변경 완료 - UserPK: {}", userPk);
    }


}
