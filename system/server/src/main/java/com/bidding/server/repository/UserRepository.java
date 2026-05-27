package com.bidding.server.repository;

import com.bidding.server.model.user.User;
import com.bidding.server.model.transaction.BiddingTransactionEntity;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import com.bidding.server.config.HibernateSessionFactory;
import org.hibernate.query.Query;
import java.util.List;

public class UserRepository {
    private static final SessionFactory factory = HibernateSessionFactory.getSessionFactory();

    // Lưu hoặc cập nhật User
    public void saveOrUpdate(User user) {
        Transaction transaction = null;
        try (Session session = factory.openSession()) {
            transaction = session.beginTransaction();
            session.merge(user); // Dùng merge cho an toàn với ID tự sinh
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw new RuntimeException("Failed to save user: " + user.getId(), e);
        }
    }

    // Tìm User bằng ID
    public User findById(String id) {
        try (Session session = factory.openSession()) {
            return session.get(User.class, id);
        }
    }

    public User findByUsername(String username) {
        try (Session session = factory.openSession()) {
            String hql = "FROM User WHERE username = :u";
            Query<User> query = session.createQuery(hql, User.class);
            query.setParameter("u", username);
            return query.uniqueResult();
        }
    }

    public User findByEmail(String email) {
        try (Session session = factory.openSession()) {
            String hql = "FROM User WHERE email = :e";
            Query<User> query = session.createQuery(hql, User.class);
            query.setParameter("e", email);
            return query.uniqueResult();
        }
    }

    // ========================================================
    // VŨ KHÍ MỚI: TÌM USER ĐỂ LOGIN
    // ========================================================
    public User findByUsernameAndPassword(String username, String password) {
        try (Session session = factory.openSession()) {
            // Câu lệnh HQL chọc thẳng vào bảng users
            String hql = "FROM User WHERE username = :u AND passwordHash = :p";
            Query<User> query = session.createQuery(hql, User.class);
            query.setParameter("u", username);
            query.setParameter("p", password); // Thực tế nếu code bảo mật thì phải Hash cái password này ra trước

            return query.uniqueResult(); // Trả về 1 thằng duy nhất, hoặc null nếu sai pass
        }
    }

    // ========================================================
    // VŨ KHÍ MỚI: LẤY LỊCH SỬ ĐẶT GIÁ CỦA 1 USER
    // ========================================================
    public List<BiddingTransactionEntity> getBidHistoryByUserId(String userId) {
        try (Session session = factory.openSession()) {
            // SỬA HQL: Ép nó lấy luôn thông tin Auction trong 1 lần query (LEFT JOIN FETCH)
            String hql = "SELECT t FROM BiddingTransactionEntity t LEFT JOIN FETCH t.auction WHERE t.bidder.id = :uid ORDER BY t.bidTime DESC";
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
