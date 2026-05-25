package com.bidding.server.service;

import com.bidding.server.enums.ItemCondition;
import com.bidding.server.enums.UserRole;
import com.bidding.server.model.item.Art;
import com.bidding.server.model.item.Electronics;
import com.bidding.server.model.item.Vehicle;
import com.bidding.server.model.user.User;
import com.bidding.server.repository.ItemRepository;
import com.bidding.server.repository.UserRepository;

import java.util.List;

public final class DatabaseSeeder {

    private DatabaseSeeder() {
    }

    public static void seedIfEmpty() {
        UserRepository userRepository = new UserRepository();
        List<User> users = userRepository.findAll();
        if (!users.isEmpty()) {
            System.out.println("[DatabaseSeeder] Database đã có dữ liệu, bỏ qua seed.");
            return;
        }

        User admin = new User("admin-demo", "admin", "admin@bidviet.local", "admin123", "Quản trị viên", UserRole.ADMIN);
        admin.setBalance(10_000_000);

        User seller = new User("seller-demo", "seller", "seller@bidviet.local", "seller123", "Người bán demo", UserRole.USER);
        seller.setBalance(5_000_000);

        User bidder = new User("bidder-demo", "bidder", "bidder@bidviet.local", "bidder123", "Người mua demo", UserRole.USER);
        bidder.setBalance(20_000_000);

        userRepository.saveOrUpdate(admin);
        userRepository.saveOrUpdate(seller);
        userRepository.saveOrUpdate(bidder);

        ItemRepository itemRepository = new ItemRepository();
        itemRepository.saveOrUpdate(new Art(
                "item-art-demo",
                "Tranh sơn dầu demo",
                "Tranh sơn dầu dùng để test đấu giá local.",
                1_500_000,
                List.of(),
                seller.getId(),
                seller.getFullName(),
                ItemCondition.LIKE_NEW,
                "Họa sĩ demo",
                "Sơn dầu",
                2024,
                "60 x 80 cm"
        ));
        itemRepository.saveOrUpdate(new Electronics(
                "item-electronics-demo",
                "Laptop demo",
                "Laptop mẫu để test chức năng thêm và mở phiên.",
                8_000_000,
                List.of(),
                seller.getId(),
                seller.getFullName(),
                ItemCondition.USED,
                "DemoBrand",
                "DB-15",
                6,
                65
        ));
        itemRepository.saveOrUpdate(new Vehicle(
                "item-vehicle-demo",
                "Xe máy demo",
                "Xe máy mẫu cho dữ liệu đấu giá.",
                12_000_000,
                List.of(),
                seller.getId(),
                seller.getFullName(),
                ItemCondition.USED,
                "Honda",
                "Wave",
                2020,
                18000,
                "Xăng"
        ));

        System.out.println("[DatabaseSeeder] Đã tạo database mẫu.");
        System.out.println("[DatabaseSeeder] Login admin:  admin / admin123");
        System.out.println("[DatabaseSeeder] Login seller: seller / seller123");
        System.out.println("[DatabaseSeeder] Login bidder: bidder / bidder123");
    }
}
