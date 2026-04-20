package com.lab.recruitment.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.recruitment.config.EmailAuthProperties;
import com.lab.recruitment.entity.EmailAuthCode;
import com.lab.recruitment.mapper.EmailAuthCodeMapper;
import com.lab.recruitment.service.EmailAuthCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class EmailAuthCodeServiceImpl extends ServiceImpl<EmailAuthCodeMapper, EmailAuthCode> implements EmailAuthCodeService {

    private static final String PURPOSE_REGISTER = "register";
    private static final String PURPOSE_RESET_PASSWORD = "reset_password";

    @Autowired
    private JavaMailSender javaMailSender;

    @Autowired
    private EmailAuthProperties emailAuthProperties;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Override
    @Transactional
    public void sendRegisterCode(String studentId, String email) {
        sendCode(studentId, email, PURPOSE_REGISTER, "实验室招新系统注册验证码", buildRegisterContent(studentId));
    }

    @Override
    @Transactional
    public void sendPasswordResetCode(String username, String email) {
        sendCode(username, email, PURPOSE_RESET_PASSWORD, "实验室招新系统密码找回验证码", buildResetContent(username));
    }

    @Override
    @Transactional
    public void verifyRegisterCode(String studentId, String email, String code) {
        verifyCode(studentId, email, PURPOSE_REGISTER, code);
    }

    @Override
    @Transactional
    public void verifyPasswordResetCode(String username, String email, String code) {
        verifyCode(username, email, PURPOSE_RESET_PASSWORD, code);
    }

    private void sendCode(String account, String email, String purpose, String subject, String content) {
        String normalizedAccount = normalizeAccount(account);
        String normalizedEmail = normalizeEmail(email);

        ensureMailConfigured();
        ensureResendAllowed(normalizedAccount, normalizedEmail, purpose);

        this.lambdaUpdate()
                .eq(EmailAuthCode::getAccount, normalizedAccount)
                .eq(EmailAuthCode::getEmail, normalizedEmail)
                .eq(EmailAuthCode::getPurpose, purpose)
                .eq(EmailAuthCode::getIsUsed, 0)
                .set(EmailAuthCode::getIsUsed, 1)
                .update();

        EmailAuthCode authCode = new EmailAuthCode();
        authCode.setAccount(normalizedAccount);
        authCode.setEmail(normalizedEmail);
        authCode.setPurpose(purpose);
        authCode.setCode(generateCode());
        authCode.setExpireTime(LocalDateTime.now().plusMinutes(emailAuthProperties.getCodeExpireMinutes()));
        authCode.setIsUsed(0);

        sendEmail(normalizedEmail, subject, content.replace("{code}", authCode.getCode()));
        this.save(authCode);
    }

    private void verifyCode(String account, String email, String purpose, String code) {
        String normalizedAccount = normalizeAccount(account);
        String normalizedEmail = normalizeEmail(email);
        String normalizedCode = normalizeCode(code);

        if (!StringUtils.hasText(normalizedCode)) {
            throw new RuntimeException("验证码不能为空");
        }

        EmailAuthCode latestCode = this.lambdaQuery()
                .eq(EmailAuthCode::getAccount, normalizedAccount)
                .eq(EmailAuthCode::getEmail, normalizedEmail)
                .eq(EmailAuthCode::getPurpose, purpose)
                .eq(EmailAuthCode::getIsUsed, 0)
                .orderByDesc(EmailAuthCode::getId)
                .last("LIMIT 1")
                .one();

        if (latestCode == null) {
            throw new RuntimeException("请先获取邮箱验证码");
        }
        if (latestCode.getExpireTime() == null || latestCode.getExpireTime().isBefore(LocalDateTime.now())) {
            latestCode.setIsUsed(1);
            this.updateById(latestCode);
            throw new RuntimeException("验证码已过期，请重新获取");
        }
        if (!normalizedCode.equals(latestCode.getCode())) {
            throw new RuntimeException("验证码错误");
        }

        latestCode.setIsUsed(1);
        this.updateById(latestCode);
    }

    private void ensureResendAllowed(String account, String email, String purpose) {
        EmailAuthCode latestCode = this.lambdaQuery()
                .eq(EmailAuthCode::getAccount, account)
                .eq(EmailAuthCode::getEmail, email)
                .eq(EmailAuthCode::getPurpose, purpose)
                .orderByDesc(EmailAuthCode::getId)
                .last("LIMIT 1")
                .one();

        if (latestCode == null || latestCode.getCreateTime() == null) {
            return;
        }

        long seconds = Duration.between(latestCode.getCreateTime(), LocalDateTime.now()).getSeconds();
        long remaining = emailAuthProperties.getResendIntervalSeconds() - seconds;
        if (remaining > 0) {
            throw new RuntimeException("验证码发送过于频繁，请 " + remaining + " 秒后再试");
        }
    }

    private void sendEmail(String to, String subject, String text) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(mailUsername, emailAuthProperties.getFromName());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, false);
            javaMailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("验证码发送失败，请稍后重试");
        }
    }

    private void ensureMailConfigured() {
        if (!StringUtils.hasText(mailUsername) || !StringUtils.hasText(mailPassword)) {
            throw new RuntimeException("邮箱服务未配置，请先设置 MAIL_PASSWORD（QQ 邮箱 SMTP 授权码）");
        }
    }

    private String generateCode() {
        return String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
    }

    private String buildRegisterContent(String studentId) {
        return "你正在注册实验室招新系统账号。\n"
                + "学号：" + studentId + "\n"
                + "验证码：{code}\n"
                + "验证码 " + emailAuthProperties.getCodeExpireMinutes() + " 分钟内有效，请勿泄露给他人。";
    }

    private String buildResetContent(String username) {
        return "你正在找回实验室招新系统账号密码。\n"
                + "账号：" + username + "\n"
                + "验证码：{code}\n"
                + "验证码 " + emailAuthProperties.getCodeExpireMinutes() + " 分钟内有效，请勿泄露给他人。";
    }

    private String normalizeAccount(String value) {
        String trimmed = value == null ? null : value.trim();
        return StringUtils.hasText(trimmed) ? trimmed : null;
    }

    private String normalizeEmail(String value) {
        String trimmed = value == null ? null : value.trim();
        return StringUtils.hasText(trimmed) ? trimmed.toLowerCase() : null;
    }

    private String normalizeCode(String value) {
        String trimmed = value == null ? null : value.trim();
        return StringUtils.hasText(trimmed) ? trimmed : null;
    }
}
