package yzh.stock.business.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import yzh.stock.business.entity.SysUser;
import yzh.stock.business.mapper.SysUserMapper;
import yzh.stock.business.utils.PasswordUtil;

import java.util.List;

/**
 * 数据初始化器：启动时自动迁移旧密码到 BCrypt
 *
 * 迁移策略：
 * 1. 已是 BCrypt 哈希（$2a$开头）→ 跳过
 * 2. 旧 AES 密文 → 尝试解密后重新 BCrypt 哈希
 * 3. 明文密码 → 直接 BCrypt 哈希
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final SysUserMapper userMapper;

    public DataInitializer(SysUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public void run(String... args) {
        migratePasswordsToBCrypt();
    }

    /**
     * 迁移所有旧格式密码到 BCrypt
     */
    private void migratePasswordsToBCrypt() {
        List<SysUser> users = userMapper.selectList(null);
        int migrated = 0;
        int alreadyBCrypt = 0;

        for (SysUser user : users) {
            String password = user.getPassword();
            if (password == null || password.isEmpty()) {
                continue;
            }

            // 已经是 BCrypt 格式，跳过
            if (PasswordUtil.isBCryptHash(password)) {
                alreadyBCrypt++;
                continue;
            }

            // 旧格式密码，尝试解密后重新哈希
            String plainPassword = tryDecryptOldPassword(password);
            if (plainPassword != null) {
                String hashed = PasswordUtil.hashPassword(plainPassword);
                user.setPassword(hashed);
                userMapper.updateById(user);
                migrated++;
                log.info("已迁移用户密码到 BCrypt: {}", user.getUsername());
            } else {
                // 既不是 BCrypt 也无法解密，可能是明文，直接哈希
                String hashed = PasswordUtil.hashPassword(password);
                user.setPassword(hashed);
                userMapper.updateById(user);
                migrated++;
                log.info("已将明文密码哈希为 BCrypt: {}", user.getUsername());
            }
        }

        if (migrated > 0) {
            log.info("密码迁移完成：{} 个用户已迁移到 BCrypt，{} 个用户已是 BCrypt 格式", migrated, alreadyBCrypt);
        } else if (alreadyBCrypt > 0) {
            log.info("所有 {} 个用户的密码已是 BCrypt 格式，无需迁移", alreadyBCrypt);
        }
    }

    /**
     * 尝试用旧 AES 密钥解密密码
     * 如果解密失败返回 null
     */
    private String tryDecryptOldPassword(String cipherText) {
        try {
            // 尝试用默认 AES 密钥解密
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/ECB/PKCS5Padding");
            byte[] keyBytes = new byte[16];
            byte[] defaultKey = "smart-nas-pwd-key".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            System.arraycopy(defaultKey, 0, keyBytes, 0, Math.min(defaultKey.length, 16));
            javax.crypto.spec.SecretKeySpec keySpec = new javax.crypto.spec.SecretKeySpec(keyBytes, "AES");
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, keySpec);
            byte[] decrypted = cipher.doFinal(java.util.Base64.getDecoder().decode(cipherText));
            return new String(decrypted, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            // 解密失败，不是旧 AES 格式
            return null;
        }
    }
}
