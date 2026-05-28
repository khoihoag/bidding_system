package com.bidding.server.repository;

import com.bidding.server.model.auction.AuctionEntity;
import com.bidding.server.model.item.Item;
import com.bidding.server.model.transaction.BiddingTransactionEntity;
import com.bidding.server.model.user.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.LockMode;
import com.bidding.server.config.HibernateSessionFactory;
import org.hibernate.query.Query;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class AuctionRepository {
    private static final AtomicBoolean ITEM_INDEX_CHECKED = new AtomicBoolean(false);
    private final SessionFactory factory;

    public static final class AuctionPaymentResult {
        private final double payerBalance;
        private final double sellerBalance;
        private final double amount;
        private final String sellerId;

        public AuctionPaymentResult(double payerBalance, double sellerBalance, double amount, String sellerId) {
            this.payerBalance = payerBalance;
            this.sellerBalance = sellerBalance;
            this.amount = amount;
            this.sellerId = sellerId;
        }

        public double getPayerBalance() {
            return payerBalance;
        }

        public double getSellerBalance() {
            return sellerBalance;
        }

        public double getAmount() {
            return amount;
        }

        public String getSellerId() {
            return sellerId;
        }
    }

    public AuctionRepository() {
        this(HibernateSessionFactory.getSessionFactory());
    }

    AuctionRepository(SessionFactory factory) {
        this.factory = factory;
    }

    /**
     * Hàm "Cất đồ": Dùng cho cả tạo mới (Insert) và cập nhật (Update)
     */
    public void saveOrUpdate(AuctionEntity entity) {
        ensureAuctionItemIdAllowsMultipleRows();
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
            throw new RuntimeException("Failed to save auction: " + entity.getId(), e);
        }
    }

    private void ensureAuctionItemIdAllowsMultipleRows() {
        if (!ITEM_INDEX_CHECKED.compareAndSet(false, true)) {
            return;
        }

        try (Session session = factory.openSession()) {
            session.doWork(connection -> {
                List<String> uniqueIndexes = findSingleColumnUniqueItemIndexes(connection);
                if (uniqueIndexes.isEmpty()) {
                    return;
                }

                ensureNonUniqueItemIndex(connection);
                try (Statement statement = connection.createStatement()) {
                    for (String indexName : uniqueIndexes) {
                        statement.execute("ALTER TABLE auctions DROP INDEX `" + quoteIdentifier(indexName) + "`");
                    }
                }
            });
        } catch (Exception e) {
            ITEM_INDEX_CHECKED.set(false);
            throw new RuntimeException("Failed to prepare reusable auction item index", e);
        }
    }

    private static List<String> findSingleColumnUniqueItemIndexes(Connection connection) throws SQLException {
        List<String> indexes = new ArrayList<>();
        String sql = """
                SELECT INDEX_NAME
                FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'auctions'
                  AND NON_UNIQUE = 0
                  AND INDEX_NAME <> 'PRIMARY'
                GROUP BY INDEX_NAME
                HAVING COUNT(*) = 1 AND MAX(COLUMN_NAME) = 'item_id'
                """;
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                indexes.add(rs.getString("INDEX_NAME"));
            }
        }
        return indexes;
    }

    private static void ensureNonUniqueItemIndex(Connection connection) throws SQLException {
        String checkSql = """
                SELECT COUNT(*) AS index_count
                FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'auctions'
                  AND COLUMN_NAME = 'item_id'
                  AND NON_UNIQUE = 1
                """;
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(checkSql)) {
            if (rs.next() && rs.getInt("index_count") > 0) {
                return;
            }
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE INDEX idx_auctions_item_id_reusable ON auctions (item_id)");
        }
    }

    private static String quoteIdentifier(String identifier) {
        return identifier == null ? "" : identifier.replace("`", "``");
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
            if (auction == null) {
                throw new IllegalStateException("Auction not found: " + auctionId);
            }
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

    public void saveRejectedBid(String auctionId, com.bidding.server.model.transaction.BiddingTransactionEntity rejectedTx) {
        Transaction transaction = null;
        try (Session session = factory.openSession()) {
            transaction = session.beginTransaction();

            AuctionEntity auction = session.get(AuctionEntity.class, auctionId);
            if (auction == null) {
                throw new IllegalStateException("Auction not found: " + auctionId);
            }

            rejectedTx.setAuction(auction);
            session.merge(rejectedTx);

            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw e;
        }
    }

    // Trong AuctionRepository.java
    public void finalizeAuction(String auctionId, com.bidding.server.enums.AuctionStatus status, com.bidding.server.model.user.User winner) {
        finalizeAuction(auctionId, status, winner, null);
    }

    public void finalizeAuction(String auctionId,
                                com.bidding.server.enums.AuctionStatus status,
                                com.bidding.server.model.user.User winner,
                                java.time.LocalDateTime endTime) {
        org.hibernate.Transaction tx = null;
        try (org.hibernate.Session session = factory.openSession()) {
            tx = session.beginTransaction();
            // Lấy thực thể tươi rói từ DB lên để bypass vụ version
            AuctionEntity entity = session.get(AuctionEntity.class, auctionId);
            if (entity != null) {
                entity.setStatus(status);
                entity.setCurrentWinner(winner);
                if (endTime != null) {
                    entity.setEndTime(endTime);
                }
                session.merge(entity);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    public AuctionEntity findByIdWithDetails(String id) {
        try (Session session = factory.openSession()) {
            return session.createQuery(
                            "SELECT DISTINCT a FROM AuctionEntity a " +
                                    "LEFT JOIN FETCH a.currentWinner " +
                                    "LEFT JOIN FETCH a.item " +
                                    "LEFT JOIN FETCH a.transactions t " +
                                    "LEFT JOIN FETCH t.bidder " +
                                    "WHERE a.id = :id",
                            AuctionEntity.class)
                    .setParameter("id", id)
                    .uniqueResult();
        }
    }

    public AuctionPaymentResult payWonAuction(String auctionId, String payerId) {
        Transaction transaction = null;
        try (Session session = factory.openSession()) {
            transaction = session.beginTransaction();

            AuctionEntity entity = session.createQuery(
                            "SELECT DISTINCT a FROM AuctionEntity a " +
                                    "LEFT JOIN FETCH a.currentWinner " +
                                    "LEFT JOIN FETCH a.item " +
                                    "WHERE a.id = :id",
                            AuctionEntity.class)
                    .setParameter("id", auctionId)
                    .uniqueResult();

            if (entity == null) {
                throw new IllegalStateException("Khong tim thay phien dau gia.");
            }
            if (entity.getStatus() != com.bidding.server.enums.AuctionStatus.FINISHED
                    || entity.getCurrentWinner() == null) {
                throw new IllegalStateException("Phien dau gia nay khong con trong trang thai cho thanh toan.");
            }
            if (!payerId.equals(entity.getCurrentWinner().getId())) {
                throw new IllegalStateException("Chi nguoi chien thang moi duoc thanh toan phien nay.");
            }

            Item item = entity.getItem();
            if (item == null || item.getSellerId() == null || item.getSellerId().isBlank()) {
                throw new IllegalStateException("Khong tim thay thong tin nguoi ban cua san pham.");
            }

            String sellerId = item.getSellerId();
            if (sellerId.equals(payerId)) {
                throw new IllegalStateException("Du lieu phien da bi chuyen quyen so huu truoc khi thanh toan.");
            }

            User payer = session.get(User.class, payerId, LockMode.PESSIMISTIC_WRITE);
            User seller = session.get(User.class, sellerId, LockMode.PESSIMISTIC_WRITE);
            if (payer == null) {
                throw new IllegalStateException("Khong tim thay tai khoan thanh toan.");
            }
            if (seller == null) {
                throw new IllegalStateException("Khong tim thay tai khoan nguoi ban.");
            }

            double amount = entity.getCurrentPrice() != null ? entity.getCurrentPrice() : 0.0;
            if (amount < 0) {
                throw new IllegalStateException("Gia thanh toan khong hop le.");
            }
            if (payer.getBalance() < amount) {
                throw new IllegalStateException("Vi cua ban khong du tien! Vui long nap them.");
            }

            payer.setBalance(payer.getBalance() - amount);
            seller.setBalance(seller.getBalance() + amount);
            item.setSellerId(payer.getId());
            item.setSellerFullName(payer.getFullName());
            entity.setCurrentWinner(payer);
            entity.setStatus(com.bidding.server.enums.AuctionStatus.PAID);

            session.merge(payer);
            session.merge(seller);
            session.merge(item);
            session.merge(entity);

            transaction.commit();
            return new AuctionPaymentResult(payer.getBalance(), seller.getBalance(), amount, sellerId);
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw e;
        }
    }

    public void addFollower(String auctionId, String userId) {
        updateFollower(auctionId, userId, true);
    }

    public void removeFollower(String auctionId, String userId) {
        updateFollower(auctionId, userId, false);
    }

    private void updateFollower(String auctionId, String userId, boolean follow) {
        if (auctionId == null || auctionId.isBlank() || userId == null || userId.isBlank()) {
            return;
        }

        Transaction transaction = null;
        try (Session session = factory.openSession()) {
            transaction = session.beginTransaction();
            AuctionEntity auction = session.get(AuctionEntity.class, auctionId);
            if (auction == null) {
                throw new IllegalStateException("Auction not found: " + auctionId);
            }
            if (auction.getFollowerIds() == null) {
                auction.setFollowerIds(new java.util.HashSet<>());
            }
            if (follow) {
                auction.getFollowerIds().add(userId);
            } else {
                auction.getFollowerIds().remove(userId);
            }
            session.merge(auction);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw e;
        }
    }

    public List<BiddingTransactionEntity> findBidHistoryByAuctionId(String auctionId) {
        try (Session session = factory.openSession()) {
            String hql = "SELECT t FROM BiddingTransactionEntity t " +
                    "LEFT JOIN FETCH t.bidder " +
                    "LEFT JOIN FETCH t.auction " +
                    "WHERE t.auction.id = :auctionId " +
                    "ORDER BY t.bidTime DESC";
            Query<BiddingTransactionEntity> query = session.createQuery(hql, BiddingTransactionEntity.class);
            query.setParameter("auctionId", auctionId);
            return query.list();
        }
    }

    public boolean existsByItemId(String itemId) {
        try (Session session = factory.openSession()) {
            Long count = session.createQuery(
                            "SELECT COUNT(a) FROM AuctionEntity a " +
                                    "WHERE a.item.id = :itemId " +
                                    "AND a.status NOT IN (:reusableStatuses)",
                            Long.class)
                    .setParameter("itemId", itemId)
                    .setParameter("reusableStatuses", List.of(
                            com.bidding.server.enums.AuctionStatus.CANCELED,
                            com.bidding.server.enums.AuctionStatus.FAILED))
                    .uniqueResult();
            return count != null && count > 0;
        }
    }

    public long countBySellerId(String sellerId) {
        try (Session session = factory.openSession()) {
            Long count = session.createQuery(
                            "SELECT COUNT(a) FROM AuctionEntity a WHERE a.item.sellerId = :sellerId",
                            Long.class)
                    .setParameter("sellerId", sellerId)
                    .uniqueResult();
            return count != null ? count : 0;
        }
    }

    public long countBySellerIdAndStatuses(String sellerId, List<com.bidding.server.enums.AuctionStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return 0;
        }
        try (Session session = factory.openSession()) {
            Long count = session.createQuery(
                            "SELECT COUNT(a) FROM AuctionEntity a " +
                                    "WHERE a.item.sellerId = :sellerId AND a.status IN (:statuses)",
                            Long.class)
                    .setParameter("sellerId", sellerId)
                    .setParameter("statuses", statuses)
                    .uniqueResult();
            return count != null ? count : 0;
        }
    }

    public long countWonByUserId(String userId) {
        try (Session session = factory.openSession()) {
            Long count = session.createQuery(
                            "SELECT COUNT(a) FROM AuctionEntity a " +
                                    "WHERE a.currentWinner.id = :userId " +
                                    "AND a.status IN (:statuses)",
                            Long.class)
                    .setParameter("userId", userId)
                    .setParameter("statuses", List.of(
                            com.bidding.server.enums.AuctionStatus.FINISHED,
                            com.bidding.server.enums.AuctionStatus.PAID))
                    .uniqueResult();
            return count != null ? count : 0;
        }
    }

    public AuctionEntity findByItemId(String itemId) {
        try (Session session = factory.openSession()) {
            return session.createQuery(
                            "SELECT a FROM AuctionEntity a " +
                                    "LEFT JOIN FETCH a.currentWinner " +
                                    "WHERE a.item.id = :itemId " +
                                    "ORDER BY a.startTime DESC",
                            AuctionEntity.class)
                    .setParameter("itemId", itemId)
                    .setMaxResults(1)
                    .uniqueResult();
        }
    }
}
