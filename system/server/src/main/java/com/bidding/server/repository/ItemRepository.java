package com.bidding.server.repository;

import com.bidding.server.model.item.Item;
import com.bidding.server.model.user.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import java.util.ArrayList;
import java.util.List;

public class ItemRepository {
    // Vẫn xài chung cái máy sản xuất Session
    private static final SessionFactory factory = new Configuration()
            .configure("hibernate.cfg.xml")
            .buildSessionFactory();

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
            if (item.getSeller() == null || item.getSeller().getId() == null) {
                throw new IllegalArgumentException("Item must have a seller before saving.");
            }
            User managedSeller = session.get(User.class, item.getSeller().getId());
            if (managedSeller == null) {
                throw new IllegalArgumentException("Seller does not exist in database: " + item.getSeller().getId());
            }
            item.setSeller(managedSeller);
            if (item.getImages() == null) {
                item.setImages(new ArrayList<>());
            }
            Item existingItem = session.get(Item.class, item.getId());
            if (existingItem == null) {
                session.persist(item);
            } else {
                session.merge(item);
            }
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw new RuntimeException("Cannot save item: " + e.getMessage(), e);
        }
    }
    public List<Item> findBySellerId(String sellerId) {
        try (Session session = factory.openSession()) {
            // HQL: Tìm tất cả Item mà có seller.id khớp với id truyền vào
            String hql = "FROM Item i WHERE i.seller.id = :sid";
            return session.createQuery(hql, Item.class)
                    .setParameter("sid", sellerId)
                    .list();
        }
    }
    // Hàm lấy tất cả vật phẩm (Ví dụ để hiển thị lên trang chủ)
    public List<Item> findAll() {
        try (Session session = factory.openSession()) {
            return session.createQuery("FROM Item", Item.class).list();
        }
    }
    public void delete(Item item) {
        Transaction transaction = null;
        try (Session session = factory.openSession()) {
            transaction = session.beginTransaction();
            // Xóa món đồ khỏi Database
            session.remove(session.contains(item) ? item : session.merge(item));
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
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
                    "AND a.status = 'FINISHED'";

            return session.createQuery(hql, Item.class)
                    .setParameter("uid", userId)
                    .list();
        }
    }
}
