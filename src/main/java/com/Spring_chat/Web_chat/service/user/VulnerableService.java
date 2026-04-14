package com.Spring_chat.Web_chat.service.user;

import com.Spring_chat.Web_chat.entity.User;
import com.Spring_chat.Web_chat.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;

@Service
public class VulnerableService {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    // LỖI 1: SQL INJECTION (Nghiêm trọng)
    public List<User> findUsersByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = '" + username + "'";
        Query query = entityManager.createNativeQuery(sql, User.class);
        return query.getResultList();
    }

    // LỖI 2: N+1 QUERY (Hiệu năng)
    public void printAllUserRoles() {
        List<User> users = userRepository.findAll(); // Giả sử User có @OneToMany với Role (Eager/Lazy)
        for (User user : users) {
            // Mỗi lần gọi getRoles() trong vòng lặp sẽ sinh ra 1 câu query mới
            System.out.println(user.getUsername() + " has roles: " + user.getRoles().size());
        }
    }

    // LỖI 3: CODE SMELL (Hardcode, Thiếu log, Try-Catch rỗng)
    public void processData() {
        try {
            String adminPassword = "ADMIN_PASSWORD_123"; // Hardcoded secret
            // ... logic phức tạp không có log
        } catch (Exception e) {
            // Nuốt lỗi (Empty catch block) - Cực kỳ nguy hiểm khi debug
        }
    }
}
