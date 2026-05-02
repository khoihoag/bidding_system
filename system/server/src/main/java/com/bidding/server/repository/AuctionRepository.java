package com.bidding.server.repository;

import com.bidding.server.model.AuctionEntity;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import java.util.List;

public class AuctionRepository {
    // Singleton: Cả cái app chỉ cần 1 cái máy sản xuất Session duy nhất
    private static final SessionFactory factory = new Configuration()
            .configure("hibernate.cfg.xml") // Nó sẽ tự mò vào thư mục resources để đọc file này
            .buildSessionFactory();

    /**
     * Hàm "Cất đồ": Dùng cho cả tạo mới (Insert) và cập nhật (Update)
     */
    public void saveOrUpdate(AuctionEntity entity) {
        Transaction transaction = null;
        try (Session session = factory.openSession()) {
            transaction = session.beginTransaction();

            // ĐÃ SỬA: Dùng merge() thay vì saveOrUpdate()
            // Vì ID (UUID) của ta tự sinh trên RAM, merge() sẽ xử lý mượt mà hơn
            session.merge(entity);

            transaction.commit();
            System.out.println("Lưu Database thành công cho Auction ID: " + entity.getId());
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
        }
    }

    /**
     * Hàm "Lấy đồ": Tìm một phiên đấu giá theo ID
     */
    // ĐÃ SỬA: Đổi kiểu dữ liệu của id từ Long sang String
    public AuctionEntity findById(String id) {
        try (Session session = factory.openSession()) {
            return session.get(AuctionEntity.class, id);
        }
    }

    /**
     * Hàm "Lấy TẤT CẢ đồ": Kéo toàn bộ từ dưới DB lên RAM
     */
    public List<AuctionEntity> findAll() {
        try (Session session = factory.openSession()) {
            // HQL: "FROM AuctionEntity" tương đương với "SELECT * FROM auctions"
            return session.createQuery("FROM AuctionEntity", AuctionEntity.class).list();
        }
    }
}