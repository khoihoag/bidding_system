package com.bidding.server.model.auction;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-02T16:29:58+0700",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 26.0.1 (Oracle Corporation)"
)
public class AuctionMapperImpl implements AuctionMapper {

    @Override
    public AuctionEntity toEntity(Auction auctionModel) {
        if ( auctionModel == null ) {
            return null;
        }

        AuctionEntity auctionEntity = new AuctionEntity();

        auctionEntity.setCurrentPrice( unwrapAtomic( auctionModel.getCurrentPrice() ) );
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
}
