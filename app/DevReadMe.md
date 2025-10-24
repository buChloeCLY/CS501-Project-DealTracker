## 连接数据库指南
平台：AWS-SQL
user="admin"
password="Test12345!"
database="dealtracker"
host="database-1.cjw0amswcib4.us-east-2.rds.amazonaws.com"
现在只有price表，可以插入数据，在detail页面显示
Table: price
Create Table: CREATE TABLE `price` (
`id` bigint NOT NULL AUTO_INCREMENT,
`pid` bigint DEFAULT NULL,
`price` double DEFAULT NULL,
`date` datetime DEFAULT NULL,
`platform` varchar(50) DEFAULT NULL,
`idInPlatform` varchar(100) DEFAULT NULL,
`link` varchar(500) DEFAULT NULL,
PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci

## 后端进度
history api基本完成，price api还没开始投入用