package com.aws.carepoint.service;


import com.aws.carepoint.mapper.UserMapper;
import net.nurigo.sdk.NurigoApp;
import net.nurigo.sdk.message.model.Message;
import net.nurigo.sdk.message.service.DefaultMessageService;
import net.nurigo.sdk.message.exception.NurigoMessageNotReceivedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SmsService {

    private final DefaultMessageService messageService;
    private final UserMapper userMapper;
    @Value("${coolsms.sender.phone}")
    private String senderPhone;


    public SmsService(
            @Value("${coolsms.api.key}") String apiKey,
            @Value("${coolsms.api.secret}") String apiSecret,
            UserMapper userMapper  // DI (의존성 주입)
    ) {
        this.messageService = NurigoApp.INSTANCE.initialize(apiKey, apiSecret, "https://api.coolsms.co.kr");
        this.userMapper = userMapper;
    }

    // 전화번호 정규화 (82+ 제거 및 010 형식 변환)
    public static String normalizePhoneNumber(String phone) {
        if (phone == null || phone.isEmpty()) {
            return null;
        }

        // 숫자와 '+'만 남기기
        phone = phone.replaceAll("[^0-9+]", "");

        // +82로 시작하면 010 형태로 변환
        if (phone.startsWith("+82")) {
            phone = phone.replace("+82", "0");
        } else if (phone.startsWith("82")) {
            phone = phone.replaceFirst("82", "0");
        }

        // 010 형식이 아닐 경우 예외 처리 (필요시 추가)
        if (!phone.startsWith("010")) {
            System.out.println("유효하지 않은 전화번호: " + phone);
            return null;
        }

        return phone;
    }


    // 모든 회원에게 문자 전송
    @Scheduled(cron = "0 30 19 * * ?")
    public void sendSmsToAllUsers() {
        System.out.println(" 모든 회원에게 문자 전송을 시작합니다...");

        List<String> phoneNumbers = userMapper.getAllUserPhoneNumbers();
        String messageText = "오늘의 식단과 운동기록을 하셨나요? 안하셨다면 지금 기록해보세요 !";

        for (String rawPhoneNumber : phoneNumbers) {
            String normalizedPhone = normalizePhoneNumber(rawPhoneNumber);
            if (normalizedPhone != null) {
                sendSms(normalizedPhone, messageText);
            }
        }

        System.out.println("모든 회원에게 문자 전송 완료!");
    }

    public void sendSms(String to, String text) {
        if (to == null || to.isEmpty()) {
            System.out.println("수신자의 전화번호가 없습니다.");
            return;
        }

        Message message = new Message();
        message.setFrom(senderPhone);  // 설정에서 불러온 발신번호
        message.setTo(to);
        message.setText(text);

        try {
            messageService.send(message);
            System.out.println("문자 전송 성공! (수신자: " + to + ")");
        } catch (NurigoMessageNotReceivedException e) {
            System.out.println("문자 전송 실패: " + e.getMessage());
            System.out.println("실패한 메시지: " + e.getFailedMessageList());
        } catch (Exception e) {
            System.out.println("예외 발생: " + e.getMessage());
        }
    }
}