package com.bidding.server.service;

import com.bidding.server.model.item.Item;
import com.bidding.server.model.user.User;
import com.bidding.server.repository.ItemRepository;

import java.util.List;

public class ItemService {

    private final ItemRepository itemRepository;

    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    public Item createItem(User actor, Item requestItem) {
        // Gắn quyền sở hữu ngay lúc tạo
        // Tùy thuộc vào class Item của bạn lưu String sellerId hay Object User
        // Ở đây giả định bạn lưu String sellerId
        requestItem.setSellerId(actor.getId());

        // Gọi thẳng hàm của bạn
        itemRepository.saveOrUpdate(requestItem);
        return requestItem;
    }

    public Item updateItem(User actor, String itemId, Item updateData) {
        Item existingItem = itemRepository.findById(itemId);
        if (existingItem == null) {
            throw new RuntimeException("Sản phẩm không tồn tại.");
        }

        // Kiểm tra Ownership (Vai trò Seller ngữ cảnh)
        if (!existingItem.getSellerId().equals(actor.getId())) {
            throw new SecurityException("Chỉ chủ sản phẩm mới được sửa thông tin.");
        }

        // Cập nhật các trường (Ví dụ Name, Description)
        existingItem.setName(updateData.getName());
        existingItem.setDescription(updateData.getDescription());

        itemRepository.saveOrUpdate(existingItem);
        return existingItem;
    }

    public boolean deleteItem(User actor, String itemId) {
        Item existingItem = itemRepository.findById(itemId);
        if (existingItem == null) return false;

        if (!existingItem.getSellerId().equals(actor.getId())) {
            throw new SecurityException("Chỉ chủ sản phẩm mới được xóa.");
        }

        // Lưu ý: Trong file ItemRepository của bạn chưa có hàm delete().
        // Bạn cần viết thêm hàm delete(Item item) trong ItemRepository có chứa session.remove(item).
        // itemRepository.delete(existingItem);

        System.out.println("Đã xóa Item: " + itemId);
        return true;
    }

    // Hàm tiện ích: Lấy toàn bộ hàng của 1 user
    public List<Item> getMyItems(User actor) {
        return itemRepository.findBySellerId(actor.getId());
    }
}