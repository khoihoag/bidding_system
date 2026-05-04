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
    // Lazy initialization: chỉ khởi tạo khi lần đầu tiên gọi getFactory()
    private static SessionFactory factory;

    private static synchronized SessionFactory getFactory() {
        if (factory == null) {
            factory = new Configuration()
                    .configure("hibernate.cfg.xml")
                    .buildSessionFactory();
        }
        return factory;
    }

    // Lưu hoặc cập nhật User
    public void saveOrUpdate(User user) {
        Transaction transaction = null;
        try (Session session = getFactory().openSession()) {
            transaction = session.beginTransaction();
            session.merge(user); // Dùng merge cho an toàn với ID tự sinh
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
        }
    }

    // Tìm User bằng ID
    public User findById(String id) {
        try (Session session = getFactory().openSession()) {
            return session.get(User.class, id);
        }
    }

    // ========================================================
    // VŨ KHÍ MỚI: TÌM USER ĐỂ LOGIN
    // ========================================================
    public User findByUsernameAndPassword(String username, String password) {
        try (Session session = getFactory().openSession()) {
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
        try (Session session = getFactory().openSession()) {
            // Lôi tất cả giao dịch trong bảng bidding_transactions mà thằng User này tham gia, sắp xếp thời gian mới nhất lên đầu
            String hql = "FROM BiddingTransactionEntity t WHERE t.bidder.id = :uid ORDER BY t.bidTime DESC";
            Query<BiddingTransactionEntity> query = session.createQuery(hql, BiddingTransactionEntity.class);
            query.setParameter("uid", userId);

            return query.list();
        }
    }
    public List<User> findAll() {
        try (Session session = getFactory().openSession()) {
            // Câu lệnh HQL đơn giản nhất thế giới: Lấy tất cả từ bảng User
            String hql = "FROM User";
            Query<User> query = session.createQuery(hql, User.class);

            return query.list(); // Trả về nguyên một danh sách giang hồ mạng
        }
    }
}