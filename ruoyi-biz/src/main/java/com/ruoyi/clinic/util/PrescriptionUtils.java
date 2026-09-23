package com.ruoyi.clinic.util;

import com.ruoyi.common.exception.ServiceException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 处方解析工具类 — 消除重复的 quantity 类型转换逻辑
 */
public final class PrescriptionUtils {

    private PrescriptionUtils() {
    }

    /**
     * 安全解析处方药品数量（兼容整型数值和整数字符串）
     *
     * @param qtyObj 原始数量对象（来自 JSONB Map）
     * @return 解析后的整型数量
     * @throws ServiceException 当数量缺失、非正数或不是整数时
     */
    public static int parseQuantity(Object qtyObj) {
        if (qtyObj == null) {
            throw new ServiceException("处方药品数量不能为空");
        }
        try {
            BigDecimal quantity = qtyObj instanceof BigDecimal
                    ? (BigDecimal) qtyObj
                    : new BigDecimal(qtyObj.toString());
            int parsed = quantity.setScale(0, RoundingMode.UNNECESSARY).intValueExact();
            if (parsed <= 0) {
                throw new ServiceException("处方药品数量必须大于0");
            }
            return parsed;
        } catch (NumberFormatException | ArithmeticException e) {
            throw new ServiceException("处方药品数量格式非法: " + qtyObj);
        }
    }

    public static String requireItemCode(Object itemCode) {
        if (!(itemCode instanceof String) || ((String) itemCode).trim().isEmpty()) {
            throw new ServiceException("处方药品编码不能为空");
        }
        return ((String) itemCode).trim();
    }
}
