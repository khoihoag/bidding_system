package com.bidding.server.repository;

import com.bidding.server.model.auction.AuctionEntity;
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
    // TÌM CHỖ NÀY TRONG AuctionRepository.java
    public List<AuctionEntity> findAll() {
        // Dùng luôn cái "factory" sếp đã khai báo ở dòng 12 ấy
        Session session = factory.openSession();
        try {
            String hql = "SELECT DISTINCT a FROM AuctionEntity a " +
                    "LEFT JOIN FETCH a.transactions t " +  // Lôi theo lịch sử đặt giá
                    "LEFT JOIN FETCH t.bidder " +          // Lôi theo người đặt giá
                    "LEFT JOIN FETCH a.currentWinner " +   // Lôi theo User (người thắng)
                    "LEFT JOIN FETCH a.item";
            return session.createQuery(hql, AuctionEntity.class).getResultList();
        } finally {
            session.close();
        }
    }
    // Dán hàm này vào AuctionRepository.java
    public void saveNewBid(String auctionId, double newPrice, com.bidding.server.model.user.User winner, java.time.LocalDateTime newEndTime, com.bidding.server.model.transaction.BiddingTransactionEntity newTx) {
        Transaction transaction = null;
        try (Session session = factory.openSession()) {
            transaction = session.beginTransaction();

            AuctionEntity auction = session.get(AuctionEntity.class, auctionId);
            auction.setCurrentPrice(newPrice);
            auction.setEndTime(newEndTime);
            auction.setCurrentWinner(winner);

            newTx.setAuction(auction);

            // ================= CHỮA BỆNH TẬN GỐC TẠI ĐÂY =================
            // Thay vì dùng session.persist(newTx); (Gây lỗi với Detached Entity)
            // Ta dùng merge để Hibernate tự đồng bộ dữ liệu RAM và DB mượt mà!
            session.merge(newTx);
            // =============================================================

            session.merge(auction);

            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
            throw e; // Quăng hẳn lỗi lên trên để Service biết đường hoàn tiền!
        }
    }
    // Trong AuctionRepository.java
    public void finalizeAuction(String auctionId, com.bidding.server.enums.AuctionStatus status, com.bidding.server.model.user.User winner) {
        org.hibernate.Transaction tx = null;
        try (org.hibernate.Session session = factory.openSession()) {
            tx = session.beginTransaction();
            // Lấy thực thể tươi rói từ DB lên để bypass vụ version
            AuctionEntity entity = session.get(AuctionEntity.class, auctionId);
            if (entity != null) {
                entity.setStatus(status);
                if (winner != null) entity.setCurrentWinner(winner);
                session.merge(entity);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }
}