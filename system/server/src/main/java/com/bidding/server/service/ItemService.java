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
        requestItem.setSellerId(actor.getId());
        requestItem.setSellerFullName(actor.getFullName());

        itemRepository.saveOrUpdate(requestItem);
        return requestItem;
    }

    public Item updateItem(User actor, String itemId, Item updateData) {
        Item existingItem = itemRepository.findById(itemId);
        if (existingItem == null) {
            throw new RuntimeException("Sản phẩm không tồn tại.");
        }

        if (!actor.getId().equals(existingItem.getSellerId())) {
            throw new SecurityException("Chỉ chủ sản phẩm mới được sửa thông tin.");
        }

        existingItem.setName(updateData.getName());
        existingItem.setDescription(updateData.getDescription());

        itemRepository.saveOrUpdate(existingItem);
        return existingItem;
    }

    public boolean deleteItem(User actor, String itemId) {
        Item existingItem = itemRepository.findById(itemId);
        if (existingItem == null) return false;

        if (!actor.getId().equals(existingItem.getSellerId())) {
            throw new SecurityException("Chỉ chủ sản phẩm mới được xóa.");
        }


        itemRepository.delete(existingItem);

        System.out.println("Đã xóa Item: " + itemId);
        return true;
    }

    public List<Item> getMyItems(User actor) {
        // Hàm này ở bước trước anh em mình vừa thêm vào ItemRepository
        return itemRepository.findBySellerId(actor.getId());
    }
    public Item findById(String itemId) {
        // Chỉ đơn giản là gọi xuống Repo để lấy đồ
        return itemRepository.findById(itemId);
    }

    // Trong ItemService.java
    public List<Item> getWonItems(User actor) {
        return itemRepository.findWonItemsByUserId(actor.getId());
    }
    // Trong ItemService.java
    public void changeItemOwner(String itemId, User newOwner) {
        Item item = itemRepository.findById(itemId);
        if (item != null) {
            item.setSellerId(newOwner.getId());
            item.setSellerFullName(newOwner.getFullName());
            itemRepository.saveOrUpdate(item);
        }
    }
}
