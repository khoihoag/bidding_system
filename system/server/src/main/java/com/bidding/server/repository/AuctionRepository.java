package com.bidding.server.repository;

import com.bidding.server.model.auction.AuctionEntity;
import com.bidding.server.model.item.Item;
import com.bidding.server.model.user.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class AuctionRepository {
    // Singleton: Cả cái app chỉ cần 1 cái máy sản xuất Session duy nhất
    private static final SessionFactory factory = new Configuration()
            .configure("hibernate.cfg.xml") // Nó sẽ tự mò vào thư mục resources để đọc file này
            .buildSessionFactory();

    static {
        dropLegacyItemForeignKeys();
    }

    /**
     * Hàm "Cất đồ": Dùng cho cả tạo mới (Insert) và cập nhật (Update)
     */
    public void saveOrUpdate(AuctionEntity entity) {
        Transaction transaction = null;
        try (Session session = factory.openSession()) {
            transaction = session.beginTransaction();

            // ĐÃ SỬA: Dùng merge() thay vì saveOrUpdate()
            // Vì ID (UUID) của ta tự sinh trên RAM, merge() sẽ xử lý mượt mà hơn
            attachManagedReferences(session, entity);
            session.merge(entity);

            transaction.commit();
            System.out.println("Lưu Database thành công cho Auction ID: " + entity.getId());
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw new RuntimeException("Cannot save auction: " + e.getMessage(), e);
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
                    "LEFT JOIN FETCH a.transactions " +    // Lôi theo lịch sử đặt giá
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
            if (auction == null) {
                throw new IllegalArgumentException("Auction does not exist in database: " + auctionId);
            }
            auction.setCurrentPrice(newPrice);
            auction.setEndTime(newEndTime);
            auction.setCurrentWinner(resolveManagedUser(session, winner, false));

            newTx.setAuction(auction);
            newTx.setBidder(resolveManagedUser(session, newTx.getBidder(), true));

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
                if (winner != null) entity.setCurrentWinner(resolveManagedUser(session, winner, true));
                session.merge(entity);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    private void attachManagedReferences(Session session, AuctionEntity entity) {
        if (entity.getItem() == null || entity.getItem().getId() == null) {
            throw new IllegalArgumentException("Auction must have an existing item before saving.");
        }

        Item managedItem = session.get(Item.class, entity.getItem().getId());
        if (managedItem == null) {
            throw new IllegalArgumentException("Item does not exist in database: " + entity.getItem().getId());
        }

        entity.setItem(managedItem);
        entity.setCurrentWinner(resolveManagedUser(session, entity.getCurrentWinner(), false));
    }

    private User resolveManagedUser(Session session, User user, boolean required) {
        if (user == null) {
            if (required) {
                throw new IllegalArgumentException("User reference is required.");
            }
            return null;
        }
        if (user.getId() == null) {
            throw new IllegalArgumentException("User reference has no id.");
        }

        User managedUser = session.get(User.class, user.getId());
        if (managedUser == null) {
            throw new IllegalArgumentException("User does not exist in database: " + user.getId());
        }
        return managedUser;
    }

    private static void dropLegacyItemForeignKeys() {
        try (Session session = factory.openSession()) {
            session.doWork(connection -> {
                List<String> constraintNames = new ArrayList<>();
                String query = """
                        SELECT CONSTRAINT_NAME
                        FROM information_schema.KEY_COLUMN_USAGE
                        WHERE TABLE_SCHEMA = DATABASE()
                          AND TABLE_NAME = 'auctions'
                          AND COLUMN_NAME = 'item_id'
                          AND REFERENCED_TABLE_NAME = 'item'
                        """;

                try (Statement statement = connection.createStatement();
                     ResultSet resultSet = statement.executeQuery(query)) {
                    while (resultSet.next()) {
                        constraintNames.add(resultSet.getString("CONSTRAINT_NAME"));
                    }
                }

                for (String constraintName : constraintNames) {
                    try (Statement statement = connection.createStatement()) {
                        statement.execute("ALTER TABLE auctions DROP FOREIGN KEY `" + constraintName + "`");
                        System.out.println("[DB] Dropped legacy auctions.item_id foreign key: " + constraintName);
                    }
                }
            });
        } catch (Exception e) {
            System.err.println("[DB] Could not check legacy auctions.item_id foreign keys: " + e.getMessage());
        }
    }

    public boolean hasFinishedAuctionForItem(String itemId) {
        try (Session session = factory.openSession()) {
            String hql = "SELECT count(a) FROM AuctionEntity a WHERE a.item.id = :itemId AND a.status = :status";
            Long count = session.createQuery(hql, Long.class)
                    .setParameter("itemId", itemId)
                    .setParameter("status", com.bidding.server.enums.AuctionStatus.FINISHED)
                    .uniqueResult();
            return count != null && count > 0;
        }
    }

    public boolean hasAnyAuctionForItem(String itemId) {
        try (Session session = factory.openSession()) {
            String hql = "SELECT count(a) FROM AuctionEntity a WHERE a.item.id = :itemId";
            Long count = session.createQuery(hql, Long.class)
                    .setParameter("itemId", itemId)
                    .uniqueResult();
            return count != null && count > 0;
        }
    }

    public String getAuctionStatusForItem(String itemId) {
        try (Session session = factory.openSession()) {
            String hql = "SELECT a.status FROM AuctionEntity a WHERE a.item.id = :itemId";
            com.bidding.server.enums.AuctionStatus status = session.createQuery(hql, com.bidding.server.enums.AuctionStatus.class)
                    .setParameter("itemId", itemId)
                    .uniqueResult();
            return status != null ? status.name() : null;
        }
    }
}
