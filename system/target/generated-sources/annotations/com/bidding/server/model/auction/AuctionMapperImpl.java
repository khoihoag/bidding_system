package com.bidding.server.model.auction;

import com.bidding.server.model.transaction.BiddingTransaction;
import com.bidding.server.model.transaction.BiddingTransactionEntity;
import com.bidding.server.model.transaction.BiddingTransactionMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.annotation.processing.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-12T12:35:00+0700",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.9 (Oracle Corporation)"
)
public class AuctionMapperImpl implements AuctionMapper {

    private final BiddingTransactionMapper biddingTransactionMapper = BiddingTransactionMapper.INSTANCE;

    @Override
    public void updateEntityFromModel(Auction model, AuctionEntity entity) {
        if ( model == null ) {
            return;
        }

        entity.setCurrentPrice( unwrapAtomic( model.getCurrentPrice() ) );
        entity.setReverse( model.isReverse() );
        entity.setDropStep( model.getDropStep() );
        entity.setId( model.getId() );
        entity.setCreatedAt( model.getCreatedAt() );
        entity.setUpdatedAt( model.getUpdatedAt() );
        entity.setItem( model.getItem() );
        entity.setStatus( model.getStatus() );
        entity.setStartTime( model.getStartTime() );
        entity.setEndTime( model.getEndTime() );
        entity.setCurrentWinner( model.getCurrentWinner() );
        entity.setAntiSnipingSeconds( model.getAntiSnipingSeconds() );
        entity.setExtensionSeconds( model.getExtensionSeconds() );
    }

    @Override
    public AuctionEntity toEntity(Auction auctionModel) {
        if ( auctionModel == null ) {
            return null;
        }

        AuctionEntity auctionEntity = new AuctionEntity();

        auctionEntity.setCurrentPrice( unwrapAtomic( auctionModel.getCurrentPrice() ) );
        auctionEntity.setReverse( auctionModel.isReverse() );
        auctionEntity.setDropStep( auctionModel.getDropStep() );
        auctionEntity.setId( auctionModel.getId() );
        auctionEntity.setCreatedAt( auctionModel.getCreatedAt() );
        auctionEntity.setUpdatedAt( auctionModel.getUpdatedAt() );
        auctionEntity.setItem( auctionModel.getItem() );
        auctionEntity.setStatus( auctionModel.getStatus() );
        auctionEntity.setStartTime( auctionModel.getStartTime() );
        auctionEntity.setEndTime( auctionModel.getEndTime() );
        auctionEntity.setCurrentWinner( auctionModel.getCurrentWinner() );
        auctionEntity.setAntiSnipingSeconds( auctionModel.getAntiSnipingSeconds() );
        auctionEntity.setExtensionSeconds( auctionModel.getExtensionSeconds() );

        return auctionEntity;
    }

    @Override
    public Auction toModel(AuctionEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Auction auction = new Auction();

        auction.setCurrentPrice( wrapAtomic( entity.getCurrentPrice() ) );
        auction.setBidHistory( biddingTransactionEntityListToBiddingTransactionCopyOnWriteArrayList( entity.getTransactions() ) );
        auction.setReverse( entity.isReverse() );
        auction.setDropStep( entity.getDropStep() );
        auction.setId( entity.getId() );
        auction.setCreatedAt( entity.getCreatedAt() );
        auction.setUpdatedAt( entity.getUpdatedAt() );
        auction.setItem( entity.getItem() );
        auction.setStatus( entity.getStatus() );
        auction.setStartTime( entity.getStartTime() );
        auction.setEndTime( entity.getEndTime() );
        auction.setCurrentWinner( entity.getCurrentWinner() );
        auction.setAntiSnipingSeconds( entity.getAntiSnipingSeconds() );
        auction.setExtensionSeconds( entity.getExtensionSeconds() );

        return auction;
    }

    @Override
    public List<AuctionEntity> toEntityList(List<Auction> models) {
        if ( models == null ) {
            return null;
        }

        List<AuctionEntity> list = new ArrayList<AuctionEntity>( models.size() );
        for ( Auction auction : models ) {
            list.add( toEntity( auction ) );
        }

        return list;
    }

    @Override
    public List<Auction> toModelList(List<AuctionEntity> entities) {
        if ( entities == null ) {
            return null;
        }

        List<Auction> list = new ArrayList<Auction>( entities.size() );
        for ( AuctionEntity auctionEntity : entities ) {
            list.add( toModel( auctionEntity ) );
        }

        return list;
    }

    protected CopyOnWriteArrayList<BiddingTransaction> biddingTransactionEntityListToBiddingTransactionCopyOnWriteArrayList(List<BiddingTransactionEntity> list) {
        if ( list == null ) {
            return null;
        }

        CopyOnWriteArrayList<BiddingTransaction> copyOnWriteArrayList = new CopyOnWriteArrayList<BiddingTransaction>();
        for ( BiddingTransactionEntity biddingTransactionEntity : list ) {
            copyOnWriteArrayList.add( biddingTransactionMapper.toModel( biddingTransactionEntity ) );
        }

        return copyOnWriteArrayList;
    }
}
