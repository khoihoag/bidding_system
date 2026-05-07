package com.bidding.server.model.transaction;

import com.bidding.server.model.auction.AuctionMapper;
import javax.annotation.processing.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-07T02:05:02+0700",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.9 (Oracle Corporation)"
)
public class BiddingTransactionMapperImpl implements BiddingTransactionMapper {

    private final AuctionMapper auctionMapper = AuctionMapper.INSTANCE;

    @Override
    public BiddingTransactionEntity toEntity(BiddingTransaction model) {
        if ( model == null ) {
            return null;
        }

        BiddingTransactionEntity biddingTransactionEntity = new BiddingTransactionEntity();

        biddingTransactionEntity.setId( model.getId() );
        biddingTransactionEntity.setBidder( model.getBidder() );
        biddingTransactionEntity.setAuction( auctionMapper.toEntity( model.getAuction() ) );
        biddingTransactionEntity.setBidAmount( model.getBidAmount() );
        biddingTransactionEntity.setBidTime( model.getBidTime() );
        biddingTransactionEntity.setStatus( model.getStatus() );
        biddingTransactionEntity.setAutoBid( model.isAutoBid() );
        biddingTransactionEntity.setExtended( model.isExtended() );

        return biddingTransactionEntity;
    }

    @Override
    public BiddingTransaction toModel(BiddingTransactionEntity entity) {
        if ( entity == null ) {
            return null;
        }

        BiddingTransaction biddingTransaction = new BiddingTransaction();

        biddingTransaction.setId( entity.getId() );
        biddingTransaction.setBidder( entity.getBidder() );
        biddingTransaction.setBidAmount( entity.getBidAmount() );
        biddingTransaction.setBidTime( entity.getBidTime() );
        biddingTransaction.setStatus( entity.getStatus() );
        biddingTransaction.setAutoBid( entity.isAutoBid() );
        biddingTransaction.setExtended( entity.isExtended() );

        return biddingTransaction;
    }
}
