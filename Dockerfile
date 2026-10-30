# 桌面小精灵 - 后端服务 Dockerfile
FROM eclipse-temurin:25-jdk

# 设置工作目录
WORKDIR /app

# 复制 Maven 依赖文件
COPY backend/aichat/pom.xml .

# 安装 Maven 依赖（利用 Docker 缓存）
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*
RUN mvn dependency:go-offline -q

# 复制源代码
COPY backend/aichat/src ./src

# 编译项目
RUN mvn package -DskipTests -q

# 设置环境变量
ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENV SPRING_PROFILES_ACTIVE=docker

# 暴露端口
EXPOSE 8080

# 启动命令
CMD ["java", "-jar", "target/aichat-0.0.1-SNAPSHOT.jar"]
