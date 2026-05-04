package com.bidding.server.repository;

import com.bidding.server.model.item.Item;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import java.util.List;

public class ItemRepository {
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

    // ========================================================
    // VŨ KHÍ CHO KHÔI: TÌM ITEM THEO ID (LÔI TỪ DB LÊN RAM)
    // ========================================================
    public Item findById(String id) {
        try (Session session = getFactory().openSession()) {
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
        try (Session session = getFactory().openSession()) {
            transaction = session.beginTransaction();
            // merge() trả về managed instance — copy lại ID vào item gốc
            Item managed = session.merge(item);
            transaction.commit();
            item.setId(managed.getId());
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
        }
    }
    public List<Item> findBySellerId(String sellerId) {
        try (Session session = getFactory().openSession()) {
            // HQL: Tìm tất cả Item mà có seller.id khớp với id truyền vào
            String hql = "FROM Item i WHERE i.seller.id = :sid";
            return session.createQuery(hql, Item.class)
                    .setParameter("sid", sellerId)
                    .list();
        }
    }
    // Hàm lấy tất cả vật phẩm (Ví dụ để hiển thị lên trang chủ)
    public List<Item> findAll() {
        try (Session session = getFactory().openSession()) {
            return session.createQuery("FROM Item", Item.class).list();
        }
    }
    public void delete(Item item) {
        Transaction transaction = null;
        try (Session session = getFactory().openSession()) {
            transaction = session.beginTransaction();
            // Lấy fresh managed instance bằng ID rồi mới xóa
            Item managed = session.get(Item.class, item.getId());
            if (managed != null) {
                session.remove(managed);
            }
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
        }
    }
}