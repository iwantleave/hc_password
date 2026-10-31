package org.example.service;

import org.example.db.DatabaseManager;
import org.example.db.EntryRepository;
import org.example.db.EntryRepository.StoredSecret;
import org.example.model.PasswordEntry;
import org.example.security.CryptoService;
import org.example.security.VaultException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public final class VaultService {
    private static final String META_SALT = "master_salt";
    private static final String META_ITERATIONS = "kdf_iterations";
    private static final String META_VERIFIER = "master_verifier";
    private static final String VERIFIER_TEXT = "HC_PASSWORD_VAULT_OK";
    private static final int DEFAULT_ITERATIONS = 210_000;
    private static final int MIN_MASTER_PASSWORD_LENGTH = 8;
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final DatabaseManager database;
    private final EntryRepository repository;
    private final CryptoService crypto = new CryptoService();

    private SecretKey key;
    private int iterations = DEFAULT_ITERATIONS;

    public VaultService(DatabaseManager database) {
        this.database = database;
        this.repository = new EntryRepository(database);
    }

    public synchronized boolean isInitialized() throws VaultException {
        try {
            return database.getMeta(META_SALT) != null;
        } catch (SQLException e) {
            throw new VaultException("无法读取保险箱信息", e);
        }
    }

    public synchronized void initialize(char[] masterPassword) throws VaultException {
        validateMasterPassword(masterPassword);
        try {
            if (isInitialized()) {
                throw new VaultException("保险箱已经初始化");
            }

            byte[] newSalt = crypto.newSalt();
            SecretKey newKey = crypto.deriveKey(masterPassword, newSalt, DEFAULT_ITERATIONS);
            String verifier = crypto.encryptString(newKey, VERIFIER_TEXT);

            database.inTransaction(() -> {
                database.putMeta(META_SALT, Base64.getEncoder().encodeToString(newSalt));
                database.putMeta(META_ITERATIONS, Integer.toString(DEFAULT_ITERATIONS));
                database.putMeta(META_VERIFIER, verifier);
            });

            this.iterations = DEFAULT_ITERATIONS;
            this.key = newKey;
        } catch (GeneralSecurityException e) {
            throw new VaultException("创建保险箱失败", e);
        } catch (SQLException e) {
            throw new VaultException("保存保险箱信息失败", e);
        }
    }

    public synchronized void unlock(char[] masterPassword) throws VaultException {
        try {
            String storedSalt = database.getMeta(META_SALT);
            if (storedSalt == null) {
                throw new VaultException("保险箱尚未初始化");
            }
            String storedVerifier = database.getMeta(META_VERIFIER);
            if (storedVerifier == null) {
                throw new VaultException("保险箱数据不完整");
            }

            byte[] decodedSalt = Base64.getDecoder().decode(storedSalt);
            int storedIterations = readIterations(database.getMeta(META_ITERATIONS));
            SecretKey candidate = crypto.deriveKey(masterPassword, decodedSalt, storedIterations);
            String plainVerifier = crypto.decryptString(candidate, storedVerifier);

            if (!MessageDigest.isEqual(
                    plainVerifier.getBytes(StandardCharsets.UTF_8),
                    VERIFIER_TEXT.getBytes(StandardCharsets.UTF_8))) {
                throw new GeneralSecurityException("验证失败");
            }

            this.iterations = storedIterations;
            this.key = candidate;
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            this.key = null;
            throw new VaultException("主密码错误", e);
        } catch (SQLException e) {
            throw new VaultException("读取保险箱失败", e);
        }
    }

    public synchronized void lock() {
        this.key = null;
    }

    public synchronized boolean isUnlocked() {
        return key != null;
    }

    public synchronized List<PasswordEntry> listEntries(String keyword, String category)
            throws VaultException {
        SecretKey currentKey = requireKey();
        try {
            List<PasswordEntry> entries = repository.search(keyword, category);
            for (PasswordEntry entry : entries) {
                entry.setPassword(decryptValue(currentKey, entry.getPassword()));
                entry.setNotes(decryptValue(currentKey, entry.getNotes()));
            }
            return entries;
        } catch (SQLException e) {
            throw new VaultException("读取密码记录失败", e);
        } catch (GeneralSecurityException e) {
            throw new VaultException("解密密码记录失败，数据可能已损坏", e);
        }
    }

    public synchronized long addEntry(PasswordEntry entry) throws VaultException {
        SecretKey currentKey = requireKey();
        validateEntry(entry);
        String now = now();
        try {
            PasswordEntry stored = prepareForStorage(entry.copy(), currentKey, now);
            long id = repository.insert(stored);
            entry.setId(id);
            entry.setCreatedAt(now);
            entry.setUpdatedAt(now);
            return id;
        } catch (SQLException e) {
            throw new VaultException("保存密码记录失败", e);
        } catch (GeneralSecurityException e) {
            throw new VaultException("加密密码记录失败", e);
        }
    }

    public synchronized void updateEntry(PasswordEntry entry) throws VaultException {
        SecretKey currentKey = requireKey();
        validateEntry(entry);
        if (entry.getId() <= 0) {
            throw new VaultException("无效的密码记录");
        }
        String now = now();
        try {
            PasswordEntry stored = prepareForStorage(entry.copy(), currentKey, now);
            repository.update(stored);
            entry.setUpdatedAt(now);
        } catch (SQLException e) {
            throw new VaultException("更新密码记录失败", e);
        } catch (GeneralSecurityException e) {
            throw new VaultException("加密密码记录失败", e);
        }
    }

    public synchronized void deleteEntry(long id) throws VaultException {
        requireKey();
        try {
            repository.delete(id);
        } catch (SQLException e) {
            throw new VaultException("删除密码记录失败", e);
        }
    }

    public synchronized List<String> getCategories() throws VaultException {
        requireKey();
        try {
            return repository.findCategories();
        } catch (SQLException e) {
            throw new VaultException("读取分类失败", e);
        }
    }

    public synchronized void changeMasterPassword(char[] newMasterPassword) throws VaultException {
        validateMasterPassword(newMasterPassword);
        SecretKey oldKey = requireKey();

        try {
            byte[] newSalt = crypto.newSalt();
            SecretKey newKey = crypto.deriveKey(newMasterPassword, newSalt, iterations);
            List<StoredSecret> reencrypted = new ArrayList<>();

            for (StoredSecret secret : repository.findAllSecrets()) {
                String password = decryptValue(oldKey, secret.password());
                String notes = decryptValue(oldKey, secret.notes());
                reencrypted.add(new StoredSecret(
                        secret.id(),
                        crypto.encryptString(newKey, password),
                        crypto.encryptString(newKey, notes)));
            }

            String newVerifier = crypto.encryptString(newKey, VERIFIER_TEXT);
            String encodedSalt = Base64.getEncoder().encodeToString(newSalt);
            String encodedIterations = Integer.toString(iterations);

            database.inTransaction(() -> {
                for (StoredSecret secret : reencrypted) {
                    repository.updateSecrets(secret.id(), secret.password(), secret.notes());
                }
                database.putMeta(META_SALT, encodedSalt);
                database.putMeta(META_ITERATIONS, encodedIterations);
                database.putMeta(META_VERIFIER, newVerifier);
            });

            this.key = newKey;
        } catch (GeneralSecurityException e) {
            throw new VaultException("修改主密码失败", e);
        } catch (SQLException e) {
            throw new VaultException("保存新主密码失败", e);
        }
    }

    private PasswordEntry prepareForStorage(PasswordEntry entry, SecretKey currentKey, String now)
            throws GeneralSecurityException {
        entry.setTitle(entry.getTitle().trim());
        entry.setUsername(nullToEmpty(entry.getUsername()));
        entry.setUrl(nullToEmpty(entry.getUrl()));
        entry.setNotes(nullToEmpty(entry.getNotes()));
        entry.setCategory(normalizeCategory(entry.getCategory()));
        entry.setPassword(crypto.encryptString(currentKey, nullToEmpty(entry.getPassword())));
        entry.setNotes(crypto.encryptString(currentKey, entry.getNotes()));
        entry.setCreatedAt(entry.getCreatedAt() == null || entry.getCreatedAt().isBlank()
                ? now : entry.getCreatedAt());
        entry.setUpdatedAt(now);
        return entry;
    }

    private String decryptValue(SecretKey currentKey, String value)
            throws GeneralSecurityException {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return crypto.decryptString(currentKey, value);
    }

    private SecretKey requireKey() throws VaultException {
        if (key == null) {
            throw new VaultException("保险箱未解锁");
        }
        return key;
    }

    private static void validateMasterPassword(char[] password) throws VaultException {
        if (password == null || password.length < MIN_MASTER_PASSWORD_LENGTH) {
            throw new VaultException("主密码至少需要 " + MIN_MASTER_PASSWORD_LENGTH + " 个字符");
        }
    }

    private static void validateEntry(PasswordEntry entry) throws VaultException {
        if (entry == null || entry.getTitle() == null || entry.getTitle().isBlank()) {
            throw new VaultException("标题不能为空");
        }
    }

    private static String normalizeCategory(String category) {
        return category == null || category.isBlank() ? "默认" : category.trim();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String now() {
        return LocalDateTime.now().format(TIMESTAMP_FORMAT);
    }

    private static int readIterations(String value) {
        if (value == null) {
            return DEFAULT_ITERATIONS;
        }
        try {
            int parsed = Integer.parseInt(value);
            return parsed >= 10_000 ? parsed : DEFAULT_ITERATIONS;
        } catch (NumberFormatException e) {
            return DEFAULT_ITERATIONS;
        }
    }
}
