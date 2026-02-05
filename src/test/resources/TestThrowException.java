package com.example.test;

/**
 * 用于测试 RULE-025（主动抛出的异常必须要填写详细的描述信息）的测试用例类。
 * 包含各种常见的主动抛出异常的方式。
 */
public class TestThrowException {

    // ==================== 1. 直接 throw new 的方式 ====================

    /**
     * 直接抛出带消息的 RuntimeException
     */
    public void throwWithMessage() {
        throw new RuntimeException("用户不存在，请检查用户ID是否正确");
    }

    /**
     * 抛出不带消息的异常（应该被检测出问题）
     */
    public void throwWithoutMessage() {
        throw new RuntimeException();
    }

    /**
     * 抛出带空字符串消息的异常
     */
    public void throwWithEmptyMessage() {
        throw new IllegalArgumentException("");
    }

    /**
     * 抛出 IllegalArgumentException 带详细消息
     */
    public void throwIllegalArgument(String param) {
        if (param == null) {
            throw new IllegalArgumentException("参数param不能为null，请传入有效的字符串值");
        }
    }

    /**
     * 抛出 IllegalStateException 带详细消息
     */
    public void throwIllegalState() {
        throw new IllegalStateException("当前状态不允许执行此操作，请先完成初始化");
    }

    /**
     * 抛出 NullPointerException 带消息
     */
    public void throwNullPointer(Object obj) {
        if (obj == null) {
            throw new NullPointerException("对象不能为空，请确保传入了有效的对象实例");
        }
    }

    // ==================== 2. 带 cause 的异常 ====================

    /**
     * 抛出带 cause 的异常
     */
    public void throwWithCause() {
        try {
            Integer.parseInt("abc");
        } catch (NumberFormatException e) {
            throw new RuntimeException("数字格式转换失败，输入值不是有效的数字格式", e);
        }
    }

    /**
     * 只有 cause 没有消息（应该被检测）
     */
    public void throwOnlyCause() {
        try {
            Integer.parseInt("abc");
        } catch (NumberFormatException e) {
            throw new RuntimeException(e);
        }
    }

    // ==================== 3. 变量方式抛出 ====================

    /**
     * 先创建异常变量再抛出，带消息
     */
    public void throwFromVariable() {
        RuntimeException exception = new RuntimeException("订单状态异常，无法进行支付操作");
        throw exception;
    }

    /**
     * 先创建异常变量再抛出，不带消息
     */
    public void throwFromVariableNoMessage() {
        RuntimeException exception = new RuntimeException();
        throw exception;
    }

    /**
     * 创建带 cause 的异常变量
     */
    public void throwFromVariableWithCause() {
        try {
            doSomethingRisky();
        } catch (Exception e) {
            IllegalStateException ex = new IllegalStateException("执行风险操作失败，原因详见嵌套异常", e);
            throw ex;
        }
    }

    // ==================== 4. 业务异常 ====================

    /**
     * 抛出自定义业务异常（假设）
     */
    public void throwBusinessException(String userId) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("用户ID不能为空，请提供有效的用户标识符");
        }
        // 模拟业务逻辑
        throw new RuntimeException("用户[" + userId + "]余额不足，无法完成扣款操作");
    }

    // ==================== 5. 条件判断中抛出 ====================

    /**
     * 在 if 条件中抛出
     */
    public void throwInIfCondition(int age) {
        if (age < 0) {
            throw new IllegalArgumentException("年龄不能为负数，当前值：" + age);
        }
        if (age > 150) {
            throw new IllegalArgumentException("年龄值异常，超出合理范围(0-150)，当前值：" + age);
        }
    }

    /**
     * 在 switch 中抛出
     */
    public void throwInSwitch(String type) {
        switch (type) {
            case "A":
                // do something
                break;
            case "B":
                // do something
                break;
            default:
                throw new IllegalArgumentException("不支持的类型：" + type + "，仅支持A或B类型");
        }
    }

    // ==================== 6. 链式/嵌套调用 ====================

    /**
     * 异常消息使用字符串拼接
     */
    public void throwWithStringConcat(String orderId, String status) {
        throw new RuntimeException("订单[" + orderId + "]状态[" + status + "]不允许此操作");
    }

    /**
     * 使用 String.format 构建消息
     */
    public void throwWithStringFormat(String name, int count) {
        throw new RuntimeException(String.format("用户%s操作次数%d超过限制", name, count));
    }

    // ==================== 7. check 异常 ====================

    /**
     * 抛出受检异常 IOException
     */
    public void throwCheckedException() throws Exception {
        throw new Exception("文件读取失败，请检查文件路径是否正确");
    }

    /**
     * 抛出自定义受检异常（模拟）
     */
    public void throwCustomCheckedException() throws Exception {
        throw new Exception("用户认证失败，token已过期或无效");
    }

    // ==================== 8. rethrow 场景 ====================

    /**
     * 捕获后直接重新抛出
     */
    public void rethrowException() {
        try {
            riskyOperation();
        } catch (RuntimeException e) {
            throw e; // 直接重抛
        }
    }

    /**
     * 捕获后包装成新异常重新抛出
     */
    public void wrapAndRethrow() {
        try {
            riskyOperation();
        } catch (RuntimeException e) {
            throw new RuntimeException("包装异常：执行风险操作时发生错误", e);
        }
    }

    // ==================== 9. 工具方法中抛出 ====================

    /**
     * 参数校验工具方法
     */
    public void validateNotNull(Object obj, String paramName) {
        if (obj == null) {
            throw new IllegalArgumentException("参数[" + paramName + "]不能为null");
        }
    }

    /**
     * 范围校验
     */
    public void validateRange(int value, int min, int max) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(
                    "参数值" + value + "不在有效范围[" + min + ", " + max + "]内");
        }
    }

    // ==================== 10. lambda/流中抛出 ====================

    /**
     * 在 lambda 中抛出异常
     */
    public void throwInLambda() {
        Runnable task = () -> {
            throw new RuntimeException("异步任务执行失败，请检查任务配置");
        };
        task.run();
    }

    // ==================== 辅助方法 ====================

    private void doSomethingRisky() throws Exception {
        throw new Exception("模拟风险操作异常");
    }

    private void riskyOperation() {
        throw new RuntimeException("风险操作失败");
    }
}
