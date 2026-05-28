package com.bidding.server.model.network;

import com.bidding.server.enums.AuctionStatus;
import com.bidding.server.enums.UserRole;
import com.bidding.server.events.AuctionEvent;
import com.bidding.server.model.user.User;
import com.bidding.server.network.ClientHandler;
import com.bidding.server.network.ClientManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientManagerTest {

    private ClientManager clientManager;

    @Mock private ClientHandler mockClient1;
    @Mock private ClientHandler mockClient2;

    @Mock private AuctionEvent mockEvent;
    @Mock private com.bidding.server.model.auction.Auction mockAuction;
    @Mock private com.bidding.server.model.item.Item mockItem;
    @Mock private com.bidding.server.model.user.User mockWinner;

    @BeforeEach
    void setUp() {
        clientManager = new ClientManager();
        clientManager.addClient(mockClient1);
        clientManager.addClient(mockClient2);
    }

    private User user(String id, String username) {
        return new User(id, username, username + "@mail.test", "pass", username, UserRole.USER);
    }

    @Test
    @DisplayName("onAuctionStarted sends notifications to known logged-in users")
    void testOnAuctionStarted() {
        when(mockEvent.getAuction()).thenReturn(mockAuction);
        when(mockAuction.getItem()).thenReturn(mockItem);
        when(mockItem.getName()).thenReturn("Test Item");
        when(mockClient1.getLoggedInUser()).thenReturn(user("U1", "client1"));
        when(mockClient2.getLoggedInUser()).thenReturn(user("U2", "client2"));

        clientManager.onAuctionStarted(mockEvent);

        verify(mockClient1, times(1)).sendMessage(contains("\"action\":\"NEW_NOTIFICATION\""));
        verify(mockClient2, times(1)).sendMessage(contains("\"action\":\"NEW_NOTIFICATION\""));
    }

    @Test
    @DisplayName("onBidPlaced broadcasts new bid updates")
    void testOnBidPlaced() {
        when(mockEvent.getAuctionId()).thenReturn("A123");
        when(mockEvent.getNewPrice()).thenReturn(150.0);
        when(mockEvent.getAuction()).thenReturn(mockAuction);
        when(mockAuction.getFollowerIds()).thenReturn(new java.util.concurrent.CopyOnWriteArrayList<>());
        when(mockAuction.getCurrentWinner()).thenReturn(mockWinner);
        when(mockWinner.getUsername()).thenReturn("USER_1");

        clientManager.onBidPlaced(mockEvent);

        verify(mockClient1, times(1)).sendMessage(contains("\"action\":\"NEW_BID\""));
        verify(mockClient1, times(1)).sendMessage(contains("\"auctionId\":\"A123\""));
        verify(mockClient1, times(1)).sendMessage(contains("\"winnerId\":\"USER_1\""));
        verify(mockClient2, times(1)).sendMessage(contains("\"action\":\"NEW_BID\""));
    }

    @Test
    @DisplayName("onAuctionClosed broadcasts status and sends result details to relevant users")
    void testOnAuctionClosed() {
        when(mockEvent.getAuctionId()).thenReturn("A999");
        when(mockEvent.getNewPrice()).thenReturn(500.0);
        when(mockEvent.getAuction()).thenReturn(mockAuction);
        when(mockAuction.getItem()).thenReturn(mockItem);
        when(mockAuction.getStatus()).thenReturn(AuctionStatus.FINISHED);
        when(mockAuction.getFollowerIds()).thenReturn(new java.util.concurrent.CopyOnWriteArrayList<>(java.util.List.of("U1")));
        when(mockAuction.getCurrentWinner()).thenReturn(mockWinner);
        when(mockItem.getName()).thenReturn("Test Item");
        when(mockItem.getSellerId()).thenReturn("SELLER");
        when(mockWinner.getId()).thenReturn("WINNER");
        when(mockWinner.getUsername()).thenReturn("winner");
        when(mockWinner.getFullName()).thenReturn("Winner Name");
        when(mockClient1.getLoggedInUser()).thenReturn(user("U1", "client1"));
        when(mockClient2.getLoggedInUser()).thenReturn(user("U2", "client2"));

        clientManager.onAuctionClosed(mockEvent);

        verify(mockClient1, times(1)).sendMessage(argThat(message ->
                message.contains("\"action\":\"AUCTION_STATUS_CHANGED\"")
                        && message.contains("\"auctionId\":\"A999\"")
                        && message.contains("\"winnerId\":\"winner\"")
        ));
        verify(mockClient1, times(1)).sendMessage(argThat(message ->
                message.contains("\"action\":\"AUCTION_FINISHED\"")
                        && message.contains("\"auctionId\":\"A999\"")
                        && message.contains("\"winnerId\":\"winner\"")
        ));
        verify(mockClient2, times(1)).sendMessage(argThat(message ->
                message.contains("\"action\":\"AUCTION_STATUS_CHANGED\"")
                        && message.contains("\"auctionId\":\"A999\"")
        ));
        verify(mockClient2, never()).sendMessage(contains("\"action\":\"AUCTION_FINISHED\""));
    }

    @Test
    @DisplayName("removeClient stops removed clients from receiving broadcasts")
    void testRemoveClient() {
        clientManager.removeClient(mockClient1);

        clientManager.broadcast("System message");

        verify(mockClient1, never()).sendMessage(anyString());
        verify(mockClient2, times(1)).sendMessage("System message");
    }
}
