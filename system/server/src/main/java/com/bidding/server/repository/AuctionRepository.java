package com.bidding.server.repository;

import com.bidding.server.model.auction.AuctionEntity;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import java.util.List;

public class AuctionRepository {
    // Lazy initialization: chỉ khởi tạo khi lần đầu tiên gọi getFactory()
    // Mockito load class để tạo mock sẽ không trigger DB connection
    private static SessionFactory factory;

    private static synchronized SessionFactory getFactory() {
        if (factory == null) {
            factory = new Configuration()
                    .configure("hibernate.cfg.xml")
                    .buildSessionFactory();
        }
        return factory;
    }

    /**
     * Hàm "Cất đồ": Dùng cho cả tạo mới (Insert) và cập nhật (Update)
     */
    public void saveOrUpdate(AuctionEntity entity) {
        Transaction transaction = null;
        try (Session session = getFactory().openSession()) {
            transaction = session.beginTransaction();

            // merge() trả về bản managed, ghi đè lại entity gốc để ID và field được cập nhật
            AuctionEntity managed = session.merge(entity);
            transaction.commit();

            // Copy trạng thái từ managed instance về entity gốc
            entity.setId(managed.getId());
            entity.setStatus(managed.getStatus());
            entity.setCurrentPrice(managed.getCurrentPrice());
            entity.setCurrentWinner(managed.getCurrentWinner());
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
        try (Session session = getFactory().openSession()) {
            return session.get(AuctionEntity.class, id);
        }
    }

    /**
     * Hàm "Lấy TẤT CẢ đồ": Kéo toàn bộ từ dưới DB lên RAM
     */
    public List<AuctionEntity> findAll() {
        try (Session session = getFactory().openSession()) {
            // HQL: "FROM AuctionEntity" tương đương với "SELECT * FROM auctions"
            return session.createQuery("FROM AuctionEntity", AuctionEntity.class).list();
        }
    }
}