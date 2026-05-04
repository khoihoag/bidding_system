CREATE DATABASE IF NOT EXISTS bidding_system
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE bidding_system;

CREATE TABLE IF NOT EXISTS users (
    user_type VARCHAR(31) NOT NULL DEFAULT 'USER',
    id VARCHAR(255) NOT NULL,
    createdAt DATETIME(6) NULL,
    updatedAt DATETIME(6) NULL,
    username VARCHAR(255) NULL,
    email VARCHAR(255) NULL,
    passwordHash VARCHAR(255) NULL,
    fullName VARCHAR(255) NULL,
    role VARCHAR(255) NULL,
    isActive BOOLEAN NOT NULL DEFAULT TRUE,
    balance DOUBLE NOT NULL DEFAULT 0,
    totalRevenue DOUBLE NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS Item (
    item_type VARCHAR(31) NOT NULL,
    id VARCHAR(255) NOT NULL,
    createdAt DATETIME(6) NULL,
    updatedAt DATETIME(6) NULL,
    name VARCHAR(255) NULL,
    description VARCHAR(255) NULL,
    startingPrice DOUBLE NOT NULL DEFAULT 0,
    `condition` TINYINT NULL,
    seller_id VARCHAR(255) NOT NULL,
    artist VARCHAR(255) NULL,
    medium VARCHAR(255) NULL,
    yearCreated INT NULL,
    dimensions VARCHAR(255) NULL,
    brand VARCHAR(255) NULL,
    model VARCHAR(255) NULL,
    warrantyMonths INT NULL,
    powerWatts INT NULL,
    make VARCHAR(255) NULL,
    vehicle_year INT NULL,
    mileage INT NULL,
    fuelType VARCHAR(255) NULL,
    PRIMARY KEY (id),
    KEY idx_item_seller (seller_id),
    CONSTRAINT fk_item_seller FOREIGN KEY (seller_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS Item_images (
    Item_id VARCHAR(255) NOT NULL,
    images VARCHAR(255) NULL,
    KEY idx_item_images_item (Item_id),
    CONSTRAINT fk_item_images_item FOREIGN KEY (Item_id) REFERENCES Item (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS auctions (
    id VARCHAR(255) NOT NULL,
    createdAt DATETIME(6) NULL,
    updatedAt DATETIME(6) NULL,
    item_id VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    start_time DATETIME(6) NULL,
    end_time DATETIME(6) NULL,
    current_price DOUBLE NULL DEFAULT 0,
    winner_id VARCHAR(255) NULL,
    anti_sniping_seconds INT NOT NULL DEFAULT 0,
    extension_seconds INT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_auctions_item (item_id),
    KEY idx_auctions_winner (winner_id),
    CONSTRAINT fk_auctions_item FOREIGN KEY (item_id) REFERENCES Item (id),
    CONSTRAINT fk_auctions_winner FOREIGN KEY (winner_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS bidding_transactions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    bidder_id VARCHAR(255) NOT NULL,
    auction_id VARCHAR(255) NOT NULL,
    bid_amount DOUBLE NOT NULL,
    bid_time DATETIME(6) NOT NULL,
    status VARCHAR(255) NOT NULL,
    is_auto_bid BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    KEY idx_bid_transactions_bidder (bidder_id),
    KEY idx_bid_transactions_auction (auction_id),
    CONSTRAINT fk_bid_transactions_bidder FOREIGN KEY (bidder_id) REFERENCES users (id),
    CONSTRAINT fk_bid_transactions_auction FOREIGN KEY (auction_id) REFERENCES auctions (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO users (
    user_type, id, createdAt, updatedAt, username, email, passwordHash, fullName, role, isActive, balance, totalRevenue
) VALUES
    ('USER', 'admin-demo', NOW(6), NOW(6), 'admin', 'admin@bidviet.local', 'admin123', 'Quan tri vien', 'ADMIN', TRUE, 10000000, 0),
    ('USER', 'seller-demo', NOW(6), NOW(6), 'seller', 'seller@bidviet.local', 'seller123', 'Nguoi ban demo', 'USER', TRUE, 5000000, 0),
    ('USER', 'bidder-demo', NOW(6), NOW(6), 'bidder', 'bidder@bidviet.local', 'bidder123', 'Nguoi mua demo', 'USER', TRUE, 20000000, 0)
ON DUPLICATE KEY UPDATE
    updatedAt = VALUES(updatedAt),
    email = VALUES(email),
    passwordHash = VALUES(passwordHash),
    fullName = VALUES(fullName),
    role = VALUES(role),
    isActive = VALUES(isActive),
    balance = VALUES(balance);

INSERT INTO Item (
    item_type, id, createdAt, updatedAt, name, description, startingPrice, `condition`, seller_id,
    artist, medium, yearCreated, dimensions
) VALUES (
    'Art', 'item-art-demo', NOW(6), NOW(6), 'Tranh son dau demo',
    'Tranh son dau dung de test dau gia local.', 1500000, 2, 'seller-demo',
    'Hoa si demo', 'Son dau', 2024, '60 x 80 cm'
) ON DUPLICATE KEY UPDATE updatedAt = VALUES(updatedAt);

INSERT INTO Item (
    item_type, id, createdAt, updatedAt, name, description, startingPrice, `condition`, seller_id,
    brand, model, warrantyMonths, powerWatts
) VALUES (
    'Electronics', 'item-electronics-demo', NOW(6), NOW(6), 'Laptop demo',
    'Laptop mau de test chuc nang them va mo phien.', 8000000, 1, 'seller-demo',
    'DemoBrand', 'DB-15', 6, 65
) ON DUPLICATE KEY UPDATE updatedAt = VALUES(updatedAt);

INSERT INTO Item (
    item_type, id, createdAt, updatedAt, name, description, startingPrice, `condition`, seller_id,
    make, model, vehicle_year, mileage, fuelType
) VALUES (
    'Vehicle', 'item-vehicle-demo', NOW(6), NOW(6), 'Xe may demo',
    'Xe may mau cho du lieu dau gia.', 12000000, 1, 'seller-demo',
    'Honda', 'Wave', 2020, 18000, 'Xang'
) ON DUPLICATE KEY UPDATE updatedAt = VALUES(updatedAt);
