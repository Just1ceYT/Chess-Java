-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Dec 27, 2024 at 12:03 PM
-- Server version: 10.4.28-MariaDB
-- PHP Version: 8.2.4

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `chess_java`
--

-- --------------------------------------------------------

--
-- Table structure for table `game_data`
--

CREATE TABLE `game_data` (
  `id` int(11) NOT NULL,
  `black_score` int(11) NOT NULL,
  `white_score` int(11) NOT NULL,
  `stalemate_count` int(11) NOT NULL,
  `total_playtime` bigint(20) NOT NULL,
  `bgm_volume` float NOT NULL,
  `sfx_volume` float NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `game_data`
--

INSERT INTO `game_data` (`id`, `black_score`, `white_score`, `stalemate_count`, `total_playtime`, `bgm_volume`, `sfx_volume`) VALUES
(1, 0, 15, 0, 232965, 69, 93.6667);

-- --------------------------------------------------------

--
-- Table structure for table `game_history`
--

CREATE TABLE `game_history` (
  `historyID` int(11) NOT NULL,
  `game_date` datetime NOT NULL,
  `result` varchar(20) NOT NULL,
  `moves` text NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `game_history`
--

INSERT INTO `game_history` (`historyID`, `game_date`, `result`, `moves`) VALUES
(4, '2024-01-04 12:15:00', 'White Wins', '1. e4 e5 2. Nf3 Nc6 3. Bb5 a6 4. Ba4 Nf6 5. O-O Nxe4'),
(5, '2024-01-05 16:20:00', 'Black Wins', '1. d4 Nf6 2. c4 g6 3. Nc3 Bg7 4. e4 d6 5. Nf3 O-O'),
(6, '2024-01-06 09:50:00', 'White Wins', '1. e4 e5 2. Nf3 Nc6 3. Bb5 a6 4. Ba4 d6 5. c3 Nf6 6. d4 exd4'),
(7, '2024-01-07 19:05:00', 'Black Wins', '1. e4 e5 2. Nf3 Nc6 3. Bb5 a6 4. Ba4 Nf6 5. d3 d6 6. O-O Be7'),
(10, '2024-01-10 13:10:00', 'Black Wins', '1. e4 c5 2. Nf3 d6 3. Bb5+ Bd7 4. Bxd7+ Qxd7 5. O-O Nxe4'),
(11, '2024-01-11 17:55:00', 'Stalemate', '1. e4 c5 2. Nf3 d6 3. Bb5+ Bd7 4. Bxd7+ Qxd7 5. O-O d6'),
(12, '2024-01-12 08:40:00', 'White Wins', '1. e4 e5 2. Nf3 Nc6 3. Bb5 a6 4. Ba4 d6 5. c3 Nf6 6. d4 exd4'),
(13, '2024-01-13 10:50:00', 'Black Wins', '1. d4 Nf6 2. c4 e6 3. Nc3 Bb4 4. Bg5 h6 5. Bh4 dxc4'),
(15, '2024-01-15 14:00:00', 'Stalemate', '1. d4 Nf6 2. c4 g6 3. Nc3 Bg7 4. e4 d6 5. Nf3 O-O'),
(36, '2024-12-25 21:41:19', 'White Wins', '1. e4 f5 2. Bd3 g5 3. Nh3 a6 4. Kg1 c6 5. Qh5#'),
(37, '2024-12-25 21:44:32', 'White Wins', '1. e4 f5 2. Bd3 g5 3. Nh3 a6 4. O-O-O b6 5. Qh5#'),
(38, '2024-12-25 21:47:05', 'White Wins', '1. e4 f5 2. f4 g5 3. Bd3 a6 4. Nh3 b6 5.  c6 6. Qh5#'),
(39, '2024-12-25 21:48:34', 'White Wins', '1. e4 f5 2. Bd3 g5 3. Nh3 a5 4. O-O b5 5. Qh5#');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `game_data`
--
ALTER TABLE `game_data`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `game_history`
--
ALTER TABLE `game_history`
  ADD PRIMARY KEY (`historyID`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `game_history`
--
ALTER TABLE `game_history`
  MODIFY `historyID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=40;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
