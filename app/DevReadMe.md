## 连接数据库指南
记得在RetrofitClient.kt里使用自己的ip和端口，并python app.py运行后端
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

11/8 建立了user表
CREATE TABLE user (
uid INT AUTO_INCREMENT PRIMARY KEY,
name VARCHAR(100) NOT NULL,
email VARCHAR(255) NOT NULL UNIQUE,
password VARCHAR(255) NOT NULL,
gender ENUM('Male', 'Female', 'Other', 'Prefer not to say') DEFAULT 'Prefer not to say',
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


## 后端进度
history api基本完成，price api还没开始投入用