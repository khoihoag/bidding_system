package com.bidding.server.model.transaction;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.bidding.server.model.auction.AuctionMapper;
@Mapper(uses = {AuctionMapper.class}) // <--- CHIÊU NÀY QUAN TRỌNG
public interface BiddingTransactionMapper {
    BiddingTransactionMapper INSTANCE = Mappers.getMapper(BiddingTransactionMapper.class);

    // Bây giờ thì ĐÉO CẦN @Mapping loằng ngoằng nữa
    // Vì tên thuộc tính cùng là 'auction' (nhưng khác kiểu dữ liệu), 
    // MapStruct sẽ tự dùng AuctionMapper để chuyển đổi.
    BiddingTransactionEntity toEntity(BiddingTransaction model);

    BiddingTransaction toModel(BiddingTransactionEntity entity);
}
