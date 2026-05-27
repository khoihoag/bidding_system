package com.bidding.server.repository;

import com.bidding.server.enums.ItemApprovalStatus;
import com.bidding.server.model.item.Item;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import com.bidding.server.config.HibernateSessionFactory;
import java.util.List;

public class ItemRepository {
    private static final SessionFactory factory = HibernateSessionFactory.getSessionFactory();

    // ========================================================
    // VŨ KHÍ CHO KHÔI: TÌM ITEM THEO ID (LÔI TỪ DB LÊN RAM)
    // ========================================================
    public Item findById(String id) {
        try (Session session = factory.openSession()) {
            // Lôi thẳng Item lên, Hibernate sẽ tự biết nó là Art, Vehicle hay Electronics
            // nhờ cái @Inheritance(strategy = SINGLE_TABLE) ông đã cài đặt.
            return session.get(Item.class, id);
        }
    }

    // ========================================================
    // TIỆN TAY LÀM LUÔN HÀM LƯU ĐỒ (DÀNH CHO CHỨC NĂNG ĐĂNG BÁN)
    // ========================================================
    public void saveOrUpdate(Item item) {
        Transaction transaction = null;
        try (Session session = factory.openSession()) {
            transaction = session.beginTransaction();
            session.merge(item); // Vẫn dùng merge cho chuẩn ID tự sinh
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw new RuntimeException("Failed to save item: " + item.getId(), e);
        }
    }
    public List<Item> findBySellerId(String sellerId) {
        try (Session session = factory.openSession()) {
            // HQL: Tìm tất cả Item mà có sellerId khớp với id truyền vào
            String hql = "FROM Item i WHERE i.sellerId = :sid";
            return session.createQuery(hql, Item.class)
                    .setParameter("sid", sellerId)
                    .list();
        }
    }
    // Hàm lấy tất cả vật phẩm (Ví dụ để hiển thị lên trang chủ)
    public long countBySellerId(String sellerId) {
        try (Session session = factory.openSession()) {
            Long count = session.createQuery(
                            "SELECT COUNT(i) FROM Item i WHERE i.sellerId = :sid",
                            Long.class)
                    .setParameter("sid", sellerId)
                    .uniqueResult();
            return count != null ? count : 0;
        }
    }

    public List<Item> findByApprovalStatus(ItemApprovalStatus status) {
        try (Session session = factory.openSession()) {
            String hql = "FROM Item i WHERE i.approvalStatus = :status";
            return session.createQuery(hql, Item.class)
                    .setParameter("status", status)
                    .list();
        }
    }

    public List<Item> findAll() {
        try (Session session = factory.openSession()) {
            return session.createQuery("FROM Item", Item.class).list();
        }
    }
    public void delete(Item item) {
        Transaction transaction = null;
        try (Session session = factory.openSession()) {
            transaction = session.beginTransaction();
            Item managedItem = session.contains(item) ? item : session.merge(item);
            if (managedItem.getImages() != null) {
                managedItem.getImages().clear();
            }
            session.flush();
            session.remove(managedItem);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw new RuntimeException("Failed to delete item: " + item.getId(), e);
        }
    }
    /**
     * VŨ KHÍ TÌM CHIẾN LỢI PHẨM:
     * Truy vấn lấy danh sách Item từ những phiên đấu giá mà User là người thắng cuộc.
     */
    // Trong ItemRepository.java
    public List<Item> findWonItemsByUserId(String userId) {
        try (Session session = factory.openSession()) {
            // SỬA: Dùng AuctionEntity (là Entity thực sự) thay vì Auction (Model)
            String hql = "SELECT a.item FROM AuctionEntity a " +
                    "WHERE a.currentWinner.id = :uid " +
                    "AND a.status IN ('FINISHED', 'PAID')";

            return session.createQuery(hql, Item.class)
                    .setParameter("uid", userId)
                    .list();
        }
    }
}
