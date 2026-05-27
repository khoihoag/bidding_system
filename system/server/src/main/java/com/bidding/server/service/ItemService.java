package com.bidding.server.service;

import com.bidding.server.enums.ItemApprovalStatus;
import com.bidding.server.model.item.Item;
import com.bidding.server.model.item.Art;
import com.bidding.server.model.item.Electronics;
import com.bidding.server.model.item.Vehicle;
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
        requestItem.setApprovalStatus(ItemApprovalStatus.PENDING);
        requestItem.setReviewedByAdminId(null);
        requestItem.setReviewedAt(null);
        requestItem.setRejectionReason(null);

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
        existingItem.setStartingPrice(updateData.getStartingPrice());
        existingItem.setBidStep(updateData.getBidStep());
        existingItem.setCondition(updateData.getCondition());
        existingItem.setImages(updateData.getImages());

        if (!existingItem.getClass().equals(updateData.getClass())) {
            throw new IllegalArgumentException("Không được đổi loại sản phẩm khi chỉnh sửa.");
        }

        if (existingItem instanceof Art existingArt && updateData instanceof Art updateArt) {
            existingArt.setArtist(updateArt.getArtist());
            existingArt.setMedium(updateArt.getMedium());
            existingArt.setYearCreated(updateArt.getYearCreated());
            existingArt.setDimensions(updateArt.getDimensions());
        } else if (existingItem instanceof Vehicle existingVehicle && updateData instanceof Vehicle updateVehicle) {
            existingVehicle.setMake(updateVehicle.getMake());
            existingVehicle.setModel(updateVehicle.getModel());
            existingVehicle.setYear(updateVehicle.getYear());
            existingVehicle.setMileage(updateVehicle.getMileage());
            existingVehicle.setFuelType(updateVehicle.getFuelType());
        } else if (existingItem instanceof Electronics existingElectronics && updateData instanceof Electronics updateElectronics) {
            existingElectronics.setBrand(updateElectronics.getBrand());
            existingElectronics.setModel(updateElectronics.getModel());
            existingElectronics.setWarrantyMonths(updateElectronics.getWarrantyMonths());
            existingElectronics.setPowerWatts(updateElectronics.getPowerWatts());
        }

        existingItem.setApprovalStatus(ItemApprovalStatus.PENDING);
        existingItem.setReviewedByAdminId(null);
        existingItem.setReviewedAt(null);
        existingItem.setRejectionReason(null);

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
    public long countItemsBySellerId(String sellerId) {
        if (sellerId == null || sellerId.isEmpty()) {
            return 0;
        }
        return itemRepository.countBySellerId(sellerId);
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
