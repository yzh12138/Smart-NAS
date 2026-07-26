package yzh.stock.business.utils;

import org.mindrot.jbcrypt.BCrypt;

/**
 * 密码工具类 — 使用 BCrypt 单向哈希
 *
 * BCrypt 特点：
 * - 单向哈希，不可逆（密钥泄露也无法还原密码）
 * - 自动加盐（每次生成不同哈希）
 * - 慢哈希（暴力破解成本极高）
 * - 哈希值固定60字符，兼容 VARCHAR(255)
 */
public class PasswordUtil {

    private static final int LOG_ROUNDS = 12;

    /**
     * 对明文密码进行 BCrypt 哈希
     */
    public static String hashPassword(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        return BCrypt.hashpw(plainText, BCrypt.gensalt(LOG_ROUNDS));
    }

    /**
     * 验证明文密码是否与 BCrypt 哈希匹配
     */
    public static boolean matches(String plainText, String hashedPassword) {
        if (plainText == null || hashedPassword == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainText, hashedPassword);
        } catch (Exception e) {
            // 哈希格式不合法（可能是旧的AES密文），返回false
            return false;
        }
    }

    /**
     * 判断密码是否为 BCrypt 哈希格式（以 $2a$ 或 $2b$ 开头）
     */
    public static boolean isBCryptHash(String password) {
        if (password == null) return false;
        return password.startsWith("$2a$") || password.startsWith("$2b$");
    }

    /**
     * 兼容旧方法名 — 加密（实际是哈希）
     */
    public static String encrypt(String plainText) {
        return hashPassword(plainText);
    }
}
