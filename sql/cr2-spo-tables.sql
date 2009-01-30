-- MySQL dump 10.9
--
-- Host: localhost    Database: cr2
-- ------------------------------------------------------
-- Server version	4.1.22

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `spo`
--

DROP TABLE IF EXISTS `spo`;
CREATE TABLE `spo` (
  `SUBJECT` bigint(20) NOT NULL default '0',
  `PREDICATE` bigint(20) NOT NULL default '0',
  `OBJECT` text NOT NULL,
  `OBJECT_HASH` bigint(20) NOT NULL default '0',
  `ANON_SUBJ` enum('Y','N') NOT NULL default 'N',
  `ANON_OBJ` enum('Y','N') NOT NULL default 'N',
  `LIT_OBJ` enum('Y','N') NOT NULL default 'Y',
  `OBJ_DERIV_SOURCE` bigint(20) NOT NULL default '0',
  `OBJ_LANG` varchar(10) NOT NULL default '',
  `SOURCE` bigint(20) NOT NULL default '0',
  `GEN_TIME` bigint(20) NOT NULL default '0',
  PRIMARY KEY  (`SUBJECT`,`PREDICATE`,`OBJECT_HASH`,`SOURCE`,`GEN_TIME`),
  KEY `SUBJECT` (`SUBJECT`),
  KEY `PREDICATE` (`PREDICATE`),
  KEY `OBJECT_HASH` (`OBJECT_HASH`),
  KEY `SOURCE` (`SOURCE`),
  KEY `GEN_TIME` (`GEN_TIME`),
  FULLTEXT KEY `OBJECT` (`OBJECT`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

--
-- Table structure for table `spo_temp`
--

DROP TABLE IF EXISTS `spo_temp`;
CREATE TABLE `spo_temp` (
  `SUBJECT` bigint(20) NOT NULL default '0',
  `PREDICATE` bigint(20) NOT NULL default '0',
  `OBJECT` text NOT NULL,
  `OBJECT_HASH` bigint(20) NOT NULL default '0',
  `ANON_SUBJ` enum('Y','N') NOT NULL default 'N',
  `ANON_OBJ` enum('Y','N') NOT NULL default 'N',
  `LIT_OBJ` enum('Y','N') NOT NULL default 'Y',
  `OBJ_LANG` varchar(10) NOT NULL default ''
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

--
-- Table structure for table `resource`
--

DROP TABLE IF EXISTS `resource`;
CREATE TABLE `resource` (
  `URI` text NOT NULL,
  `URI_HASH` bigint(20) NOT NULL default '0',
  `FIRSTSEEN_SOURCE` bigint(20) NOT NULL default '0',
  `FIRSTSEEN_TIME` bigint(20) NOT NULL default '0',
  PRIMARY KEY  (`URI_HASH`),
  KEY `FIRSTSEEN_SOURCE` (`FIRSTSEEN_SOURCE`),
  KEY `FIRSTSEEN_TIME` (`FIRSTSEEN_TIME`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

--
-- Table structure for table `resource_temp`
--

DROP TABLE IF EXISTS `resource_temp`;
CREATE TABLE `resource_temp` (
  `URI` text NOT NULL,
  `URI_HASH` bigint(20) NOT NULL default '0',
  PRIMARY KEY  (`URI_HASH`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

