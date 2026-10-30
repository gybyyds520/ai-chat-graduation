package com.example.aichat.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 分布式算力调度服务
 * 
 * 功能：
 * 1. 算力节点注册与管理
 * 2. 任务分发与负载均衡
 * 3. 计算结果聚合
 * 4. 故障检测与转移
 * 5. 资源监控
 */
@Service
public class DistributedComputingService {

    private static final Logger logger = LoggerFactory.getLogger(DistributedComputingService.class);

    // 在线节点列表
    private final Map<String, ComputeNode> activeNodes = new ConcurrentHashMap<>();
    
    // 任务队列（线程安全：ConcurrentLinkedQueue 支持无锁并发入队/出队）
    private final Queue<ComputeTask> taskQueue = new ConcurrentLinkedQueue<>();

    // 队列容量上限：超过后拒绝新任务（B3 线程安全修复）
    private static final int MAX_QUEUE_CAPACITY = 1000;

    // 任务结果缓存
    private final Map<String, TaskResult> taskResults = new ConcurrentHashMap<>();
    
    // 当前任务ID计数器
    private static final AtomicInteger taskIdCounter = new AtomicInteger(0);

    /**
     * 出队操作的锁：getNextTask() 需要"扫描全队列找最高优先级 + 移除"两步复合操作，
     * ConcurrentLinkedQueue 单个 offer/poll 是线程安全的，但复合扫描删除需要加锁保证原子性。
     */
    private final Object dequeueLock = new Object();

    /**
     * 计算节点信息
     */
    public static class ComputeNode {
        private String nodeId;
        private String address;
        private int port;
        private int cpuCores;
        private long memoryMB;
        private long availableMemoryMB;
        private double cpuLoad;
        private NodeStatus status;
        private long lastHeartbeat;
        private List<String> capabilities;

        public enum NodeStatus {
            ACTIVE,      // 活跃
            BUSY,        // 忙碌
            IDLE,        // 空闲
            OFFLINE,     // 离线
            MAINTENANCE  // 维护中
        }

        // 构造函数、getter和setter
        public ComputeNode(String nodeId, String address, int port) {
            this.nodeId = nodeId;
            this.address = address;
            this.port = port;
            this.status = NodeStatus.ACTIVE;
            this.lastHeartbeat = System.currentTimeMillis();
            this.capabilities = new ArrayList<>();
        }

        // Getters and Setters
        public String getNodeId() { return nodeId; }
        public void setNodeId(String nodeId) { this.nodeId = nodeId; }
        
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        
        public int getCpuCores() { return cpuCores; }
        public void setCpuCores(int cpuCores) { this.cpuCores = cpuCores; }
        
        public long getMemoryMB() { return memoryMB; }
        public void setMemoryMB(long memoryMB) { this.memoryMB = memoryMB; }
        
        public long getAvailableMemoryMB() { return availableMemoryMB; }
        public void setAvailableMemoryMB(long availableMemoryMB) { this.availableMemoryMB = availableMemoryMB; }
        
        public double getCpuLoad() { return cpuLoad; }
        public void setCpuLoad(double cpuLoad) { this.cpuLoad = cpuLoad; }
        
        public NodeStatus getStatus() { return status; }
        public void setStatus(NodeStatus status) { this.status = status; }
        
        public long getLastHeartbeat() { return lastHeartbeat; }
        public void setLastHeartbeat(long lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }
        
        public List<String> getCapabilities() { return capabilities; }
        public void setCapabilities(List<String> capabilities) { this.capabilities = capabilities; }

        /**
         * 计算节点负载分数（越低越好）
         */
        public double getLoadScore() {
            return (cpuLoad * 0.6) + ((double) availableMemoryMB / memoryMB * 0.4);
        }

        /**
         * 检查节点是否可用
         */
        public boolean isAvailable() {
            return status == NodeStatus.ACTIVE || status == NodeStatus.IDLE;
        }

        /**
         * 更新心跳
         */
        public void updateHeartbeat() {
            this.lastHeartbeat = System.currentTimeMillis();
        }
    }

    /**
     * 计算任务
     */
    public static class ComputeTask {
        private String taskId;
        private String taskType;
        private Map<String, Object> inputData;
        private TaskPriority priority;
        private long submitTime;
        private String assignedNodeId;
        private TaskStatus status;

        public enum TaskPriority {
            LOW, NORMAL, HIGH, URGENT
        }

        public enum TaskStatus {
            QUEUED, RUNNING, COMPLETED, FAILED, CANCELLED
        }

        public ComputeTask(String taskType, Map<String, Object> inputData, TaskPriority priority) {
            this.taskId = "task-" + taskIdCounter.incrementAndGet();
            this.taskType = taskType;
            this.inputData = inputData;
            this.priority = priority;
            this.submitTime = System.currentTimeMillis();
            this.status = TaskStatus.QUEUED;
        }

        // Getters and Setters
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        
        public String getTaskType() { return taskType; }
        public void setTaskType(String taskType) { this.taskType = taskType; }
        
        public Map<String, Object> getInputData() { return inputData; }
        public void setInputData(Map<String, Object> inputData) { this.inputData = inputData; }
        
        public TaskPriority getPriority() { return priority; }
        public void setPriority(TaskPriority priority) { this.priority = priority; }
        
        public long getSubmitTime() { return submitTime; }
        public void setSubmitTime(long submitTime) { this.submitTime = submitTime; }
        
        public String getAssignedNodeId() { return assignedNodeId; }
        public void setAssignedNodeId(String assignedNodeId) { this.assignedNodeId = assignedNodeId; }
        
        public TaskStatus getStatus() { return status; }
        public void setStatus(TaskStatus status) { this.status = status; }
    }

    /**
     * 任务结果
     */
    public static class TaskResult {
        private String taskId;
        private boolean success;
        private Map<String, Object> resultData;
        private String errorMessage;
        private long startTime;
        private long endTime;
        private String executedBy;

        public TaskResult(String taskId) {
            this.taskId = taskId;
            this.startTime = System.currentTimeMillis();
        }

        // Getters and Setters
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public Map<String, Object> getResultData() { return resultData; }
        public void setResultData(Map<String, Object> resultData) { this.resultData = resultData; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public long getStartTime() { return startTime; }
        public void setStartTime(long startTime) { this.startTime = startTime; }
        
        public long getEndTime() { return endTime; }
        public void setEndTime(long endTime) { this.endTime = endTime; }
        
        public String getExecutedBy() { return executedBy; }
        public void setExecutedBy(String executedBy) { this.executedBy = executedBy; }

        public long getDuration() {
            return endTime - startTime;
        }
    }

    /**
     * 注册新的计算节点
     */
    public String registerNode(String address, int port) {
        String nodeId = "node-" + UUID.randomUUID().toString().substring(0, 8);
        
        ComputeNode node = new ComputeNode(nodeId, address, port);
        activeNodes.put(nodeId, node);
        
        logger.info("新节点注册: {} ({}:{})", nodeId, address, port);
        
        return nodeId;
    }

    /**
     * 更新节点心跳
     */
    public void updateNodeHeartbeat(String nodeId, int cpuCores, long memoryMB, 
                                   long availableMemoryMB, double cpuLoad) {
        ComputeNode node = activeNodes.get(nodeId);
        if (node != null) {
            node.setCpuCores(cpuCores);
            node.setMemoryMB(memoryMB);
            node.setAvailableMemoryMB(availableMemoryMB);
            node.setCpuLoad(cpuLoad);
            node.updateHeartbeat();
            
            // 根据负载更新状态
            if (cpuLoad > 0.8) {
                node.setStatus(ComputeNode.NodeStatus.BUSY);
            } else if (cpuLoad < 0.3) {
                node.setStatus(ComputeNode.NodeStatus.IDLE);
            } else {
                node.setStatus(ComputeNode.NodeStatus.ACTIVE);
            }
        }
    }

    /**
     * 提交计算任务。
     * 队列已满时拒绝提交，抛出 IllegalStateException（B3：拒绝策略）。
     */
    public String submitTask(String taskType, Map<String, Object> inputData, 
                           ComputeTask.TaskPriority priority) {
        ComputeTask task = new ComputeTask(taskType, inputData, priority);
        // 容量上限 + 拒绝策略：队列满则拒绝新任务
        if (taskQueue.size() >= MAX_QUEUE_CAPACITY) {
            logger.warn("任务队列已满({}/{})，拒绝新任务: {}", taskQueue.size(), MAX_QUEUE_CAPACITY, task.getTaskId());
            throw new IllegalStateException("任务队列已满，稍后再试");
        }
        taskQueue.offer(task);
        
        logger.info("任务已提交: {} (类型: {}, 优先级: {})", 
                    task.getTaskId(), taskType, priority);
        
        return task.getTaskId();
    }

    /**
     * 获取下一个待执行任务（按优先级取最高）。
     * 复合扫描+删除操作加锁保证原子性，避免并发出队丢失任务。
     */
    public ComputeTask getNextTask() {
        synchronized (dequeueLock) {
            // 按优先级排序取出任务
            ComputeTask highestPriority = null;
            
            for (ComputeTask task : taskQueue) {
                if (highestPriority == null || 
                    task.getPriority().ordinal() > highestPriority.getPriority().ordinal()) {
                    highestPriority = task;
                }
            }
            
            if (highestPriority != null) {
                taskQueue.remove(highestPriority);
                highestPriority.setStatus(ComputeTask.TaskStatus.RUNNING);
            }
            
            return highestPriority;
        }
    }

    /**
     * 选择最优节点执行任务
     */
    public String selectBestNode(String taskType) {
        ComputeNode bestNode = null;
        double bestScore = Double.MAX_VALUE;

        for (ComputeNode node : activeNodes.values()) {
            if (!node.isAvailable()) continue;
            
            // 检查节点是否支持该任务类型
            if (!node.getCapabilities().isEmpty() && 
                !node.getCapabilities().contains(taskType)) {
                continue;
            }

            double score = node.getLoadScore();
            if (score < bestScore) {
                bestScore = score;
                bestNode = node;
            }
        }

        if (bestNode != null) {
            logger.info("选择节点 {} 执行任务 (负载分数: {})", 
                       bestNode.getNodeId(), bestScore);
            return bestNode.getNodeId();
        }

        return null;
    }

    /**
     * 保存任务结果
     */
    public void saveTaskResult(TaskResult result) {
        result.setEndTime(System.currentTimeMillis());
        taskResults.put(result.getTaskId(), result);
        
        logger.info("任务完成: {} (执行节点: {}, 耗时: {}ms)", 
                   result.getTaskId(), result.getExecutedBy(), result.getDuration());
    }

    /**
     * 获取任务结果
     */
    public TaskResult getTaskResult(String taskId) {
        return taskResults.get(taskId);
    }

    /**
     * 获取系统统计信息
     */
    public Map<String, Object> getSystemStats() {
        Map<String, Object> stats = new HashMap<>();

        // 节点统计
        stats.put("totalNodes", activeNodes.size());
        stats.put("activeNodes", activeNodes.values().stream()
            .filter(ComputeNode::isAvailable).count());

        // 任务统计
        stats.put("queuedTasks", taskQueue.size());
        stats.put("completedTasks", taskResults.values().stream()
            .filter(TaskResult::isSuccess).count());
        stats.put("failedTasks", taskResults.values().stream()
            .filter(r -> !r.isSuccess()).count());

        // 平均执行时间
        double avgTime = taskResults.values().stream()
            .filter(TaskResult::isSuccess)
            .mapToLong(TaskResult::getDuration)
            .average()
            .orElse(0);
        stats.put("avgExecutionTime", avgTime);

        return stats;
    }

    /**
     * 健康检查 - 检测离线节点
     */
    public void checkNodeHealth() {
        long now = System.currentTimeMillis();
        long timeout = 60000; // 60秒超时

        for (Map.Entry<String, ComputeNode> entry : activeNodes.entrySet()) {
            ComputeNode node = entry.getValue();
            if (now - node.getLastHeartbeat() > timeout) {
                node.setStatus(ComputeNode.NodeStatus.OFFLINE);
                logger.warn("节点 {} 已离线", node.getNodeId());
            }
        }
    }

    /**
     * 移除离线节点
     */
    public void removeOfflineNodes() {
        activeNodes.entrySet().removeIf(entry -> 
            entry.getValue().getStatus() == ComputeNode.NodeStatus.OFFLINE);
    }

    /**
     * 获取所有活跃节点
     */
    public List<ComputeNode> getActiveNodes() {
        return new ArrayList<>(activeNodes.values());
    }

    /**
     * 取消任务
     */
    public boolean cancelTask(String taskId) {
        synchronized (dequeueLock) {
            for (ComputeTask task : taskQueue) {
                if (task.getTaskId().equals(taskId)) {
                    task.setStatus(ComputeTask.TaskStatus.CANCELLED);
                    taskQueue.remove(task);
                    logger.info("任务已取消: {}", taskId);
                    return true;
                }
            }
        }
        return false;
    }
}
