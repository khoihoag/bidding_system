package com.bidding.server.repository;

import com.bidding.server.model.user.User;
import com.bidding.server.model.transaction.BiddingTransactionEntity;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;
import java.util.List;

public class UserRepository {
    // Vẫn xài chung cấu hình Hibernate
    private static final SessionFactory factory = new Configuration()
            .configure("hibernate.cfg.xml")
            .buildSessionFactory();

    // Lưu hoặc cập nhật User
    public void saveOrUpdate(User user) {
        Transaction transaction = null;
        try (Session session = factory.openSession()) {
            transaction = session.beginTransaction();
            session.merge(user); // Dùng merge cho an toàn với ID tự sinh
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw new RuntimeException("Khong luu duoc user vao database", e);
        }
    }

    // Tìm User bằng ID
    public User findById(String id) {
        try (Session session = factory.openSession()) {
            return session.get(User.class, id);
        }
    }

    // ========================================================
    // VŨ KHÍ MỚI: TÌM USER ĐỂ LOGIN
    // ========================================================
    public User findByUsernameAndPassword(String username, String password) {
        try (Session session = factory.openSession()) {
            // Câu lệnh HQL chọc thẳng vào bảng users
            String hql = "FROM User WHERE username = :u";
            Query<User> query = session.createQuery(hql, User.class);
            query.setParameter("u", username);
            List<User> users = query.list();

            if (users.isEmpty()) {
                return null;
            }
            if (users.size() > 1) {
                throw new RuntimeException("Database co " + users.size() + " tai khoan trung username '" + username + "'. Hay xoa ban ghi trung trong bang users.");
            }

            User user = users.get(0);
            return password.equals(user.getPasswordHash()) ? user : null;
        }
    }

    public boolean existsByUsername(String username) {
        try (Session session = factory.openSession()) {
            Long count = session.createQuery(
                            "SELECT COUNT(u) FROM User u WHERE u.username = :username",
                            Long.class)
                    .setParameter("username", username)
                    .uniqueResult();
            return count != null && count > 0;
        }
    }

    // ========================================================
    // VŨ KHÍ MỚI: LẤY LỊCH SỬ ĐẶT GIÁ CỦA 1 USER
    // ========================================================
    public List<BiddingTransactionEntity> getBidHistoryByUserId(String userId) {
        try (Session session = factory.openSession()) {
            // Lôi tất cả giao dịch trong bảng bidding_transactions mà thằng User này tham gia, sắp xếp thời gian mới nhất lên đầu
            String hql = "FROM BiddingTransactionEntity t WHERE t.bidder.id = :uid ORDER BY t.bidTime DESC";
            Query<BiddingTransactionEntity> query = session.createQuery(hql, BiddingTransactionEntity.class);
            query.setParameter("uid", userId);

            return query.list();
        }
    }
    public List<User> findAll() {
        try (Session session = factory.openSession()) {
            // Câu lệnh HQL đơn giản nhất thế giới: Lấy tất cả từ bảng User
            String hql = "FROM User";
            Query<User> query = session.createQuery(hql, User.class);

            return query.list(); // Trả về nguyên một danh sách giang hồ mạng
        }
    }
}
