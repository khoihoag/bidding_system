package com.bidding.server.network;

import com.bidding.server.repository.ItemRepository;
import com.bidding.server.repository.UserRepository;
import com.bidding.server.service.AdminService;
import com.bidding.server.service.AuctionService;
import com.bidding.server.service.ItemService;
import com.bidding.server.service.UserService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerMain {
    private static final int PORT = 8080;

    public static void main(String[] args) {
        System.out.println("--- STARTING BIDDING SERVER ---");

        ServerDependencies dependencies;
        try {
            dependencies = initializeDependencies();
        } catch (IllegalStateException e) {
            System.err.println("Cannot start server: " + e.getMessage());
            return;
        }

        ExecutorService pool = Executors.newCachedThreadPool();
        dependencies.auctionService().setUserService(dependencies.userService());

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server listening on port " + PORT + ". Waiting for clients...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("\n[+] Client connected: " + clientSocket.getInetAddress());

                ClientHandler clientHandler = new ClientHandler(
                        clientSocket,
                        dependencies.auctionService(),
                        dependencies.clientManager(),
                        dependencies.userService(),
                        dependencies.itemService(),
                        dependencies.adminService());

                dependencies.clientManager().addClient(clientHandler);
                pool.execute(clientHandler);
            }
        } catch (IOException e) {
            System.err.println("Port " + PORT + " is unavailable: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static ServerDependencies initializeDependencies() {
        AuctionService auctionService = new AuctionService();
        auctionService.loadAllAuctions();

        ClientManager clientManager = new ClientManager();
        UserRepository userRepository = new UserRepository();
        ItemRepository itemRepository = new ItemRepository();
        AdminService adminService = new AdminService(userRepository, auctionService, itemRepository);
        UserService userService = new UserService();
        ItemService itemService = new ItemService(itemRepository);

        auctionService.addObserver(clientManager);

        return new ServerDependencies(
                auctionService,
                clientManager,
                userService,
                itemService,
                adminService);
    }

    private record ServerDependencies(
            AuctionService auctionService,
            ClientManager clientManager,
            UserService userService,
            ItemService itemService,
            AdminService adminService) {
    }
}
